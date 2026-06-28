# SOV Web v6.1.17 — Calendar multiday/date fix

- Creation form now shows date inputs as dd/mm/yyyy text fields.
- Trip creation now sends both start and end dates to Supabase.
- Trip payload parser now reads explicit endDate/end_date instead of relying only on a text range.
- Trip normalization now stores ISO start/end internally and formats display as dd/mm/yyyy.
- Calendar month cells now receive end_date and show multiday trips on every covered day.
