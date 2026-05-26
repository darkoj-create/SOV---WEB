# v5.04 — Speleo SQL import duplicate ID hardfix

Fix za grešku:
`ON CONFLICT DO UPDATE command cannot affect row a second time`

Uzrok: source JSON ima ponovljene numeričke `id` vrijednosti za različite objekte.

Rješenje:
- import prije upserta deduplicira batch po finalnom SQL ID-u
- prva pojava source ID-a ostaje na originalnom ID-u
- sljedeće pojave istog source ID-a dobiju stabilan interni bigint ID
- import i dalje ide u chunkovima i prikazuje progress
- nema nove SQL migracije
