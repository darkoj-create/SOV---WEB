#!/usr/bin/env python3
"""
SOV legacy WordPress importer / media localizer.

Purpose:
- Fetch public posts/pages from https://sovelebit.wordpress.com through the WordPress REST API when internet is available.
- Render imported posts as static SOV-styled HTML files.
- Download every WordPress upload/media file into assets/legacy-wordpress/.
- Rewrite existing build files so the frontend does not hotlink old WordPress media.

Safe defaults:
- Existing static articles are not overwritten unless --overwrite is passed.
- Existing local media files are not overwritten unless --overwrite-media is passed.
- Source URLs are kept only in data manifests for audit/downloader use.

Typical use from the extracted web build root:
  python tools/sov_wordpress_legacy_importer.py --root . --rewrite-existing --download-assets
  python tools/sov_wordpress_legacy_importer.py --root . --import-rest --download-assets
"""
from __future__ import annotations

import argparse
import html
import json
import mimetypes
import os
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

WP_UPLOAD_RE = re.compile(r"https?://(?:i\d+\.wp\.com/)?(?:sovelebit\.wordpress\.com|www\.pdsvelebit\.hr)/[^\"'\)\s<>]+", re.I)
SCAN_EXTS = {".html", ".js", ".json", ".css", ".sql"}
IMAGE_EXTS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg"}

HR_TRANSLIT = str.maketrans({
    "č": "c", "ć": "c", "š": "s", "ž": "z", "đ": "d",
    "Č": "c", "Ć": "c", "Š": "s", "Ž": "z", "Đ": "d",
})

@dataclass
class AssetItem:
    local_path: str
    source_url: str
    source_urls: List[str]
    files: List[str]
    downloaded: bool = False
    error: str = ""


def log(msg: str) -> None:
    print(msg, flush=True)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def strip_tags(s: str) -> str:
    s = re.sub(r"<script[\s\S]*?</script>", " ", s or "", flags=re.I)
    s = re.sub(r"<style[\s\S]*?</style>", " ", s, flags=re.I)
    s = re.sub(r"<[^>]+>", " ", s)
    s = html.unescape(s)
    return re.sub(r"\s+", " ", s).strip()


def slugify(value: str, fallback: str = "objava") -> str:
    value = html.unescape(value or "").translate(HR_TRANSLIT)
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    value = re.sub(r"[^a-zA-Z0-9]+", "-", value).strip("-").lower()
    return value or fallback


def normalize_url(raw: str) -> str:
    u = html.unescape(raw.strip())
    while u and u[-1] in ".,;":
        u = u[:-1]
    return u


def source_candidates(url: str) -> List[str]:
    """Return robust download candidates for wp.com proxy and original WP URLs."""
    u = normalize_url(url)
    out = [u]
    parsed = urllib.parse.urlparse(u)
    host = parsed.netloc.lower()
    path = parsed.path
    if host.startswith("i") and host.endswith(".wp.com"):
        parts = path.lstrip("/").split("/", 1)
        if len(parts) == 2:
            original = urllib.parse.urlunparse(("https", parts[0], "/" + parts[1], "", "", ""))
            out.append(original)
            out.append(original + ("?" + parsed.query if parsed.query else ""))
    no_query = urllib.parse.urlunparse(parsed._replace(query="", fragment=""))
    if no_query not in out:
        out.append(no_query)
    return out


def local_rel_from_url(url: str) -> Optional[str]:
    parsed = urllib.parse.urlparse(normalize_url(url))
    host = parsed.netloc.lower()
    path = urllib.parse.unquote(parsed.path)
    if host.startswith("i") and host.endswith(".wp.com"):
        parts = path.lstrip("/").split("/", 1)
        if len(parts) == 2:
            host = parts[0].lower()
            path = "/" + parts[1]
    if "/wp-content/uploads/" not in path:
        return None
    rel = path.split("/wp-content/uploads/", 1)[1].lstrip("/")
    safe_parts: List[str] = []
    for part in rel.split("/"):
        part = part.strip().replace("\\", "-")
        if not part or part in {".", ".."}:
            continue
        safe_parts.append(part)
    if not safe_parts:
        return None
    prefix = "assets/legacy-wordpress/"
    if host == "www.pdsvelebit.hr":
        prefix += "pdsvelebit/"
    return prefix + "/".join(safe_parts)


