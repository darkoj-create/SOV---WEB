# SOV Web v6.1.43c — Gmail sync bez praznog endpointa

- Uklonjena ovisnost o nedostajućem Apps Script `/exec` endpointu.
- Gumb ručnog synca sada upisuje stvarni zahtjev u `sov_gmail_sync_requests`.
- Povezana automatika obrađuje queued zahtjeve koristeći Gmail i Supabase pristup.
- Web prikazuje queued, processing, completed i failed stanje te rezultate zadnjeg synca.
- Raspon ručnog synca ostaje 28 dana.
