# SOV Admin v1.4.43a — Accessibility labels

Baza: `v1.4.42a-logger-thread-hygiene`.

## Opseg

Faza 3 / 3.1 UX cleanup: dodani su `contentDescription` opisi za Compose ikone koje su bile bez opisa.

## Izmjene

- Bottom navigation ikone sada imaju opis: Pretraga, Karta, Izleti, Kalkulator/Alati, Slojevi.
- Akcijske ikone u karticama, dialogima i toolbarima više ne koriste `contentDescription = null`.
- Generičke ikone za akcije dobile su neutralne opise: Akcija, Status, Ikona, Očisti, Preuzmi, Podijeli, Uvoz datoteke itd.
- Namjerno je ostavljen `contentDescription = null` samo na dekorativnoj pozadinskoj slici u `CaveScreenBackground.kt`, jer to ne treba čitati screen reader.

## Nije dirano

- Supabase i RLS.
- Self-update sustav.
- Apps Script zaštita.
- Manifest intent-filteri.
- UI layout i poslovna logika.

## Verzija

- `versionCode = 900141`
- `versionName = "1.4.43a-accessibility-labels"`
