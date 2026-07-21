#!/usr/bin/env python3
"""
SOV Nacrti Matcher v3
=====================
Offline matching of cave survey drawings to database objects.

Key improvements over v2:
  1. Groups multi-page drawings (_strana_N) into single logical drawings
  2. Strict number/code matching — mismatched numbers = reject
  3. Clean tiered matching: tile → code → exact name → synonym → name+number → fuzzy
  4. Generates index.generated.json + merges match-overrides.json
  5. Dry-run report with categories

Usage:
  python match_nacrti_v3.py [--dry-run]

Reads:
  app/src/main/assets/nacrti_bundled/index.json  (current drawing list)
  app/src/main/assets/baza_velebit_2026_android_v2.json.gz  (object database)
  match-overrides.json  (manual corrections, optional)

Writes:
  app/src/main/assets/nacrti_bundled/index.json  (updated with recordId + matchStatus)
  MATCHING_REPORT_v3.md  (dry-run report)
"""

import json, gzip, re, sys, os
from collections import defaultdict
from pathlib import Path
from difflib import SequenceMatcher

# ─── paths ───────────────────────────────────────────────────────────────────
SCRIPT_DIR  = Path(__file__).resolve().parent
ASSETS_DIR  = SCRIPT_DIR / "app" / "src" / "main" / "assets"
INDEX_PATH  = ASSETS_DIR / "nacrti_bundled" / "index.json"
BAZA_PATH   = ASSETS_DIR / "baza_velebit_2026_android_v2.json.gz"
OVERRIDES   = SCRIPT_DIR / "match-overrides.json"
REPORT_PATH = SCRIPT_DIR / "MATCHING_REPORT_v3.md"

# ─── constants ───────────────────────────────────────────────────────────────
# Words to strip from drawing filenames before matching
STRIP_WORDS = {
    'nacrt', 'nacrta', 'nacrte', 'nacrti', 'skenirani', 'skeniran', 'sken', 'scan',
    'uredeni', 'uređeni', 'sređeni', 'sredeni', 'radni',
    'digitalizirani', 'digitalni', 'strana', 'tlocrt', 'profil', 'presjek',
    'prikaz', 'pop', 'knjige', 'zapisnik', 'mm', 'fin',
    'master', 'paos', 'dio', 'sa', 'primjedbama', 'dodane', 'udruge',
    'sl.', 'img', 'pdf', 'slovaci', 'slovački', 'slovacki',
    'hbsd', 'sož',
}
# Prefixes to strip from beginning of cleaned name
STRIP_PREFIXES = [
    'cro speleo -', 'cro speleo-', 'cro speleo ',
    'nacrt ', 'sken nacrta ', 'sken nacrta_', 'scan nacrta ',
    'digitalizirani nacrt ', 'skenirani nacrt ',
    'sl.', 'radni nacrt ',
]
# Words NOT to strip (kept even though they look generic)
KEEP_WORDS = {'jama', 'špilja', 'spilja', 'ponor', 'izvor', 'pod', 'velika', 'mali', 'gornja', 'donja'}

# Roman ↔ Arabic
ROMAN_MAP = {'I':1,'II':2,'III':3,'IV':4,'V':5,'VI':6,'VII':7,'VIII':8,'IX':9,
             'X':10,'XI':11,'XII':12,'XIII':13,'XIV':14,'XV':15}
ARABIC_TO_ROMAN = {v:k for k,v in ROMAN_MAP.items()}

# Code patterns: SK-12, JD-3, SOV 35, PT 9, MR 1, BL 2, MP 3, G1, G3
CODE_PAT = re.compile(
    r'\b(SK|JD|SOV|PT|MR|BL|MP|MiG|MiTu|G)[\s_-]?(\d+)\b', re.IGNORECASE
)
# Tile pattern in filename: 05-0090, 023-024, 029-002
TILE_PAT = re.compile(r'^(\d{2,3})[_\s-](\d{2,5})')
# Page pattern
PAGE_PAT = re.compile(r'[_\s]strana[_\s]+(\d+)', re.IGNORECASE)
# Year pattern (4 digits standing alone)
YEAR_PAT = re.compile(r'\b(19\d{2}|20\d{2})\b')
# Roman numeral (standalone)
ROMAN_PAT = re.compile(r'\b(X{0,3}(?:IX|IV|V?I{0,3}))\b')
# Extension
EXT_PAT = re.compile(r'\.(webp|png|jpg|jpeg|tiff?|pdf)$', re.IGNORECASE)
# Leading number (sequence number like "01 ", "05 ")
LEADING_NUM = re.compile(r'^(\d{1,3})\s+')


