# SOV web v6.1.45ak — Karta refresh loop hard fix

## Problem
`Karta.html` je bio samo redirect-wrapper prema `karta.html` i imao je `<meta http-equiv="refresh">`.
Ako browser, Vercel route, cache ili stari link ponovno vrati korisnika na `Karta.html`, stranica ulazi u refresh/redirect petlju.

## Rješenje
- `Karta.html` je zamijenjen punom funkcionalnom kartom, istog sadržaja kao `karta.html`.
- Uklonjen je sav redirect/meta-refresh iz `Karta.html`.
- Dodani su kompatibilni aliasi bez redirecta:
  - `KARTA.HTM`
  - `KARTA.HTML`
  - `Karta.htm`
  - `karta.htm`

## Namjerno nije mijenjano
- Nema promjene SQL-a.
- Nema promjene Supabase API/RPC poziva.
- Nema promjene poslovne logike karte, markera, baze objekata ni WMS slojeva.

## Provjera
- `Karta.html`, `KARTA.HTM`, `KARTA.HTML`, `Karta.htm` i `karta.htm` nemaju `<meta refresh>`.
- `karta.html` ostaje glavna kanonska stranica.
