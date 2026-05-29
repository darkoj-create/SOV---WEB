# SOV Web 5.45 — Armory Core Cleanup

- SQL view ostaje jedini canonical layer za kategorije/podkategorije/grouping/search.
- Web fallback kategorizacija je svedena na minimalni fallback, bez novog client-side regex mozga.
- Statusni model posudbi usklađen s APK-om: pending/requested → issued → returned / partial_return (+ cancelled).
- Uklonjen UI korak odobravanja/pripreme iz starijih oružarskih rendera.
- Dodan SQL `SUPABASE_ORUZARSTVO_CORE_CLEANUP_v5_45.sql`.