# ═══════════════════════════════════════════════════════════════════════════════
#  STEP 1: Load data
# ═══════════════════════════════════════════════════════════════════════════════

def load_index():
    with open(INDEX_PATH, 'r', encoding='utf-8') as f:
        return json.load(f)

def load_baza():
    with gzip.open(BAZA_PATH, 'rt', encoding='utf-8') as f:
        data = json.load(f)
    return data['records']

def load_overrides():
    if OVERRIDES.exists():
        with open(OVERRIDES, 'r', encoding='utf-8') as f:
            return json.load(f)
    return {}


# ═══════════════════════════════════════════════════════════════════════════════
#  STEP 2: Group multi-page drawings
# ═══════════════════════════════════════════════════════════════════════════════

def drawing_group_key(filename):
    """Return (base_name, page_number). Drawings with same base are one logical drawing."""
    name = filename
    # Remove extension
    name = EXT_PAT.sub('', name)
    # Extract and remove page number
    page = 0
    m = PAGE_PAT.search(name)
    if m:
        page = int(m.group(1))
        name = name[:m.start()].rstrip('_ ')
    # Also group "Nacrt XXX 001" / "Nacrt XXX 002" style pages
    m2 = re.search(r'[\s_](\d{3})(_\d)?$', name)
    if m2:
        page = int(m2.group(1))
        name = name[:m2.start()].rstrip('_ ')
    return name.strip(), page

def group_drawings(drawings):
    """Group drawings by logical unit. Returns list of DrawingGroup dicts."""
    groups = defaultdict(list)
    for d in drawings:
        base, page = drawing_group_key(d['fileName'])
        groups[base].append((page, d))

    result = []
    for base, pages in groups.items():
        pages.sort(key=lambda x: x[0])
        result.append({
            'groupKey': base,
            'pages': [d for _, d in pages],
            'pageCount': len(pages),
            'primaryFile': pages[0][1],  # first page or single-page
        })
    return result


# ═══════════════════════════════════════════════════════════════════════════════
#  STEP 3: Parse identifiers from drawing name
# ═══════════════════════════════════════════════════════════════════════════════

def parse_tile(filename):
    """Extract tile like '05-0090' from filename prefix."""
    m = TILE_PAT.match(filename)
    if m:
        return f"{m.group(1)}-{m.group(2)}"
    return None

def normalize_tile(tile):
    """05-0090 → (5, 90), 023-024 → (23, 24)"""
    if not tile:
        return None
    parts = tile.split('-')
    if len(parts) == 2:
        try:
            return (int(parts[0]), int(parts[1]))
        except ValueError:
            pass
    return None

def parse_codes(text):
    """Extract codes like SK-12, SOV 35, G3 from text. Returns set of normalized codes."""
    codes = set()
    for m in CODE_PAT.finditer(text):
        prefix = m.group(1).upper()
        num = m.group(2)
        codes.add(f"{prefix}-{num}")
    return codes

def extract_roman(text):
    """Extract Roman numerals from text, return as set of ints."""
    nums = set()
    for m in ROMAN_PAT.finditer(text):
        r = m.group(1)
        if r and r in ROMAN_MAP:
            nums.add(ROMAN_MAP[r])
    return nums

def extract_arabic(text):
    """Extract trailing/standalone Arabic numbers (not tile, not year, not page)."""
    nums = set()
    # Find numbers that aren't years and aren't part of tile prefix
    clean = TILE_PAT.sub('', text)
    clean = YEAR_PAT.sub('', clean)
    clean = PAGE_PAT.sub('', clean)
    clean = CODE_PAT.sub('', clean)
    for m in re.finditer(r'\b(\d+)\b', clean):
        n = int(m.group(1))
        if 1 <= n <= 200:  # reasonable object numbering range
            nums.add(n)
    return nums

def extract_numbers(text):
    """Extract all meaningful numbers (Roman + Arabic) as set of ints."""
    return extract_roman(text) | extract_arabic(text)

