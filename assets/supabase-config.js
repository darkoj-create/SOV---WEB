// SOV Cloud Supabase config
// 1) U Supabase Project settings > API kopiraj Project URL i anon public key.
// 2) Upisi ih ovdje prije deploya na Vercel.
window.SOV_SUPABASE_URL = 'https://ncomefzkuixyfixisrhi.supabase.co';
window.SOV_SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5jb21lZnprdWl4eWZpeGlzcmhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1ODQwOTYsImV4cCI6MjA5NTE2MDA5Nn0.WFSiENYXv48Npaz7vFcY-ksYvg_Ja40iNGsEqb1nUDk';

// v5.30 Nacrti sync endpoint — SOV Drawings Index WebApp v2.0.2 FAST SEARCH.
// Web sync sada cita cached index preko ?action=listDrawings&limit=2000, ne skenira Drive iz browsera.
window.SOV_DRAWINGS_SYNC_ENDPOINT = window.SOV_DRAWINGS_SYNC_ENDPOINT || 'https://script.google.com/macros/s/AKfycbx1Hg_s6mAdWgB7p559USC8dAMIhteJQ3RFhFgp8rkqzYEVqMfwZm-lrl2v7UmW8gvSyg/exec';
// Legacy fallback ostaje prazan namjerno; primarni izvor je Apps Script fast-search endpoint.
window.SOV_GOOGLE_DRIVE_API_KEY = window.SOV_GOOGLE_DRIVE_API_KEY || '';

// v5.24 hard-restored trips calendar Apps Script endpoint
window.SOV_TRIPS_WEBAPP_URL = window.SOV_TRIPS_WEBAPP_URL || 'https://script.google.com/macros/s/AKfycbybGi7p6_ImXAXEErJ6P9K0GYHy8lHW850K9cQe2py8yUV2oJO6UW1DJi00quorVTHOGQ/exec';
