# SOV Web v6.1.27 — UI fixes

Baseline: sov-web-build-v6.1.26-ui-ux-cleanup.zip

## Changed
- dashboard.html: v6.1.4 tag ažuriran na v6.1.26 u sekciji "Build pravila"
- dashboard.html: timeout fallback za "Učitavam prava…" (6s → "Prijavi se...")
- dashboard.html: audit/device prazno stanje promijenjeno u "Nema podataka."
- index.html: uklonjen .yt-note developer komentar iz YouTube sekcije
- login.html: dodana statička napomena o pending registraciji i kontaktu

## Not changed
- Nema SQL promjena
- Nema APK promjena
- Nema promjena u vanjskim JS fileovima (auth.js, sov-shell, sov-trips-cloud itd.)
- Nema promjena u Supabase RPC pozivima

## Out of scope (needs JS changes)
- "Napiši članak" u navigaciji — zahtijeva promjenu sov-shell-v55825.js LINKS array
- Dva kalendara — zahtijeva JS/data konsolidaciju
