# SOV Web v6.1.35 — Oružar inventura search

Baseline: sov-web-build-v6.1.34-user-portal-topbar-fix.zip

## Changed
- oruzar-master-inventura.html: dodana pretraga opreme direktno u kontrole inventure
- assets/oruzar-master-clean.js: dodana tolerantna pretraga koja trpi tipfelere, dijakritiku i djelomične nazive
- assets/oruzar-master-clean.js: kada je pretraga aktivna, prikazuju se artikli direktno umjesto da korisnik mora prolaziti kategorija → podkategorija
- assets/oruzar-master-clean.js: dodan gumb "Očisti pretragu"
- oružar master stranice: cache-bust za oruzar-master-clean.js dignut na v6.1.35

## Search behavior
- radi bez hrvatskih kvačica
- trpi sitne tipfelere
- radi po nazivu, kategoriji, podkategoriji, modelu, lokaciji i napomeni
- ako ima previše rezultata, prikazuje prvih 120 da UI ne eksplodira

## Not changed
- Nema SQL promjena
- Nema APK promjena
- Nema promjena u Supabase pozivima
- Nema promjena u save/delete/export funkcijama
- Nema promjena u CleanArmory onclick handlerima
- Interni source/XLS natpisi ostaju uklonjeni iz kartica
