# v5.57.7 Arhivar human detail panel

- Frontend-only fix: detail panel no longer shows raw speleo_objects_staging / SQL row dump.
- Uses the existing detail RPC output, but renders it as normal archive sections: osnovni podaci, opis/pristup/istraživanje, katastar/status, zapisnici and nacrti.
- Keeps full object loading behavior from v5.57.6.
