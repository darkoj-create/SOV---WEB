#!/usr/bin/env python3
from __future__ import annotations
import re, json, html, shutil, hashlib
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime, timezone
from urllib.parse import urlparse, urlunparse, unquote
from bs4 import BeautifulSoup, NavigableString, Tag

ROOT = Path('/mnt/data/sov_text_import_work')
XML = Path('/mnt/data/wp_export/sovelebit.wordpress.com-2026-06-24-16_13_45/sovelebit.wordpress.com.2026-06-24.000.xml')
BUILD_VERSION = '6.1.44b'
BUILD_SLUG = 'legacy-wordpress-wxr-text-only'
BUILD_NAME = f'v{BUILD_VERSION}-{BUILD_SLUG}'
BUILD_FILE = f'sov-web-build-v{BUILD_VERSION}-{BUILD_SLUG}'
CACHE_BUST = '6144b-wxr-text-only'

NS = {
    'wp':'http://wordpress.org/export/1.2/',
    'content':'http://purl.org/rss/1.0/modules/content/',
    'excerpt':'http://wordpress.org/export/1.2/excerpt/',
    'dc':'http://purl.org/dc/elements/1.1/',
}

CRO_MAP = str.maketrans({'č':'c','ć':'c','š':'s','ž':'z','đ':'d','Č':'c','Ć':'c','Š':'s','Ž':'z','Đ':'d'})
WP_MEDIA_PAT = re.compile(r'https?://(?:i\d+\.wp\.com/)?sovelebit\.wordpress\.com/wp-content/uploads/[^\s"\'<>\)]+', re.I)
WP_ANY_MEDIA_PAT = re.compile(r'https?://[^\s"\'<>\)]*wp-content/uploads/[^\s"\'<>\)]+', re.I)


def slugify(s: str, fallback='item') -> str:
    s = html.unescape(s or '').translate(CRO_MAP).lower()
    s = re.sub(r'[^a-z0-9]+', '-', s).strip('-')
    return (s[:96].strip('-') or fallback)


def text(el, path, default=''):
    x = el.find(path, NS)
    return x.text if x is not None and x.text is not None else default


def child_text(el, tag, default=''):
    x = el.find(tag)
    return x.text if x is not None and x.text is not None else default


def parse_date(value: str) -> str:
    value = (value or '').strip()
    if not value: return ''
    # WP: 2026-05-07 04:59:36 or RFC pubDate
    for fmt in ('%Y-%m-%d %H:%M:%S','%a, %d %b %Y %H:%M:%S %z','%a, %d %b %Y %H:%M:%S %Z'):
        try:
            dt = datetime.strptime(value, fmt)
            if dt.tzinfo is None: dt = dt.replace(tzinfo=timezone.utc)
            return dt.isoformat()
        except Exception:
            pass
    try:
        return datetime.fromisoformat(value).isoformat()
    except Exception:
        return value


def hr_date(iso: str) -> str:
    try:
        dt = datetime.fromisoformat(iso.replace('Z','+00:00'))
        return dt.strftime('%d.%m.%Y.')
    except Exception:
        return ''


def clean_url(u: str) -> str:
    if not u: return ''
    u = html.unescape(u).strip()
    # strip i0.wp.com wrapper query for manifest normalization only
    return u


def collect_media_urls(raw_html: str) -> list[str]:
    urls = []
    for pat in (WP_MEDIA_PAT, WP_ANY_MEDIA_PAT):
        for m in pat.findall(raw_html or ''):
            u = clean_url(m).rstrip('.,;')
            if u not in urls:
                urls.append(u)
    try:
        soup = BeautifulSoup(raw_html or '', 'html.parser')
        for tag in soup.find_all(['img','a','source','video','iframe','embed']):
            for attr in ['src','href','data-orig-file','data-large-file','data-medium-file','poster']:
                u = tag.get(attr)
                if u and ('wp-content/uploads' in u or 'i0.wp.com/sovelebit.wordpress.com' in u):
                    u = clean_url(u)
                    if u not in urls: urls.append(u)
            srcset = tag.get('srcset') or tag.get('data-srcset')
            if srcset:
                for part in srcset.split(','):
                    u = part.strip().split(' ')[0]
                    if 'wp-content/uploads' in u and u not in urls:
                        urls.append(clean_url(u))
    except Exception:
        pass
    return urls


def normalize_whitespace_text(s: str) -> str:
    return re.sub(r'\s+', ' ', html.unescape(s or '')).strip()


def strip_empty_tags(soup: BeautifulSoup):
    for tag in list(soup.find_all()):
        if tag.name in ['br','hr']:
            continue
        if tag.name in ['p','div','span','strong','em','i','b']:
            if not normalize_whitespace_text(tag.get_text()) and not tag.find(['a','ul','ol','table']):
                tag.decompose()