def decode_unicode_escapes(name):
    """Handle filenames with _UXXXX Unicode escapes: Dru_U017eanica → Družanica."""
    def repl(m):
        try:
            return chr(int(m.group(1), 16))
        except (ValueError, OverflowError):
            return m.group(0)
    return re.sub(r'_U([0-9a-fA-F]{4})', repl, name)

def split_concatenated(name):
    """Split concatenated names like 'JamaSOV10' → 'Jama SOV 10'."""
    # Insert space between lowercase→uppercase transition
    name = re.sub(r'([a-zčćšžđ])([A-ZČĆŠŽĐ])', r'\1 \2', name)
    # Insert space between letters→digits and digits→letters
    name = re.sub(r'([a-zA-ZčćšžđČĆŠŽĐ])(\d)', r'\1 \2', name)
    name = re.sub(r'(\d)([a-zA-ZčćšžđČĆŠŽĐ])', r'\1 \2', name)
    return name

def clean_drawing_name(filename):
    """
    Clean a drawing filename to extract the pure object name.
    Removes: extension, page info, tile prefix, nacrt/skenirani/etc, years.
    Keeps: numbers, Roman numerals, codes.
    """
    name = filename
    # Remove extension
    name = EXT_PAT.sub('', name)
    # Decode Unicode escapes
    name = decode_unicode_escapes(name)
    # Fix encoding artifacts: ƒ→š (Windows-1252 → UTF8 garble)
    name = name.replace('ƒ', 'š')
    # τ→ša (another encoding garble)
    name = name.replace('τ', 'ša')
    # Remove page
    name = PAGE_PAT.sub('', name)
    # Remove tile prefix (05-0090_)
    name = TILE_PAT.sub('', name)
    # Remove leading sequence number (01 , 05 )
    name = LEADING_NUM.sub('', name)
    # Handle doubled names (Bizek_II__Bizek_II) BEFORE splitting
    name = deduplicate_name(name)
    # Strip parenthetical content (metadata in drawing names, not object names)
    name = re.sub(r'\([^)]*\)', '', name).strip()
    # Split concatenated names
    name = split_concatenated(name)
    # Remove years
    name = YEAR_PAT.sub('', name)
    # Strip known prefixes (before tokenizing)
    name_lower = name.lower().strip()
    for prefix in STRIP_PREFIXES:
        if name_lower.startswith(prefix.lower()):
            name = name[len(prefix):].strip(' -_')
            name_lower = name.lower().strip()
            # Allow chaining (e.g., "Sken nacrta_Digitalizirani nacrt_...")
            for prefix2 in STRIP_PREFIXES:
                if name_lower.startswith(prefix2.lower()):
                    name = name[len(prefix2):].strip(' -_')
                    break
            break
    # Split by underscores and spaces
    tokens = re.split(r'[_\s]+', name)
    # Remove strip words
    filtered = []
    for t in tokens:
        t_lower = t.lower().strip().rstrip('.')
        if not t_lower:
            continue
        if t_lower in STRIP_WORDS:
            continue
        # Remove 'iz' standalone
        if t_lower == 'iz':
            continue
        filtered.append(t)
    result = ' '.join(filtered).strip(' -_.()')
    # Remove trailing ", Nacrt" or similar
    result = re.sub(r',?\s*Nacrt\s*$', '', result, flags=re.IGNORECASE).strip(' ,')
    # Remove trailing tile-like numbers (05-911, 05 857) that aren't part of the name
    result = re.sub(r'\s+\d{2,3}[\s-]\d{3,5}\s*$', '', result).strip()
    # Remove trailing "pdf" artifact
    result = re.sub(r'\s*pdf\s*$', '', result, flags=re.IGNORECASE).strip()
    # Clean up any remaining parenthetical fragments
    result = re.sub(r'\([^)]*\)', '', result).strip(' ,')
    result = re.sub(r'\s+', ' ', result).strip()
    return result


def deduplicate_name(name):
    """Handle doubled names like 'Bizek_II__Bizek_II' → 'Bizek II'
    and variants like 'Druzanica_I__Druzanica_I_skica' → 'Druzanica I'."""
    parts = re.split(r'__+', name)
    if len(parts) == 2:
        a = parts[0].strip()
        b = parts[1].strip()
        al = a.lower()
        bl = b.lower()
        if al == bl:
            return a
        # If one is a prefix of the other (skica/nacrt/Fin suffix), take the shorter
        if bl.startswith(al) or al.startswith(bl):
            return a if len(a) <= len(b) else b
    return name

