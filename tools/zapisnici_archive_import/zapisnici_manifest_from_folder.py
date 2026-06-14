#!/usr/bin/env python3
"""Create a CSV manifest for SOV zapisnici archive upload.
Usage: python zapisnici_manifest_from_folder.py /path/to/zapisnici > manifest.csv
This does not upload files; it prepares metadata for SQL/storage migration.
"""
import csv, sys, re
from pathlib import Path

def parse(path: Path, root: Path):
    rel = path.relative_to(root).as_posix()
    hay = rel
    y = re.search(r'(19[6-9][0-9]|20[0-2][0-9])', hay)
    year = int(y.group(1)) if y else ''
    date = ''
    month = ''
    day = ''
    m = re.search(r'(19[6-9][0-9]|20[0-2][0-9])[-_. ]([01]?[0-9])[-_. ]([0-3]?[0-9])', hay)
    if m:
        year = int(m.group(1)); month = int(m.group(2)); day = int(m.group(3)); date=f'{year:04d}-{month:02d}-{day:02d}'
    ext = path.suffix.lower().lstrip('.') or 'file'
    title = re.sub(r'[_-]+',' ',path.stem).strip()
    safe = re.sub(r'[^a-zA-Z0-9_.-]+','-',path.name).strip('-')
    storage_path = f'zapisnici/{year or "unsorted"}/{safe}'
    return {
        'title': title,
        'document_type': 'zapisnik sastanka',
        'document_date': date,
        'year': year,
        'month': month,
        'day': day,
        'original_filename': path.name,
        'storage_path': storage_path,
        'format': ext.upper(),
        'size_bytes': path.stat().st_size,
        'source_batch': root.name,
        'summary': ''
    }

def main():
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr); return 2
    root = Path(sys.argv[1]).expanduser().resolve()
    exts = {'.pdf','.doc','.docx','.jpg','.jpeg','.png','.webp','.txt'}
    rows = [parse(p, root) for p in sorted(root.rglob('*')) if p.is_file() and p.suffix.lower() in exts]
    w = csv.DictWriter(sys.stdout, fieldnames=['title','document_type','document_date','year','month','day','original_filename','storage_path','format','size_bytes','source_batch','summary'])
    w.writeheader(); w.writerows(rows)
    return 0
if __name__ == '__main__':
    raise SystemExit(main())
