# v5.16 — Trip Offline Area Picker Real Fix

Popravlja stvarni bug u `izleti.html`: `startAreaSelect()` je prvo palio `selecting=true`, a zatim `clearArea()` odmah gasio odabir. Zato klikovi na karti nisu radili ništa.

Sada:
- clear ide prije paljenja odabira
- prvi klik/tap sprema prvi kut
- drugi klik/tap sprema drugi kut
- crta se pravokutnik
- karta zooma na odabrano područje
- gumb je preimenovan u “Preuzmi offline mapu”
- live status jasno kaže što treba napraviti

Nema SQL promjene.
