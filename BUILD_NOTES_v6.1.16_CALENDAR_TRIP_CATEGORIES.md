# SOV Web v6.1.16 — calendar range + trip categories

- Calendar now treats multi-day trips/events as spanning every covered day.
- Trip dashboard list is month-scoped with previous/next month controls.
- Trip dashboard has category filter: Sve, Izlet, Seminar, Skup, Ekspedicija, Inventura, Skupština, Predavanje.
- New trip form includes category selector.
- Categories render with distinct colors in list/calendar.
- Trip dates display as dd/mm/yyyy.
- Requires SQL patch `SUPABASE_SOV_TRIP_CATEGORIES_CALENDAR_v6_1_16.sql` for durable `trip_category` storage and mobile feed support.