def sanitize_content(raw_html: str, link_map: dict[str,str]|None=None) -> tuple[str, bool]:
    link_map = link_map or {}
    raw_html = re.sub(r'<!--\s*/?wp:.*?-->', '', raw_html or '', flags=re.S)
    soup = BeautifulSoup(raw_html, 'html.parser')
    had_media = False
    # Remove comments
    from bs4 import Comment
    for c in soup.find_all(string=lambda s: isinstance(s, Comment)):
        c.extract()
    # Remove unsafe/non-textual blocks
    for tag in soup.find_all(['script','style','noscript']):
        tag.decompose()
    for tag in soup.find_all(['img','picture','source','video','audio','iframe','embed','object']):
        had_media = True
        tag.decompose()
    # Figures/galleries that are now empty or mostly media
    for tag in list(soup.find_all(['figure'])):
        txt = normalize_whitespace_text(tag.get_text())
        had_media = True
        if txt:
            p = soup.new_tag('p')
            p.string = txt
            tag.replace_with(p)
        else:
            tag.decompose()
    # Remove gallery wrappers (keep text inside if any)
    for tag in list(soup.find_all(['div'])):
        cls=' '.join(tag.get('class') or [])
        if 'wp-block-gallery' in cls or 'gallery' == cls:
            had_media = True
            if not normalize_whitespace_text(tag.get_text()):
                tag.decompose()
            else:
                tag.unwrap()
    # Strip WP/media attributes and classes to keep simple HTML
    allowed = {'a': {'href','title','target','rel'}, 'table': set(), 'thead':set(),'tbody':set(),'tr':set(),'th':set(),'td':set()}
    for tag in soup.find_all(True):
        if tag.name not in ['p','br','hr','h1','h2','h3','h4','h5','h6','ul','ol','li','blockquote','pre','code','strong','b','em','i','a','table','thead','tbody','tr','th','td']:
            tag.unwrap()
            continue
        for attr in list(tag.attrs.keys()):
            if attr not in allowed.get(tag.name, set()):
                del tag.attrs[attr]
    # Rewrite or remove links to old WP/media
    for a in soup.find_all('a'):
        href = clean_url(a.get('href',''))
        if not href:
            a.unwrap(); continue
        # media links removed, keep visible text
        if 'wp-content/uploads' in href or 'i0.wp.com/sovelebit.wordpress.com' in href:
            had_media=True
            a.unwrap(); continue
        # internal old WP article/page links
        norm_href = href.rstrip('/') + '/'
        if norm_href in link_map:
            a['href'] = link_map[norm_href]
        elif href in link_map:
            a['href'] = link_map[href]
        elif 'sovelebit.wordpress.com' in href:
            parsed=urlparse(href)
            key = f'https://sovelebit.wordpress.com{parsed.path}'.rstrip('/') + '/'
            if key in link_map:
                a['href'] = link_map[key]
            else:
                # Don't send people back to old WP for internal links; keep text only.
                a.unwrap()
        else:
            a['target']='_blank'; a['rel']='noopener'
    strip_empty_tags(soup)
    body = ''.join(str(x) for x in soup.contents).strip()
    body = re.sub(r'\n{3,}', '\n\n', body)
    return body, had_media


def excerpt_from(raw_html: str, excerpt: str='', limit=210) -> str:
    raw = excerpt or raw_html or ''
    soup = BeautifulSoup(raw, 'html.parser')
    for t in soup.find_all(['script','style','img','figure','iframe']): t.decompose()
    txt = normalize_whitespace_text(soup.get_text(' '))
    if len(txt) > limit:
        txt = txt[:limit].rsplit(' ',1)[0] + '…'
    return txt


def item_terms(item):
    cats=[]; tags=[]
    for c in item.findall('category'):
        domain = c.attrib.get('domain','')
        nicename = c.attrib.get('nicename','')
        val = normalize_whitespace_text(c.text or nicename)
        if not val: continue
        if domain == 'category': cats.append(val)
        elif domain == 'post_tag': tags.append(val)
    # unique preserve order
    def uniq(xs):
        out=[]
        for x in xs:
            if x not in out: out.append(x)
        return out
    return uniq(cats), uniq(tags)


def parse_wxr():
    tree = ET.parse(XML)
    chan = tree.getroot().find('channel')
    items=[]; attachments=[]
    for item in chan.findall('item'):
        post_type = text(item,'wp:post_type')
        status = text(item,'wp:status')
        post_id = text(item,'wp:post_id')
        title = normalize_whitespace_text(child_text(item,'title')) or '(bez naslova)'
        link = clean_url(child_text(item,'link'))
        slug = slugify(text(item,'wp:post_name') or title, fallback=f'wp-{post_id}')
        raw = text(item,'content:encoded')
        exc = text(item,'excerpt:encoded')
        date_iso = parse_date(text(item,'wp:post_date') or child_text(item,'pubDate'))
        cats,tags = item_terms(item)
        creator = text(item,'dc:creator')
        parent = text(item,'wp:post_parent')
        attach_url = text(item,'wp:attachment_url')
        meta = []
        for m in item.findall('wp:postmeta', NS):
            meta.append({'key':text(m,'wp:meta_key'), 'value':text(m,'wp:meta_value')})
        rec = {
            'id': post_id, 'type': post_type, 'status': status, 'title': title, 'slug': slug, 'link': link,
            'date_iso': date_iso, 'date': hr_date(date_iso), 'categories': cats, 'tags': tags,
            'category': cats[0] if cats else 'Novosti', 'creator': creator, 'content_raw': raw,
            'excerpt_raw': exc, 'excerpt': excerpt_from(raw, exc), 'parent': parent, 'attachment_url': attach_url,
            'meta': meta,
        }
        if post_type == 'attachment': attachments.append(rec)
        items.append(rec)
    return items, attachments


def build_link_map(posts, pages):
    m={}
    for p in posts:
        target=f'../novosti/{p["slug"]}.html'
        # target is article-relative? We'll handle later; map root-rel and relative separately
    # create root-relative map (no leading slash)
    out={}
    for p in posts:
        for key in [p['link'], p['link'].rstrip('/')+'/' if p['link'] else '']:
            if key: out[key]=f'novosti/{p["slug"]}.html'
    for p in pages:
        target = public_page_target(p)
        for key in [p['link'], p['link'].rstrip('/')+'/' if p['link'] else '']:
            if key: out[key]=target
    return out


