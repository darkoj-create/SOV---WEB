# SOV Web v6.1.45ag — Gmail zapisnici → Izleti category approval fix

Popravlja grešku kod odobravanja najava iz zapisnika u kalendar:

`new row for relation "sov_trips" violates check constraint "sov_trips_trip_category_check"`

## Uzrok

Parser najava iz Gmail/native zapisnika mogao je spremiti `trip_category` kao `izlet`, `seminar`, `ekspedicija` ili `vježba`, dok `sov_trips` check constraint prima samo kanonske vrijednosti s velikim slovom.

## Promjene

- Dodan SQL patch `SUPABASE_SOV_GMAIL_TRIP_CATEGORY_APPROVAL_FIX_v6_1_45ag.sql`.
- SQL dodaje `public.sov_normalize_trip_category(text)`.
- SQL dodaje trigger `trg_sov_trips_normalize_category` koji prije svakog upisa/izmjene u `sov_trips` normalizira kategoriju.
- `assets/zapisnici-najave-v6143a.js` sada nove najave sprema s kanonskom kategorijom.
- `assets/sov-trips-cloud.js` dodatno normalizira kategorije prije spremanja iz web forme.
- `zapisnici-najave.html` dobio novi cache-bust.

## Napomena

SQL patch je već primijenjen na Supabase projektu `SOV-web`, ali je file uključen u ZIP radi dokumentacije i ponovnog deploya ako zatreba.
