# SOV Nacrt — vizualni registar v6

Referentni korpus: `SOV-svi-nacrti-minimalni-webp(1).zip`.

## Korpus

- 1.109 preglednih stranica iz 1.096 izvornih datoteka
- 771 portretna, 299 pejzažnih i 39 približno kvadratnih stranica
- uzorak od 300 neskeniranih stranica: 125 s dominantnim gornjim blokom, 45 donjim, 39 lijevim, 26 desnim, 65 bez jasnog rubnog bloka
- WebP korpus je namjerno grayscale; iz njega se uče oblici, gustoća, raspored i hijerarhija
- značenje boja dolazi iz izvornih PDF/TopoDroid parova i SOV pravila: plavo voda/led, zeleno vegetacija, smeđe drvo, sivo kamenje

## Pravilo boje

Boja se koristi samo kada nosi kartografsko značenje. Šupljina jame i generička stijena ostaju bijele/crne.

| Značenje | Prikaz |
|---|---|
| voda | svijetloplava ispuna i plave valovite linije |
| led | vrlo svijetla cijan ispuna i ledene pukotine |
| snijeg | svijetla cijan ispuna i plave zvjezdice |
| vegetacija | prigušena zelena |
| drvo, korijen, trupac | smeđa |
| blokovi i sipar | neutralne sive plohe |
| pijesak | svijetla bež točkasta ispuna |
| glina/blato | topla bež valovita ispuna |
| tlo/humus | smeđe-bež sitna granulacija |
| siga/kalcit | svijetla oker valovita ispuna |
| mjerne stanice/poligonala | crveno |

## Stabilnost

- isključivo SVG
- nema Canvas obrade ni skeniranja piksela
- nepoznati TopoDroid tipovi ostaju u sigurnom neutralnom fallbacku
- parser, mjerna geometrija i postojeći scrapovi ne mijenjaju se ovim slojem
