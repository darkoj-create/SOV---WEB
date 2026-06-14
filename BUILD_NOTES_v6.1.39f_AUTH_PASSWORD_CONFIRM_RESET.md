# SOV web v6.1.39f — Auth password confirmation + reset password

Baseline: `sov-web-build-v6.1.39e-oruzar-xls-three-tabs-combined.zip`.

## Što je promijenjeno

### Registracija
- Login/registracija forma sada traži dvostruki unos lozinke:
  - `Lozinka`
  - `Ponovi lozinku`
- Frontend blokira registraciju ako se lozinke ne poklapaju.
- `SOVAuth.register()` dodatno provjerava lozinku na auth helper razini.
- Minimalna duljina lozinke: 8 znakova.

### Reset lozinke
- Login stranica dobila je treći tab: `Reset lozinke`.
- Dodan je flow za slanje Supabase Auth reset emaila:
  - `SOVAuth.requestPasswordReset(email)`
- Dodana je nova javna stranica:
  - `reset-password.html`
- Reset stranica traži dvostruki unos nove lozinke i poziva:
  - `SOVAuth.updatePassword(password, passwordConfirm)`
- Nakon uspješnog resetiranja korisnik se odjavljuje i vraća na login.

### Cache
- HTML stranice sada učitavaju `assets/auth.js?v=6.1.39f-auth-reset`.

## Što nije dirano
- SQL nije mijenjan.
- Oružarstvo nije dirano.
- Inventar/inventura nisu dirani.
- Role i admin approval logika nisu mijenjani.
- Postojeći profile/RPC signup sync ostaje isti.

## Napomena za deploy
Za reset email mora biti dopušten redirect URL u Supabase Auth postavkama, npr.:
- `https://tvoja-domena/reset-password.html`
- za preview/staging dodati i odgovarajući preview URL.
