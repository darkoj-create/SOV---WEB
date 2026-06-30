# SOV web v6.1.45ak — Karta refresh loop hard fix

- `Karta.html` više nije redirect-wrapper nego puna karta.
- Uklonjen je meta refresh/redirect loop.
- Dodani uppercase/HTM aliasi bez redirecta za stare linkove.
- Nema SQL-a i nema promjene poslovne logike.

---

# SOV web v6.1.45aj — prikaz izleta iz zapisnika u Izletima i kalendaru

## Problem

Odobravanje najave iz Gmail/native zapisnika sada uspješno upisuje red u `sov_trips`, ali korisnik ga nije vidio u modulu Izleti ni na kalendaru.

Live provjera baze pokazala je da je izlet uredno spremljen:

- naslov/lokacija: Bunovac
- datum: 10.07.2026. – 12.07.2026.
- `status`: `planned`
- `visibility`: `club`
- `trip_category`: `Izlet`
- `source`: `meeting_minutes_announcement`

Uzrok nije bio insert nego prikaz: UI je ostajao na tekućem mjesecu, dok je novi izlet u srpnju 2026.

## Promjene

- `izleti-cloud.html`
  - nakon učitavanja izleta automatski se odabire mjesec prvog budućeg izleta ako trenutni mjesec nema stavki;
  - ručno mijenjanje mjeseca se poštuje;
  - klik na Osvježi resetira ručni odabir i ponovno pronalazi najbliži relevantni mjesec.

- `kalendar-izleta.html`
  - ista logika primijenjena na kalendar;
  - kalendar se automatski prebacuje na mjesec prvog budućeg izleta/događaja ako je trenutni mjesec prazan;
  - ručni pomak mjeseca ostaje ručni dok korisnik opet ne klikne Osvježi.

- Verzije usklađene na `6.1.45aj`.

## SQL

Nema SQL promjena. Baza već sadrži red u `sov_trips`, a `sov_trips_mobile_feed` ga vidi.

## Smoke test

1. Otvori `izleti-cloud.html`.
2. Klikni Osvježi.
3. Ako u tekućem mjesecu nema izleta, stranica se treba prebaciti na mjesec prvog budućeg izleta.
4. Provjeri da se Bunovac vidi u kategoriji `Izlet`.
5. Otvori `kalendar-izleta.html` i provjeri isti mjesec/prikaz.
