#!/usr/bin/env python3
"""SOV static pre-release audit.

Scans the complete repository without third-party dependencies and writes
AUDIT_REPORT.md plus AUDIT_REPORT.json. Errors are release blockers; warnings
must be reviewed before production merge.
"""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from dataclasses import dataclass, asdict
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {".git", "node_modules", ".vercel", "dist", "build", "coverage"}
TEXT_EXTS = {".html", ".htm", ".js", ".mjs", ".css", ".json", ".xml", ".txt", ".md", ".webmanifest"}
IGNORED_SCHEMES = {"http", "https", "mailto", "tel", "data", "javascript", "blob", "about"}


@dataclass
class Issue:
    severity: str
    code: str
    file: str
    detail: str


issues: list[Issue] = []


def add(severity: str, code: str, file: Path | str, detail: str) -> None:
    try:
        rel = str(Path(file).resolve().relative_to(ROOT))
    except Exception:
        rel = str(file)
    issues.append(Issue(severity, code, rel.replace("\\", "/"), detail))


def iter_files(suffixes: set[str] | None = None):
    for p in ROOT.rglob("*"):
        if not p.is_file() or any(part in SKIP_DIRS for part in p.parts):
            continue
        if suffixes is None or p.suffix.lower() in suffixes:
            yield p


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="replace")


class PageParser(HTMLParser):
    URL_ATTRS = {"href", "src", "action", "poster", "data-src"}

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.ids: list[str] = []
        self.refs: list[tuple[str, str, int]] = []
        self.title_parts: list[str] = []
        self.in_title = False
        self.lang = ""
        self.viewport = False
        self.canonical = ""
        self.meta_refresh = ""
        self.img_missing_alt = 0
        self.blank_missing_rel = 0
        self.inline_scripts: list[str] = []
        self.in_inline_script = False
        self.script_buf: list[str] = []
        self.local_scripts: list[str] = []

    def handle_starttag(self, tag: str, attrs):
        attrs_d = {k.lower(): (v or "") for k, v in attrs}
        line = self.getpos()[0]
        if tag == "html":
            self.lang = attrs_d.get("lang", "").strip()
        if tag == "title":
            self.in_title = True
        if tag == "meta":
            name = attrs_d.get("name", "").lower()
            equiv = attrs_d.get("http-equiv", "").lower()
            if name == "viewport":
                self.viewport = True
            if equiv == "refresh":
                self.meta_refresh = attrs_d.get("content", "")
        if tag == "link" and attrs_d.get("rel", "").lower() == "canonical":
            self.canonical = attrs_d.get("href", "")
        if "id" in attrs_d and attrs_d["id"].strip():
            self.ids.append(attrs_d["id"].strip())
        if tag == "img" and "alt" not in attrs_d:
            self.img_missing_alt += 1
        if attrs_d.get("target", "").lower() == "_blank":
            rel_tokens = set(attrs_d.get("rel", "").lower().split())
            if not ({"noopener", "noreferrer"} & rel_tokens):
                self.blank_missing_rel += 1
        for attr in self.URL_ATTRS:
            if attrs_d.get(attr):
                self.refs.append((attr, attrs_d[attr].strip(), line))
        if attrs_d.get("srcset"):
            for item in attrs_d["srcset"].split(","):
                ref = item.strip().split()[0] if item.strip() else ""
                if ref:
                    self.refs.append(("srcset", ref, line))
        if tag == "script":
            src = attrs_d.get("src", "").strip()
            if src:
                self.refs.append(("src", src, line))
                self.local_scripts.append(src)
            else:
                typ = attrs_d.get("type", "").lower()
                if typ not in {"application/ld+json", "application/json", "importmap"}:
                    self.in_inline_script = True
                    self.script_buf = []

    def handle_endtag(self, tag: str):
        if tag == "title":
            self.in_title = False
        if tag == "script" and self.in_inline_script:
            self.inline_scripts.append("".join(self.script_buf))
            self.in_inline_script = False
            self.script_buf = []

    def handle_data(self, data: str):
        if self.in_title:
            self.title_parts.append(data)
        if self.in_inline_script:
            self.script_buf.append(data)

    @property
    def title(self) -> str:
        return " ".join("".join(self.title_parts).split())


def is_external(ref: str) -> bool:
    if not ref or ref.startswith("#") or ref.startswith("//"):
        return True
    parts = urlsplit(ref)
    return parts.scheme.lower() in IGNORED_SCHEMES


def resolve_local(page: Path, ref: str) -> Path | None:
    if is_external(ref):
        return None
    raw = unquote(urlsplit(ref).path)
    if not raw:
        return None
    target = ROOT / raw.lstrip("/") if raw.startswith("/") else page.parent / raw
    if raw.endswith("/"):
        target = target / "index.html"
    return target.resolve()


