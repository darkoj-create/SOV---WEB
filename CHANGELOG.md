
## v5.58.18 — Oružar Master UX/mobile cleanup

- Sređen UI za `oruzar-master-posudbe.html`, `oruzar-master-inventar.html` i `oruzar-master-inventura.html`.
- Uklonjen vidljivi debug/izvor tekst iz posudbi i nepotrebni `Notes` ulaz iz master navigacije.
- Dodan novi premium responsive CSS za operativne oružarske ekrane.
- Mobile browser layout: sticky header, horizontal nav, jedan stupac, veći touch gumbi i bottom-sheet modali.
- Nema SQL promjena.

## v5.58.16 — Oružarstvo access fix + mobile UX

- Popravljen ulaz u Oružarski dio: stari legacy patch je skrivao i brisao Oružar Master gumb.
- Dodan robustan čist ulaz za Webmaster/Admin/Oružar u navigaciji i ispod hero sekcije.
- Oružarstvo dodatno optimizirano za mobile browser: sticky header, horizontalni tabovi, bolji drawer i touch razmaci.
- Oružar Master stranice očišćene od inbox artefakta i starih version/dev tekstova.
- `sync-status.html` usklađen na v5.58.16.

## v5.58.14 — Oružarstvo clean UI + mobile browser polish

- Cleaned `oruzarstvo.html` for ordinary users: removed visible version/dev text, role explanation cards, noisy counters and storage-table wording.
- Added cleaner hero, simpler filters, cleaner category cards and item cards.
- Removed `sov-inbox` assets from Oružarstvo to avoid notification artefacts.
- Added mobile-browser layout polish: compact hero, one-column flow, sticky tabs, bottom-sheet request drawer and touch-friendly spacing.
- `sync-status.html` updated to v5.58.14.

## v5.58.13 — SQL function drop fix
- SQL hotfix for PostgreSQL 42P13 on `sov_has_permission(text)`: drops/recreates helper safely before applying Webmaster/Admin split.
- Supersedes v5.58.11/v5.58.12 SQL if either failed halfway.
- No frontend behavior change beyond version label.



## v5.58.13 — Webmaster/Admin role split + SQL function drop fix

- Added Webmaster role for Darko (`darko.jeras@gmail.com`) as the only tech/superadmin role.
- Admin no longer receives SQL/sync/tech tools.
- Admin keeps user approvals, notifications, normal operational module visibility and news editing.
- Dashboard role preview now includes Webmaster and separates Admin operative card from Webmaster tech card.
- Added clean `admin-notifications.html` instead of the old floating inbox artifact.
- `sync-status.html`, role manager and SQL tools are Webmaster-only.
- Added SQL patch `SUPABASE_SOV_ROLES_WEBMASTER_ADMIN_SPLIT_v5_58_12.sql`.
# Changelog

## v5.58.11 — Urednik role fix
- Popravljen `sov_current_role()`: više ne uzima Supabase JWT system role `authenticated` kao SOV rolu.
- `sov_can_edit_news()` sada ispravno prihvaća `admin`, `editor`, `urednik` i role permission `can_edit_news`.
- Dodan `sov_news_auth_debug()` za provjeru role/can_edit_news u Supabaseu.
- `sync-status.html` usklađen na v5.58.11.


## v5.58.10 — Cloud User UI: oprema + predaja članka

- User dashboard vraća korisne članske module: Oprema i Napiši članak.
- Oprema vodi na postojeću člansku pretragu oružarstva / zahtjev za posudbu.
- Novi `napisi-clanak.html` sprema članski tekst kao skriveni draft za urednika.
- Dodan SQL `SUPABASE_SOV_NEWS_USER_ARTICLES_v5_58_10.sql` s RPC-em `sov_news_submit_article`.
- Maknut `sov-inbox.js` s dashboarda da nestane donji notification/inbox artefakt.
- `sync-status.html` usklađen na v5.58.10.

# v5.58.9 — News editor save RPC fix

- Urednik vijesti više ne koristi Supabase `.single()` u save flowu, što je rušilo spremanje s porukom `Cannot coerce the result to a single JSON object`.
- Dodan SQL RPC `sov_news_save(p_id, p_payload)` koji vraća jedan JSON objekt ili jasnu grešku.
- `news-editor.html` cache-bust na `assets/news-editor.js?v=5.58.9`.
- `sync-status.html` usklađen na v5.58.9.


