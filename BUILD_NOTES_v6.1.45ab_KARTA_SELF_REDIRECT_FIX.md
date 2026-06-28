# SOV web v6.1.45ab — karta self-redirect hard fix

- Uklonjen svaki self-redirect iz `karta.html`.
- `karta.html` sada ima build marker i ne sadrži `location.replace('karta.html'...)` ni meta refresh na samu sebe.
- `Karta.html` ostaje samo sigurni alias na `karta.html`, ali sa guardom koji ne može vrtjeti petlju ako ga server ikad servira pod krivom putanjom.
- `baza.html` redirect je guardan protiv self-loop scenarija.
- Provjereno grepom: `karta.html` nema `location.replace`.
- Nema SQL promjena.
