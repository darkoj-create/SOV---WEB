# SOV web v6.1.45ac — Predaj novu jamu simple flow

## Fokus
Pojednostavljen `predaj-novu-jamu.html` za članove: na prvom ekranu su samo obavezni/minimalni elementi, a napredno je skriveno pod "Dodaj više detalja".

## Promjene
- Dodan "Brza predaja" flow na vrhu.
- Obavezno jasno odvojeno od neobaveznog.
- Uklonjen vizualni šum: stepper i lijeva dupla navigacija više se ne prikazuju.
- Po defaultu se prikazuje WGS84 lat/lon; HTRS96/TM otvara se klikom.
- KML/GPX upload ostaje dostupan kao alternativa koordinatama.
- Fotke i nacrt/skica su jedini uploadi na prvom ekranu; TopoDroid i ostalo su u naprednom dijelu.
- Neobavezni detalji grupirani u `details` blokove.
- Zadržana postojeća Supabase POST/upload logika.
- Zadržan autosave, clear draft potvrda, file validation i coordinate warning.
- Success/error status ostaje kod gumba, bez skrolanja u prazno.
- Karta self-redirect fix iz 45ab ostaje.

## SQL
Nema SQL promjena.

## DOC/DOCX
Nema DOC/DOCX datoteka u buildu.
