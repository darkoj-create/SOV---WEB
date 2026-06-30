# SOV Admin APK 1.4.2 — Armory Core Cleanup

- Oružarstvo katalog više ne pokušava kanonizirati kategorije u Kotlinu; SQL view je izvor istine.
- Uklonjen je mrtvi statusni korak "Odobreno" iz APK workflowa.
- Statusni model: requested/pending → issued → returned / partial_return (+ cancelled).
- Katalog sada dodaje artikle u košaricu i šalje jedan multi-artikl zahtjev.
- Forma zahtjeva ima stepper po artiklu i brze datumske shortcutove.
- Oružarski red ide direktno iz "Za izdati" u "Izdano".
- Inventura ostaje raw/offline-first i ne koristi grouped katalog.

Gradle build nije potvrđen u sandboxu jer wrapper pokušava skinuti Gradle 9 distribuciju bez interneta.
