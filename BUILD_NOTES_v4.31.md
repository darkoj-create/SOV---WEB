# v4.31 Oružarstvo rope SKU duplicate import fix

- Popravljen import užadi kad dva užeta imaju isti generirani SKU.
- Duplikati sada dobivaju stabilan suffix `--2`, `--3` itd.
- Fix je primijenjen u `oruzarstvo-data.json`, `oruzarstvo-data-v1-model.json` i runtime import logici.
- SQL ne treba ponovno pokretati.