def case_insensitive_candidate(target: Path) -> Path | None:
    try:
        rel = target.relative_to(ROOT)
    except ValueError:
        return None
    current = ROOT
    for part in rel.parts:
        if not current.exists() or not current.is_dir():
            return None
        matches = [p for p in current.iterdir() if p.name.lower() == part.lower()]
        if not matches:
            return None
        current = matches[0]
    return current


def audit_html() -> tuple[list[Path], list[tuple[Path, int, str]]]:
    pages = sorted(iter_files({".html", ".htm"}))
    inline_scripts: list[tuple[Path, int, str]] = []
    for page in pages:
        parser = PageParser()
        text = read_text(page)
        try:
            parser.feed(text)
        except Exception as exc:
            add("error", "HTML_PARSE", page, f"Parser failed: {exc}")
            continue

        if not parser.lang:
            add("warning", "HTML_LANG", page, "Missing <html lang>.")
        if not parser.title:
            add("error", "HTML_TITLE", page, "Missing or empty <title>.")
        if not parser.viewport:
            add("warning", "HTML_VIEWPORT", page, "Missing viewport meta tag.")
        if parser.meta_refresh:
            add("error", "META_REFRESH", page, f"Meta refresh present: {parser.meta_refresh}")
        if parser.img_missing_alt:
            add("warning", "IMG_ALT", page, f"{parser.img_missing_alt} image(s) missing alt attribute.")
        if parser.blank_missing_rel:
            add("warning", "BLANK_REL", page, f"{parser.blank_missing_rel} target=_blank link(s) missing rel=noopener/noreferrer.")

        dup_ids = [k for k, v in Counter(parser.ids).items() if v > 1]
        if dup_ids:
            add("error", "DUPLICATE_ID", page, "Duplicate IDs: " + ", ".join(sorted(dup_ids)[:20]))

        script_counts = Counter(parser.local_scripts)
        duplicated_scripts = [s for s, n in script_counts.items() if n > 1]
        if duplicated_scripts:
            add("warning", "DUPLICATE_SCRIPT", page, "Repeated script src: " + ", ".join(duplicated_scripts))

        for attr, ref, line in parser.refs:
            target = resolve_local(page, ref)
            if target is None:
                if ref.lower().startswith("http://"):
                    add("warning", "INSECURE_HTTP", page, f"Line {line}: {ref}")
                continue
            try:
                target.relative_to(ROOT)
            except ValueError:
                add("error", "PATH_ESCAPE", page, f"Line {line}: {attr}={ref}")
                continue
            if target.exists():
                continue
            candidate = case_insensitive_candidate(target)
            if candidate and candidate.exists():
                add("error", "CASE_MISMATCH", page, f"Line {line}: {ref} resolves only with different filename case ({candidate.name}).")
            else:
                add("error", "BROKEN_LOCAL_REF", page, f"Line {line}: {attr}={ref}")

        for index, code in enumerate(parser.inline_scripts, 1):
            if code.strip():
                inline_scripts.append((page, index, code))

        if parser.canonical:
            host = urlsplit(parser.canonical).netloc.lower()
            if host and host not in {"so-velebit.hr", "www.so-velebit.hr"}:
                add("warning", "CANONICAL_HOST", page, f"Unexpected canonical host: {host}")
    return pages, inline_scripts


def audit_auth_registry() -> None:
    auth = ROOT / "assets" / "auth.js"
    if not auth.exists():
        add("error", "AUTH_MISSING", auth, "assets/auth.js missing.")
        return
    text = read_text(auth)
    match = re.search(r"REGISTERED_PAGES\s*=\s*new\s+Set\s*\(\s*\[(.*?)\]\s*\)", text, re.S)
    if not match:
        add("error", "AUTH_REGISTRY_PARSE", auth, "Could not parse REGISTERED_PAGES.")
        return
    names = re.findall(r"['\"]([^'\"]+\.(?:html?|HTML?))['\"]", match.group(1))
    for name in names:
        if not (ROOT / name).exists():
            add("error", "AUTH_PAGE_MISSING", auth, f"Registered page does not exist: {name}")


def audit_vercel() -> None:
    path = ROOT / "vercel.json"
    if not path.exists():
        add("warning", "VERCEL_CONFIG", path, "vercel.json missing.")
        return
    try:
        data = json.loads(read_text(path))
    except Exception as exc:
        add("error", "VERCEL_JSON", path, f"Invalid JSON: {exc}")
        return
    redirects = data.get("redirects", []) or []
    rewrites = data.get("rewrites", []) or []
    r_sources = {x.get("source") for x in redirects}
    w_sources = {x.get("source") for x in rewrites}
    for source in sorted((r_sources & w_sources) - {None}):
        add("warning", "VERCEL_ROUTE_CONFLICT", path, f"Source exists in redirects and rewrites: {source}")
    for item in redirects + rewrites:
        dest = str(item.get("destination", ""))
        if dest.startswith("/") and not any(ch in dest for ch in "*:()"):
            local = ROOT / dest.lstrip("/").split("?", 1)[0]
            if not local.exists():
                add("error", "VERCEL_DEST_MISSING", path, f"Destination missing for {item.get('source')}: {dest}")


