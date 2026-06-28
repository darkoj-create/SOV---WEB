#!/usr/bin/env python3
from __future__ import annotations
import re, json, html, shutil, hashlib
import xml.etree.ElementTree as ET
from pathlib import Path
from datetime import datetime, timezone
from urllib.parse import urlparse, unquote
from bs4 import BeautifulSoup, Comment

ROOT = Path('/mnt/data/sov_6144c_work')
XML = Path('/mnt/data/wp_export_c/sovelebit.wordpress.com-2026-06-24-16_13_45/sovelebit.wordpress.com.2026-06-24.000.xml')
BUILD_VERSION = '6.1.44c'
BUILD_SLUG = 'wordpress-text-import-preserve-current-public'
BUILD_FILE = f'sov-web-build-v{BUILD_VERSION}-{BUILD_SLUG}'
CACHE_BUST = '6144c-wxr-text-preserve-current'

NS = {
    'wp':'http://wordpress.org/export/1.2/',
    'content':'http://purl.org/rss/1.0/modules/content/',
    'excerpt':'http://wordpress.org/export/1.2/excerpt/',
    'dc':'http://purl.org/dc/elements/1.1/',
}
CRO_MAP = str.maketrans({'č':'c','ć':'c','š':'s','ž':'z','đ':'d','Č':'c','Ć':'c','Š':'s','Ž':'z','Đ':'d'})
WP_MEDIA_PAT = re.compile(r'https?://(?:i\d+\.wp\.com/)?sovelebit\.wordpress\.com/wp-content/uploads/[^\s"\'<>)]+', re.I)


def slugify(s: str, fallback='item') -> str:
    s = html.unescape(s or '').translate(CRO_MAP).lower()
    s = re.sub(r'[^a-z0-9]+', '-', s).strip('-')
    return (s[:96].strip('-') or fallback)

def get_ns(el, path, default=''):
    x = el.find(path, NS)
    return x.text if x is not None and x.text is not None else default

def get(el, tag, default=''):
    x = el.find(tag)
    return x.text if x is not None and x.text is not None else default

def norm_text(s: str) -> str:
    return re.sub(r'\s+', ' ', html.unescape(s or '')).strip()

def parse_date(value: str) -> str:
    value=(value or '').strip()
    if not value: return ''
    for fmt in ('%Y-%m-%d %H:%M:%S','%a, %d %b %Y %H:%M:%S %z','%a, %d %b %Y %H:%M:%S %Z'):
        try:
            dt=datetime.strptime(value, fmt)
            if dt.tzinfo is None: dt=dt.replace(tzinfo=timezone.utc)
            return dt.isoformat()
        except Exception: pass
    try: return datetime.fromisoformat(value).isoformat()
    except Exception: return value

def hr_date(iso: str) -> str:
    try: return datetime.fromisoformat(iso.replace('Z','+00:00')).strftime('%d.%m.%Y.')
    except Exception: return ''

def excerpt_from(raw_html: str, excerpt: str='', limit=220) -> str:
    raw=excerpt or raw_html or ''
    soup=BeautifulSoup(raw, 'html.parser')
    for t in soup.find_all(['script','style','img','figure','iframe','video','audio','source']): t.decompose()
    txt=norm_text(soup.get_text(' '))
    if len(txt)>limit: txt=txt[:limit].rsplit(' ',1)[0]+'…'
    return txt

def item_terms(item):
    cats=[]; tags=[]
    for c in item.findall('category'):
        domain=c.attrib.get('domain',''); nicename=c.attrib.get('nicename','')
        val=norm_text(c.text or nicename)
        if not val: continue
        if domain=='category': cats.append(val)
        elif domain=='post_tag': tags.append(val)
    def uniq(xs):
        out=[]
        for x in xs:
            if x not in out: out.append(x)
        return out
    return uniq(cats), uniq(tags)

