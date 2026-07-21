# 1.4.55a — Offline nacrti u APK-u (bundled WebP)

## Sto je novo
- Nacrti se mogu ugraditi u APK: assets/nacrti_bundled/ (*.webp + index.json)
- DriveDrawingsRepository: bundled index je PRVI izvor (100% offline);
  "Osvjezi" spaja s Drive rezultatima; pad mreze -> fallback na bundled
- ensureLocalCopy: asset se kopira u filesDir/Offline/nacrti/bundled/ kod prvog otvaranja
- RecordDetailFeature: thumbnail + viewer koriste ensureLocalCopy (2 linije)

## Kako napuniti nacrte
1. pip install pillow
2. python convert_nacrti.py "D:\nacrti_png" --out "app\src\main\assets\nacrti_bundled"
3. Build APK kao inace (nista u gradle-u ne treba mijenjati)
Napomena: velicina foldera nacrti_bundled ~ koliko APK naraste.
Manji APK: --max-side 2000 --quality 70

## Izmijenjene datoteke
- app/src/main/java/com/darko/speleov1/util/DriveDrawingsRepository.kt
- app/src/main/java/com/darko/speleov1/RecordDetailFeature.kt
- app/src/main/assets/nacrti_bundled/index.json (placeholder, prazan)

## Test
Airplane mode -> objekt -> Nacrti: thumbnaili odmah, klik -> fullscreen, bez mreze.