## v5.58.8 — Dashboard user UI cleanup

- Cleaned SOV Cloud dashboard for normal User view.
- Removed visible statistics/counters/status cards from User view.
- Kept only user-relevant entry points: Karta, Predaj novu jamu, Izleti, Baza.
- Preserved Admin role preview and Admin technical card.
- Updated sync-status.html to v5.58.8.


## v5.58.1 — Arhivar submissions SQL profile fix
- Fixed SQL install error caused by hard-coded `public.sov_user_profiles`.
- Added safe role/profile helpers compatible with existing `public.profiles` role model.
- No frontend behavior change from v5.58.0.


## v5.39 — Premium UI/UX consolidation
- Rebuilt dashboard as role-aware SOV Cloud control center.
- Added premium module cards, better hierarchy, and friendlier empty/system states.
- Added read-only dashboard KPI helper for profiles/devices/audit/drawings when Supabase views are available.
- No SQL changes and no object database changes.

# v5.36 — Oružarstvo canonical role/audit integration

- Added canonical safe SQL for armory tables, RLS and audit integration.
- Oružarstvo UI now respects unified `can_manage_equipment` permission, not only raw role name.
- Admin/Oružar operational controls stay hidden for regular users.
- Keeps existing equipment data and object SQL logic intact.

# v5.34 — Unified Identity + Sync Control

- Dodan dashboard sync control card.
- Dodan `sov-sync-control.js` lightweight helper za lokalni sync badge/state.
- Dodan SQL foundation `SUPABASE_SOV_UNIFIED_IDENTITY_SYNC_v5_34.sql`.
- SQL je aditivan: dodaje sync queue i user devices, bez rušenja postojeće SQL baze objekata.


## v5.33 — Role Control Center
- Live auth by default; preview role switcher opt-in only.
- Web permission checks read Supabase `sov_current_user_permissions`.
- Dashboard modules gated by Admin / Urednik / Oružar / Arhivar / Član permissions.
- SQL shortcuts restricted to admin SQL permission.


## v5.32 — Role manager

- Added `role-manager.html` as admin UI for SOV ecosystem roles and permissions.
- Added `SUPABASE_SOV_ECOSYSTEM_ROLES_v5_32.sql` with admin RLS policies, helper functions and role manifest views.
- Dashboard now links to Role manager for Admin users.
- This build does not touch the SQL object database, Drive drawings pipeline, Trips GS, news or armory data model.
# v5.31 — Ecosystem sync dashboard

- Dodan `sync-status.html` za provjeru nacrti GS, izleti GS i Supabase cache/role statusa.
- Dodana pripremna SQL shema `SUPABASE_SOV_ECOSYSTEM_ROLES_v5_31.sql` za unified APK role/permissions model.
- Dodan dashboard link “SOV Sync”.
- SQL baza objekata nije dirana; sve provjere su read-only.

## v5.30 — Drawings fast-search sync

- Web sync nacrta usklađen s novim GS v2.0.2 fast-search endpointom.
- Nacrti se syncaju iz cached Drive indexa (`listDrawings`), bez full Drive skeniranja iz weba.
- Ograničeno na obične nacrte: PDF/JPG/PNG/WEBP/TIF/TIFF.
- SQL baza objekata nije mijenjana; upsert ide samo u `speleo_object_drawings`.

## v4.56 - Oružar Master real HTML podstranice

# v4.56 - Oružar Master real HTML podstranice

Popravak strukture: Oružar Master više nije samo set skrivenih tabova u `oruzarstvo.html`.

Dodani pravi HTML fajlovi:
- `oruzar-master.html`
- `oruzar-master-inventar.html`
- `oruzar-master-inventura.html`
- `oruzar-master-posudbe.html`
- `oruzar-master-narudzbe.html`
- `oruzar-master-nabave.html`
- `oruzar-master-postavke.html`

`oruzarstvo.html` ostaje članski katalog/zahtjevi. Klik na Oružar Master vodi u zasebni workspace.


# v4.55 — Posudbe two-card workflow

- Oružar Master > Posudbe sada ima dvije jasne kartice: Zatražene posudbe i Izdano vani.
- Zatražene posudbe prikazuju web zahtjeve i ručni unos zahtjeva za situacije izvan weba.
- Klik "Označi izdano" prebacuje zahtjev u aktivne posudbe i koristi postojeći inventory update.
- Klik "Označi vraćeno" miče stavku iz aktivnih posudbi i vraća inventory.
- Vraćene stvari se ne gomilaju u aktivnom pregledu.

