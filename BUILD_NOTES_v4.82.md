# v4.82 — Global Inbox / kuverta obavijesti

Dodano:
- globalna kuverta/inbox desno gore na SOV Cloud stranicama
- svi korisnici mogu primati i čitati obavijesti
- admin i oružar mogu kroz istu kuvertu napisati novu obavijest
- targetiranje: svi korisnici, samo članovi, admin+oružar, samo admin
- unread badge i označi pročitano
- Supabase tablice + RLS u `SUPABASE_SOV_INBOX_v4_82.sql`
- localStorage fallback ako Supabase još nije spreman

Ne dira inventar, import, posudbe ni postojeće oružarstvo tablice.
