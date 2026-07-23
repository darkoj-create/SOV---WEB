#!/usr/bin/env python3
from __future__ import annotations

import html as htmlmod
import json
import re
import shutil
import urllib.request
from pathlib import Path

from bs4 import BeautifulSoup
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
API = "https://sovelebit.wordpress.com/wp-json/wp/v2/posts?per_page=10&_embed"
HEADERS = {"User-Agent": "SOV WordPress migration/2026-07", "Accept": "application/json"}

SPECS = {
    6924: {
        "local_slug": "dabarski-kukovi-i-15-v-rockas",
        "title": "Dabarski kukovi i 15. „V ročkas”",
        "date_display": "17.07.2026.",
        "published_at": "2026-07-17T07:12:34+00:00",
        "author": "Jana Požeg Krišković",
        "category": "Istraživanja",
        "summary": "Zaželjela sam se malo spuštanja u jame, kad ugledam Edinu poruku u grupi za izlet na Dabarskim kukovima. Bez previše razmišljanja, javljam Edi da bih im se pridružila.",
        "featured_name": "dabarski-kukovi-2026-featured.webp",
        "image_prefix": "dabarski-kukovi-2026",
        "legacy_url": "https://sovelebit.wordpress.com/2026/07/17/6924/",
        "tags": ["dabarski kukovi", "jana požeg krišković", "so pds velebit", "so velebit"],
    },
    6907: {
        "local_slug": "burinka-3-5-srpnja-2026",
        "title": "Burinka, 3.–5. srpnja 2026.",
        "date_display": "13.07.2026.",
        "published_at": "2026-07-13T12:57:47+00:00",
        "author": "Jelena Babić",
        "category": "Istraživanja",
        "summary": "Članak od prošlog izleta ostao mi je napola pročitan, a informacije o jami sam čula tek u letu, ni ne sluteći da ću za vikend biti tu.",
        "featured_name": "burinka-srpanj-2026-featured.webp",
        "image_prefix": "burinka-srpanj-2026",
        "legacy_url": "https://sovelebit.wordpress.com/2026/07/13/burinka-3-5-srpnja-2026/",
        "tags": ["burinka", "ide dalje", "jelena babić", "so pds velebit", "so velebit", "sov"],
    },
}


def fetch_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()


def save_webp(raw: bytes, destination: Path, max_dimension: int = 1800, quality: int = 84) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temp = destination.with_suffix(destination.suffix + ".source")
    temp.write_bytes(raw)
    try:
        image = Image.open(temp)
        image.load()
        if max(image.size) > max_dimension:
            scale = max_dimension / max(image.size)
            image = image.resize(
                (round(image.width * scale), round(image.height * scale)),
                Image.Resampling.LANCZOS,
            )
        if image.mode not in ("RGBA", "LA"):
            image = image.convert("RGB")
        image.save(destination, "WEBP", quality=quality, method=6)
    finally:
        temp.unlink(missing_ok=True)


def clean_content(post: dict, spec: dict) -> tuple[str, str, str]:
    content = BeautifulSoup(post.get("content", {}).get("rendered", ""), "html.parser")
    news_dir = ROOT / "assets" / "news"
    for old in news_dir.glob(spec["image_prefix"] + "-*.webp"):
        old.unlink()

    embedded = post.get("_embedded") or {}
    featured_media = embedded.get("wp:featuredmedia") or []
    if not featured_media or not featured_media[0].get("source_url"):
        raise RuntimeError(f"Post {post.get('id')} has no featured image")
    save_webp(fetch_bytes(featured_media[0]["source_url"]), news_dir / spec["featured_name"], 1600, 86)

    for index, image in enumerate(content.find_all("img"), 1):
        source = htmlmod.unescape(image.get("src") or "")
        if not source:
            continue
        filename = f"{spec['image_prefix']}-{index:02d}.webp"
        save_webp(fetch_bytes(source), news_dir / filename)
        image["src"] = f"/assets/news/{filename}"
        image["loading"] = "lazy"
        image["decoding"] = "async"
        for attr in (
            "srcset", "sizes", "data-orig-file", "data-orig-size", "data-comments-opened",
            "data-image-meta", "data-image-title", "data-image-description", "data-image-caption",
            "data-large-file", "data-medium-file",
        ):
            image.attrs.pop(attr, None)
        if image.parent and image.parent.name == "a":
            image.parent["href"] = f"/assets/news/{filename}"
            image.parent.attrs.pop("data-orig-file", None)

    for tag in content.find_all(True):
        if tag.has_attr("class"):
            tag["class"] = [
                value for value in tag["class"]
                if not value.startswith("wp-image-") and value not in {"alignnone", "aligncenter", "size-large", "size-full"}
            ]
            if not tag["class"]:
                del tag["class"]

    db_html = str(content)
    static_html = db_html.replace('src="/assets/news/', 'src="../assets/news/').replace('href="/assets/news/', 'href="../assets/news/')
    plain_text = content.get_text("\n", strip=True)
    return db_html, static_html, plain_text