def parse_wxr():
    tree=ET.parse(XML); chan=tree.getroot().find('channel')
    items=[]; attachments=[]
    for item in chan.findall('item'):
        post_type=get_ns(item,'wp:post_type')
        status=get_ns(item,'wp:status')
        post_id=get_ns(item,'wp:post_id')
        title=norm_text(get(item,'title')) or '(bez naslova)'
        link=(get(item,'link') or '').strip()
        slug=slugify(get_ns(item,'wp:post_name') or title, fallback=f'wp-{post_id}')
        raw=get_ns(item,'content:encoded')
        exc=get_ns(item,'excerpt:encoded')
        date_iso=parse_date(get_ns(item,'wp:post_date') or get(item,'pubDate'))
        cats,tags=item_terms(item)
        rec={'id':post_id,'type':post_type,'status':status,'title':title,'slug':slug,'link':link,
             'date_iso':date_iso,'date':hr_date(date_iso),'categories':cats,'tags':tags,
             'category':cats[0] if cats else 'Novosti','creator':get_ns(item,'dc:creator'),
             'content_raw':raw,'excerpt_raw':exc,'excerpt':excerpt_from(raw, exc),
             'parent':get_ns(item,'wp:post_parent'), 'attachment_url':get_ns(item,'wp:attachment_url')}
        if post_type=='attachment': attachments.append(rec)
        items.append(rec)
    return items, attachments

def collect_media_urls(raw_html: str) -> list[str]:
    urls=[]
    for m in WP_MEDIA_PAT.findall(raw_html or ''):
        u=html.unescape(m).rstrip('.,;')
        if u not in urls: urls.append(u)
    try:
        soup=BeautifulSoup(raw_html or '', 'html.parser')
        for tag in soup.find_all(['img','a','source','video','iframe','embed']):
            for attr in ['src','href','data-orig-file','data-large-file','data-medium-file','poster']:
                u=tag.get(attr)
                if u and ('wp-content/uploads' in u or 'i0.wp.com/sovelebit.wordpress.com' in u):
                    u=html.unescape(u)
                    if u not in urls: urls.append(u)
            srcset=tag.get('srcset') or tag.get('data-srcset')
            if srcset:
                for part in srcset.split(','):
                    u=part.strip().split(' ')[0]
                    if 'wp-content/uploads' in u and u not in urls: urls.append(html.unescape(u))
    except Exception:
        pass
    return urls

def build_link_map(posts):
    out={}
    for p in posts:
        for key in [p['link'], p['link'].rstrip('/')+'/' if p.get('link') else '']:
            if key: out[key]=f'../novosti/{p["slug"]}.html'
    # known static pages already in the current public site
    known = {
        'https://sovelebit.wordpress.com/o-nama/':'../o-drustvu.html',
        'https://sovelebit.wordpress.com/povijest/':'../povijest.html',
        'https://sovelebit.wordpress.com/speleoskola/':'../speleoskola.html',
        'https://sovelebit.wordpress.com/velebitaski-duh/':'../velebitaski-duh.html',
        'https://sovelebit.wordpress.com/procelnistvo/':'../procelnistvo.html',
        'https://sovelebit.wordpress.com/velebiten/':'../velebiten.html',
        'https://sovelebit.wordpress.com/kontakt/':'../pridruzi-nam-se.html',
    }
    out.update(known)
    return out

def sanitize_content(raw_html: str, link_map: dict[str,str]) -> str:
    raw_html = re.sub(r'<!--\s*/?wp:.*?-->', '', raw_html or '', flags=re.S)
    soup=BeautifulSoup(raw_html, 'html.parser')
    for c in soup.find_all(string=lambda s:isinstance(s, Comment)): c.extract()
    for tag in soup.find_all(['script','style','noscript']): tag.decompose()
    # remove media silently for now (media phase later)
    for tag in soup.find_all(['img','picture','source','video','audio','iframe','embed','object']): tag.decompose()
    for tag in list(soup.find_all('figure')):
        # keep textual captions only when meaningful
        txt=norm_text(tag.get_text(' '))
        if txt and len(txt)>8:
            p=soup.new_tag('p'); p.string=txt; tag.replace_with(p)
        else:
            tag.decompose()
    allowed={'p','br','hr','h1','h2','h3','h4','h5','h6','ul','ol','li','blockquote','pre','code','strong','b','em','i','a','table','thead','tbody','tr','th','td'}
    for tag in list(soup.find_all(True)):
        if tag.name not in allowed:
            tag.unwrap(); continue
        for attr in list(tag.attrs.keys()):
            if not (tag.name=='a' and attr in {'href','title','target','rel'}): del tag.attrs[attr]
    for a in soup.find_all('a'):
        href=html.unescape(a.get('href','')).strip()
        if not href: a.unwrap(); continue
        if 'wp-content/uploads' in href or 'i0.wp.com/sovelebit.wordpress.com' in href:
            a.unwrap(); continue
        if 'sovelebit.wordpress.com' in href:
            key=href.rstrip('/')+'/'
            if key in link_map: a['href']=link_map[key]
            else: a.unwrap()
        elif href.startswith('#'):
            pass
        else:
            a['target']='_blank'; a['rel']='noopener'
    # remove empty paragraphs/spans/etc.
    for tag in list(soup.find_all(['p','div','span','strong','em','i','b'])):
        if not norm_text(tag.get_text(' ')) and not tag.find(['a','ul','ol','table']): tag.decompose()
    body=''.join(str(x) for x in soup.contents).strip()
    body=re.sub(r'\n{3,}','\n\n',body)
    return body or '<p>Tekst ove objave nije imao zaseban tekstualni dio u WordPress exportu.</p>'