def normalize_name(name):
    """Normalize an object name for comparison: lowercase, strip accents-insensitive, collapse spaces."""
    n = name.lower().strip()
    # Remove apostrophes and special quotes
    n = n.replace("'", "").replace("'", "").replace("`", "")
    # Collapse multiple spaces
    n = re.sub(r'\s+', ' ', n)
    # Normalize common Croatian letter variations
    n = n.replace('đ', 'dj').replace('č', 'c').replace('ć', 'c')
    n = n.replace('š', 's').replace('ž', 'z')
    n = n.replace('ö', 'o').replace('ü', 'u').replace('ä', 'a')
    # Normalize ƒ (sometimes used for š in old encodings)
    n = n.replace('ƒ', 's')
    return n


# ═══════════════════════════════════════════════════════════════════════════════
#  STEP 4: Build database lookup structures
# ═══════════════════════════════════════════════════════════════════════════════

def build_db_index(records):
    """Build multiple lookup indices from database records."""
    by_name_norm = {}          # normalized name → record
    by_name_exact_lower = {}   # lowercase exact → record
    by_synonym_norm = {}       # normalized synonym → record
    by_code = {}               # CODE-NUM → record
    by_tile_name = {}          # tile string like "05-1050" → record (for objects named by tile)
    all_names_norm = {}        # for fuzzy matching: norm_name → record

    for r in records:
        rid = r['id']
        name = r['name']
        norm = normalize_name(name)

        by_name_norm[norm] = r
        by_name_exact_lower[name.lower()] = r
        all_names_norm[norm] = r

        # Check if object name IS a tile number
        if TILE_PAT.match(name):
            by_tile_name[name] = r

        # Index codes from object name
        for code in parse_codes(name):
            by_code[code] = r

        # Extract parenthetical part as synonym: "Barićeva pećina (Barića pećina)" → "Barića pećina"
        paren_match = re.search(r'\(([^)]+)\)', name)
        if paren_match:
            paren_syn = paren_match.group(1).strip()
            if paren_syn and len(paren_syn) > 1:
                by_synonym_norm[normalize_name(paren_syn)] = r
            # Also index the name WITHOUT parenthetical
            name_no_paren = name[:paren_match.start()].strip()
            if name_no_paren:
                by_name_norm[normalize_name(name_no_paren)] = r
                all_names_norm[normalize_name(name_no_paren)] = r

        # Index synonyms
        syn_text = (r.get('content', {}).get('synonyms') or '')
        other_syn = (r.get('content', {}).get('other_synonyms') or '')
        for syn_raw in [syn_text, other_syn]:
            if not syn_raw or syn_raw.strip() == '0':
                continue
            for syn in re.split(r'[;,/]', syn_raw):
                syn = syn.strip()
                if syn and len(syn) > 1:
                    by_synonym_norm[normalize_name(syn)] = r
                    # Index codes from synonyms too
                    for code in parse_codes(syn):
                        by_code[code] = r

    return {
        'by_name_norm': by_name_norm,
        'by_name_exact_lower': by_name_exact_lower,
        'by_synonym_norm': by_synonym_norm,
        'by_code': by_code,
        'by_tile_name': by_tile_name,
        'all_names_norm': all_names_norm,
        'records': records,
    }


# ═══════════════════════════════════════════════════════════════════════════════
#  STEP 5: Matching engine
# ═══════════════════════════════════════════════════════════════════════════════

def numbers_compatible(nums_a, nums_b):
    """Check if number sets are compatible. Empty = compatible with anything."""
    if not nums_a or not nums_b:
        return True
    return bool(nums_a & nums_b)

def numbers_conflict(nums_a, nums_b):
    """Check if number sets explicitly conflict."""
    if not nums_a or not nums_b:
        return False
    return not bool(nums_a & nums_b)

