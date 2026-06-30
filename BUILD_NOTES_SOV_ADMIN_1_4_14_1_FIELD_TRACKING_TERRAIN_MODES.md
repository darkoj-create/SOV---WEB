# SOV Admin v1.4.14.1 — Field Tracking tereni + modovi

- Tracking kartica na izletu sada nudi modove: Lite auto ping i Ruta/GPX.
- Lite mod šalje rjeđe pingove 60–120 s za minimalnu bateriju.
- Ruta/GPX mod šalje gušći trail oko 20 s normalno, 90–120 s low battery.
- API prvo pokušava `sov_tracking_start_session_v2`, uz fallback na stari RPC.
- Home login ikonica dobila visok zIndex da bude klikabilna i otvara pravi login route.
