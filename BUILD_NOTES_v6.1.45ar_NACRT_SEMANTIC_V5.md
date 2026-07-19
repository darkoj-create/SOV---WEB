# SOV Web v6.1.45ar — Nacrt Semantic V5

Referentni test-set:
- Kopitarka ZIP + službeni PDF
- Kozje tajne ZIP + službeni PDF
- Ledeno gnijezdo ZIP + službeni PDF
- raniji testovi: Spilja s fundom, Jama u Koritima, Gorski prištić

## Parser i scrapovi
- učitavaju se svi neprazni `.tdr` scrapovi iz ZIP-a
- čuvaju se odvojene liste `plans`, `profiles`, `sections` i `allScraps`
- dodatni plan/profil/presjek više se ne gubi; do dva dodatna scrapa prikazuju se u pomoćnim panelima
- nepoznati tipovi završavaju u `semanticDiagnostics` i sigurnom fallbacku

## Semantičke linije
- `wall` — puna crna linija
- `wall:presumed` — isprekidana crna linija
- `wall:ice` — plava linija leda
- `pit` — linija s bočnim zupcima
- `overhang` — isprekidani prevjes
- `arrow` i `air-draught` — orijentirane strelice
- `border` — lagana isprekidana granica materijala

## Materijali i simboli
- `blocks` — jedan fasetirani kamen po TopoDroid točki
- `debris` — granulirano područje šuta s kamenjem
- `pebbles` — skup oblutaka
- `snow` — zvjezdasti simbol
- `paleo-material` — paleontološki/bone simbol
- `continuation` — jasan znak `?`
- vegetacija — četinjača ili listopadno drvo prema eksplicitnom simbolu ili odvojenoj user-line komponenti

## Stabilnost
- samo SVG operacije
- nema Canvasa, skeniranja piksela ni generičkog farbanja šupljine/terena
- nepoznati materijali ostaju neobojeni