def public_page_target(page):
    slug = page['slug']
    known = {
        'home':'index.html', 'vijesti':'vijesti.html', 'povijest':'povijest.html', 'o-nama':'o-drustvu.html',
        'velebitaski-duh':'velebitaski-duh.html', 'procelnistvo':'procelnistvo.html', 'velebiten':'velebiten.html',
        'speleoskola':'speleoskola.html', 'contact':'pridruzi-nam-se.html'
    }
    return known.get(slug, f'legacy-wordpress/stranice/{slug}.html')


def rel_prefix(path: Path):
    depth=len(path.relative_to(ROOT).parts)-1
    return '../'*depth


def html_escape(s): return html.escape(str(s or ''), quote=True)


def base_head(title, prefix='', extra_css=''):
    return f'''<!DOCTYPE html>
<html lang="hr"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>{html_escape(title)}</title>
<link href="{prefix}assets/site.css" rel="stylesheet"/>
<link href="{prefix}assets/mobile.css" rel="stylesheet"/>
<link href="{prefix}assets/sov-logo.png" rel="icon"/>
<link href="{prefix}assets/sov-foundation-v55822.css" rel="stylesheet"/>
<link href="{prefix}assets/sov-shell-v55825.css" rel="stylesheet"/>
<link href="{prefix}assets/sov-polish-v55826.css" rel="stylesheet"/>
<link href="{prefix}assets/sov-wow-v6.css" rel="stylesheet"/>
<style>
:root{{--legacy-gold:#d5a85b;--legacy-line:rgba(255,255,255,.12);--legacy-muted:#a9b4c0;--legacy-panel:rgba(255,255,255,.045)}}
body.legacy-public{{margin:0;background:radial-gradient(circle at 18% 0%,rgba(213,168,91,.12),transparent 34%),linear-gradient(180deg,#05070a,#0b1016 46%,#05070a);color:#f6f0e7;font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif}}
.legacy-top{{position:sticky;top:0;z-index:50;background:rgba(5,7,10,.82);backdrop-filter:blur(18px);border-bottom:1px solid var(--legacy-line)}}
.legacy-nav{{max-width:1220px;margin:auto;padding:14px 24px;display:flex;align-items:center;justify-content:space-between;gap:18px}}
.legacy-brand{{display:flex;gap:12px;align-items:center;text-decoration:none;color:#fff;font-weight:950;letter-spacing:.06em;text-transform:uppercase;font-size:13px}}
.legacy-brand img{{width:38px;height:38px;object-fit:contain}}
.legacy-links{{display:flex;gap:12px;align-items:center;flex-wrap:wrap}}
.legacy-links a{{color:#d8e3de;text-decoration:none;border:1px solid rgba(255,255,255,.10);border-radius:999px;padding:8px 11px;font-weight:850;font-size:13px;background:rgba(255,255,255,.035)}}
.legacy-links a:hover{{color:#fff;border-color:rgba(213,168,91,.5)}}
.legacy-shell{{width:min(1180px,calc(100% - 32px));margin:0 auto;padding:34px 0 84px}}
.legacy-hero{{border:1px solid var(--legacy-line);border-radius:34px;background:linear-gradient(135deg,rgba(255,255,255,.085),rgba(255,255,255,.03));padding:clamp(24px,5vw,54px);box-shadow:0 24px 90px rgba(0,0,0,.32);margin:20px 0 28px;position:relative;overflow:hidden}}
.legacy-hero:before{{content:"";position:absolute;inset:0;background:radial-gradient(circle at 82% 8%,rgba(215,246,111,.11),transparent 26%),radial-gradient(circle at 4% 95%,rgba(120,215,255,.12),transparent 28%);pointer-events:none}}
.legacy-hero>*{{position:relative}}
.legacy-eyebrow{{display:inline-flex;border:1px solid rgba(213,168,91,.50);border-radius:999px;padding:8px 12px;color:#f0c477;text-transform:uppercase;letter-spacing:.14em;font-size:12px;font-weight:950;background:rgba(0,0,0,.18)}}
.legacy-hero h1{{font-size:clamp(42px,7vw,88px);letter-spacing:-.07em;line-height:.92;margin:18px 0 16px}}
.legacy-hero p{{font-size:clamp(17px,2vw,22px);color:#d8e0e8;line-height:1.5;max-width:850px;margin:0}}
.legacy-toolbar{{display:flex;gap:10px;flex-wrap:wrap;margin:22px 0}}
.legacy-input,.legacy-select{{border:1px solid var(--legacy-line);border-radius:999px;background:rgba(255,255,255,.055);color:#fff;padding:12px 14px;font-weight:850;outline:0}}
.legacy-input{{min-width:min(460px,100%);flex:1}}
.legacy-select option{{color:#111}}
.legacy-grid{{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}}
.legacy-card{{display:flex;flex-direction:column;gap:10px;text-decoration:none;color:#fff;border:1px solid var(--legacy-line);border-radius:26px;background:rgba(255,255,255,.045);padding:20px;min-height:220px;box-shadow:0 18px 60px rgba(0,0,0,.18)}}
.legacy-card:hover{{transform:translateY(-2px);border-color:rgba(213,168,91,.5)}}
.legacy-meta{{color:#f0c477;font-size:12px;text-transform:uppercase;letter-spacing:.12em;font-weight:950}}
.legacy-card h2,.legacy-card h3{{margin:0;font-size:clamp(22px,2.4vw,31px);line-height:1.04;letter-spacing:-.045em}}
.legacy-card p{{color:#b9c4cf;line-height:1.55;margin:0}}
.legacy-pillrow{{display:flex;gap:6px;flex-wrap:wrap;margin-top:auto}}
.legacy-pill{{font-size:11px;font-weight:900;color:#e8f0ed;border:1px solid rgba(255,255,255,.12);border-radius:999px;padding:5px 8px;background:rgba(255,255,255,.04)}}
.legacy-article{{display:grid;grid-template-columns:minmax(0,780px) 280px;gap:44px;align-items:start}}
.legacy-content{{border:1px solid var(--legacy-line);border-radius:30px;background:rgba(255,255,255,.045);padding:clamp(22px,4vw,44px);box-shadow:0 24px 80px rgba(0,0,0,.24)}}
.legacy-content h1{{font-size:clamp(38px,7vw,74px);line-height:.94;letter-spacing:-.065em;margin:12px 0 18px}}
.legacy-lead{{font-size:20px;line-height:1.55;color:#d7e0de;margin:0 0 28px}}
.legacy-body{{font-family:Georgia,'Times New Roman',serif;font-size:19px;line-height:1.82;color:#f0eadf}}
.legacy-body p{{margin:0 0 1.25em}}.legacy-body h2,.legacy-body h3{{font-family:Inter,system-ui,sans-serif;color:#fff;letter-spacing:-.04em;line-height:1.08;margin:1.7em 0 .65em}}.legacy-body h2{{font-size:36px}}.legacy-body h3{{font-size:28px}}.legacy-body a{{color:#9dd2ff}}.legacy-body blockquote{{border-left:4px solid #d7f66f;background:rgba(215,246,111,.07);margin:24px 0;padding:18px;border-radius:0 18px 18px 0}}
.legacy-side{{position:sticky;top:96px;border:1px solid var(--legacy-line);border-radius:24px;background:rgba(255,255,255,.045);padding:20px;color:#d8e3de}}
.legacy-side h3{{margin:0 0 10px}}.legacy-side p{{color:#a9b4c0;line-height:1.5}}.legacy-side a{{display:block;color:#fff;text-decoration:none;border-top:1px solid var(--legacy-line);padding:12px 0}}
.media-deferred{{border:1px dashed rgba(213,168,91,.42);border-radius:20px;background:rgba(213,168,91,.07);padding:14px 16px;color:#f7dfb1;font-family:Inter,system-ui,sans-serif;font-size:15px;line-height:1.5;margin:22px 0}}
.legacy-table{{width:100%;border-collapse:collapse;border:1px solid var(--legacy-line);border-radius:18px;overflow:hidden}}.legacy-table th,.legacy-table td{{border-bottom:1px solid var(--legacy-line);padding:10px;text-align:left}}.legacy-footer{{max-width:1180px;margin:0 auto;padding:28px 20px 56px;color:#7f8b96;border-top:1px solid var(--legacy-line)}}
@media(max-width:980px){{.legacy-grid{{grid-template-columns:repeat(2,minmax(0,1fr))}}.legacy-article{{grid-template-columns:1fr}}.legacy-side{{position:relative;top:0}}}}
@media(max-width:700px){{.legacy-nav{{align-items:flex-start;flex-direction:column}}.legacy-links{{width:100%;overflow:auto;flex-wrap:nowrap;padding-bottom:3px}}.legacy-links a{{white-space:nowrap}}.legacy-grid{{grid-template-columns:1fr}}.legacy-body{{font-size:18px}}}}
{extra_css}
</style>
</head><body class="legacy-public">'''