def codes_conflict(codes_a, codes_b):
    """Check if code sets explicitly conflict."""
    if not codes_a or not codes_b:
        return False
    # Same prefix but different number = conflict
    prefixes_a = {c.split('-')[0] for c in codes_a}
    prefixes_b = {c.split('-')[0] for c in codes_b}
    common_prefixes = prefixes_a & prefixes_b
    if not common_prefixes:
        return False
    for prefix in common_prefixes:
        a_nums = {c for c in codes_a if c.startswith(prefix + '-')}
        b_nums = {c for c in codes_b if c.startswith(prefix + '-')}
        if a_nums and b_nums and not (a_nums & b_nums):
            return True
    return False

def fuzzy_score(a, b):
    """Fuzzy string similarity between two normalized names."""
    return SequenceMatcher(None, a, b).ratio()


def match_drawing_group(group, db_idx):
    """
    Match a drawing group to a database record.
    Returns (record_or_None, match_method, score, candidates_list).
    """
    primary = group['primaryFile']
    filename = primary['fileName']
    clean_name = clean_drawing_name(filename)
    clean_norm = normalize_name(clean_name)
    drawing_tile = parse_tile(filename)
    drawing_codes = parse_codes(clean_name) | parse_codes(filename)
    drawing_nums = extract_numbers(clean_name)

    candidates = []

    # ─── TIER 1: Tile match (object named by its tile number) ────────────
    if drawing_tile:
        # Normalize tile for comparison: "05-0090" → try "05-0090", "05-90", "05-090"
        tile_norm = normalize_tile(drawing_tile)
        if tile_norm:
            tile_variants = set()
            t1, t2 = tile_norm
            tile_variants.add(f"{t1:02d}-{t2}")
            tile_variants.add(f"{t1:02d}-{t2:03d}")
            tile_variants.add(f"{t1:02d}-{t2:04d}")
            tile_variants.add(f"{t1}-{t2}")
            for tv in tile_variants:
                if tv in db_idx['by_tile_name']:
                    r = db_idx['by_tile_name'][tv]
                    return r, 'tile_name', 1.0, []

    # ─── TIER 2: Code match (SK-12, SOV 35, G3, etc.) ───────────────────
    if drawing_codes:
        code_matches = []
        for code in drawing_codes:
            if code in db_idx['by_code']:
                r = db_idx['by_code'][code]
                # Verify no number conflict
                obj_nums = extract_numbers(r['name'])
                if not numbers_conflict(drawing_nums - extract_arabic(code.split('-')[-1]), obj_nums - extract_arabic(code.split('-')[-1])):
                    code_matches.append(r)
        if len(code_matches) == 1:
            return code_matches[0], 'code', 1.0, []
        elif len(code_matches) > 1:
            # Multiple code matches — ambiguous
            candidates.extend([{'record': r, 'method': 'code', 'score': 0.9} for r in code_matches])

    # ─── TIER 3: Exact name match ────────────────────────────────────────
    if clean_norm and clean_norm in db_idx['by_name_norm']:
        r = db_idx['by_name_norm'][clean_norm]
        obj_nums = extract_numbers(r['name'])
        obj_codes = parse_codes(r['name'])
        if not numbers_conflict(drawing_nums, obj_nums) and not codes_conflict(drawing_codes, obj_codes):
            return r, 'exact_name', 1.0, []

    # Also try with original objectName from index
    obj_name = primary.get('objectName', '')
    if obj_name:
        obj_norm = normalize_name(obj_name)
        if obj_norm in db_idx['by_name_norm']:
            r = db_idx['by_name_norm'][obj_norm]
            obj_nums = extract_numbers(r['name'])
            obj_codes = parse_codes(r['name'])
            if not numbers_conflict(drawing_nums, obj_nums) and not codes_conflict(drawing_codes, obj_codes):
                return r, 'exact_name', 1.0, []

    # ─── TIER 4: Synonym match ───────────────────────────────────────────
    if clean_norm and clean_norm in db_idx['by_synonym_norm']:
        r = db_idx['by_synonym_norm'][clean_norm]
        obj_nums = extract_numbers(r['name'])
        if not numbers_conflict(drawing_nums, obj_nums):
            return r, 'synonym', 0.95, []
    if obj_name:
        obj_norm2 = normalize_name(obj_name)
        if obj_norm2 in db_idx['by_synonym_norm']:
            r = db_idx['by_synonym_norm'][obj_norm2]
            return r, 'synonym', 0.95, []

    # ─── TIER 5: Name + number match ─────────────────────────────────────
    # Strip numbers from drawing name and try matching base + verify number
    if drawing_nums and clean_norm:
        # Remove roman/arabic from the name to get base
        base = clean_norm
        for rom_str, rom_val in ROMAN_MAP.items():
            base = re.sub(r'\b' + rom_str.lower() + r'\b', '', base)
        base = re.sub(r'\b\d+[a-z]?\b', '', base)
        base = re.sub(r'\s+', ' ', base).strip()

        if base and len(base) >= 3:
            # Find all objects with this base name
            base_matches = []
            for norm_name, r in db_idx['all_names_norm'].items():
                obj_base = norm_name
                for rom_str in ROMAN_MAP:
                    obj_base = re.sub(r'\b' + rom_str.lower() + r'\b', '', obj_base)
                obj_base = re.sub(r'\b\d+[a-z]?\b', '', obj_base)
                obj_base = re.sub(r'\s+', ' ', obj_base).strip()

                if obj_base == base:
                    obj_nums = extract_numbers(r['name'])
                    if numbers_compatible(drawing_nums, obj_nums) and drawing_nums == obj_nums:
                        base_matches.append(r)

            if len(base_matches) == 1:
                return base_matches[0], 'name_number', 0.92, []
            elif len(base_matches) > 1:
                candidates.extend([{'record': r, 'method': 'name_number', 'score': 0.85} for r in base_matches])

    # ─── TIER 6: Fuzzy matching (last resort) ────────────────────────────
    if clean_norm and len(clean_norm) >= 3:
        best_score = 0
        best_record = None
        fuzzy_candidates = []

        for norm_name, r in db_idx['all_names_norm'].items():
            if not norm_name:
                continue
            score = fuzzy_score(clean_norm, norm_name)
            if score >= 0.72:
                obj_nums = extract_numbers(r['name'])
                obj_codes = parse_codes(r['name'])
                # Hard reject on number/code conflict
                if numbers_conflict(drawing_nums, obj_nums):
                    continue
                if codes_conflict(drawing_codes, obj_codes):
                    continue
                fuzzy_candidates.append({'record': r, 'method': 'fuzzy', 'score': score})
                if score > best_score:
                    best_score = score
                    best_record = r

        # Only auto-assign if single clear winner with high score
        if best_score >= 0.88 and best_record:
            # Check no close runner-up
            sorted_fc = sorted(fuzzy_candidates, key=lambda x: x['score'], reverse=True)
            if len(sorted_fc) == 1 or (len(sorted_fc) > 1 and sorted_fc[0]['score'] - sorted_fc[1]['score'] > 0.08):
                return best_record, 'fuzzy', best_score, []

        if fuzzy_candidates:
            candidates.extend(fuzzy_candidates)

    # ─── No confident match ──────────────────────────────────────────────
    # Deduplicate candidates by record id
    seen = set()
    unique_cands = []
    for c in sorted(candidates, key=lambda x: x['score'], reverse=True):
        rid = c['record']['id']
        if rid not in seen:
            seen.add(rid)
            unique_cands.append(c)

    return None, 'none', 0, unique_cands[:5]