def update_json_ld(template: BeautifulSoup, spec: dict, description: str) -> None:
    article_url = f"https://www.so-velebit.hr/novosti/{spec['local_slug']}.html"
    image_url = f"https://www.so-velebit.hr/assets/news/{spec['featured_name']}"
    for script in template.find_all("script", attrs={"type": "application/ld+json"}):
        try:
            data = json.loads(script.string or "{}")
        except Exception:
            continue
        if data.get("@type") == "Article":
            data.update({
                "headline": spec["title"],
                "description": description,
                "url": article_url,
                "image": image_url,
                "author": {"@type": "Person", "name": spec["author"]},
                "datePublished": spec["published_at"],
            })
        elif data.get("@type") == "BreadcrumbList" and data.get("itemListElement"):
            data["itemListElement"][-1]["name"] = spec["title"]
            data["itemListElement"][-1]["item"] = article_url
        script.string = json.dumps(data, ensure_ascii=False, separators=(",", ":"))


def write_article(spec: dict, static_content: str) -> None:
    template_path = ROOT / "novosti" / "sve-sto-je-lijepo-kratko-traje-osim-puta-do-velebitaskog-duha.html"
    page = BeautifulSoup(template_path.read_text(encoding="utf-8"), "html.parser")
    page.title.string = spec["title"]
    description = f"{spec['category']} · {spec['date_display']} {spec['title']} {spec['summary']}"
    article_url = f"https://www.so-velebit.hr/novosti/{spec['local_slug']}.html"
    image_url = f"https://www.so-velebit.hr/assets/news/{spec['featured_name']}"

    for meta in page.find_all("meta"):
        name = meta.get("name")
        prop = meta.get("property")
        if name in ("description", "twitter:description"):
            meta["content"] = description
        elif name == "twitter:title":
            meta["content"] = spec["title"]
        elif name == "twitter:image":
            meta["content"] = image_url
        elif prop == "og:title":
            meta["content"] = spec["title"]
        elif prop == "og:description":
            meta["content"] = description
        elif prop == "og:url":
            meta["content"] = article_url
        elif prop == "og:image":
            meta["content"] = image_url

    canonical = page.find("link", rel="canonical")
    if canonical:
        canonical["href"] = article_url
    update_json_ld(page, spec, description)

    page.select_one(".article-hero")["style"] = f"background-image:url('../assets/news/{spec['featured_name']}')"
    page.select_one(".article-meta").string = f"{spec['category']} · {spec['date_display']}"
    page.select_one(".article-title").string = spec["title"]
    page.select_one(".article-desc").string = spec["summary"]
    article = page.select_one("article.article-content")
    article.clear()
    fragment = BeautifulSoup(static_content, "html.parser")
    for child in list(fragment.contents):
        article.append(child)

    logo = page.select_one(".news-brand img")
    if logo:
        logo["alt"] = ""
    side = page.select_one(".article-side")
    side.select_one("p").string = "Tekst i fotografije preneseni su iz izvorne objave SOV-a bez prepisivanja; prilagođen je samo prikaz na novom portalu."
    source = page.new_tag("a", href=spec["legacy_url"], target="_blank", rel="noopener")
    source.string = "Izvorna WordPress objava"
    side.append(source)
    footer = page.select_one(".footer")
    if footer:
        footer.string = "Speleološki odsjek PDS Velebit · Zagreb"

    destination = ROOT / "novosti" / f"{spec['local_slug']}.html"
    destination.write_text(str(page), encoding="utf-8")