def top_nav(prefix=''):
    return f'''<header class="legacy-top"><nav class="legacy-nav"><a class="legacy-brand" href="{prefix}index.html"><img alt="SOV" src="{prefix}assets/sov-logo.png"/>SOV Velebit</a><div class="legacy-links"><a href="{prefix}index.html">Naslovnica</a><a href="{prefix}vijesti.html">Arhiva novosti</a><a href="{prefix}o-drustvu.html">O društvu</a><a href="{prefix}speleoskola.html">Speleoškola</a><a href="{prefix}legacy-wordpress/index.html">Stari web</a><a href="{prefix}dashboard.html">Članski ulaz</a></div></nav></header>'''


def page_end(prefix=''):
    return f'''<footer class="legacy-footer">Speleološki odsjek PDS Velebit · text-only migracija starog WordPress sadržaja</footer><script defer src="{prefix}assets/sov-version.js?v={BUILD_VERSION}"></script></body></html>'''


def article_page(post, body_html, had_media, prefix='../'):
    media_note = '<div class="media-deferred">Fotografije, galerije i ostali mediji iz ove objave nisu uključeni u ovu text-only fazu. Evidentirani su u media manifestu i bit će dodani nakon rješavanja storagea/servera.</div>' if had_media or post.get('media_count') else ''
    tags = ''.join(f'<span class="legacy-pill">{html_escape(t)}</span>' for t in (post.get('tags') or [])[:8])
    cats = ', '.join(post.get('categories') or ['Novosti'])
    body = body_html or (f'<p>{html_escape(post.get("excerpt") or "Sadržaj ove objave nije imao tekstualni dio u exportu.")}</p>')
    return base_head(f'{post["title"]} — SOV arhiva', prefix) + top_nav(prefix) + f'''
<main class="legacy-shell legacy-article">
  <article class="legacy-content">
    <a class="legacy-eyebrow" href="{prefix}vijesti.html">← Arhiva novosti</a>
    <div class="legacy-meta" style="margin-top:18px">{html_escape(cats)} · {html_escape(post.get('date',''))}</div>
    <h1>{html_escape(post['title'])}</h1>
    {f'<p class="legacy-lead">{html_escape(post.get("excerpt"))}</p>' if post.get('excerpt') else ''}
    {media_note}
    <div class="legacy-body">{body}</div>
  </article>
  <aside class="legacy-side"><h3>SOV arhiva</h3><p>Tekst je prenesen iz WordPress XML exporta. Slike nisu hotlinkane ni uključene u ovaj build.</p><a href="{prefix}vijesti.html">Sve objave</a><a href="{prefix}legacy-wordpress/index.html">Stranice starog weba</a><a href="{prefix}speleoskola.html">Speleoškola</a><a href="{prefix}o-drustvu.html">O društvu</a>{('<div class="legacy-pillrow" style="margin-top:14px">'+tags+'</div>') if tags else ''}</aside>
</main>''' + page_end(prefix)


