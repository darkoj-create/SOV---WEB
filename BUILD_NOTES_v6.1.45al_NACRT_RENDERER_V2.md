# SOV Web v6.1.45al — Nacrt Renderer v2

## Promjene

- TopoDroid `P`, `L`, `A`, `U`, `X` i `T` zapisi čitaju se prema stvarnom TDR rasporedu i verzijskim pragovima.
- Nacrt više ne računa profilnu poligonalnu liniju iz približne SQL E-komponente, nego povezuje stvarne TDR pozicije stanica.
- Profil je postavljen lijevo i prikazan kao glavni crtež; tlocrt je desno.
- Dodana je dubinska skala 0–40+ m i mjerilo tlocrta.
- Učitavaju se TopoDroid presjeci (`plotType = 0`) i prikazuju uz pripadnu stanicu.
- Dodani su prikazi simbola za stup, sipar, blokove, ulaz, nastavak i presjek.
- Naziv ZIP/survey zapisa pretvara se iz `Jama_u_koritima_1` u čitljiv oblik.
- Naslovni blok proširen je poljima za lokaciju, topografa, mjerača, digitalnu obradu, HTRS96/TM koordinate, nadmorsku visinu, broj pločice i članove ekipe.
- Izvoz SVG i PNG ostaje kompatibilan s postojećim korisničkim tokom.

## Validacija

Provjereno s `Jama_u_koritima_1-1s.zip` / TopoDroid 6.3.12:

- profil: 29 linija, 9 simbola, 7 stanica
- tlocrt: 43 linije, 4 simbola, 7 stanica
- presjek 4: 1 linija, 1 stanica
- duljina 46,41 m, horizontalna 18,19 m, dubina 38,41 m
- SVG render 1240 × 1754 bez `NaN` koordinata

## Ograničenje

TopoDroid ZIP ovog objekta nema area-poligone ni ručno dodane teksture iz finalnog PDF-a. Renderer zato može prenijeti geometriju, poligonalnu liniju, stanice, presjeke i simbole, ali ne može automatski rekonstruirati sve ručno crtane boje i teksture iz naknadne digitalne obrade.