# v4.6 - Brand header logo fit fix

- Zaobljen i uklopljen crni SOV wordmark u gornjem headeru.
- Dodan clipping/overflow fix da logo ne izlazi iz okvira.
- Hero logo dobio isti nenametljiv rounded treatment.
- Nema promjena funkcionalnosti.

# v4.5 — Pročelništvo restore + hero logo fix

- Vraćen jasan ulaz na Pročelništvo iz stranice O društvu.
- O društvu više nije prazna/placeholder stranica nego ima aktualno pročelništvo i jasne linkove.
- Popravljen hero SOV logo gore desno da crni wordmark ne izlazi iz okvira.
- Dodan Pročelništvo link u glavnu navigaciju/quick links.


## v4.4 — Pridruži nam se
- Dodana nova javna stranica `pridruzi-nam-se.html`.
- Dodan link/ikona “Pridruži nam se” u glavni navigacijski header i quick links.
- Sadržaj prilagođen SOV portalu iz PDS Velebit članstvo stranice.
- Uključen direktan link na radno vrijeme činovnika.


## v3.7 — DOF + Geo karta layeri
- Iz Android app WMS preset logike preneseni DGU DOF i Geo karta/HOK layeri.
- SOV Karta sada ima TK25, DOF, Geo karta/HOK i OSM fallback kao base layere.
- Izleti karta dobila iste layer gumbe i Leaflet layer control.
- TopoDroid/Nacrti karta dobila isti skup base layera za pregled nacrta uz teren.
- TK25 ostaje default.


## v3.6 — TopoDroid web bridge
- Dodana nova stranica `topodroid.html` za nacrte, TopoDroid exporte i survey fileove.
- Upload nacrta/exporta u browseru s automatskim matchanjem na objekte iz `data/sov-baza.json`.
- Dodan preview/download, link na Drive arhiv, link na full objekt u Bazi i lokalni JSON index export.
- Dodano osnovno georeferenciranje: station, azimut, procijenjena duljina i napomena; prikaz smjera/protegnutosti na karti.
- Dashboard dobio direktan ulaz `Nacrti / TopoDroid`; SOV Karta dobila link i bogatiji panel za nacrte.
- Pripremljena logika za budući backend: Supabase/Drive sync, thumbnail cache, offline izlet paket i pravi TopoDroid parser.

## v3.2 — Dokumenti + Speleo zapisnik link
- Dashboard: **Speleo zapisnik** sada vodi na službeni Google Forms link.
- Sekcija **Zapisnici** preimenovana u **Dokumenti**.
- `dokumentacija.html` podijeljena na: Zapisnici, Upute, Dopuštenja.
- `speleo-zapisnik.html` pretvoren u jednostavnu landing stranicu s gumbom za otvaranje obrasca.


## v3.1 — Kalendar arhiva + kreiranje izleta u Sheet
- Kalendar izleta sada ima eksplicitan pregled prošlih izleta / arhive.
- Gumb Kreiraj izlet vodi na postojeću stranicu `izleti.html`.
- `izleti.html` sada šalje izlet u isti Google Sheet endpoint i istom `addTripV2` logikom kao Android app.
- Dodano polje Grad / regija za prognozu (`weatherCity`) koje se sprema u Sheet i koristi za vremensku prognozu u kalendaru.
- Ako browser/CORS blokira upis, payload se sprema u `localStorage.sov_pending_web_trips` za debug.

# v2.9 — YouTube video integration

- Main page Video section now embeds the SOV YouTube uploads list.
- Added standalone `videos.html` page.
- Added direct links to `https://www.youtube.com/@speleologija/videos` and channel fallback.
- No YouTube API key required for this static build.
- Public news, dashboard and registered modules preserved.


## v2.8 — Gallery + Video on main page
- Main page now includes a cinematic Galerija section.
- Added Video section with embedded Velebitaški duh video and SOV YouTube channel link.
- Added Galerija and Video anchors to top navigation.
- Copied same update to vijesti.html so the news landing stays consistent.

# v2.7 — Dashboard extra actions

- Dashboard: dodana lijeva ikona/menu item **Speleo zapisnik**.
- Dashboard: dodana desna ikona/menu item **Napiši članak**.
- Dodane privremene static stranice `speleo-zapisnik.html` i `napisi-clanak.html` za dev preview.
- Javni news dio i baza nisu dirani.