def esc(s): return html.escape(str(s or ''), quote=True)

def article_html(post, body):
    tags=''.join(f'<span>{esc(t)}</span>' for t in (post.get('tags') or [])[:8])
    cats=', '.join(post.get('categories') or ['Novosti'])
    return f'''<!DOCTYPE html>
<html lang="hr"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>{esc(post['title'])} — SOV Novosti</title>
<link href="../assets/site.css" rel="stylesheet"/><link href="../assets/mobile.css" rel="stylesheet"/><link href="../assets/sov-logo.png" rel="icon"/>
<link href="../assets/sov-foundation-v55822.css" rel="stylesheet"/><link href="../assets/sov-shell-v55825.css" rel="stylesheet"/><link href="../assets/sov-polish-v55826.css" rel="stylesheet"/><link href="../assets/sov-wow-v6.css" rel="stylesheet"/>
<style>
body{{margin:0;background:radial-gradient(circle at 18% 0%,rgba(213,168,91,.12),transparent 34%),linear-gradient(180deg,#05070a,#0b1016 46%,#05070a);color:#f6f0e7;font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif}}.topbar{{position:sticky;top:0;z-index:50;background:rgba(5,7,10,.82);backdrop-filter:blur(18px);border-bottom:1px solid rgba(255,255,255,.12)}}.nav{{max-width:1180px;margin:auto;padding:14px 22px;display:flex;align-items:center;justify-content:space-between;gap:16px}}.brand{{display:flex;gap:12px;align-items:center;color:#fff;text-decoration:none;font-weight:950;letter-spacing:.06em;text-transform:uppercase;font-size:13px}}.brand img{{width:36px;height:36px}}.links{{display:flex;gap:14px;align-items:center;flex-wrap:wrap}}.links a{{color:#d8e3de;text-decoration:none;font-size:14px}}.links a:hover{{color:#fff}}.wrap{{max-width:1120px;margin:0 auto;padding:34px 22px 86px}}.article-shell{{display:grid;grid-template-columns:minmax(0,780px) 280px;gap:48px;align-items:start}}.article-card{{border:1px solid rgba(255,255,255,.12);border-radius:34px;background:rgba(255,255,255,.045);padding:clamp(22px,4vw,48px);box-shadow:0 26px 100px rgba(0,0,0,.30)}}.back{{display:inline-flex;margin-bottom:22px;color:#f0c477;text-decoration:none;font-weight:900}}.meta{{color:#f0c477;font-size:12px;text-transform:uppercase;letter-spacing:.13em;font-weight:900;margin-bottom:14px}}h1{{font-size:clamp(40px,7vw,84px);letter-spacing:-.065em;line-height:.93;margin:0 0 18px}}.lead{{font-size:20px;line-height:1.55;color:#d4ddd8;margin:0 0 28px}}.body{{font-family:Georgia,'Times New Roman',serif;font-size:19px;line-height:1.82;color:#f0eadf}}.body p{{margin:0 0 1.25em}}.body h2,.body h3{{font-family:Inter,system-ui,sans-serif;color:#fff;letter-spacing:-.04em;line-height:1.08;margin:1.8em 0 .7em}}.body h2{{font-size:36px}}.body h3{{font-size:28px}}.body a{{color:#9dd2ff}}.body blockquote{{border-left:4px solid #d7f66f;background:rgba(215,246,111,.07);margin:24px 0;padding:18px;border-radius:0 18px 18px 0}}.side{{position:sticky;top:92px;border:1px solid rgba(255,255,255,.12);border-radius:24px;background:rgba(255,255,255,.045);padding:20px}}.side h3{{margin:0 0 10px}}.side p{{color:#a9b4c0;line-height:1.5;font-size:14px}}.side a{{display:block;color:#fff;text-decoration:none;border-top:1px solid rgba(255,255,255,.12);padding:12px 0}}.tagrow{{display:flex;gap:8px;flex-wrap:wrap;margin-top:18px}}.tagrow span{{border:1px solid rgba(255,255,255,.12);border-radius:999px;padding:5px 8px;font-size:12px;color:#d8e3de;background:rgba(255,255,255,.04)}}footer{{max-width:1120px;margin:0 auto;padding:28px 22px 56px;color:#7f8b96;border-top:1px solid rgba(255,255,255,.12)}}@media(max-width:900px){{.article-shell{{grid-template-columns:1fr}}.side{{position:relative;top:0}}.links{{display:none}}h1{{font-size:44px}}.body{{font-size:18px}}}}
</style></head><body><header class="topbar"><nav class="nav"><a class="brand" href="../index.html"><img src="../assets/sov-logo.png" alt="SOV"/>SOV Velebit</a><div class="links"><a href="../index.html">Novosti</a><a href="../vijesti.html">Sve objave</a><a href="../o-drustvu.html">O društvu</a><a href="../speleoskola.html">Speleoškola</a><a href="../dashboard.html">SOV Cloud</a></div></nav></header>
<main class="wrap article-shell"><article class="article-card"><a class="back" href="../vijesti.html">← Sve objave</a><div class="meta">{esc(post.get('category','Novosti'))} · {esc(post.get('date',''))}</div><h1>{esc(post['title'])}</h1>{f'<p class="lead">{esc(post.get("excerpt"))}</p>' if post.get('excerpt') else ''}<div class="body">{body}</div>{f'<div class="tagrow">{tags}</div>' if tags else ''}</article><aside class="side"><h3>SOV Novosti</h3><p>{esc(cats)}<br>{esc(post.get('date',''))}</p><a href="../vijesti.html">Sve objave</a><a href="../index.html">Naslovnica</a><a href="../speleoskola.html">Speleoškola</a></aside></main><footer>Speleološki odsjek PDS Velebit · Novosti</footer><script defer src="../assets/sov-version.js?v={BUILD_VERSION}"></script></body></html>'''

