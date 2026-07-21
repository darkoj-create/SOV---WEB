#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
convert_nacrti.py — batch konverzija PNG/JPG/TIFF nacrta u lossy WebP + generiranje
index.json za SOV app (assets/nacrti_bundled/).

Upotreba (Windows):
    pip install pillow
    python convert_nacrti.py "D:\\nacrti_png" --out "..\\app\\src\\main\\assets\\nacrti_bundled"

Opcije:
    --max-side 2600     najduža stranica slike (px); veće se smanjuju (0 = bez smanjivanja)
    --quality 78        WebP kvaliteta (lossy). 70–85 je razuman raspon za nacrte
    --lossless          lossless WebP umjesto lossy (puno veće datoteke)
    --include-pdf       kopiraj i PDF-ove (bez konverzije)
    --drive-index X     opcionalno: JSON s Drive indeksa (listDrawings) — prenosi
                        metapodatke (recordId, katastarId, objectName...) po imenu datoteke

Rezultat:
    <out>/index.json + <out>/*.webp
    App (DriveDrawingsRepository) čita assets/nacrti_bundled/index.json i matcha
    nacrte po imenu datoteke, potpuno offline.
"""
import argparse
import json
import os
import re
import sys
import unicodedata
from datetime import datetime, timezone

try:
    from PIL import Image
except ImportError:
    sys.exit("Nedostaje Pillow. Instaliraj:  pip install pillow")

Image.MAX_IMAGE_PIXELS = None  # veliki skenovi nacrta

IMAGE_EXT = {".png", ".jpg", ".jpeg", ".tif", ".tiff", ".webp", ".bmp"}


def sanitize_filename(name: str) -> str:
    # zadrži dijakritike (app ih normalizira kod matchanja), makni samo nedozvoljene znakove
    name = re.sub(r'[\\/:*?"<>|]+', "_", name)
    name = re.sub(r"\s+", " ", name).strip()
    return name[:140]


def normalize_for_match(value: str) -> str:
    """Ista normalizacija kao u appu (normalizeForMatch) — za spajanje s Drive indeksom."""
    value = re.sub(r"\.[A-Za-z0-9]{1,5}$", " ", value)
    value = unicodedata.normalize("NFD", value)
    value = "".join(c for c in value if not unicodedata.combining(c))
    value = value.replace("đ", "dj").replace("Đ", "dj")  # đ/Đ
    value = value.lower()
    value = re.sub(r"[_\-–—/.,;:()\[\]{}]+", " ", value)
    value = re.sub(r"[^a-z0-9 ]+", " ", value)
    return re.sub(r"\s+", " ", value).strip()


def load_drive_metadata(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    meta = {}
    for d in data.get("drawings", []):
        key = normalize_for_match(d.get("fileName") or d.get("name") or "")
        if key:
            meta[key] = d
    return meta


META_FIELDS = [
    "recordId", "katastarId", "objectName", "detectedObjectName",
    "detectedKatastarNumber", "detectedCadastralNumber", "detectedTile",
    "detectedLocation", "matchStatus", "notes",
]


def convert_one(src: str, dst: str, max_side: int, quality: int, lossless: bool) -> int:
    with Image.open(src) as im:
        im.load()
        if im.mode in ("P", "LA"):
            im = im.convert("RGBA")
        elif im.mode not in ("RGB", "RGBA", "L"):
            im = im.convert("RGB")
        w, h = im.size
        if max_side > 0 and max(w, h) > max_side:
            scale = max_side / float(max(w, h))
            im = im.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
        if lossless:
            im.save(dst, "WEBP", lossless=True, method=6)
        else:
            im.save(dst, "WEBP", quality=quality, method=6)
    return os.path.getsize(dst)


def main() -> None:
    ap = argparse.ArgumentParser(description="PNG -> WebP konverzija nacrta za SOV APK")
    ap.add_argument("input", help="folder s izvornim nacrtima (rekurzivno)")
    ap.add_argument("--out", default="nacrti_bundled", help="izlazni folder (assets/nacrti_bundled)")
    ap.add_argument("--max-side", type=int, default=2600)
    ap.add_argument("--quality", type=int, default=78)
    ap.add_argument("--lossless", action="store_true")
    ap.add_argument("--include-pdf", action="store_true")
    ap.add_argument("--drive-index", default=None)
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    drive_meta = load_drive_metadata(args.drive_index) if args.drive_index else {}

    drawings, used_names = [], set()
    total_in = total_out = converted = copied = failed = 0

    sources = []
    for root, _dirs, files in os.walk(args.input):
        for fn in files:
            sources.append(os.path.join(root, fn))
    sources.sort(key=lambda p: os.path.basename(p).lower())

    for src in sources:
        base, ext = os.path.splitext(os.path.basename(src))
        ext = ext.lower()
        is_img = ext in IMAGE_EXT
        is_pdf = ext == ".pdf"
        if not is_img and not (is_pdf and args.include_pdf):
            continue

        out_name = sanitize_filename(base) + (".pdf" if is_pdf else ".webp")
        stem, suffix = os.path.splitext(out_name)
        n = 2
        while out_name.lower() in used_names:
            out_name = f"{stem}_{n}{suffix}"
            n += 1
        used_names.add(out_name.lower())

        dst = os.path.join(args.out, out_name)
        src_size = os.path.getsize(src)
        try:
            if is_pdf:
                with open(src, "rb") as fi, open(dst, "wb") as fo:
                    fo.write(fi.read())
                out_size = src_size
                copied += 1
            else:
                out_size = convert_one(src, dst, args.max_side, args.quality, args.lossless)
                converted += 1
        except Exception as err:
            print(f"  GRESKA {os.path.basename(src)}: {err}")
            failed += 1
            continue

        total_in += src_size
        total_out += out_size

        entry = {
            "fileId": "asset:" + out_name,
            "fileName": out_name,
            "mimeType": "application/pdf" if is_pdf else "image/webp",
            "sizeBytes": out_size,
        }
        meta = drive_meta.get(normalize_for_match(os.path.basename(src)))
        if meta:
            for field in META_FIELDS:
                v = meta.get(field)
                if v not in (None, ""):
                    entry[field] = v
        drawings.append(entry)
        print(f"  {os.path.basename(src)} ({src_size/1024:.0f} KB) -> {out_name} ({out_size/1024:.0f} KB)")

    index = {
        "ok": True,
        "updatedAt": datetime.now(timezone.utc).isoformat(),
        "count": len(drawings),
        "totalCount": len(drawings),
        "indexBuiltAt": datetime.now(timezone.utc).isoformat(),
        "drawings": drawings,
    }
    with open(os.path.join(args.out, "index.json"), "w", encoding="utf-8") as f:
        json.dump(index, f, ensure_ascii=False, indent=1)

    print("\n=== SAZETAK ===")
    print(f"Konvertirano: {converted}, kopirano PDF: {copied}, greske: {failed}")
    print(f"Ulaz:  {total_in/1024/1024:.1f} MB")
    print(f"Izlaz: {total_out/1024/1024:.1f} MB  ({(total_out/max(total_in,1))*100:.1f}% originala)")
    print(f"Index: {os.path.join(args.out, 'index.json')} ({len(drawings)} nacrta)")


if __name__ == "__main__":
    main()