def extract(pattern: str, text: str) -> str:
    m = re.search(pattern, text, re.S)
    return m.group(1).strip() if m else ""


def audit_version_contract() -> None:
    version_file = ROOT / "VERSION.txt"
    update_file = ROOT / "update.json"
    helper = ROOT / "assets" / "sov-version.js"
    if not (version_file.exists() and update_file.exists() and helper.exists()):
        add("error", "VERSION_FILES", ROOT, "VERSION.txt, update.json and assets/sov-version.js are required.")
        return
    version = read_text(version_file).strip()
    try:
        update = json.loads(read_text(update_file))
    except Exception as exc:
        add("error", "UPDATE_JSON", update_file, str(exc))
        return
    helper_text = read_text(helper)
    fallback = extract(r"FALLBACK_VERSION\s*=\s*['\"]([^'\"]+)", helper_text)
    values = {"VERSION.txt": version, "update.json": str(update.get("version", "")), "sov-version.js": fallback}
    if len(set(values.values())) != 1:
        add("error", "VERSION_MISMATCH", ROOT, json.dumps(values, ensure_ascii=False))
    readme = ROOT / "README.md"
    if readme.exists():
        readme_version = extract(r"#\s*SOV web build v([^\s]+)", read_text(readme))
        if readme_version and readme_version != version:
            add("warning", "README_VERSION", readme, f"README says {readme_version}, release contract says {version}.")


def audit_secrets_and_domains() -> None:
    secret_patterns = [
        (re.compile(r"sb_secret_[A-Za-z0-9_-]{20,}"), "Supabase secret key"),
        (re.compile(r"SUPABASE_SERVICE_ROLE(?:_KEY)?\s*[:=]\s*['\"][^'\"]+", re.I), "service-role key assignment"),
        (re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"), "private key"),
    ]
    for path in iter_files({".html", ".htm", ".js", ".mjs", ".json", ".webmanifest"}):
        text = read_text(path)
        for pattern, label in secret_patterns:
            if pattern.search(text):
                add("error", "SECRET_EXPOSURE", path, label)
        if "sov-web.vercel.app" in text and path.name not in {"AUDIT_REPORT.md", "AUDIT_REPORT.json"}:
            add("warning", "OLD_PRODUCTION_HOST", path, "Contains sov-web.vercel.app; verify this is intentional.")


def write_reports(pages: list[Path]) -> None:
    counts = Counter(x.severity for x in issues)
    ordered = sorted(issues, key=lambda x: ({"error": 0, "warning": 1, "info": 2}.get(x.severity, 9), x.file, x.code, x.detail))
    payload = {
        "summary": {"pages": len(pages), "errors": counts["error"], "warnings": counts["warning"], "issues": len(issues)},
        "issues": [asdict(x) for x in ordered],
    }
    (ROOT / "AUDIT_REPORT.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = [
        "# SOV pre-release audit",
        "",
        f"- HTML pages scanned: **{len(pages)}**",
        f"- Release blockers: **{counts['error']}**",
        f"- Warnings: **{counts['warning']}**",
        "",
    ]
    for severity in ("error", "warning", "info"):
        subset = [x for x in ordered if x.severity == severity]
        if not subset:
            continue
        lines += [f"## {severity.upper()} ({len(subset)})", ""]
        for item in subset:
            lines.append(f"- `{item.code}` — `{item.file}` — {item.detail}")
        lines.append("")
    (ROOT / "AUDIT_REPORT.md").write_text("\n".join(lines), encoding="utf-8")
    print("SOV PRE-RELEASE AUDIT")
    print(f"pages={len(pages)} errors={counts['error']} warnings={counts['warning']}")
    for item in ordered[:250]:
        print(f"[{item.severity.upper()}] {item.code} {item.file}: {item.detail}")
    if len(ordered) > 250:
        print(f"... {len(ordered) - 250} more issue(s); see AUDIT_REPORT.md")


def main() -> int:
    pages, _inline_scripts = audit_html()
    audit_auth_registry()
    audit_vercel()
    audit_version_contract()
    audit_secrets_and_domains()
    write_reports(pages)
    return 1 if any(x.severity == "error" for x in issues) else 0


if __name__ == "__main__":
    sys.exit(main())
