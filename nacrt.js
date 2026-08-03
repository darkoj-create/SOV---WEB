/**
 * SOV Nacrt loader — corpus-informed semantic renderer v7.
 * Active stack: core + TopoDroid parser fix + renderer v2 + official branding + semantic SVG modules + corpus style v6 + corpus style v7 + corrected PDS logo.
 * v7 je kalibriran na dovršene nacrte ekspedicije Sjeverni Velebit 2025/2026
 * (sjena stijenki, sitne točke poligona, graduirano mjerilo s omjerom, kompasna ruža,
 * gusti ježevi na skoku, kosina, blokovi kao poligoni, kota dubine, oznaka ULAZ).
 * Generic v3/v4 colouring and the older heuristic symbols layer remain disabled.
 */
try { document.documentElement.style.visibility = 'hidden'; } catch (e) {}
document.addEventListener('DOMContentLoaded', async () => {
  try {
    if (!window.SOVAuth || !(await SOVAuth.requireApproved())) return;
    document.documentElement.style.visibility = '';
  } catch (err) {
    location.href = 'login.html?next=nacrt.html';
  }
});

document.write(
  '<script src="nacrt-core.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-tdr-fix.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-v2.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-branding.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-semantic-core.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-semantic-styles.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-semantic-layout.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-corpus-style-v6.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-corpus-style-v7.js?v=6151pdf"></' + 'script>' +
  '<script src="nacrt-pds-logo-fix.js?v=6150v7"></' + 'script>' +
  '<script src="nacrt-pdf-export.js?v=6151pdf"></' + 'script>'
);
