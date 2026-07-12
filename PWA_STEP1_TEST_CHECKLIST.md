# PWA Step 1 test checklist

## Chrome / Edge desktop

1. Otvori production/preview deploy.
2. DevTools → Application → Manifest:
   - nema crvenih grešaka
   - name/short_name = SOV
   - display = standalone
   - ikone 192/512 i maskable postoje
3. DevTools → Application → Service Workers:
   - `sw.js` registriran pod scope `/`
   - nema stalnih errora u konzoli
4. Lighthouse → PWA:
   - manifest prolazi
   - service worker postoji
5. DevTools → Network → Offline:
   - `index.html` se otvara iz cachea
   - nepoznata/nespremljena stranica vraća `offline.html`
6. Login i Supabase pozivi:
   - normalno rade online
   - u Network tabu Supabase pozivi ne dolaze iz ServiceWorker cachea
   - POST/PUT/PATCH/DELETE se ne presreću

## Kill-switch

1. Uploadaj prazni `sw-disable.flag` u root deploya.
2. Otvori site.
3. SW se mora unregisterati i očistiti SOV cacheve.
4. Ukloni `sw-disable.flag` prije sljedećeg deploya.

## iPhone Safari

1. Otvori site u Safari browseru.
2. Share → Add to Home Screen.
3. Pokreni SOV ikonu s home screena.
4. App se otvara fullscreen/standalone bez Safari trake.
5. Offline nakon prvog učitavanja otvara spremljeni shell.

## Normalna iOS ograničenja

- Nema background GPS trackinga.
- Nema background synca.
- Safari može izbrisati cache ako se app dugo ne koristi.