def rel_link(root: Path, file_path: Path, local_path: str) -> str:
    return os.path.relpath(root / local_path, file_path.parent).replace(os.sep, "/")


def scan_and_rewrite_existing(root: Path, dry_run: bool = False) -> Dict[str, AssetItem]:
    assets: Dict[str, AssetItem] = {}
    touched: List[str] = []
    for path in root.rglob("*"):
        if not path.is_file() or path.suffix.lower() not in SCAN_EXTS:
            continue
        text = read_text(path)
        replacements: Dict[str, str] = {}
        for match in WP_UPLOAD_RE.finditer(text):
            raw = match.group(0)
            url = normalize_url(raw)
            local = local_rel_from_url(url)
            if not local:
                continue
            rel_file = path.relative_to(root).as_posix()
            item = assets.setdefault(local, AssetItem(local, url, [], []))
            if url not in item.source_urls:
                item.source_urls.append(url)
            if rel_file not in item.files:
                item.files.append(rel_file)
            replacement = rel_link(root, path, local)
            replacements[raw] = replacement
            replacements[html.unescape(raw)] = replacement
            replacements[html.unescape(raw).replace("&", "&amp;")] = replacement
        if replacements:
            new_text = text
            for old, new in sorted(replacements.items(), key=lambda kv: len(kv[0]), reverse=True):
                new_text = new_text.replace(old, new)
            if new_text != text:
                touched.append(path.relative_to(root).as_posix())
                if not dry_run:
                    write_text(path, new_text)
    write_media_manifest(root, assets, touched, dry_run=dry_run)
    log(f"rewrite-existing: assets={len(assets)} touched_files={len(touched)}")
    return assets


def load_media_manifest(root: Path) -> Dict[str, AssetItem]:
    path = root / "data" / "legacy-wordpress-media-manifest.json"
    if not path.exists():
        return {}
    data = json.loads(read_text(path))
    out: Dict[str, AssetItem] = {}
    for row in data.get("assets", []):
        item = AssetItem(
            local_path=row.get("local_path", ""),
            source_url=row.get("source_url", ""),
            source_urls=list(row.get("source_urls") or [row.get("source_url", "")]),
            files=list(row.get("files") or []),
            downloaded=bool(row.get("downloaded", False)),
            error=row.get("error", ""),
        )
        if item.local_path:
            out[item.local_path] = item
    return out


def write_media_manifest(root: Path, assets: Dict[str, AssetItem], touched: Iterable[str] = (), dry_run: bool = False) -> None:
    data = {
        "generated_by": "SOV legacy WordPress importer/localizer",
        "note": "Frontend uses local_path. source_urls are kept only for importer/downloader/audit.",
        "asset_count": len(assets),
        "touched_file_count": len(set(touched)),
        "touched_files": sorted(set(touched)),
        "assets": [asdict(v) for v in sorted(assets.values(), key=lambda x: x.local_path)],
    }
    if not dry_run:
        write_text(root / "data" / "legacy-wordpress-media-manifest.json", json.dumps(data, ensure_ascii=False, indent=2))


def download_one(urls: List[str], target: Path, timeout: int = 45, overwrite: bool = False) -> Tuple[bool, str]:
    if target.exists() and target.stat().st_size > 500 and not overwrite:
        return True, "exists"
    target.parent.mkdir(parents=True, exist_ok=True)
    last_err = ""
    headers = {"User-Agent": "SOVLegacyImporter/1.0 (+https://sovelebit.wordpress.com)"}
    for url in urls:
        for candidate in source_candidates(url):
            try:
                req = urllib.request.Request(candidate, headers=headers)
                with urllib.request.urlopen(req, timeout=timeout) as resp:
                    data = resp.read()
                    if not data or len(data) < 50:
                        raise RuntimeError("empty/too-small response")
                    target.write_bytes(data)
                    return True, candidate
            except Exception as exc:  # noqa: BLE001 - importer should continue
                last_err = f"{candidate}: {exc}"
                time.sleep(0.2)
    return False, last_err


