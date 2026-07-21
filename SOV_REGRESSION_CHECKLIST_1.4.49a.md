# SOV v1.4.49a — Regression checklist prije stable baselinea

Ovo je ručni smoke/regression test prije zamrzavanja `v1.4.50a`.
Cilj nije testirati svaku sitnicu, nego potvrditi da zadnji security/UX cleanup nije slomio core tokove.

## 1. Build

- [ ] `./gradlew :app:compileDebugKotlin` prolazi
- [ ] `./gradlew :app:assembleDebug` prolazi
- [ ] `./gradlew :app:testDebugUnitTest` prolazi ili su neuspjeli testovi jasno nevezani uz ovu rundu

## 2. Login / session

- [ ] Fresh install otvara app bez crasha
- [ ] Login radi
- [ ] Logout radi
- [ ] Nakon restartanja app pamti session
- [ ] Nakon re-installa ne očekujemo stare private podatke iz internog storagea, ali native SOV folderi se i dalje skeniraju

## 3. Glavni ekrani

- [ ] Home se otvara
- [ ] Bottom navigation radi
- [ ] Tema: Tamna / Svijetla / Sustav radi bez crasha
- [ ] Settings se otvara
- [ ] Field status ekrani: GPS, kompas, signal se otvaraju

## 4. Karta / slojevi / importi

- [ ] Karta se otvara kao prijavljeni član
- [ ] WMS slojevi rade
- [ ] GPX import iz appa radi
- [ ] KML/KMZ import iz appa radi
- [ ] GPX/KML/KMZ otvaranje iz File Managera/WhatsAppa ne crasha
- [ ] Import ostaje vidljiv pod slojevima / imports
- [ ] MBTiles / GeoPackage ako ih koristiš rade kao prije

## 5. Moja baza / Katastar

- [ ] Moja baza se otvara
- [ ] Search radi
- [ ] Native folder scan ne zamrzava app
- [ ] Katastar je vidljiv samo registriranom/prijavljenom korisniku prema očekivanju
- [ ] Neprijavljen korisnik dobije login gate, ne crash/prazan ekran

## 6. Izleti

- [ ] Izleti feed se učitava kao prijavljeni korisnik
- [ ] Kalendar prikazuje izlete
- [ ] Detalj izleta se otvara
- [ ] Prijava/odjava na izlet radi ako je omogućeno
- [ ] Team/broadcast/tracking ekrani se ne ruše

## 7. Oružarstvo

- [ ] Katalog se učitava
- [ ] Kategorije i search rade
- [ ] Zahtjev člana se može otvoriti
- [ ] Oružar vidi ime/email tražitelja
- [ ] Statusi zahtjeva ostaju: Zatraženo → Izdano → Vraćeno / Djelomično vraćeno
- [ ] Inventura se otvara
- [ ] Bulk confirm traži potvrdu
- [ ] Undo nakon bulk confirma radi

## 8. Arhivar / predane jame

- [ ] Arhivar ekran se otvara za korisnika s rolom
- [ ] Obični user ne dobiva pristup bez role
- [ ] Detalj objekta se otvara
- [ ] Status/update akcije rade ako su testne

## 9. Speleo Runner

- [ ] Igra se pokreće
- [ ] Leaderboard se učitava
- [ ] Submit score radi
- [ ] LAKE/čamac dio ne crasha

## 10. Field Hub / Laptop Hub

- [ ] Laptop Hub ekran se otvara
- [ ] Adresa i PIN se mogu spremiti
- [ ] Lokalni `http://192.168.x.x` hub nije blokiran
- [ ] Test/fetch radi ako je hub aktivan

## 11. Web sanity check nakon Supabase security promjena

- [ ] Public homepage radi
- [ ] Public vijesti list/detail rade bez logina
- [ ] Public Runner leaderboard radi bez logina
- [ ] Dashboard/member web radi nakon logina
- [ ] Karta/Izleti bez logina ili traže login ili imaju namjerno public-lite ponašanje

## 12. Što ne dirati u finalnom baselineu

- [ ] Ne dirati self-update
- [ ] Ne dirati Supabase policy/RPC osim ako test pokaže konkretan bug
- [ ] Ne dodavati novi feature prije `v1.4.50a`
- [ ] Ne raditi masovni refactor `HomeAndToolsScreens.kt`
