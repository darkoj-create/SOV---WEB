/**
 * SOV Nacrt loader.
 * Emergency rollback: v4 material halo disabled because its per-pixel
 * distance scan can freeze/crash the browser on larger drawings.
 * Stable active stack: core + TDR fix + renderer v2 + visual v3.
 */
document.write(
  '<script src="nacrt-core.js?v=6145ao"></' + 'script>' +
  '<script src="nacrt-tdr-fix.js?v=6145ao"></' + 'script>' +
  '<script src="nacrt-v2.js?v=6145ao"></' + 'script>' +
  '<script src="nacrt-v3.js?v=6145ao"></' + 'script>'
);