## v2.6 — Baza alphabetic + SOV app link
- `pregled-baze.html` pretvoren u konkretan abecedni popis objekata.
- Dodan search po nazivu, lokaciji, pločici, katastru i opisu.
- Klik na objekt otvara full info panel.
- Dodan link na SOV Kartu s direktnim otvaranjem odabranog objekta.
- Dodan link na nacrte/dokumentaciju kada je nacrt evidentiran u bazi.
- Dashboard SOV app kartica sada vodi na Google Drive link za aktualni Android build.

# Changelog

## v2.2-news-first-editorial — 2026-05-24
- Landing page je prebačen na Novosti-first logiku.
- Dodano 9 modernih article stranica u `/novosti/`.
- Članci su poredani kronološki od najnovijeg prema starijima.
- Tekstovi članaka nisu mijenjani; preuzet je originalni `.entry-content` iz WordPress HTML exporta.
- Dodan `data/news.json` za budući CMS/Supabase/WordPress-style unos.
- Registered/SOV Cloud dio iz v2.1 ostaje otvoren bez login guarda za dev preview.

# Changelog

## v2.1 — Dev open registered preview
- Registered/SOV Cloud pages are temporarily accessible without login while Supabase is not connected.
- Removed client-side auth redirects from dashboard, SOV Karta, Izleti, Pregled baze, Oružarstvo, Dokumentacija and Admin preview.
- Login/register page remains available as visual flow for later Supabase wiring.
- Added visible DEV PREVIEW badge on registered pages so this is not mistaken for production auth.

## v1.9 — SOV Karta popup + Map download
- Klik na marker otvara full popup pločicu s podacima objekta.
- Klik na search rezultat centrira kartu, otvara isti popup i desni detaljni panel.
- MBTiles wording preimenovan u Map download.
- Download odabranog područja ostaje stvarni .mbtiles generiran iz WMS tileova u browseru.


## v2.3 — Dashboard redesign
- Registered dashboard redizajniran kao premium SOV Cloud cockpit.
- Dodan veliki cinematic hero, operativni panel, status kartice, glavni moduli, mini-map preview, quick actions i razvojni status.
- Javni news-first landing, novosti i registered open preview nisu dirani.


## v2.4 — dodatne vijesti
- Dodane nove vijesti: Zec il žaba, Duman, Žumberački odred za čistoću, Fugro/QGIS/Orux.
- Regeneriran kronološki news feed i /novosti article stranice.
- Poboljšan fallback za naslovne slike: og:image → prva velika slika iz članka → default SOV hero.
- Javni i registered/dashboard dijelovi nisu funkcionalno mijenjani.


## v2.4 — dodatne vijesti
- Dodane nove vijesti: Zec il žaba, Duman, Žumberački odred za čistoću, Fugro/QGIS/Orux.
- Regeneriran kronološki news feed i /novosti article stranice.
- Poboljšan fallback za naslovne slike: og:image → prva velika slika iz članka → default SOV hero.
- Javni i registered/dashboard dijelovi nisu funkcionalno mijenjani.

## v2.5 — Dashboard menu layout
- Dashboard pojednostavljen na jedan hero/cockpit ekran.
- Lijeva kartica sada je stvarni glavni izbornik: SOV Karta, Baza, Izleti, GPX/KML.
- Desna kartica sada ima: Kalendar izleta, Oružarstvo, Zapisnici, SOV app.
- Izbačeni su admin approval i nepotrebni statistički/module blokovi s dashboarda.
- Sredina je očišćena: SOV logo + kratki CTA.


## v3.0 — Kalendar izleta Android sync
- Dodan `kalendar-izleta.html` kao web verzija Android logike za zajednički raspored.
- Učitavanje izleta iz Apps Script endpointa `action=listTrips`.
- Prijava na izlet preko `signupTrip`, uključujući opciju vozača.
- Raspored automobila preko `kreirajTab` + spremanje URL-a preko `updateRasporedUrl`.
- Weather integracija preko Open‑Meteo geocoding/forecast logike.
- Dashboard link `Kalendar izleta` sada vodi na novu dedicated stranicu.