def download_assets(root: Path, overwrite_media: bool = False, limit: int = 0, dry_run: bool = False) -> Dict[str, AssetItem]:
    assets = load_media_manifest(root)
    if not assets:
        log("download-assets: no data/legacy-wordpress-media-manifest.json found; run --rewrite-existing or --import-rest first")
        return assets
    count = 0
    for item in assets.values():
        if limit and count >= limit:
            break
        target = root / item.local_path
        urls = item.source_urls or [item.source_url]
        if dry_run:
            log(f"DRY download {item.local_path} <- {urls[0] if urls else ''}")
            continue
        ok, msg = download_one(urls, target, overwrite=overwrite_media)
        item.downloaded = ok
        item.error = "" if ok else msg
        count += 1
        log(("OK   " if ok else "FAIL ") + f"{item.local_path} {msg}")
    write_media_manifest(root, assets)
    return assets


def fetch_json(url: str, timeout: int = 45):
    req = urllib.request.Request(url, headers={"User-Agent": "SOVLegacyImporter/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8")), resp.headers


def wp_paginated(base: str, endpoint: str, limit: int = 0) -> List[dict]:
    rows: List[dict] = []
    page = 1
    while True:
        url = f"{base.rstrip('/')}/wp-json/wp/v2/{endpoint}?per_page=100&page={page}&_embed=1"
        data, headers = fetch_json(url)
        if not isinstance(data, list) or not data:
            break
        rows.extend(data)
        log(f"REST {endpoint} page {page}: +{len(data)}")
        if limit and len(rows) >= limit:
            return rows[:limit]
        total_pages = int(headers.get("X-WP-TotalPages") or page)
        if page >= total_pages:
            break
        page += 1
    return rows


def collect_asset_urls_from_html(content: str) -> List[str]:
    urls = []
    for m in WP_UPLOAD_RE.finditer(content or ""):
        u = normalize_url(m.group(0))
        if local_rel_from_url(u) and u not in urls:
            urls.append(u)
    return urls


def rewrite_content_asset_urls(root: Path, output_file: Path, content: str, assets: Dict[str, AssetItem], source_url: str = "") -> str:
    replacements: Dict[str, str] = {}
    for u in collect_asset_urls_from_html(content):
        local = local_rel_from_url(u)
        if not local:
            continue
        item = assets.setdefault(local, AssetItem(local, u, [], []))
        if u not in item.source_urls:
            item.source_urls.append(u)
        rel_file = output_file.relative_to(root).as_posix()
        if rel_file not in item.files:
            item.files.append(rel_file)
        replacements[u] = rel_link(root, output_file, local)
        replacements[u.replace("&", "&amp;")] = rel_link(root, output_file, local)
    for old, new in sorted(replacements.items(), key=lambda kv: len(kv[0]), reverse=True):
        content = content.replace(old, new)
    return content


def render_post_html(root: Path, output_file: Path, item: dict, content_html: str, featured_local: str = "") -> str:
    title = strip_tags(item.get("title", {}).get("rendered", "")) or "Objava"
    date = (item.get("date") or "")[:10]
    summary = strip_tags(item.get("excerpt", {}).get("rendered", ""))[:240]
    category = "Legacy WordPress"
    hero_style = f" style=\"background-image:url('{html.escape(rel_link(root, output_file, featured_local))}')\"" if featured_local else ""
    return f"""<!DOCTYPE html>
<html lang="hr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>{html.escape(title)} — SOV Velebit</title>
<link rel="stylesheet" href="../assets/site.css"><link rel="stylesheet" href="../assets/mobile.css">
<link rel="stylesheet" href="../assets/sov-foundation-v55822.css"><link rel="stylesheet" href="../assets/sov-shell-v55825.css"><link rel="stylesheet" href="../assets/sov-polish-v55826.css"><link rel="stylesheet" href="../assets/sov-public-polish-v609.css">
<link rel="icon" href="../assets/sov-logo.png"></head><body class="public-polish-v609">
<header class="topbar"><nav class="nav"><a class="brand brand-sov" href="../index.html"><img alt="SOV logo" class="brand-round" src="../assets/brand/sov-round-logo.png"><img alt="Speleološki odsjek Velebit" class="brand-wordmark" src="../assets/brand/sov-wordmark.png"><span>SOV Velebit</span></a><div class="navlinks navlinks-main"><a href="../index.html">Novosti</a><a href="../o-drustvu.html">O nama</a><a href="../speleoskola.html">Speleoškola</a><a href="../pridruzi-nam-se.html">Pridruži nam se</a><a class="login" href="../dashboard.html">Članski ulaz</a></div></nav></header>
<section class="article-hero"{hero_style}><div class="article-hero-inner"><span class="article-meta">{html.escape(category)} · {html.escape(date)}</span><h1 class="article-title">{html.escape(title)}</h1><p class="article-desc">{html.escape(summary)}</p></div></section>
<main class="article-wrap"><article class="article-card"><div class="legacy-source-note">Migrirano sa starog WordPress weba. Slike su lokalizirane u novi SOV web build.</div>{content_html}</article></main>
<footer class="footer">Speleološki odsjek PDS Velebit · Zagreb</footer>
<script defer src="../assets/sov-foundation-v55822.js"></script><script defer src="../assets/sov-shell-v55825.js"></script><script defer src="../assets/sov-polish-v55826.js"></script></body></html>"""


