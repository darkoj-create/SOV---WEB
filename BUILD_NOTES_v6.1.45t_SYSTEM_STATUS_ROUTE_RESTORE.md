# SOV web v6.1.45t — System status route restore

## Popravljeno
- Vraćen pristup system status dashboardu na clean URL-u `/system-status`.
- Dodani eksplicitni Vercel rewrites: `/system-status`, `/sov-system-status`, `/status` -> `system-status.html`.
- Dodani fizički fallback folderi `system-status/index.html`, `sov-system-status/index.html`, `status/index.html`, da ruta radi i ako cleanUrls/rewrite ne bude aktivan.
- U `dashboard.html` maknut floating debug gumb i dodana normalna kartica `Status sustava` u internim/admin alatima.
- Nema SQL promjena.