## v3.4 — Zapisnici editor + DOCX workflow
- Dokumenti: Zapisnici sada imaju samo Novi zapisnik, Pregled zapisnika i Zapisnici skupštine.
- Dodan `novi-zapisnik.html`: datum se automatski popunjava, traži se tko vodi sastanak i tko je zapisničar.
- Editor koristi stalni format prema primjeru: SPELEO IZLETI, INI IZLETI, NAJAVE, RAZNO, Sastanak vodio, Zapisnik vodila.
- Dodan live preview u stilu zapisnika.
- “Spremi i pošalji” sprema zapisnik za urednika i generira DOCX download.
- Dodan `pregled-zapisnika.html`: lista pending zapisnika, preview, approve, edit, delete i DOCX export.
- Dodan `zapisnici-skupstine.html` kao posebna arhiva skupštinskih zapisnika.


## v3.5 — Nacrti / TopoDroid Drive match
- U Baza dodan panel za Nacrti / TopoDroid.
- Dodan link na glavni Google Drive folder nacrta.
- Dodan lokalni upload/index nacrta i TopoDroid exporta.
- Web automatski matcha fileove s objektima po nazivu jame/spilje.
- Full kartica objekta prikazuje pronađene nacrte s Prikaži / Skini akcijama.
- Dodan export lokalnog indexa nacrta kao JSON za kasnije spajanje na backend/Drive API.

## v3.8 — Supabase login + role permissions

- Replaced localStorage/dev-preview auth with Supabase Auth flow.
- Added `assets/supabase-config.js` for Supabase URL/anon key.
- Added `profiles` role/status model: `admin`, `user`, `editor`, `oruzar`.
- Registration now creates pending user profile.
- Login blocks users until admin approval.
- Admin approval page now loads users from Supabase and supports approve/reject/role changes.
- Registered pages are protected again.
- Oružarstvo is limited to `admin` and `oruzar`.
- Admin-only user approval is hidden from non-admin users.
- Added `SUPABASE_SETUP.md` with SQL and setup instructions.


## v3.9 — Mobile-first UX polish
- Dodan globalni `assets/mobile.css` koji se učitava na svim HTML stranicama.
- Sređena mobilna navigacija: sticky header, horizontalni scroll meniji, veći touch targeti.
- Dashboard preuređen za mobitel: logo kompaktniji, kartice čitljivije, izbornici u jednoj koloni.
- SOV Karta/Baza prilagođena mobitelu: karta gore, search i detalji ispod, toolbar i filteri skrolabilni.
- Kalendar izleta, dokumenti, zapisnici, editor članaka i admin panel dobili responsive kartice i forme.
- Poboljšani inputi, gumbi, dialogi, tablice, article stranice i news kartice na malim ekranima.
- Desktop layout ostaje netaknut; izmjene su aktivne samo na tablet/mobile breakpointovima.


## v4.0 Links main home
- Dodana sekcija Linkovi na glavnu stranicu.
- Dodani PDS Velebit, Komisija za speleologiju HPS, HPS, HGSS, Katastar speleoloških objekata RH i UIS.
- Ubačeni priloženi logotipi u assets/links.
- Dodan nav link na #linkovi i responsive/mobile layout.

## v4.7 — Mobile nav fit
- Dodatno sređen mobile header.
- Navigacija je horizontalni scroll bez lomljenja riječi.
- Kratke stavke poput “Video” ostaju u jednom redu.
- Brand/logo u headeru se skaluje unutar širine ekrana.

## v4.10 Modern polish + O nama logic
- Header logo refined into stable pill layout.
- Main nav simplified: O nama contains Pregled, Pročelništvo, Povijest, Velebitaški duh.
- O nama rebuilt as a clean, modern sectioned page.


## v4.11 Modern O nama polish
- Uklonjen osjećaj dva menija u O nama: interna navigacija više nije sticky/floating i ne prelazi preko sadržaja.
- O nama preuređen u jasan flow: hero, pregled, pročelništvo, povijest, Velebitaški duh.
- Pročelništvo i Povijest ostaju integrirani kao subsekcije O nama.
- Dodatno ispoliran gornji lijevi SOV logo i responsive ponašanje navigacije.


## v4.22 — Oružarstvo role + inventure
- Oružarstvo je dostupno svim odobrenim članovima kao katalog i zahtjev za opremu.
- Oružar/admin vide interne module: zahtjevi, užad, inventure, nabava, rashod, izgubljeno i oprema na terenu.
- Dodan `data/oruzarstvo-data.json` generiran iz XLS inventure.
- Dashboard link za Oružarstvo više nije sakriven običnim korisnicima.