def archive_card(p, existing=False):
    img = p.get('image') or ''
    media = f'<span class="archive-img" style="background-image:url(\'{esc(img)}\')"></span>' if img else '<span class="archive-img no-photo"></span>'
    return f'''<a class="archive-card" data-title="{esc((p['title']+' '+p.get('excerpt','')+' '+p.get('desc','')).lower())}" data-category="{esc('|'.join(p.get('categories') or [p.get('category','Novosti')]))}" data-year="{esc((p.get('date_iso') or '')[:4])}" href="{esc(p.get('url') or 'novosti/'+p['slug']+'.html')}">{media}<span class="archive-copy"><span class="archive-meta">{esc(p.get('category','Novosti'))} · {esc(p.get('date',''))}</span><strong>{esc(p['title'])}</strong><em>{esc(p.get('desc') or p.get('excerpt') or '')}</em></span></a>'''

def generate_vijesti(all_posts, current_cards):
    # current_cards: image-heavy known current posts. all_posts: WP export posts sorted desc.
    slugs_seen=set()
    entries=[]
    for c in current_cards:
        cc=dict(c); cc['categories']=[cc.get('category','Novosti')]; cc['url']=cc.get('url') or f"novosti/{cc['slug']}.html"; cc['excerpt']=cc.get('desc','')
        entries.append(cc); slugs_seen.add(cc['slug'])
    for p in all_posts:
        if p['slug'] in slugs_seen: continue
        pp=dict(p); pp['url']=f"novosti/{p['slug']}.html"; pp['image']=''
        entries.append(pp); slugs_seen.add(pp['slug'])
    cats=[]; years=[]
    for p in entries:
        for c in p.get('categories') or [p.get('category','Novosti')]:
            if c not in cats: cats.append(c)
        y=(p.get('date_iso') or '')[:4]
        if y and y not in years: years.append(y)
    cats_opts=''.join(f'<option value="{esc(c)}">{esc(c)}</option>' for c in cats)
    year_opts=''.join(f'<option value="{esc(y)}">{esc(y)}</option>' for y in sorted(years, reverse=True))
    cards='\n'.join(archive_card(p, p.get('image')) for p in entries)
    doc=f'''<!DOCTYPE html>
<html lang="hr"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width, initial-scale=1"/><title>Sve objave — Speleološki odsjek PDS Velebit</title><link href="assets/site.css" rel="stylesheet"/><link href="assets/mobile.css" rel="stylesheet"/><link href="assets/sov-logo.png" rel="icon"/><link href="assets/sov-foundation-v55822.css" rel="stylesheet"/><link href="assets/sov-shell-v55825.css" rel="stylesheet"/><link href="assets/sov-polish-v55826.css" rel="stylesheet"/><link href="assets/sov-wow-v6.css" rel="stylesheet"/><link href="assets/sov-public-polish-v609.css" rel="stylesheet"/>
<style>
body{{margin:0;background:radial-gradient(circle at 15% 5%,rgba(213,168,91,.14),transparent 32%),linear-gradient(180deg,#05070a,#0b1016 45%,#05070a);color:#f6f0e7;font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif}}.topbar{{position:sticky;top:0;z-index:50;background:rgba(5,7,10,.76);backdrop-filter:blur(18px);border-bottom:1px solid rgba(255,255,255,.12)}}.nav{{max-width:1220px;margin:auto;padding:14px 24px;display:flex;align-items:center;justify-content:space-between;gap:20px}}.brand{{display:flex;gap:12px;align-items:center;text-decoration:none;color:#fff;font-weight:900;letter-spacing:.08em;text-transform:uppercase}}.brand img{{width:38px;height:38px;object-fit:contain}}.navlinks{{display:flex;gap:18px;align-items:center;flex-wrap:wrap}}.navlinks a{{color:#dce3ea;text-decoration:none;font-size:14px}}.navlinks .login{{border:1px solid rgba(213,168,91,.48);color:#f0c477;border-radius:999px;padding:9px 14px}}.hero{{max-width:1220px;margin:0 auto;padding:76px 24px 36px}}.eyebrow{{display:inline-flex;border:1px solid rgba(213,168,91,.52);border-radius:999px;padding:8px 12px;color:#f0c477;text-transform:uppercase;letter-spacing:.16em;font-size:12px;background:rgba(0,0,0,.28)}}h1{{font-size:clamp(48px,9vw,108px);line-height:.88;margin:24px 0 18px;letter-spacing:-.075em;max-width:1060px}}.sub{{font-size:clamp(18px,2vw,23px);color:#d8e0e8;max-width:760px;line-height:1.45}}.archive{{max-width:1220px;margin:0 auto;padding:20px 24px 90px}}.toolbar{{display:flex;gap:10px;flex-wrap:wrap;margin:18px 0 28px}}.toolbar input,.toolbar select,.count{{border:1px solid rgba(255,255,255,.12);border-radius:999px;background:rgba(255,255,255,.055);color:#fff;padding:12px 14px;font-weight:850;outline:0}}.toolbar input{{min-width:min(460px,100%);flex:1}}.toolbar select option{{color:#111}}.archive-grid{{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}}.archive-card{{text-decoration:none;color:#fff;background:rgba(255,255,255,.045);border:1px solid rgba(255,255,255,.12);border-radius:26px;overflow:hidden;display:flex;flex-direction:column;min-height:310px;box-shadow:0 18px 70px rgba(0,0,0,.22);transition:transform .2s ease,border-color .2s ease}}.archive-card:hover{{transform:translateY(-3px);border-color:rgba(213,168,91,.45)}}.archive-img{{height:150px;background-size:cover;background-position:center;background-color:rgba(255,255,255,.06);border-bottom:1px solid rgba(255,255,255,.09)}}.archive-img.no-photo{{background:radial-gradient(circle at 20% 10%,rgba(215,246,111,.14),transparent 30%),linear-gradient(135deg,rgba(255,255,255,.075),rgba(255,255,255,.025))}}.archive-copy{{padding:18px;display:flex;flex-direction:column;gap:10px;flex:1}}.archive-meta{{color:#f0c477;font-size:12px;text-transform:uppercase;letter-spacing:.12em;font-weight:900}}.archive-copy strong{{font-size:clamp(21px,2.2vw,28px);line-height:1.05;letter-spacing:-.045em}}.archive-copy em{{font-style:normal;color:#b9c4cf;line-height:1.52}}.note{{color:#a9b4c0;line-height:1.5;margin:0 0 14px}}footer{{max-width:1220px;margin:0 auto;padding:26px 24px 56px;border-top:1px solid rgba(255,255,255,.12);color:#7f8b96}}@media(max-width:980px){{.archive-grid{{grid-template-columns:repeat(2,minmax(0,1fr))}}}}@media(max-width:700px){{.navlinks{{display:none}}.archive-grid{{grid-template-columns:1fr}}}}
</style></head><body><header class="topbar"><nav class="nav"><a class="brand" href="index.html"><img alt="SOV" src="assets/brand/sov-round-logo.png"/>SOV Velebit</a><div class="navlinks"><a href="index.html">Novosti</a><a href="vijesti.html">Sve objave</a><a href="o-drustvu.html">O nama</a><a href="speleoskola.html">Speleoškola</a><a href="pridruzi-nam-se.html">Pridruži nam se</a><a class="login" href="dashboard.html">Članski ulaz</a></div></nav></header><section class="hero"><span class="eyebrow">Novosti · SOV Velebit</span><h1>Sve objave.</h1><p class="sub">Aktualne objave ostaju s postojećim slikama, a stariji tekstovi iz WordPress exporta dodani su kao čiste tekstualne stranice.</p></section><main class="archive"><div class="toolbar"><input id="q" placeholder="Pretraži naslov ili tekst..."/><select id="cat"><option value="">Sve kategorije</option>{cats_opts}</select><select id="year"><option value="">Sve godine</option>{year_opts}</select><span id="count" class="count"></span></div><p class="note">Slike za starije tekstove dodajemo u drugoj fazi, kad riješimo storage/server. Postojeće aktualne objave i dalje koriste slike koje već imamo u trenutnom webu.</p><div class="archive-grid">{cards}</div></main><footer>Speleološki odsjek PDS Velebit · Zagreb</footer><script>(function(){{const q=document.getElementById('q'),cat=document.getElementById('cat'),year=document.getElementById('year'),cards=[...document.querySelectorAll('.archive-card')],count=document.getElementById('count');function f(){{const qq=(q.value||'').toLowerCase().trim(),cc=cat.value,yy=year.value;let n=0;cards.forEach(c=>{{const ok=(!qq||c.dataset.title.includes(qq))&&(!cc||c.dataset.category.split('|').includes(cc))&&(!yy||c.dataset.year===yy);c.style.display=ok?'flex':'none';if(ok)n++;}});count.textContent=n+' objava';}}[q,cat,year].forEach(x=>x.addEventListener('input',f));f();}})();</script><script defer src="assets/sov-version.js?v={BUILD_VERSION}"></script></body></html>'''
    (ROOT/'vijesti.html').write_text(doc, encoding='utf-8')
    return entries

