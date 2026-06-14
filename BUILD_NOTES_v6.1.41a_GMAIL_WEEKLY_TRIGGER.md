# SOV web v6.1.41a — Gmail zapisnici weekly trigger

Baseline: v6.1.41 Gmail minutes import.

## Promjena

Apps Script trigger za `processSovGmailZapisnici` više nije svakih 15 minuta.

`installSovGmailZapisniciTrigger()` sada briše stare triggere za tu funkciju i instalira samo jedan tjedni trigger:

- srijeda
- oko 23:50
- jednom tjedno

## Nije dirano

- Supabase SQL/schema
- parser najava
- dedupe logika
- Oružarstvo
- XLS export
- auth

## Napomena

Google Apps Script weekly trigger koristi `nearMinute(50)`, pa pokretanje može biti par minuta oko 23:50. Timezone Apps Script projekta treba biti Europe/Zagreb.
