# SOV Web v6.1.45an — Nacrt Material V4

Promjena sloja nacrta prema referentnom PDF-u “Gorski prištić”.

## Što je popravljeno
- jama/šupljina više se ne puni smeđom bojom
- v3 interior fill raster je uklonjen
- uvodi se vanjski bež/sivi halo terena oko zidova
- kaverna ostaje bijela
- footer/debug tekstovi su uklonjeni iz završnog outputa
- dubina se zaokružuje normalno umjesto agresivnog `ceil`

## Što ostaje isto
- parser TDR i ZIP logika
- geometrija stanica i poligonale
- raspored v2/v3 (profil/tlocrt/presjek)
- postojeći kameni i materijalni simboli iz v3

## Napomena
Ovo je korekcijski sloj iznad v3. Najvažniji cilj je da semantika nacrta bude ispravna: bijela jama, obojeni teren izvana.
