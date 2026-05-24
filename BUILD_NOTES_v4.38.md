# v4.38 — Role-gated oružarstvo

- Oružar panel, Inventura i Import/SQL su vidljivi samo za role `admin` i `oruzar`.
- Uklonjen preview/localStorage bypass koji je običnog korisnika mogao prebaciti u admin prikaz.
- Direktni ulaz u `oruzarstvo-import.html` sada traži armory ovlast i preusmjerava bez prava.
- Članovima ostaju katalog, zahtjevi i moji zahtjevi.