def page_article(page, body_html, had_media, prefix='../../'):
    media_note = '<div class="media-deferred">Fotografije/mediji s ove stare stranice nisu uključeni u text-only fazu. Evidentirani su za kasniji media import.</div>' if had_media or page.get('media_count') else ''
    body = body_html or (f'<p>{html_escape(page.get("excerpt") or "Sadržaj stranice nije imao tekstualni dio u exportu.")}</p>')
    return base_head(f'{page["title"]} — stari SOV web', prefix) + top_nav(prefix) + f'''
<main class="legacy-shell legacy-article">
  <article class="legacy-content">
    <a class="legacy-eyebrow" href="{prefix}legacy-wordpress/index.html">← Stranice starog weba</a>
    <div class="legacy-meta" style="margin-top:18px">Stara WordPress stranica · {html_escape(page.get('date',''))}</div>
    <h1>{html_escape(page['title'])}</h1>
    {f'<p class="legacy-lead">{html_escape(page.get("excerpt"))}</p>' if page.get('excerpt') else ''}
    {media_note}
    <div class="legacy-body">{body}</div>
  </article>
  <aside class="legacy-side"><h3>Stari web</h3><p>Ovo je tekstualna migracija statične WordPress stranice.</p><a href="{prefix}legacy-wordpress/index.html">Sve stare stranice</a><a href="{prefix}vijesti.html">Arhiva novosti</a><a href="{prefix}index.html">Naslovnica</a></aside>
</main>''' + page_end(prefix)


def archive_card(p, prefix=''):
    terms = ''.join(f'<span class="legacy-pill">{html_escape(c)}</span>' for c in (p.get('categories') or [])[:2])
    return f'''<a class="legacy-card" data-title="{html_escape((p['title']+' '+p.get('excerpt','')).lower())}" data-category="{html_escape('|'.join(p.get('categories') or ['Novosti']))}" data-year="{html_escape((p.get('date_iso') or '')[:4])}" href="{prefix}novosti/{p['slug']}.html"><span class="legacy-meta">{html_escape(p.get('category','Novosti'))} · {html_escape(p.get('date',''))}</span><h3>{html_escape(p['title'])}</h3><p>{html_escape(p.get('excerpt',''))}</p><span class="legacy-pillrow">{terms}</span></a>'''


def generate_index(posts):
    latest=posts[:9]
    cards='\n'.join(archive_card(p, '') for p in latest)
    html_doc = base_head('Speleološki odsjek PDS Velebit — Novosti') + top_nav('') + f'''
<main class="legacy-shell">
  <section class="legacy-hero">
    <span class="legacy-eyebrow">SOV · text-only arhiva</span>
    <h1>Iz podzemlja, s terena i iz društva.</h1>
    <p>Najnovije objave, speleoškola, istraživanja, ekspedicije i Velebitaški duh. Stari WordPress sadržaj je u ovoj fazi prenesen tekstualno, bez slika i bez hotlinkanja medija.</p>
    <div class="legacy-toolbar"><a class="legacy-input" style="flex:0;text-decoration:none;color:#10180f;background:linear-gradient(135deg,#d7f66f,#83e6c2);border:0" href="vijesti.html">Otvori kompletnu arhivu novosti →</a><a class="legacy-input" style="flex:0;text-decoration:none;color:#fff" href="legacy-wordpress/index.html">Stranice starog weba</a></div>
  </section>
  <section><div class="section-head"><h2>Najnovije iz arhive</h2><p>Prikaz zadnjih objava iz WordPress exporta. Fotografije dolaze u drugoj fazi migracije.</p></div><div class="legacy-grid">{cards}</div></section>
</main>''' + page_end('')
    (ROOT/'index.html').write_text(html_doc, encoding='utf-8')