def import_rest(root: Path, source: str, limit: int = 0, overwrite: bool = False, dry_run: bool = False) -> Dict[str, AssetItem]:
    posts = wp_paginated(source, "posts", limit=limit)
    pages = wp_paginated(source, "pages", limit=limit)
    assets = load_media_manifest(root)
    imported = []
    for kind, rows in (("post", posts), ("page", pages)):
        for row in rows:
            title = strip_tags(row.get("title", {}).get("rendered", ""))
            slug = slugify(row.get("slug") or title, fallback=f"wp-{row.get('id')}")
            if kind == "post":
                out = root / "novosti" / f"{slug}.html"
            else:
                out = root / "legacy-wordpress" / f"{slug}.html"
            if out.exists() and not overwrite:
                log(f"SKIP existing {out.relative_to(root)}")
                continue
            content = row.get("content", {}).get("rendered", "") or ""
            content = rewrite_content_asset_urls(root, out, content, assets)
            featured_local = ""
            embedded = row.get("_embedded") or {}
            media = (embedded.get("wp:featuredmedia") or [])
            if media:
                src = media[0].get("source_url") or ""
                local = local_rel_from_url(src) if src else None
                if local:
                    featured_local = local
                    item = assets.setdefault(local, AssetItem(local, src, [], []))
                    if src not in item.source_urls:
                        item.source_urls.append(src)
                    if out.relative_to(root).as_posix() not in item.files:
                        item.files.append(out.relative_to(root).as_posix())
            page_html = render_post_html(root, out, row, content, featured_local)
            imported.append({"kind": kind, "id": row.get("id"), "slug": slug, "title": title, "output": out.relative_to(root).as_posix(), "source_url": row.get("link", "")})
            log(f"IMPORT {out.relative_to(root)}")
            if not dry_run:
                write_text(out, page_html)
    if not dry_run:
        write_text(root / "data" / "legacy-wordpress-import-manifest.json", json.dumps({"source": source, "count": len(imported), "items": imported}, ensure_ascii=False, indent=2))
    write_media_manifest(root, assets, touched=[x["output"] for x in imported], dry_run=dry_run)
    return assets


def main(argv: Optional[List[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="SOV legacy WordPress importer and media localizer")
    ap.add_argument("--root", default=".", help="Extracted web build root")
    ap.add_argument("--source", default="https://sovelebit.wordpress.com", help="Legacy WordPress site base URL")
    ap.add_argument("--rewrite-existing", action="store_true", help="Rewrite existing build files from old WP media URLs to local assets/legacy-wordpress paths")
    ap.add_argument("--download-assets", action="store_true", help="Download assets listed in data/legacy-wordpress-media-manifest.json")
    ap.add_argument("--import-rest", action="store_true", help="Fetch posts/pages via WordPress REST API and render SOV static pages")
    ap.add_argument("--overwrite", action="store_true", help="Overwrite existing rendered imported pages")
    ap.add_argument("--overwrite-media", action="store_true", help="Overwrite existing downloaded media")
    ap.add_argument("--limit", type=int, default=0, help="Limit imported/downloaded rows for testing")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args(argv)

    root = Path(args.root).resolve()
    if not root.exists():
        raise SystemExit(f"Root does not exist: {root}")
    if args.rewrite_existing:
        scan_and_rewrite_existing(root, dry_run=args.dry_run)
    if args.import_rest:
        import_rest(root, args.source, limit=args.limit, overwrite=args.overwrite, dry_run=args.dry_run)
    if args.download_assets:
        download_assets(root, overwrite_media=args.overwrite_media, limit=args.limit, dry_run=args.dry_run)
    if not (args.rewrite_existing or args.import_rest or args.download_assets):
        ap.print_help()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
