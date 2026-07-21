# AI HANDOVER — SOV Admin 1.4.55a (offline nacrti + baza + matching v3)

> Konvencija: ovaj dokument se ažurira NAKON SVAKOG radnog ciklusa s AI asistentom
> (Claude/GPT/bilo koji), da sljedeći thread ima puni kontekst bez ponavljanja.
> Zadnje ažuriranje: 2026-07-19 (v3 matcher). Projekt: `D:\SOV-app builds\Admin` (Android, Kotlin/Compose).
> Bazna verzija: 1.4.54a-dem-track-profile (versionCode 900153) + sve ispod. APK još NIJE buildan s ovim.

## STANJE — što je implementirano i verificirano

### 1. Offline nacrti u APK-u
- `app/src/main/assets/nacrti_bundled/`: **1091 nacrta (~246MB)** = 1019 PNG→WebP konverzija
  (iz `D:\nacrti_png`, max 2600px q78) + 19 PDF iz istog foldera + 54 iz
  `C:\Users\darko\Documents\Sjeverni Velebit Ekspedicija 2026` (samo `Nacrt/` podfolderi; fotke/DXF ne).
- `index.json` u istom folderu: format kao Drive index (`fileId="asset:<ime>"`), s objectName,
  detectedKatastarNumber/detectedTile (parsirano iz prefiksa imena), **recordId** i **matchStatus** (vidi §3).
  Novost v3: polje `drawingGroup` (grupira stranice istog nacrta) i `pageCount`.
- Arhitektura u `util/DriveDrawingsRepository.kt`: bundled-first — `fetchMatchesForRecord` prvo gleda
  APK, Drive/Apps Script samo ako bundled nema match ili na "Osvježi" (merge, bundled prednost);
  pad mreže → bundled fallback; baza učitana a nema nacrta → uredan prazan rezultat s "Baza: N".
  Ekstrakcija asseta u `filesDir/Offline/nacrti/bundled/` na prvo otvaranje (atomski tmp+rename+lock, IO thread).
- Održavanje: novi nacrti u `D:\nacrti_png` → dupli klik **`UBACI_NACRTE.bat`** (sam nađe Python,
  `convert_nacrti.py --include-pdf`) → **zatim `python match_nacrti_v3.py`** → rebuild APK.

### 2. Crash fixevi (OOM)
- `RecordDetailFeature.kt`: `decodeDrawingImageBitmap` dekodira subsamplirano na ciljanu širinu
  (prije: puna rezolucija ≈20MB/nacrt → OOM kod 10 thumbnailova). Viewer 1800px, thumbnail 520px.
- Sva ekstrakcija/dekodiranje isključivo na Dispatchers.IO.

### 3. Matching nacrta — v3 (AKTUALNO STANJE)

**Offline matcher skripta: `match_nacrti_v3.py`** (root projekta, Python 3).

Ključne razlike od v2:
1. **Grupiranje stranica**: `_strana_N` i `_001/_002` sufiksi se grupiraju u jedan logični nacrt.
   881 logičnih grupa od 1091 datoteka. Polje `drawingGroup` u index.json.
2. **Čišćenje naziva**: uklanja nacrt/skenirani/sređeni/digitalizirani/tlocrt/profil/strana/godinu/ext,
   Unicode escape (_U0161→š), encoding artefakte (ƒ→š), CRO Speleo prefix, duple nazive (X__X),
   razdvaja slijepljene nazive (JamaSOV10→Jama SOV 10), miče zagrade (metadata).
3. **Strogi brojevi**: ako nacrt ima broj (rimski/arapski/oznaku), a objekt ima DRUGI broj → odbačeno.
   Rudnica VI nikad neće završiti na Rudnica VIII.
4. **Redoslijed sparivanja** (6 razina, svaka strožija od sljedeće):
   1) Pločica — objekt čije je IME pločica (npr. "05-1050")
   2) Oznaka — SK-12, SOV 35, G3, PT 9 itd. (CODE_PAT regex)
   3) Potpuno isto ime (normalizirano: č→c, š→s, ž→z, apostrofi uklonjeni)
   4) Sinonim — content.synonyms, other_synonyms, zagrade u imenu objekta
   5) Bazno ime + isti broj — "Rudnica" + VI → Rudnica VI
   6) Fuzzy (SequenceMatcher ≥0.88, jasan pobjednik s razmakom ≥0.08 od drugog)
5. **Status polja**: `matchStatus` = "verified" | "review" | "unmatched"
6. **Ručne korekcije odvojene**: `match-overrides.json` (key=filename, value={"recordId":"..."}).
   Matcher PRVO čita overrides, tek onda automatski sparuje. Override se nikad ne pregazi.

**Rezultati v3:**
- 881 logičnih grupa (1091 datoteka)
- **777 dodijeljeno (88.2%)**: 671 exact name, 73 code, 12 synonym, 1 name+number, 20 fuzzy
- **41 za pregled**: višestruki kandidati, čeka ručnu odluku
- **63 nedodijeljeno**: uglavnom ne postoji u bazi, generička imena (img108, nacrt), ili multi-object crteži
- **Nula lažnih pozitiva** u fuzzy kategoriji (ručno verificirano)

