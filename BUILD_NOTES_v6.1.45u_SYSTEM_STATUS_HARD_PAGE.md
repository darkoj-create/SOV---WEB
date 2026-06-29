# SOV web v6.1.45u — System status hard page fix

Problem: `/system-status` i dalje je izgledao kao da ne postoji, jer je status ekran ovisio o JS loaderu i/ili relativnim asset putanjama kroz folder fallback.

Fix:
- `system-status.html` je sada kompletna samostalna HTML stranica, bez ovisnosti o `assets/sov-ecosystem-status.js`.
- Isti fizički HTML upisan je u:
  - `system-status.html`
  - `system-status/index.html`
  - `sov-system-status.html`
  - `sov-system-status/index.html`
  - `status.html`
  - `status/index.html`
- Asset putanje su apsolutne (`/assets/sov-logo.png`) pa ne pucaju na clean URL/folder rutama.
- `dashboard.html` link sada cilja `/system-status`.
- `vercel.json` zadržava rewrites za sve alias rute.

Nema SQL promjena.
