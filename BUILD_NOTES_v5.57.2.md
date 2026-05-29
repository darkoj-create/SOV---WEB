# SOV Web v5.57.2 — Arhivar full object details

- Arhivar detail panel now shows full text/context for selected object.
- Adds sections: base speleo data, reports/research/members, drawings/attachments and raw source record.
- SQL view `sov_arhivar_worklist` now exposes `base_details_text`, `report_details_text`, `drawing_details_text`, `full_details_text`, `raw_text`.
- Status logic remains based on speleo base/field tasks, not on uploaded archive attachments.