## v4.23 — Oružarstvo XLS v1 model import
- Zamijenjen oružarski data model novim `SOV_Oruzarstvo_v1.xlsx` importom.
- Dodani katalog, komadi, užad, posudbe, inventure, servis, nabava, rashod, izgubljeno i oprema na terenu kao čiste JSON cjeline.
- Oružarstvo UI prilagođen novom modelu: član vidi katalog/zahtjeve, oružar/admin vidi interne panele.

## v4.24 — Oružarstvo Supabase SQL backend
- Dodan kompletan SQL schema/RLS za Oružarstvo.
- Dodan Supabase bridge za zahtjeve opreme.
- Dodana import stranica za početni katalog iz Claude XLS JSON modela.
- Obični članovi mogu slati zahtjeve; oružar/admin vide i obrađuju sve zahtjeve.
- Local storage ostaje samo dev fallback ako Supabase nije konfiguriran.


## v4.28 Armory quick packages
- Dodani brzi paketi za zahtjeve opreme: osobna oprema, postavljanje i crtanje.
- Količine u zahtjevu mogu se doraditi plus/minus kontrolama prije slanja.


## v4.29
- Oružarstvo import: popravljena normalizacija datuma (`09/2022` → `2022-09-01`) za Supabase.


## v4.42 — Oružar drill-down + aktivne posudbe
- Oružar master sada prvo traži kategoriju, zatim podkategoriju, pa tek onda module.
- Inventar, posudbe, narudžbe, inventura i nabave se filtriraju po odabiru da ekran nije overwhelming.
- Posudbe prikazuju samo aktivno vani; vraćeno nestaje iz pregleda posudbi i ostaje kao povijest/status.
- Gumb Označi vraćeno ažurira status i vraća količine u inventar.

## v4.46 - User category catalog
- User katalog prebačen na category-first workflow.
- Član prvo bira kategoriju, zatim podkategoriju, tek onda artikle.
- Descenderi su vidljivi pod Osobna oprema.
- Bušilice, baterije, punjači i svrdla su grupirani u Bušilice i baterije.
- Oružar master ostaje odvojen workspace.


## v5.35 — Audit Log + User Devices
- Admin audit/device panel.
- Safe SQL for `sov_audit_log`, device telemetry columns and recent views.
- Partner build for APK 1.3.1.

## v5.37 — Arhiva/Nacrti canonical role/audit
- Added canonical drawings/archive SQL with role-gated RLS.
- Added audit trigger for drawing metadata changes.
- Upgraded TopoDroid/Nacrti page to read from `sov_drawings_public`.
- Admin/Arhivar can save drawing metadata from web UI.
- Normal users remain read-only for public drawings.

## v5.38 — TopoDroid Import Pipeline
- Dodan batch import/review pipeline za TopoDroid i nacrte.
- Dodane safe SQL tablice za import queue.
- Dodan RPC za publish import itema u canonical arhivu nacrta.
- Admin/Arhivar write, User read-only.


## v5.58.2 — Arhivar archive export iz uređivanja arhive
- U `arhivar.html` dodan export postojeće arhive direktno iz ekrana Uređivanje arhive.
- Export scope: trenutni filter, sve, sve gdje nešto fali, pojedinačne falinke (`pločica`, `koordinate`, `nacrt`, `zapisnik`, `fotka`, `ponoviti nacrt`) i odabrani objekt.
- Formati: CSV, Excel-compatible XLS i PDF/print izvještaj.
- Pojedinačni PDF uključuje i puni opisni detail kad je dostupan kroz `sov_arhivar_get_object_detail`.
- Nema novog SQL-a: koristi postojeći `sov_arhivar_worklist` i postojeći detail RPC.


## v5.58.15 — karta.html clean UI + real DB sync
- `baza.html` je zamijenjen s `karta.html`; stari `baza.html` ostaje samo redirect radi starih linkova.
- Nova Karta ima čišći desktop i mobile browser UX bez SQL/dev gumba, statističkih kartica i inbox artefakata.
- Karta primarno čita `sov_map_objects()` / `speleo_objects_staging`, odnosno aktualnu Supabase bazu objekata koju koristi Arhivar.
- Dashboard, navigacija i sync-status prebačeni su na `karta.html`.
- Dodan SQL `SUPABASE_SOV_MAP_v5_58_15_KARTA_REAL_DB.sql`.
