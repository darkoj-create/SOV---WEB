/**
 * SOV Nacrt loader — semantic renderer v5 + corrected PDS logo.
 * Active stack: core + TopoDroid parser fix + renderer v2 + official branding + semantic SVG modules.
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
  '<script src="nacrt-core.js?v=6145as"></' + 'script>' +
  '<script src="nacrt-tdr-fix.js?v=6145as"></' + 'script>' +
  '<script src="nacrt-v2.js?v=6145as"></' + 'script>' +
  '<script src="nacrt-branding.js?v=6145as"></' + 'script>' +
  '<script src="nacrt-semantic-core.js?v=6145as"></' + 'script>' +
  '<script src="nacrt-semantic-styles.js?v=6145as"></' + 'script>' +
  '<script src="nacrt-semantic-layout.js?v=6145as"></' + 'script>' +
  '<script src="nacrt-pds-logo-fix.js?v=6145as"></' + 'script>'
);