def proposed_media_path(url, post_id=''):
    u=html.unescape(url).split('?')[0]
    path=unquote(urlparse(u).path)
    m=re.search(r'/wp-content/uploads/(\d{4})/(\d{2})/([^/]+)$', path)
    ext=Path(path).suffix.lower() or '.bin'
    stem=Path(path).stem
    year,month=(m.group(1),m.group(2)) if m else ('unknown','unknown')
    digest=hashlib.sha1(url.encode()).hexdigest()[:10]
    short=slugify(stem,'media')[:55].strip('-')
    fname=f'{post_id+"-" if post_id else ""}{short}-{digest}{ext}'
    return f'legacy-wordpress/{year}/{month}/{fname}'

def patch_index_link():
    p=ROOT/'index.html'
    txt=p.read_text(encoding='utf-8', errors='ignore')
    if 'vijesti.html">Sve objave' in txt:
        return
    # Add link near section head if possible, not changing visible cards/images.
    txt=txt.replace('<div class="section-head"><h2>Najnovije</h2><a class="news-edit-inline"', '<div class="section-head"><h2>Najnovije</h2><a href="vijesti.html" style="color:#f0c477;text-decoration:none;font-weight:950">Sve objave →</a><a class="news-edit-inline"')
    p.write_text(txt, encoding='utf-8')

