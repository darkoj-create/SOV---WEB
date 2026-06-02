/* SOV Oružarstvo taxonomy v6.1.5 — single canonical category source */
(function(){
  const VERSION='6.1.5';
  const ORDER=['Osobna oprema','Užad i užetna oprema','Oprema za postavljanje','Oprema za crtanje','Bušilice i svrdla','Elektro, rasvjeta i foto','Dronovi','Oprema za logor','Oprema za proširivanje','Medicinska oprema','Alat i radionica','Alpinistička oprema','Ronilačka oprema','Čisto podzemlje','Ostalo / provjeriti'];
  const META={
    'Osobna oprema':['🧑‍🚒','Pojasevi, crollovi, descenderi, blokeri, kacige i osobna speleo oprema.'],
    'Užad i užetna oprema':['🪢','Užad, prusici, gurtne, transportne vreće i užetni pribor.'],
    'Oprema za postavljanje':['⚓','Spitovi, pločice, ringovi, sidrišta, karabineri i rigging pribor.'],
    'Oprema za crtanje':['📐','Busole/kompasi, Suunto, DistoX, TopoDroid i crtaći pribor.'],
    'Bušilice i svrdla':['🔩','Bušilice, svrdla, SDS pribor, Bosch/Hilti/Makita baterije i punjači.'],
    'Elektro, rasvjeta i foto':['🔦','Lampe, obične baterije AA/AAA/9V, USB, punjači, komunikacija, foto i video.'],
    'Dronovi':['🚁','Dronovi, dron baterije, punjači, elise, kontroleri i transportni koferi.'],
    'Oprema za logor':['⛺','Logor, kuhinja, voda, spavanje, terenski boravak i higijena.'],
    'Oprema za proširivanje':['🔨','Čekići, macole, dlijeta i oprema za proširivanje.'],
    'Medicinska oprema':['🧰','Prva pomoć, sanitet i medicinski kompleti.'],
    'Alat i radionica':['🧰','Opći alat, radionica, servis i popravci.'],
    'Alpinistička oprema':['⛰️','Alpinistička i penjačka oprema koja nije standardna speleo osobna oprema.'],
    'Ronilačka oprema':['🤿','Ronilačka oprema, neopreni, maske, peraje i boce.'],
    'Čisto podzemlje':['🧹','Vreće, rukavice i oprema za akcije čišćenja.'],
    'Ostalo / provjeriti':['📦','Stavke koje još treba ručno provjeriti.'],
    'Ostalo':['📦','Stavke koje još treba ručno provjeriti.']
  };
  function plain(s){return String(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();}
  function text(row){return plain([row&&row.name,row&&row.model,row&&row.item_name,row&&row.subcategory,row&&row.raw_subcategory,row&&row.category,row&&row.category_name,row&&row.main_category,row&&row.internal_note,row&&row.note,row&&row.manufacturer,row&&row.sku].filter(Boolean).join(' '));}
  function category(row){
    const raw=String((row&&(row.main_category||row.category_name||row.category))||row||'').trim();
    if(ORDER.includes(raw))return raw;
    const x=plain(raw), t=text(row)||x;
    const ordinary=/(^| )(aa|aaa|9v|18650|cr123|cr2032)( |$)|baterije?\s+(aa|aaa|9v)|punjac.*(aa|aaa)|mini usb|usb kabel|solarni punjac|powerbank/.test(t);
    const drill=/(bosch|hilti|makita|gbh|sds|busil|busilic|buzil|svrd|borer|akumulatorsk)/.test(t);
    const drone=/(dron|dji|phantom|mavic|spark|gl300|elisa|propeler|ph4c|ade019|4480mah|5350mah)/.test(t);
    if(raw==='Užad')return 'Užad i užetna oprema';
    if(x.includes('busilice i baterije')){if(drone)return 'Dronovi'; if(ordinary&&!drill)return 'Elektro, rasvjeta i foto'; return 'Bušilice i svrdla';}
    if(x.includes('elektro')||x.includes('foto')||x==='rasvjeta')return 'Elektro, rasvjeta i foto';
    if(['ostali alat','alat','ostalo','razno'].includes(x))return 'Alat i radionica';
    if(x.includes('medicin'))return 'Medicinska oprema'; if(x.includes('logor')||x.includes('kamp'))return 'Oprema za logor'; if(x.includes('prosir'))return 'Oprema za proširivanje'; if(x.includes('alpin'))return 'Alpinistička oprema'; if(x.includes('ronil'))return 'Ronilačka oprema'; if(x.includes('cisto podzemlje'))return 'Čisto podzemlje';
    if(drone)return 'Dronovi';
    if(/kompas|busol|suunto|disto|distox|topodroid|klinomet|laser|nacrt|skic|olov/.test(t))return 'Oprema za crtanje';
    if(ordinary&&!drill)return 'Elektro, rasvjeta i foto';
    if(/lampa|rasvjet|svjetl|ceona|kamera|foto|video|walkie|radio stanica|punjenje walkie/.test(t))return 'Elektro, rasvjeta i foto';
    if(drill && /bater|aku|punjac|busil|svrd|borer|sds/.test(t))return 'Bušilice i svrdla';
    const personal=/(croll|krol|crol|prsni|descender|spustal|(^| )stop($| )|rig|maestro|\bid\b|zumar|jumar|ascender|rucni bloker|(^| )bloker|pedal|stremen|pupak|pupcan|pojas|sjedal|kacig|kombinezon|odijel|rukavic|cizm|obuc)/.test(t)&&!/(penjac|penjack|alpin)/.test(t);
    if(/spit|sidr|sidrist|anker|bolt|ploc|ring|fikser|karab|hms|matica|maillon|omni|triact|trilock/.test(t)&&!personal)return 'Oprema za postavljanje';
    if(personal)return 'Osobna oprema';
    if(/uzad|(^| )uze($| )|rope|strik|statick|staticno|dinamick|transportna vreca|transportne vrece|prusik|gurt|traka/.test(t))return 'Užad i užetna oprema';
    if(/sator|podloga|vreca za spavanje|kuhal|plin|kanister|posud|tanjur|lonac|cerada|agregat|stol|stolica|logor|kamp/.test(t))return 'Oprema za logor';
    if(/prosir|klin|cekic|macol|dlijet|stem/.test(t))return 'Oprema za proširivanje';
    if(/ronil|ronjenje|neopren|maska|peraj|boca/.test(t))return 'Ronilačka oprema';
    if(/alpinist|alpin|penjack|penjac/.test(t))return 'Alpinistička oprema';
    if(/cisto podzemlje|ciscenje|otpad/.test(t))return 'Čisto podzemlje';
    if(/alat|kljuc|odvijac|klijest|lopat|skare|pila|metar/.test(t))return 'Alat i radionica';
    return raw||'Ostalo / provjeriti';
  }
  function subcategory(row){
    const raw=String((row&&(row.subcategory_name||row.subcategory||row.raw_subcategory))||'').trim();
    const t=text(row); const c=category(row);
    const map={'Karabineri i spojnice':'Karabineri','Centralni karabineri / spojnice':'Karabineri','Sidrišta i fiksevi':'Spitovi i sidrišta','Pločice i ringovi':'Pločice / ringovi'};
    if(map[raw])return map[raw];
    if(/kompas|busol|suunto/.test(t))return 'Busole / kompasi / Suunto';
    if(/disto|distox|topodroid|laser|klinomet|mjeren/.test(t))return 'Mjerenje / Disto / TopoDroid';
    if(/crtan|nacrt|skic|olov|papir/.test(t))return 'Crtaći pribor';
    if(c==='Dronovi'){if(/bater|aku|4480mah|5350mah/.test(t))return 'Dron baterije'; if(/punjac|charging|ph4c|ade019/.test(t))return 'Dron punjači'; if(/elisa|propeler/.test(t))return 'Elise / propeleri'; if(/kontrol|gl300|remote/.test(t))return 'Kontroleri'; if(/kofer|torba|drzac|transport/.test(t))return 'Transport i zaštita drona'; return 'Dronovi i pribor';}
    if(/(^| )(busilica|busilice|busilic[ae]|boschhammer)( |$)/.test(t))return 'Bušilice';
    if(/bosch|hilti|makita|gbh|sds|busil|busilic|buzil/.test(t)){if(/punjac/.test(t))return 'Punjači za bušilice'; if(/svrd|borer|sds|spica/.test(t))return 'Svrdla i špicevi'; if(/bater|aku/.test(t))return 'Baterije za bušilice'; return 'Bušilice';}
    if(/(^| )(aa|aaa|9v|18650|cr123|cr2032)( |$)|baterije?\s+(aa|aaa|9v)|mini usb|usb kabel|solarni punjac|powerbank|punjac.*(aa|aaa)/.test(t))return 'Baterije, punjači i powerbankovi';
    if(/walkie|radio stanica|stanica za punjenje/.test(t))return 'Komunikacija'; if(/lampa|rasvjet|svjetl|ceona/.test(t))return 'Lampe i rasvjeta'; if(/kamera|foto|video|gopro/.test(t))return 'Foto / video';
    if(/descender|spustal|(^| )stop($| )|rig|maestro|\bid\b/.test(t))return 'Descenderi'; if(/croll|krol|crol|prsni/.test(t))return 'Croll / prsni blokeri'; if(/zumar|jumar|ascender|rucni|bloker/.test(t))return 'Ručni blokeri'; if(/pupak|pupcan/.test(t))return 'Pupčana užad'; if(/pedal|stremen|pantin/.test(t))return 'Pedale / stremeni'; if(/pojas|sjedal/.test(t)&&!/(penjac|alpin)/.test(t))return 'Pojasevi i sjedalice'; if(/kacig|helmet/.test(t))return 'Kacige'; if(/kombinezon|odijel|rukavic|cizm|obuc/.test(t))return 'Odjeća i obuća';
    if(/karab|hms|matica|maillon|omni|triact|trilock|screw|twist|oval/.test(t))return 'Karabineri'; if(/spit|sidr|sidrist|anker|bolt|fikser|spiter/.test(t))return 'Spitovi i sidrišta'; if(/ploc|ring/.test(t))return 'Pločice / ringovi'; if(/transportna vreca|transportne vrece/.test(t))return 'Transportne vreće'; if(/prusik/.test(t))return 'Prusici'; if(/gurt|traka|sling/.test(t))return 'Gurtne i trake'; if(/uzad|(^| )uze($| )|rope|strik/.test(t))return 'Užad';
    if(/sator|podloga|vreca za spavanje/.test(t))return 'Spavanje i šatori'; if(/kuhal|plin|posud|tanjur|lonac|kuhin/.test(t))return 'Logorska kuhinja'; if(/kanister|voda|bidon/.test(t))return 'Voda i kanisteri'; if(/cekic|macol|dlijet|stem|prosir/.test(t))return 'Alat za proširivanje'; if(/prva pomoc|sanitet|medic/.test(t))return 'Prva pomoć';
    return raw||'Ostalo';
  }
  function normalizeRow(row){const c=category(row), s=subcategory(row); return Object.assign(row||{}, {main_category:c, category:c, category_name:c, subcategory:s, subcategory_name:s, taxonomy_version:VERSION});}
  function sortCategories(arr){return [...arr].sort((a,b)=>(ORDER.indexOf(a)<0?999:ORDER.indexOf(a))-(ORDER.indexOf(b)<0?999:ORDER.indexOf(b))||String(a).localeCompare(String(b),'hr'));}
  window.SOVArmoryTaxonomy={version:VERSION, order:ORDER, meta:META, plain, text, category, subcategory, normalizeRow, sortCategories};
})();
