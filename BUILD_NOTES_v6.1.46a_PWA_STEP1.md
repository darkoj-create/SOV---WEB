# SOV web v6.1.46a — PWA Step 1

## Opseg
Korak 1 PWA temelja za statični SOV web:

- `manifest.webmanifest`
- root `sw.js` service worker
- `offline.html`
- `assets/pwa-register.js`
- PWA ikone 192/512 + maskable varijante
- `tools/apply-pwa-step1.py` za dodavanje PWA tagova u sve HTML stranice
- `bump-version.py` proširen za `pwa-register.js` i `SW_VERSION`

## Pravila koja su poštovana

- Nema promjene postojeće poslovne logike.
- Nema promjene Supabase auth/logike.
- Service worker ne presreće Supabase, Apps Script ni ne-GET zahtjeve.
- SW ne radi automatski `skipWaiting`; nova verzija se aktivira tek klikom na banner “Nova verzija — osvježi”.
- Kill-switch `sw-disable.flag` je podržan.

## Ručni koraci za primjenu u web repou

1. Kopiraj nove datoteke u root web repoa.
2. Pokreni:

```bash
python tools/apply-pwa-step1.py 6.1.46a
python bump-version.py 6.1.46a
```

3. Provjeri diff.
4. Deploy na Vercel.

## Napomena

Ovdje je napravljena PWA Step 1 isporuka. Koraci 2 i 3 nisu rađeni jer se PWA mora testirati na stvarnom deployu prije uvođenja offline podataka i offline karte.
