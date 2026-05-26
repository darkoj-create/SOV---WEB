# SOV Web v4.96 — Nacrti sync config fallback fix

Popravljeno:
- Sync nacrta više ne puca odmah ako endpoint nije upisan u config.
- Ako endpoint nije konfiguriran, admin/arhivar može zalijepiti Apps Script /exec URL u prompt, sprema se u localStorage.
- `baza.html` i `pregled-baze.html` koriste isti fallback.
- Dodan `GOOGLE_APPS_SCRIPT_NACRTI_SYNC_v4_96.gs`.

Preporučeno trajno rješenje:
1. Deployati `GOOGLE_APPS_SCRIPT_NACRTI_SYNC_v4_96.gs` kao Web App.
2. Execute as: Me.
3. Who has access: Anyone with the link.
4. /exec URL upisati u `assets/supabase-config.js` kao `window.SOV_DRAWINGS_SYNC_ENDPOINT`.