# ═══════════════════════════════════════════════════════════════════════════════
#  STEP 6: Run matching + apply overrides
# ═══════════════════════════════════════════════════════════════════════════════

def run_matching(dry_run=False):
    print("Loading data...")
    index_data = load_index()
    drawings = index_data['drawings']
    records = load_baza()
    overrides = load_overrides()

    print(f"  Drawings: {len(drawings)}")
    print(f"  Database records: {len(records)}")
    print(f"  Manual overrides: {len(overrides)}")

    db_idx = build_db_index(records)
    records_by_id = {r['id']: r for r in records}

    # Group multi-page drawings
    groups = group_drawings(drawings)
    print(f"  Logical drawing groups: {len(groups)}")
    multi_page = [g for g in groups if g['pageCount'] > 1]
    print(f"  Multi-page drawings: {len(multi_page)}")

    # Match each group
    results = {
        'assigned': [],        # confident match
        'review': [],          # multiple candidates
        'unmatched': [],       # no candidate found
        'conflict': [],        # number/code conflict detected
        'overridden': [],      # from match-overrides.json
    }

    assignment_map = {}  # groupKey → (recordId, method, score)

    for group in groups:
        gkey = group['groupKey']
        primary_fn = group['primaryFile']['fileName']

        # Check override first
        override_key = None
        for fn_key in [primary_fn, gkey]:
            if fn_key in overrides:
                override_key = fn_key
                break
        # Also check each page filename
        if not override_key:
            for page in group['pages']:
                if page['fileName'] in overrides:
                    override_key = page['fileName']
                    break

        if override_key:
            ov = overrides[override_key]
            rid = ov.get('recordId')
            if rid and rid in records_by_id:
                assignment_map[gkey] = (rid, 'override', 1.0)
                results['overridden'].append({
                    'group': group,
                    'record': records_by_id[rid],
                    'method': 'override',
                })
                continue
            elif rid is None:
                # Explicit "no match" override
                results['unmatched'].append({
                    'group': group,
                    'candidates': [],
                    'note': 'explicitly unmatched by override',
                })
                continue

        # Run matching
        record, method, score, candidates = match_drawing_group(group, db_idx)

        if record:
            assignment_map[gkey] = (record['id'], method, score)
            results['assigned'].append({
                'group': group,
                'record': record,
                'method': method,
                'score': score,
            })
        elif candidates:
            results['review'].append({
                'group': group,
                'candidates': candidates,
            })
        else:
            results['unmatched'].append({
                'group': group,
                'candidates': [],
                'note': '',
            })

    # ─── Stats ───────────────────────────────────────────────────────────
    total_groups = len(groups)
    total_pages = len(drawings)
    print(f"\n=== MATCHING RESULTS ===")
    print(f"  Total groups:     {total_groups}")
    print(f"  Total files:      {total_pages}")
    print(f"  Assigned:         {len(results['assigned'])} ({_pct(len(results['assigned']), total_groups)})")
    print(f"    by tile_name:   {sum(1 for r in results['assigned'] if r['method']=='tile_name')}")
    print(f"    by code:        {sum(1 for r in results['assigned'] if r['method']=='code')}")
    print(f"    by exact_name:  {sum(1 for r in results['assigned'] if r['method']=='exact_name')}")
    print(f"    by synonym:     {sum(1 for r in results['assigned'] if r['method']=='synonym')}")
    print(f"    by name_number: {sum(1 for r in results['assigned'] if r['method']=='name_number')}")
    print(f"    by fuzzy:       {sum(1 for r in results['assigned'] if r['method']=='fuzzy')}")
    print(f"  Overridden:       {len(results['overridden'])}")
    print(f"  For review:       {len(results['review'])}")
    print(f"  Unmatched:        {len(results['unmatched'])}")

    # ─── Generate report ─────────────────────────────────────────────────
    generate_report(results, groups, total_pages)

    # ─── Write index if not dry-run ──────────────────────────────────────
    if not dry_run:
        apply_to_index(index_data, groups, assignment_map, results)
        print(f"\nIndex updated: {INDEX_PATH}")
    else:
        print(f"\nDry-run mode — index NOT updated. Review {REPORT_PATH}")

    return results


