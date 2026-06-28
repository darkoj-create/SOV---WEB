# SOV web v6.1.45y — System status route hard link

Fix nakon live 404 na `/system-status`.

## Promjene
- Dashboard kartica sada linka direktno na `system-status.html`, fizički HTML file.
- `/system-status`, `/status`, `/sov-system-status` folder fallbackovi ostaju i redirectaju na `/system-status.html`.
- `vercel.json` koristi explicit redirects na `/system-status.html`.
- System status WOW admin dashboard iz 45v/45x ostaje.
- Member auth restore iz 45x ostaje.

## SQL
- Nema SQL promjena.