def generate_vijesti(posts):
    cats=[]; years=[]
    for p in posts:
        for c in p.get('categories') or ['Novosti']:
            if c not in cats: cats.append(c)
        y=(p.get('date_iso') or '')[:4]
        if y and y not in years: years.append(y)
    cats_opts=''.join(f'<option value="{html_escape(c)}">{html_escape(c)}</option>' for c in cats)
    year_opts=''.join(f'<option value="{html_escape(y)}">{html_escape(y)}</option>' for y in sorted(years, reverse=True))
    cards='\n'.join(archive_card(p,'') for p in posts)
    script='''<script>
(function(){
 const q=document.getElementById('q'), cat=document.getElementById('cat'), year=document.getElementById('year'), cards=[...document.querySelectorAll('.legacy-card')], count=document.getElementById('count');
 function f(){const qq=(q.value||'').toLowerCase().trim(), cc=cat.value, yy=year.value;let n=0;cards.forEach(c=>{const ok=(!qq||c.dataset.title.includes(qq))&&(!cc||c.dataset.category.split('|').includes(cc))&&(!yy||c.dataset.year===yy);c.style.display=ok?'flex':'none'; if(ok)n++;}); count.textContent=n+' objava';}
 [q,cat,year].forEach(x=>x.addEventListener('input',f)); f();
})();</script>'''
    html_doc=base_head('SOV — Arhiva novosti')+top_nav('')+f'''
<main class="legacy-shell">
<section class="legacy-hero"><span class="legacy-eyebrow">WordPress export · 198 objava</span><h1>Arhiva novosti</h1><p>Kompletna tekstualna arhiva objavljenih WordPress postova. Slike i galerije nisu u ovom buildu; pripremljen je samo manifest za kasniju media fazu.</p></section>
<div class="legacy-toolbar"><input id="q" class="legacy-input" placeholder="Pretraži naslov ili tekst..."/><select id="cat" class="legacy-select"><option value="">Sve kategorije</option>{cats_opts}</select><select id="year" class="legacy-select"><option value="">Sve godine</option>{year_opts}</select><span id="count" class="legacy-select" style="border-radius:999px"></span></div>
<div class="legacy-grid">{cards}</div>
</main>{script}'''+page_end('')
    (ROOT/'vijesti.html').write_text(html_doc, encoding='utf-8')


def generate_legacy_pages_index(pages, posts, media_manifest):
    rows='\n'.join(f'''<a class="legacy-card" href="stranice/{p['slug']}.html"><span class="legacy-meta">Stranica · {html_escape(p.get('date',''))}</span><h3>{html_escape(p['title'])}</h3><p>{html_escape(p.get('excerpt',''))}</p></a>''' for p in pages)
    html_doc=base_head('SOV — Stari WordPress web', '../')+top_nav('../')+f'''
<main class="legacy-shell">
<section class="legacy-hero"><span class="legacy-eyebrow">Migracija starog weba</span><h1>Stari WordPress sadržaj</h1><p>Ovdje su statične stranice iz exporta. Objave su u arhivi novosti. Media nije uključen u ovaj build kako ne bismo gurali gigabajte slika kroz Git/Vercel.</p></section>
<section class="legacy-grid">
<a class="legacy-card" href="../vijesti.html"><span class="legacy-meta">Objave</span><h3>{len(posts)} tekstualno migriranih objava</h3><p>Novosti, istraživanja, speleoškola, putopisi i ostali tekstovi iz WordPress exporta.</p></a>
<a class="legacy-card" href="../data/legacy-wordpress-media-manifest.json"><span class="legacy-meta">Media manifest</span><h3>{len(media_manifest.get('media_urls', []))} media referenci</h3><p>Slike i privici su evidentirani, ali nisu uključeni u build.</p></a>
<a class="legacy-card" href="../data/legacy-wordpress-content.json"><span class="legacy-meta">Content index</span><h3>JSON indeks</h3><p>Strukturirani pregled uvezenih postova i stranica.</p></a>
</section>
<h2 style="margin:34px 0 18px;font-size:42px;letter-spacing:-.04em">Stare WordPress stranice</h2>
<div class="legacy-grid">{rows}</div>
</main>'''+page_end('../')
    out=ROOT/'legacy-wordpress'/'index.html'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(html_doc, encoding='utf-8')


def proposed_media_path(url, post_id=''):
    u=html.unescape(url).split('?')[0]
    parsed=urlparse(u)
    path=unquote(parsed.path)
    m=re.search(r'/wp-content/uploads/(\d{4})/(\d{2})/([^/]+)$', path)
    ext=Path(path).suffix.lower() or '.bin'
    stem=Path(path).stem
    if m:
        year, month, name=m.group(1),m.group(2),m.group(3)
    else:
        year, month = 'unknown','unknown'
    digest=hashlib.sha1(url.encode()).hexdigest()[:10]
    short=slugify(stem, 'media')[:55].strip('-')
    if post_id:
        fname=f'{post_id}-{short}-{digest}{ext}'
    else:
        fname=f'{short}-{digest}{ext}'
    return f'legacy-wordpress/{year}/{month}/{fname}'


