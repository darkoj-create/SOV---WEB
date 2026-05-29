# SOV Web v5.46 — Oružar Master cleanup

- Web Oružar Master koristi isti statusni model kao APK: `requested/pending -> issued -> returned | partial_return (+ cancelled)`.
- Maknut je web-side regex kao glavni izvor kategorizacije u Master JS-u; SQL canonical view ostaje mozak.
- Posudbe su očišćene na dvije glavne kolone: Za izdati i Izdano vani, plus arhiva zadnjih povrata/zatvorenih zahtjeva.
- Povrat po artiklu ostaje centralni flow za web oružara.
- Nema novog SQL-a za ovaj build; koristi prethodni v5.45/v5.44 canonical SQL sloj.