Runtime engine u appu (DriveDrawingsRepository.kt `findMatches`) je NEPROMIJENJEN — pinned nacrt
(ima recordId) se prikazuje isključivo kod svog objekta; unpinned idu kroz IDF fuzzy s pragovima.

**Datoteke:**
- `match_nacrti_v3.py` — skripta (root projekta). `python match_nacrti_v3.py [--dry-run]`
- `match-overrides.json` — ručne korekcije (root projekta)
- `MATCHING_REPORT_v3.md` — izvještaj zadnjeg pokretanja (root projekta)
- Stari `MATCHING_REPORT_NACRTI.md` — zastarjelo, zamijenjeno v3 izvještajem

**Postupak za ručni pregled:**
1. Otvoriti `MATCHING_REPORT_v3.md`, sekcija "Needs review"
2. Za svaki nacrt odlučiti koji je točan objekt
3. Upisati u `match-overrides.json`: `{"filename.webp": {"recordId": "ID"}}`
4. Ponovo pokrenuti `python match_nacrti_v3.py` → novi index.json

### 4. Search screen
- `SearchFeature.kt`: izbačeni "Pametni početak" i "Brzi filteri"; umjesto njih kartica "Filter"
  s chipom "U katastru" (SourceFilter.KATASTAR toggle). "Fino podešavanje" netaknuto.
- `RecordCard` (RecordDetailFeature.kt): badge donji desni kut — zeleno "Nacrt" / sivo "Bez nacrta"
  (`hasBundledDrawingFor`, async + keš po record.id).

### 5. SOV baza — update "Sjeverni Velebit 2026"
- ETL `update_sov_baza.py` (root projekta): KML (740 obj.) + XLSX dopuna (3 obj. bez koordinata:
  Chladny odpočinok, Firn I, Komaria) → **baza 1457 → 1750 zapisa** (450 update, 293 novih, id 1585–1877).
- Verificirano protiv izvorne XLSX tablice: 740/740 objekata, sve vrijednosti točne.
- Primijenjeno: APK (`assets/baza_velebit_2026_android_v2.json.gz` — isto ime!), web
  (`SOV---WEB/data/sov-baza.json` + `baza_velebit_2026_appready.json`), Supabase SQL pripremljen.
- Pravila ETL-a: KML pobjeđuje za neprazna polja; isto ime >2km = novi objekt (Jamica, Ključanica!);
  koordinate 0,0 = bez koordinata (81 objekt); Kategorija2→record_status, Kategorija→field_tasks.

## OTVORENO / TODO
1. **Supabase SQL NIJE pokrenut**: `SOV---WEB/SUPABASE_SPELEO_BAZA_STAGING_UPSERT_SJEVERNI_VELEBIT_2026.sql`
   (705 redova, upsert u speleo_objects_staging, čuva sandbox polja) → zalijepiti u SQL Editor
   (projekt SOV-web / ncomefzkuixyfixisrhi), pa promocija po CONTROLLED_PROMOTION flowu.
2. **Rebuild APK-a** (Clean + Build; očekivano ~270MB) + test: airplane mode → objekt → Nacrti
   ("Baza: 1091"; testni objekti: Debela ljut, Vražja jama (PDF), (Ne)radni disto, Zakičnica II, Lubuška jama 15 str.).
3. Pregledati `MATCHING_REPORT_v3.md` (**41 review + 63 unmatched**) i po potrebi upisati u `match-overrides.json`.
4. **172 dupla ID-a u bazi** (otprije) — zato Supabase staging guta duplikate (PK source_id). Počistiti jednom.
5. Objekti ekspedicije bez `Nacrt/` foldera: Jama u šumi na pločama 1, MR, Patkov gušt, Prvi Spit, SOV 28.
6. LiDAR NPSV 2025/2026 kartice u XLSX-u (~525 točaka) — nisu obrađene, čekaju odluku.

## KAKO NASTAVITI U NOVOM THREADU
Reci AI-ju da pročita ovaj dokument (`AI_HANDOVER_...md` u rootu projekta) + po potrebi:
`MATCHING_REPORT_v3.md`, `match_nacrti_v3.py`, `match-overrides.json`.
Relevantne datoteke s izmjenama:
`util/DriveDrawingsRepository.kt` (bundled + matching), `RecordDetailFeature.kt` (viewer/thumbnail/badge),
`SearchFeature.kt` (filter). Web repo: `D:\Downloads\Mood RC\MoodyPulse\SOV---WEB` (git!).
Izvori podataka: `D:\nacrti_png` (1041 datoteka), `C:\Users\darko\Documents\Sjeverni Velebit Ekspedicija 2026`.
Nakon svakog ciklusa: ažurirati ovaj MD + napraviti handover zip (bez teških asseta).
