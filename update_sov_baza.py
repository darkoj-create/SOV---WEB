#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
update_sov_baza.py — merge 'Sjeverni Velebit 2026.kml' u SOV bazu.
Ažurira: web JSON (flat), android JSON.gz (nested). Dry-run: piše u --outdir.
Politika: KML pobjeđuje za NEprazna polja; prazna KML polja ne diraju bazu.
Match: normalizirano ime; udaljenost >500m se označava u reportu.
"""
import argparse, collections, gzip, json, math, re, sys, unicodedata
from datetime import datetime, timezone

def fold(s):
    s = unicodedata.normalize('NFD', s or '')
    return re.sub(r'\s+', ' ', ''.join(c for c in s if not unicodedata.combining(c)).lower().strip())

def num(v):
    if not v: return None
    v = str(v).strip().replace(',', '.')
    m = re.search(r'-?\d+(?:\.\d+)?', v)
    return float(m.group(0)) if m else None

def intnum(v):
    f = num(v)
    return int(f) if f is not None else None

def danet(v):
    t = (v or '').strip().lower()
    return 'da' if t == 'da' else ('ne' if t == 'ne' else (t or None))

def iso_date(v):
    m = re.match(r'\s*(\d{1,2})\.(\d{1,2})\.(\d{4})\.?\s*$', v or '')
    return f'{m.group(3)}-{int(m.group(2)):02d}-{int(m.group(1)):02d}' if m else None

def hav(lat1, lon1, lat2, lon2):
    R = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = math.radians(lat2-lat1), math.radians(lon2-lon1)
    a = math.sin(dp/2)**2 + math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 2*R*math.asin(math.sqrt(a))

STATUS_K2 = {
    'objekt u katastru': 'u_katastru',
    'objekt u katastru - editirati': 'u_katastru_editirati',
    'objekt na provjeri': 'na_provjeri',
    'nije objekt': 'nije_objekt',
    'objekt druge udruge': 'objekt_druge_udruge',
    'ima podatke za unos u katastar': 'za_unos_u_katastar',
}
TASK_MAP = {
    'ponoviti nacrt': 'ponoviti_nacrt', 'fotka': 'fotka', 'plocica': 'plocica',
    'pločica': 'plocica', 'koordinate': 'koordinate', 'digitalizirati nacrt': 'digitalizirati_nacrt',
    'srediti nacrt': 'srediti_nacrt', 'nastaviti nacrt': 'nastaviti_nacrt',
    'pronaći': 'pronaci', 'pronaci': 'pronaci', 'zapisnik': 'zapisnik',
    'provjeriti': 'provjeriti', 'istražiti': 'istraziti', 'istraziti': 'istraziti',
    'skinuti pločicu': 'skinuti_plocicu', 'zamijeniti pločicu': 'zamijeniti_plocicu',
}

def parse_tasks(*texts):
    out = []
    for t in texts:
        for part in re.split(r'[;,]', t or ''):
            key = fold(part)
            if key in TASK_MAP and TASK_MAP[key] not in out:
                out.append(TASK_MAP[key])
    return out

def cadastre_from(kat):
    t = (kat or '').strip()
    tf = fold(t)
    if re.match(r'^hr\s*\d+', tf):
        return 'u_katastru', t.replace(' ', ''), True
    if tf == 'nije u katastru': return 'nije_u_katastru', None, False
    m = re.match(r'^(\d{1,2})\.\s*krug$', tf)
    if m: return f'krug_{m.group(1)}', None, False
    return None, None, None

def parse_kml(path):
    kml = open(path, encoding='utf-8', errors='replace').read()
    out = []
    for pm in re.findall(r'<Placemark.*?</Placemark>', kml, re.S):
        nm = re.search(r'<name>(.*?)</name>', pm, re.S)
        if not nm: continue
        name = re.sub(r'\s+', ' ', nm.group(1)).strip()
        co = re.search(r'<coordinates>(.*?)</coordinates>', pm, re.S)
        lat = lon = None
        if co:
            parts = co.group(1).strip().split(',')
            if len(parts) >= 2:
                lon, lat = float(parts[0]), float(parts[1])
                if abs(lon) < 0.01 and abs(lat) < 0.01:
                    lon = lat = None  # 0,0 = koordinate nepoznate
        sd = dict(re.findall(r'<SimpleData name="([^"]+)">(.*?)</SimpleData>', pm, re.S))
        sd = {k: re.sub(r'\s+', ' ', v).strip() for k, v in sd.items()}
        out.append({'name': name, 'lat': lat, 'lon': lon, 'sd': sd})
    return out

def flat_updates(k):
    """KML placemark -> dict web (flat) polja s NEpraznim vrijednostima."""
    sd = k['sd']
    status_k2 = STATUS_K2.get(fold(sd.get('Kategorija2', '')))
    cstat, cnum, cbool = cadastre_from(sd.get('Katastarski_broj'))
    tasks = parse_tasks(sd.get('Kategorija'), sd.get('Kategorija2'))
    year = intnum(sd.get('Godina zadnjeg istraživanja'))
    upd = {
        'lat': k['lat'], 'lon': k['lon'],
        'altitude_m': num(sd.get('Nadmorska_visina_(očitana)')) or num(sd.get('Nadmorska visina generirana (m)')),
        'county': sd.get('Županija'), 'municipality': sd.get('Općina'),
        'nearest_place': sd.get('Najbliže_mjesto'), 'locality': sd.get('Lokalitet'),
        'depth_m': num(sd.get('Dubina(m)')),
        'length_m': num(sd.get('Duljina(m)')) or num(sd.get('Horizontalna_duljina(m)')),
        'vertical_range_m': num(sd.get('Vertikalna_razlika(m)')),
        'entrance_count': intnum(sd.get('Broj_ulaza')),
        'plate_number': (sd.get('Broj_pločice') or None) if fold(sd.get('Broj_pločice', '')) != 'nema' else None,
        'main_entrance_status': sd.get('Stanje_glavnog_ulaza'),
        'access_description': sd.get('Položaj_i_pristup_objektu'),
        'technical_description': sd.get('Osnovni_opis_s_tehničkim_podacima'),
        'research_perspective': {'da': True, 'ne': False}.get(fold(sd.get('Perspektiva da/ne', ''))),
        'research_perspective_note': sd.get('Perspektiva daljnjeg istraživanja'),
        'last_research_year': year,
        'last_research_date': sd.get('Datum zadnjeg istraživanja'),
        'last_research_date_iso': iso_date(sd.get('Datum zadnjeg istraživanja')),
        'clubs': sd.get('Istražile_udruge'),
        'survey_authors': sd.get('Autori_nacrta') or sd.get('Mjerio'),
        'team_members': sd.get('Članovi_ekipe'),
        'hazards': sd.get('Opasnosti'), 'pollution': sd.get('Onečišćenja'),
        'digital_survey_status': danet(sd.get('Nacrt u DIGITALNOJ BAZI')),
        'bibliography_status': (sd.get('Podatak u bibliografskoj bazi?') or '').lower() or None,
        'ice_present': danet(sd.get('led da/ne')),
        'hydrology': sd.get('Hidrološka_karakterisitika'),
        'hydrogeology': sd.get('Hidrogeološka_funkcija'),
        'georef_record': sd.get('Georef_zapis'),
        'note': sd.get('Napomena'),
        'object_type_final': sd.get('Vrsta_objekta'),
        'record_status': status_k2 or cstat,
        'cadastre_status': cstat, 'cadastral_number': cnum, 'in_cadastre_bool': cbool,
        'field_tasks': ';'.join(tasks) if tasks else None,
        # extra polja (samo android koristi):
        '_literature': sd.get('Literatura'), '_name_origin': sd.get('Podrijetlo_imena'),
        '_synonyms': sd.get('Sinonimi'), '_other_synonyms': sd.get('Sinonimi - podaci u drugim bazama, radna imena'),
    }
    if upd['object_type_final']: upd['object_type_source'] = 'kml_sjeverni_velebit_2026'
    return {f: v for f, v in upd.items() if v not in (None, '')}

ANDROID_PATHS = {
    'lat': ('location', 'lat'), 'lon': ('location', 'lon'), 'county': ('location', 'county'),
    'municipality': ('location', 'municipality'), 'nearest_place': ('location', 'nearest_place'),
    'locality': ('location', 'locality'), 'altitude_m': ('location', 'altitude_m'),
    'cadastre_status': ('cadastre', 'status'), 'cadastral_number': ('cadastre', 'cadastral_number'),
    'in_cadastre_bool': ('cadastre', 'in_cadastre'),
    'object_type_final': ('classification', 'object_type'), 'object_type_source': ('classification', 'object_type_source'),
    'record_status': ('classification', 'record_status'),
    'depth_m': ('metrics', 'depth_m'), 'length_m': ('metrics', 'length_m'),
    'vertical_range_m': ('metrics', 'vertical_range_m'), 'entrance_count': ('metrics', 'entrance_count'),
    'plate_number': ('condition', 'plate_number'), 'main_entrance_status': ('condition', 'main_entrance_status'),
    'hazards': ('condition', 'hazards'), 'pollution': ('condition', 'pollution'),
    'last_research_year': ('research', 'last_research_year'), 'last_research_date': ('research', 'last_research_date'),
    'clubs': ('research', 'clubs'), 'team_members': ('research', 'team_members'),
    'survey_authors': ('research', 'survey_author'), 'bibliography_status': ('research', 'bibliography_record'),
    'georef_record': ('research', 'georef_record'), 'research_perspective': ('research', 'further_research_possible'),
    'research_perspective_note': ('research', 'further_research_note'),
    'access_description': ('content', 'access_description'), 'technical_description': ('content', 'technical_description'),
    'note': ('content', 'note'), '_literature': ('content', 'literature'), '_name_origin': ('content', 'name_origin'),
    '_synonyms': ('content', 'synonyms'), '_other_synonyms': ('content', 'other_synonyms'),
}

def apply_android(rec, upd):
    changed = 0
    for f, v in upd.items():
        if f in ('field_tasks',):
            lst = [t for t in v.split(';') if t]
            if rec['classification'].get('field_tasks') != lst:
                rec['classification']['field_tasks'] = lst; changed += 1
            continue
        if f == 'digital_survey_status':
            b = True if v == 'da' else (False if v == 'ne' else None)
            if b is not None and rec['research'].get('survey_in_digital_base') != b:
                rec['research']['survey_in_digital_base'] = b; changed += 1
            continue
        path = ANDROID_PATHS.get(f)
        if not path: continue
        sect, key = path
        if rec.setdefault(sect, {}).get(key) != v:
            rec[sect][key] = v; changed += 1
    return changed

def new_android_record(rid, name, upd):
    rec = {'id': str(rid), 'source': 'sov', 'source_labels': ['sov'], 'name': name,
           'location': {'lat': None, 'lon': None, 'county': None, 'municipality': None, 'nearest_place': None,
                        'locality': None, 'island': None, 'altitude_m': None, 'protected_area': None},
           'cadastre': {'status': None, 'cadastral_number': None, 'in_cadastre': None, 'not_in_cadastre_candidate': None},
           'classification': {'object_type': None, 'object_type_source': None, 'record_status': None,
                              'field_tasks': [], 'priority': None, 'kml_export_candidate': True},
           'metrics': {'depth_m': None, 'length_m': None, 'vertical_range_m': None, 'entrance_count': None},
           'condition': {'plate_number': None, 'main_entrance_status': None, 'hazards': None, 'pollution': None},
           'research': {'last_research_year': None, 'last_research_date': None, 'clubs': None, 'team_members': None,
                        'survey_author': None, 'survey_in_digital_base': None, 'bibliography_record': None,
                        'georef_record': None, 'further_research_possible': None, 'further_research_note': None},
           'content': {'access_description': None, 'technical_description': None, 'note': None, 'literature': None,
                       'name_origin': None, 'synonyms': None, 'other_synonyms': None,
                       'clean_cave_report': None, 'geological_or_anthropogenic_activities': None},
           'raw': {'workflow_raw': 'kml_sjeverni_velebit_2026', 'katastar_id': None,
                   'katastar_coordinate_source': None, 'katastar_coordinate_uncertainty_m': None},
           'search_text': None}
    apply_android(rec, upd)
    return rec

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--kml', required=True)
    ap.add_argument('--web-json', required=True)
    ap.add_argument('--android-gz', required=True)
    ap.add_argument('--outdir', required=True)
    args = ap.parse_args()

    kml = parse_kml(args.kml)
    web = json.load(open(args.web_json, encoding='utf-8'))
    agz = json.loads(gzip.open(args.android_gz, 'rt', encoding='utf-8').read())

    web_by = {}
    for r in web: web_by.setdefault(fold(r['name']), []).append(r)
    and_by = {}
    for r in agz['records']: and_by.setdefault(fold(r['name']), []).append(r)

    next_id = max(int(r['id']) for r in web if str(r['id']).isdigit()) + 1
    rep = {'updated': [], 'added': [], 'far': [], 'dupname': [], 'numeric_names': []}

    for k in kml:
        key = fold(k['name'])
        if re.fullmatch(r'[\d\s\-.]+', k['name']):
            rep['numeric_names'].append(k['name'])
        upd = flat_updates(k)
        wrecs = web_by.get(key, [])
        if len(wrecs) > 1:
            rep['dupname'].append(k['name'])
        if wrecs and wrecs[0].get('lat') and k['lat'] and hav(wrecs[0]['lat'], wrecs[0]['lon'], k['lat'], k['lon']) > 2000:
            # Isto ime, ali >2km daleko: drugi objekt — dodaj kao novi, ne prepisuj postojeći.
            d = hav(wrecs[0]['lat'], wrecs[0]['lon'], k['lat'], k['lon'])
            rep['far'].append((k['name'], round(d)))
            wrecs = []
        if wrecs:
            w = wrecs[0]
            if w.get('lat') and k['lat']:
                d = hav(w['lat'], w['lon'], k['lat'], k['lon'])
                if d > 500: rep['far'].append((k['name'], round(d)))
            nchg = 0
            for f, v in upd.items():
                if f.startswith('_'): continue
                if w.get(f) != v: w[f] = v; nchg += 1
            for a in and_by.get(key, [])[:1]:
                apply_android(a, upd)
            rep['updated'].append((k['name'], nchg))
        else:
            w = {f: None for f in web[0].keys()}
            w['id'] = next_id; w['name'] = k['name']  # web koristi int id (kao original)
            for f, v in upd.items():
                if not f.startswith('_'): w[f] = v
            web.append(w)
            agz['records'].append(new_android_record(next_id, k['name'], upd))
            rep['added'].append(k['name'])
            next_id += 1

    now = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    agz['generated_at_utc'] = now
    agz['source_file'] = (agz.get('source_file') or '') + ' + Sjeverni Velebit 2026.kml'
    agz.setdefault('stats', {})['record_count'] = len(agz['records'])

    import os
    os.makedirs(args.outdir, exist_ok=True)
    json.dump(web, open(f'{args.outdir}/sov-baza_NOVO.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    with gzip.open(f'{args.outdir}/baza_velebit_2026_android_v3.json.gz', 'wt', encoding='utf-8') as f:
        json.dump(agz, f, ensure_ascii=False)

    with open(f'{args.outdir}/IZVJESTAJ.md', 'w', encoding='utf-8') as f:
        f.write(f'# Update SOV baze — Sjeverni Velebit 2026.kml\n\n')
        f.write(f'- KML objekata: {len(kml)}\n- Ažurirano postojećih: {len(rep["updated"])}\n')
        f.write(f'- Dodano novih: {len(rep["added"])} (id {next_id-len(rep["added"])}–{next_id-1})\n')
        f.write(f'- Baza ukupno nakon: {len(web)} zapisa\n\n')
        f.write(f'## Isto ime, daleke koordinate (>2km = dodan kao NOVI; 0.5-2km = ažuriran uz oprez): {len(rep["far"])}\n\n')
        for n, d in sorted(rep['far'], key=lambda x: -x[1])[:30]: f.write(f'- {n}: {d} m\n')
        f.write(f'\n## Duplikat imena u bazi (ažuriran samo prvi): {len(rep["dupname"])}\n\n')
        for n in rep['dupname'][:20]: f.write(f'- {n}\n')
        f.write(f'\n## Numerička/čudna imena u KML-u: {len(rep["numeric_names"])}\n\n')
        for n in rep['numeric_names'][:30]: f.write(f'- {n}\n')
        f.write(f'\n## Top ažurirani (po broju promijenjenih polja)\n\n')
        for n, c in sorted(rep['updated'], key=lambda x: -x[1])[:20]: f.write(f'- {n}: {c} polja\n')
    print(f'OK: {len(rep["updated"])} azurirano, {len(rep["added"])} novih, {len(rep["far"])} dalekih, ukupno {len(web)}')

if __name__ == '__main__':
    main()
