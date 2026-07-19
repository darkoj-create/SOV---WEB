# SOV Web v6.1.45ao — Nacrt V4 Crash Rollback

Hitni rollback nakon što je v4 halo algoritam zamrzavao ili rušio browser.

## Uzrok
`nacrt-v4.js` je za gotovo svaki piksel pretraživao velik broj okolnih piksela kako bi izračunao udaljenost do zida. Na većim canvasima to vodi do milijardi JavaScript operacija.

## Rollback
- `nacrt-v4.js` ostaje u repozitoriju, ali se više ne učitava
- aktivni stabilni stack je: `nacrt-core.js` + `nacrt-tdr-fix.js` + `nacrt-v2.js` + `nacrt-v3.js`
- nema promjena parsera, geometrije ili podataka

## Plan za optimizirani v4
- zadržati linearnu scanline masku šupljine
- halo napraviti Canvas/SVG blur/dilation operacijom, bez per-pixel radius pretrage
- iz halo sloja oduzeti masku unutrašnjosti (`destination-out`)
- ograničiti render canvas na siguran broj piksela
- dodati brzi fallback bez halo efekta ako obrada traje predugo
- testirati na Gorskom prištiću i Jami u Koritima prije ponovne aktivacije
