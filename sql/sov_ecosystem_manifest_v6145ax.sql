-- SOV v6.1.45ax ecosystem manifest

update public.sov_ecosystem_manifest
set backend_contract='2026.07.20-system-status-v3',
    web_version='6.1.45ax-gmail-trips-status-visual',
    apk_target_version='2.0.1-context-buildfix',
    notes=jsonb_build_array(
      'Status sustava v3 odvaja aktualne incidente od povijesnih, obrađenih i lokalnih testnih događaja.',
      'Android pad je potvrđen samo kada aplikacija pošalje severity=fatal i handled=false.',
      'Izostanak fatalnog zapisa ne dokazuje da se pad nije dogodio; znači da ga klijent nije prijavio.',
      'Android cleanup RPC više ne briše storage.objects iz SQL-a i zato ne vraća očekivani 403.',
      'Gmail zapisnici provjeravaju se satno, a izleti se osvježavaju automatski.'
    ),
    updated_at=now()
where id='current';