def _pct(n, total):
    return f"{n/total*100:.1f}%" if total else "0%"


def apply_to_index(index_data, groups, assignment_map, results):
    """Write recordId + matchStatus back into every page of each group."""
    drawings = index_data['drawings']
    drawings_by_fn = {d['fileName']: d for d in drawings}

    # Build sets for review/unmatched
    review_keys = {r['group']['groupKey'] for r in results['review']}
    unmatched_keys = {r['group']['groupKey'] for r in results['unmatched']}

    for group in groups:
        gkey = group['groupKey']
        if gkey in assignment_map:
            rid, method, score = assignment_map[gkey]
            status = 'verified'
            notes = f'v3 match: {method}'
        elif gkey in review_keys:
            rid = None
            status = 'review'
            notes = 'v3: multiple candidates, needs review'
        elif gkey in unmatched_keys:
            rid = None
            status = 'unmatched'
            notes = 'v3: no match found'
        else:
            continue

        for page in group['pages']:
            fn = page['fileName']
            if fn in drawings_by_fn:
                d = drawings_by_fn[fn]
                if rid:
                    d['recordId'] = rid
                else:
                    d.pop('recordId', None)
                d['matchStatus'] = status
                d['notes'] = notes
                # Set groupKey for multi-page grouping
                if group['pageCount'] > 1:
                    d['drawingGroup'] = gkey
                    d['pageCount'] = group['pageCount']

    # Update metadata
    from datetime import datetime, timezone
    now = datetime.now(timezone.utc).isoformat()
    index_data['updatedAt'] = now
    index_data['matcherVersion'] = 'v3'

    with open(INDEX_PATH, 'w', encoding='utf-8') as f:
        json.dump(index_data, f, ensure_ascii=False, indent=1)