def main():
    items, attachments = parse_wxr()
    posts=[x for x in items if x['type']=='post' and x['status']=='publish']
    pages=[x for x in items if x['type']=='page' and x['status']=='publish']
    posts.sort(key=lambda x:x.get('date_iso') or '', reverse=True)
    pages.sort(key=lambda x:x.get('date_iso') or '')
    link_map_root=build_link_map(posts,pages)
    # Need link maps relative from article pages
    link_map_article={k:('../'+v if not v.startswith('../') else v) for k,v in link_map_root.items()}
    link_map_page={k:('../../'+v if not v.startswith('../') else v) for k,v in link_map_root.items()}

    # Remove old generated/hotlinked artefacts
    for f in (ROOT/'novosti').glob('*.html'):
        f.unlink()
    shutil.rmtree(ROOT/'archive-original-wordpress', ignore_errors=True)
    (ROOT/'novosti').mkdir(exist_ok=True)
    (ROOT/'legacy-wordpress'/'stranice').mkdir(parents=True, exist_ok=True)

    # Build attachment/media manifest
    media_by_item={}
    media_urls=[]
    attach_by_parent={}
    for a in attachments:
        u=clean_url(a.get('attachment_url',''))
        if u:
            attach_by_parent.setdefault(a.get('parent') or '', []).append(a)
            if u not in media_urls: media_urls.append(u)
    for rec in posts+pages:
        urls=collect_media_urls(rec.get('content_raw',''))
        for a in attach_by_parent.get(rec['id'],[]):
            if a.get('attachment_url') and a['attachment_url'] not in urls:
                urls.append(a['attachment_url'])
        media_by_item[rec['id']]=urls
        for u in urls:
            if u not in media_urls: media_urls.append(u)

    media_manifest={
        'build': BUILD_FILE,
        'source_export': XML.name,
        'strategy': 'text-only build; media not included; URLs are for later storage migration only',
        'counts': {'published_posts':len(posts),'published_pages':len(pages),'attachments':len(attachments),'unique_media_urls':len(media_urls)},
        'media_urls': [{'url':u, 'proposed_path': proposed_media_path(u)} for u in media_urls],
        'by_item': {}
    }

    content_index={'build':BUILD_FILE,'source_export':XML.name,'text_only':True,'posts':[],'pages':[],'counts':{}}

    # Generate post pages
    for p in posts:
        p['media_count']=len(media_by_item.get(p['id'],[]))
        body, had_media = sanitize_content(p.get('content_raw',''), link_map_article)
        (ROOT/'novosti'/f'{p["slug"]}.html').write_text(article_page(p, body, had_media, '../'), encoding='utf-8')
        content_index['posts'].append({k:p.get(k) for k in ['id','title','slug','link','date_iso','date','category','categories','tags','excerpt','creator']})
        content_index['posts'][-1]['url']=f'novosti/{p["slug"]}.html'
        content_index['posts'][-1]['media_count']=p['media_count']
        media_manifest['by_item'][p['id']]={'type':'post','title':p['title'],'slug':p['slug'],'url':f'novosti/{p["slug"]}.html','media_urls':media_by_item.get(p['id'],[])}
    # Generate static WP pages under legacy-wordpress/stranice
    for pg in pages:
        pg['media_count']=len(media_by_item.get(pg['id'],[]))
        body, had_media = sanitize_content(pg.get('content_raw',''), link_map_page)
        out=ROOT/'legacy-wordpress'/'stranice'/f'{pg["slug"]}.html'
        out.write_text(page_article(pg, body, had_media, '../../'), encoding='utf-8')
        content_index['pages'].append({k:pg.get(k) for k in ['id','title','slug','link','date_iso','date','excerpt','creator']})
        content_index['pages'][-1]['url']=f'legacy-wordpress/stranice/{pg["slug"]}.html'
        content_index['pages'][-1]['public_equivalent']=public_page_target(pg)
        content_index['pages'][-1]['media_count']=pg['media_count']
        media_manifest['by_item'][pg['id']]={'type':'page','title':pg['title'],'slug':pg['slug'],'url':f'legacy-wordpress/stranice/{pg["slug"]}.html','media_urls':media_by_item.get(pg['id'],[])}

    # News JSON with no image urls (legacy text-only)
    news_json=[]
    for p in posts:
        news_json.append({
            'title':p['title'], 'slug':p['slug'], 'date_iso':p['date_iso'], 'date':p['date'], 'desc':p['excerpt'],
            'image':'', 'category':p.get('category','Novosti'), 'url':f'novosti/{p["slug"]}.html', 'text_only':True,
            'media_deferred_count':len(media_by_item.get(p['id'],[])), 'wp_id':p['id']
        })
    (ROOT/'data'/'news.json').write_text(json.dumps(news_json, ensure_ascii=False, indent=2), encoding='utf-8')
    content_index['counts']={'published_posts':len(posts),'published_pages':len(pages),'attachments_in_export':len(attachments),'unique_media_urls_manifested':len(media_urls)}
    (ROOT/'data'/'legacy-wordpress-content.json').write_text(json.dumps(content_index, ensure_ascii=False, indent=2), encoding='utf-8')
    (ROOT/'data'/'legacy-wordpress-media-manifest.json').write_text(json.dumps(media_manifest, ensure_ascii=False, indent=2), encoding='utf-8')

    generate_index(posts)
    generate_vijesti(posts)
    generate_legacy_pages_index(pages, posts, media_manifest)

    # Clean known public pages with old WP image refs without redesigning them fully:
    # Remove image gallery blocks and WP background URLs. Keep text.
    for name in ['velebitaski-duh.html','povijest.html','speleoskola.html']:
        pth=ROOT/name
        if not pth.exists(): continue
        txt=pth.read_text(encoding='utf-8', errors='ignore')
        # Remove img tags with old WP/srcs and background-image WP URLs
        txt=re.sub(r'<img\b[^>]*(?:sovelebit\.wordpress\.com|i\d+\.wp\.com)[^>]*>', '', txt, flags=re.I)
        txt=re.sub(r'background-image\s*:\s*url\(["\']?https?://(?:i\d+\.wp\.com/)?sovelebit\.wordpress\.com/[^\)]+\)', 'background-image:none', txt, flags=re.I)
        txt=re.sub(r'url\(["\']?https?://(?:i\d+\.wp\.com/)?sovelebit\.wordpress\.com/wp-content/uploads/[^\)]+\)', 'none', txt, flags=re.I)
        # remove empty gallery divs/sections maybe
        txt=txt.replace('<h2>Maliganove ilustracije</h2><div class="gallery"></div>', '<h2>Maliganove ilustracije</h2><p class="media-deferred">Ilustracije su evidentirane za iduću media fazu i nisu uključene u text-only build.</p>')
        txt=txt.replace('<section class="photo-grid"></section>', '<section class="photo-grid"><p>Fotografije povijesti evidentirane su za iduću media fazu.</p></section>')
        pth.write_text(txt, encoding='utf-8')
    # CSS WP background URL -> gradient only
    css=ROOT/'assets'/'sov-form-v603.css'
    if css.exists():
        txt=css.read_text(encoding='utf-8', errors='ignore')
        txt=re.sub(r',url\(["\']?https?://(?:i\d+\.wp\.com/)?sovelebit\.wordpress\.com/wp-content/uploads/[^\)]+\) center/cover', '', txt, flags=re.I)
        txt=re.sub(r'url\(["\']?https?://(?:i\d+\.wp\.com/)?sovelebit\.wordpress\.com/wp-content/uploads/[^\)]+\)', 'none', txt, flags=re.I)
        css.write_text(txt, encoding='utf-8')

    # Version files
    (ROOT/'VERSION.txt').write_text(BUILD_VERSION+'\n', encoding='utf-8')
    (ROOT/'BUILD_VERSION.txt').write_text(BUILD_FILE+'\n', encoding='utf-8')
    update={
        'app':'SOV web','version':BUILD_VERSION,'versionName':BUILD_NAME,'build':BUILD_FILE,
        'createdAt':datetime.now(timezone.utc).isoformat().replace('+00:00','Z'),'cacheBust':CACHE_BUST,
        'base':'sov-web-build-v6.1.43e-smart-location-future-trips.zip','requiresSql':False,'requiredSql':[],
        'releaseType':'legacy-wordpress-text-only-import',
        'changedFiles':['index.html','vijesti.html','novosti/*.html','legacy-wordpress/index.html','legacy-wordpress/stranice/*.html','data/news.json','data/legacy-wordpress-content.json','data/legacy-wordpress-media-manifest.json','tools/sov_wordpress_wxr_text_importer.py','VERSION.txt','BUILD_VERSION.txt','update.json'],
        'notes':['Imported published WordPress posts and pages from WXR/XML export as text-only content.','No legacy images, galleries, attachments or media binaries are included in the build.','Legacy media references are recorded in data/legacy-wordpress-media-manifest.json for a later storage/server phase.','Public news archive now uses generated static HTML pages and JSON index.','No Supabase SQL, roles, Izleti, Oružarstvo, Arhivar, Karta or APK compatibility changes.']
    }
    (ROOT/'update.json').write_text(json.dumps(update, ensure_ascii=False, indent=2), encoding='utf-8')
    # Update version helper carefully
    sv=ROOT/'assets'/'sov-version.js'
    if sv.exists():
        txt=sv.read_text(encoding='utf-8')
        txt=re.sub(r"const FALLBACK_VERSION='[^']+'", f"const FALLBACK_VERSION='{BUILD_VERSION}'", txt)
        txt=re.sub(r"const FALLBACK_CACHE='[^']+'", f"const FALLBACK_CACHE='{CACHE_BUST}'", txt)
        txt=re.sub(r"const FALLBACK_BUILD='[^']+'", f"const FALLBACK_BUILD='{BUILD_FILE}'", txt)
        txt=re.sub(r"const FALLBACK_NAME='[^']+'", f"const FALLBACK_NAME='{BUILD_NAME}'", txt)
        sv.write_text(txt, encoding='utf-8')
    # Save importer script for reproducibility
    tools=ROOT/'tools'; tools.mkdir(exist_ok=True)
    shutil.copy2(Path(__file__), tools/'sov_wordpress_wxr_text_importer.py')
    notes=f'''# SOV web v{BUILD_VERSION} — WordPress WXR text-only import

Baseline: `sov-web-build-v6.1.43e-smart-location-future-trips.zip`
Source: `sovelebit.wordpress.com.2026-06-24.000.xml`

## Što je napravljeno

- Uvezeno je {len(posts)} objavljenih WordPress postova kao text-only stranice u `novosti/`.
- Uvezeno je {len(pages)} objavljenih WordPress stranica kao text-only stranice u `legacy-wordpress/stranice/`.
- Generiran je novi `vijesti.html` s pretragom, filterom kategorije i filterom godine.
- Generiran je novi `index.html` s najnovijim objavama iz XML exporta.
- Generiran je `data/news.json` bez image URL-ova.
- Generiran je `data/legacy-wordpress-content.json` kao strukturirani indeks.
- Generiran je `data/legacy-wordpress-media-manifest.json` s {len(media_urls)} media referenci za kasniju fazu.

## Što NIJE napravljeno

- Nisu preuzete slike, galerije, PDF-ovi ni ostali media fajlovi.
- Nema ubacivanja media foldera u Git.
- Nema SQL promjena.
- Nisu dirani Izleti, Oružarstvo, Arhivar, Karta, role, login ni APK kompatibilnost.

## Zašto ovako

Prethodni full media import je proizveo preko 2 GiB Git pack i tisuće binarnih fajlova. Ovaj build namjerno prenosi samo tekst i strukturu, a media ostavlja za fazu storage/servera.

## Kasnija media faza

Koristiti `data/legacy-wordpress-media-manifest.json` kao source-of-truth za skidanje, optimizaciju i upload slika na SOV server / R2 / drugi storage.
'''
    (ROOT/f'BUILD_NOTES_v{BUILD_VERSION}_LEGACY_WORDPRESS_WXR_TEXT_ONLY.md').write_text(notes, encoding='utf-8')
    print(json.dumps(content_index['counts'], ensure_ascii=False, indent=2))

if __name__=='__main__':
    main()