def side_card(spec: dict) -> str:
    return (
        f'<a class="news-side-card" href="novosti/{spec["local_slug"]}.html">'
        f'<span class="news-img" style="background-image:url(\'assets/news/{spec["featured_name"]}\')"></span>'
        f'<span class="news-copy"><span class="news-meta">{spec["category"]} · {spec["date_display"]}</span>'
        f'<strong>{htmlmod.escape(spec["title"])}</strong><em>{htmlmod.escape(spec["summary"])}</em></span></a>'
    )


def archive_card(spec: dict) -> str:
    searchable = (spec["title"] + " " + spec["summary"]).lower()
    return (
        f'<a class="archive-card" data-category="{spec["category"]}" data-title="{htmlmod.escape(searchable, quote=True)}" '
        f'data-year="2026" href="novosti/{spec["local_slug"]}.html">'
        f'<span class="archive-img" style="background-image:url(\'assets/news/{spec["featured_name"]}\')"></span>'
        f'<span class="archive-copy"><span class="archive-meta">{spec["category"]} · {spec["date_display"]}</span>'
        f'<strong>{htmlmod.escape(spec["title"])}</strong><em>{htmlmod.escape(spec["summary"])}</em></span></a>'
    )


def update_homepage() -> None:
    path = ROOT / "index.html"
    page = BeautifulSoup(path.read_text(encoding="utf-8"), "html.parser")
    new_hrefs = {f"novosti/{spec['local_slug']}.html" for spec in SPECS.values()}
    stack = page.select_one(".news-stack")
    grid = page.select_one(".news-grid")
    if not stack or not grid:
        raise RuntimeError("Homepage news containers not found")

    for card in list(grid.find_all("a", recursive=False)):
        if card.get("href") in new_hrefs:
            card.decompose()
    old_stack = [card.extract() for card in stack.find_all("a", recursive=False) if card.get("href") not in new_hrefs]
    stack.clear()
    for post_id in (6924, 6907):
        stack.append(BeautifulSoup(side_card(SPECS[post_id]), "html.parser").a)

    existing = {card.get("href") for card in grid.find_all("a", recursive=False)}
    for old in reversed(old_stack):
        if old.get("href") in existing:
            continue
        old["class"] = ["news-card"]
        grid.insert(0, old)
    path.write_text(str(page), encoding="utf-8")


def update_archive() -> None:
    path = ROOT / "vijesti.html"
    text = path.read_text(encoding="utf-8")
    page = BeautifulSoup(text, "html.parser")
    grid = page.select_one(".archive-grid")
    if not grid:
        raise RuntimeError("Archive grid not found")
    new_hrefs = {f"novosti/{spec['local_slug']}.html" for spec in SPECS.values()}
    for card in list(grid.find_all("a", recursive=False)):
        if card.get("href") in new_hrefs:
            card.decompose()
    for post_id in reversed((6924, 6907)):
        grid.insert(0, BeautifulSoup(archive_card(SPECS[post_id]), "html.parser").a)
    path.write_text(str(page), encoding="utf-8")


def main() -> None:
    posts = json.loads(fetch_bytes(API).decode("utf-8"))
    by_id = {post.get("id"): post for post in posts}
    missing = set(SPECS) - set(by_id)
    if missing:
        raise RuntimeError(f"WordPress posts missing: {sorted(missing)}")

    database_rows = []
    for post_id, spec in SPECS.items():
        db_html, static_html, plain_text = clean_content(by_id[post_id], spec)
        write_article(spec, static_html)
        database_rows.append({
            **spec,
            "content_html": db_html,
            "body": plain_text,
            "image_url": f"https://www.so-velebit.hr/assets/news/{spec['featured_name']}",
            "cta_url": f"vijest.html?slug={spec['local_slug']}",
        })

    update_homepage()
    update_archive()
    (ROOT / "WP_NEWS_IMPORT_ROWS.json").write_text(json.dumps(database_rows, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Imported:", ", ".join(row["title"] for row in database_rows))


if __name__ == "__main__":
    main()
