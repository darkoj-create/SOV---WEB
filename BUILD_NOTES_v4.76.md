# SOV web v4.76 — article dedupe model fix

- samo užad ostaje individualno po kodu/SKU
- količinski artikli se spajaju po kategoriji + podkategoriji + normaliziranom nazivu
- krol/crol/croll se tretiraju kao Croll, osim jasnih podtipova Croll S/L
- stremen/pedala se tretira kao Stremen
- frontend više ne prikazuje duple količinske artikle
- import sada upserta spojene količinske artikle pod stabilnim ART-* legacy_id ključem
- SQL migracija čisti postojeće duplikate i dodaje unique index za budućnost