def generate_report(results, groups, total_pages):
    """Write MATCHING_REPORT_v3.md"""
    lines = []
    lines.append("# SOV Nacrti Matching Report v3")
    lines.append(f"\nGenerated: {__import__('datetime').datetime.now().isoformat()}")
    lines.append(f"\nTotal drawing groups: {len(groups)} ({total_pages} files)")
    lines.append(f"Assigned: {len(results['assigned'])}")
    lines.append(f"Overridden: {len(results['overridden'])}")
    lines.append(f"For review: {len(results['review'])}")
    lines.append(f"Unmatched: {len(results['unmatched'])}")

    # Multi-page drawings
    multi = [g for g in groups if g['pageCount'] > 1]
    if multi:
        lines.append(f"\n## Multi-page drawings ({len(multi)})\n")
        lines.append("| Drawing | Pages |")
        lines.append("|---------|-------|")
        for g in sorted(multi, key=lambda x: x['groupKey']):
            fns = ', '.join(p['fileName'] for p in g['pages'])
            lines.append(f"| {g['groupKey']} | {g['pageCount']} ({fns}) |")

    # Assigned by method
    lines.append(f"\n## Assigned ({len(results['assigned'])})\n")
    by_method = defaultdict(list)
    for r in results['assigned']:
        by_method[r['method']].append(r)
    for method in ['tile_name', 'code', 'exact_name', 'synonym', 'name_number', 'fuzzy']:
        items = by_method.get(method, [])
        if items:
            lines.append(f"\n### {method} ({len(items)})\n")
            lines.append("| Drawing | → Object | ID |")
            lines.append("|---------|----------|----|")
            for r in sorted(items, key=lambda x: x['group']['groupKey']):
                lines.append(f"| {r['group']['groupKey']} | {r['record']['name']} | {r['record']['id']} |")

    # Overridden
    if results['overridden']:
        lines.append(f"\n## Overridden ({len(results['overridden'])})\n")
        lines.append("| Drawing | → Object | ID |")
        lines.append("|---------|----------|----|")
        for r in sorted(results['overridden'], key=lambda x: x['group']['groupKey']):
            lines.append(f"| {r['group']['groupKey']} | {r['record']['name']} | {r['record']['id']} |")

    # Review needed
    if results['review']:
        lines.append(f"\n## Needs review ({len(results['review'])})\n")
        lines.append("Add correct match to `match-overrides.json`:\n")
        lines.append("```json")
        lines.append('{')
        lines.append('  "FILENAME.webp": { "recordId": "CORRECT_ID" }')
        lines.append('}')
        lines.append("```\n")
        for r in sorted(results['review'], key=lambda x: x['group']['groupKey']):
            lines.append(f"### {r['group']['groupKey']}")
            lines.append(f"Files: {', '.join(p['fileName'] for p in r['group']['pages'])}\n")
            lines.append("| Candidate | Score | Method |")
            lines.append("|-----------|-------|--------|")
            for c in r['candidates']:
                lines.append(f"| {c['record']['name']} (id={c['record']['id']}) | {c['score']:.2f} | {c['method']} |")
            lines.append("")

    # Unmatched
    if results['unmatched']:
        lines.append(f"\n## Unmatched ({len(results['unmatched'])})\n")
        lines.append("| Drawing | Files | Note |")
        lines.append("|---------|-------|------|")
        for r in sorted(results['unmatched'], key=lambda x: x['group']['groupKey']):
            fns = ', '.join(p['fileName'] for p in r['group']['pages'])
            note = r.get('note', '')
            lines.append(f"| {r['group']['groupKey']} | {fns} | {note} |")

    with open(REPORT_PATH, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    print(f"Report written: {REPORT_PATH}")


# ═══════════════════════════════════════════════════════════════════════════════
#  Main
# ═══════════════════════════════════════════════════════════════════════════════

if __name__ == '__main__':
    dry_run = '--dry-run' in sys.argv
    run_matching(dry_run=dry_run)