def main():
    items, attachments=parse_wxr()
    posts=[x for x in items if x['type']=='post' and x['status']=='publish']
    pages=[x for x in items if x['type']=='page' and x['status']=='publish']
    posts.sort(key=lambda x:x.get('date_iso') or '', reverse=True)
    # Existing modern article pages/current homepage stay untouched.
    existing_slugs={p.stem for p in (ROOT/'novosti').glob('*.html')}
    link_map=build_link_map(posts)
    # Media manifest for later only.
    media_urls=[]; media_by_item={}
    attach_by_parent={}
    for a in attachments:
        if a.get('attachment_url'):
            attach_by_parent.setdefault(a.get('parent') or '', []).append(a)
            if a['attachment_url'] not in media_urls: media_urls.append(a['attachment_url'])
    for p in posts+pages:
        urls=collect_media_urls(p.get('content_raw',''))
        for a in attach_by_parent.get(p['id'],[]):
            if a.get('attachment_url') and a['attachment_url'] not in urls: urls.append(a['attachment_url'])
        media_by_item[p['id']]=urls
        for u in urls:
            if u not in media_urls: media_urls.append(u)
    # Generate only missing WordPress post pages.
    (ROOT/'novosti').mkdir(exist_ok=True)
    generated=0; preserved=0
    for p in posts:
        if p['slug'] in existing_slugs:
            preserved += 1
            continue
        body=sanitize_content(p.get('content_raw',''), link_map)
        (ROOT/'novosti'/f'{p["slug"]}.html').write_text(article_html(p, body), encoding='utf-8')
        generated += 1
    # Preserve data/news.json (current cards/images). Load for archive top.
    current=[]
    # hardcoded custom/current expedition card from existing homepage
    current.append({'title':'Speleološka ekspedicija Sjeverni Velebit 2026','slug':'speleoloska-ekspedicija-sjeverni-velebit-2026','date_iso':'2026-07-25T00:00:00+02:00','date':'25.07.–09.08.2026.','desc':'SO PDS Velebit poziva na ekspediciju u Hajdučkim i Rožanskim kukovima. Prijave su otvorene do 20. 7., a plakat/PDF je dostupan u objavi.','image':'assets/news/ekspedicija-sov-2026.jpg','category':'Ekspedicije','categories':['Ekspedicije'],'url':'novosti/speleoloska-ekspedicija-sjeverni-velebit-2026.html'})
    news_path=ROOT/'data'/'news.json'
    if news_path.exists():
        try:
            data=json.loads(news_path.read_text(encoding='utf-8'))
            for n in data:
                nn=dict(n); nn['url']=f"novosti/{nn.get('slug','')}.html"; nn['excerpt']=nn.get('desc',''); nn['categories']=[nn.get('category','Novosti')]
                current.append(nn)
        except Exception:
            pass
    archive_entries=generate_vijesti(posts, current)
    patch_index_link()
    # Content index and media manifest; no pages generated.
    (ROOT/'data').mkdir(exist_ok=True)
    content_index={
        'build':BUILD_FILE,
        'source_export':XML.name,
        'strategy':'Preserve current public pages and current image-rich news; add only missing WordPress post text pages.',
        'counts':{'published_posts_in_export':len(posts),'existing_modern_news_pages_preserved':preserved,'new_text_post_pages_generated':generated,'published_pages_in_export_skipped':len(pages),'attachments_in_export':len(attachments),'unique_media_urls_manifested':len(media_urls)},
        'posts':[{'id':p['id'],'title':p['title'],'slug':p['slug'],'date_iso':p['date_iso'],'date':p['date'],'category':p.get('category'),'categories':p.get('categories'), 'url':f'novosti/{p["slug"]}.html','preserved_existing_modern_page':p['slug'] in existing_slugs,'excerpt':p.get('excerpt',''), 'media_count':len(media_by_item.get(p['id'],[]))} for p in posts],
        'skipped_pages':[{'id':pg['id'],'title':pg['title'],'slug':pg['slug'],'link':pg['link'],'reason':'current SOV public page already exists / static pages are not imported in this build'} for pg in pages]
    }
    media_manifest={'build':BUILD_FILE,'source_export':XML.name,'strategy':'media not included in Git/build; preserve current images only; use this later for storage/server media phase','counts':content_index['counts'],'media_urls':[{'url':u,'proposed_path':proposed_media_path(u)} for u in media_urls],'by_item':{}}
    for p in posts+pages:
        media_manifest['by_item'][p['id']]={'type':p['type'],'title':p['title'],'slug':p['slug'],'media_urls':media_by_item.get(p['id'],[])}
    (ROOT/'data'/'legacy-wordpress-content.json').write_text(json.dumps(content_index, ensure_ascii=False, indent=2), encoding='utf-8')
    (ROOT/'data'/'legacy-wordpress-media-manifest.json').write_text(json.dumps(media_manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    # remove prior generated legacy-wordpress if present; no static page import.
    shutil.rmtree(ROOT/'legacy-wordpress', ignore_errors=True)
    # version info
    (ROOT/'VERSION.txt').write_text(BUILD_VERSION+'\n', encoding='utf-8')
    (ROOT/'BUILD_VERSION.txt').write_text(BUILD_FILE+'\n', encoding='utf-8')
    update={'app':'SOV web','version':BUILD_VERSION,'versionName':f'v{BUILD_VERSION}-{BUILD_SLUG}','build':BUILD_FILE,'createdAt':datetime.now(timezone.utc).isoformat().replace('+00:00','Z'),'cacheBust':CACHE_BUST,'base':'sov-web-build-v6.1.43e-smart-location-future-trips.zip','requiresSql':False,'releaseType':'wordpress-text-import-preserve-current-public','changedFiles':['vijesti.html','novosti/*.html (only missing old posts)','data/legacy-wordpress-content.json','data/legacy-wordpress-media-manifest.json','index.html (small link to all posts)','VERSION.txt','BUILD_VERSION.txt','update.json'],'notes':['Current homepage and existing image-rich news cards remain as they were.','Existing public pages like O nama, Povijest, Speleoškola, Velebitaški duh, Pročelništvo and Velebiten were not overwritten by WordPress page export.','Only missing WordPress posts were added as clean text article pages.','No legacy image/media bulk import; no 2GB Git push.','No SQL, Izleti, Oružarstvo, Arhivar, Karta, role, login or APK compatibility changes.']}
    (ROOT/'update.json').write_text(json.dumps(update, ensure_ascii=False, indent=2), encoding='utf-8')
    # version JS
    sv=ROOT/'assets'/'sov-version.js'
    if sv.exists():
        txt=sv.read_text(encoding='utf-8', errors='ignore')
        txt=re.sub(r"const FALLBACK_VERSION='[^']+'", f"const FALLBACK_VERSION='{BUILD_VERSION}'", txt)
        txt=re.sub(r"const FALLBACK_CACHE='[^']+'", f"const FALLBACK_CACHE='{CACHE_BUST}'", txt)
        txt=re.sub(r"const FALLBACK_BUILD='[^']+'", f"const FALLBACK_BUILD='{BUILD_FILE}'", txt)
        txt=re.sub(r"const FALLBACK_NAME='[^']+'", f"const FALLBACK_NAME='v{BUILD_VERSION}-{BUILD_SLUG}'", txt)
        sv.write_text(txt, encoding='utf-8')
    # tool + notes
    tools=ROOT/'tools'; tools.mkdir(exist_ok=True)
    shutil.copy2(Path(__file__), tools/'sov_wordpress_wxr_refined_text_importer.py')
    notes=f'''# SOV web v{BUILD_VERSION} — WordPress text import, current public site preserved

Baseline: `sov-web-build-v6.1.43e-smart-location-future-trips.zip`  
Source: `sovelebit.wordpress.com.2026-06-24.000.xml`

## Što je napravljeno

- Zadržana je postojeća naslovnica i postojeće aktualne objave sa slikama.
- Zadržane su postojeće javne stranice: `o-drustvu.html`, `povijest.html`, `speleoskola.html`, `velebitaski-duh.html`, `procelnistvo.html`, `velebiten.html`, `pridruzi-nam-se.html`.
- Iz WordPress XML-a dodani su samo postovi koji nisu već imali modernu `novosti/*.html` stranicu.
- Generirano novih text-only post stranica: {generated}.
- Sačuvano postojećih modernih post stranica: {preserved}.
- `vijesti.html` je sada normalna stranica “Sve objave” s pretragom/filterima, ne tehnička arhiva.
- Media/slike nisu bulk-importane; samo su evidentirane u `data/legacy-wordpress-media-manifest.json` za kasniju fazu.

## Što nije dirano

- Nisu pregažene stranice “O nama”, “Povijest”, “Speleoškola”, “Velebitaški duh”, “Pročelništvo”, “Velebiten”.
- Nije dodan `assets/legacy-wordpress/` media folder.
- Nije diran SQL, Supabase, Izleti, Oružarstvo, Arhivar, Karta, login, role ni APK kompatibilnost.

## Namjera

Ovo nije “stari web / tehnička arhiva” build. Ovo je normalan SOV web kojem su dodane starije objave kao tekstualne stranice, dok aktualni vizualni dio ostaje kakav je bio.
'''
    (ROOT/f'BUILD_NOTES_v{BUILD_VERSION}_WORDPRESS_TEXT_IMPORT_PRESERVE_CURRENT.md').write_text(notes, encoding='utf-8')
    print(json.dumps(content_index['counts'], ensure_ascii=False, indent=2))

if __name__=='__main__': main()
