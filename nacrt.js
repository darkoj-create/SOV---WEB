/**
 * SOV Nacrt loader — corpus-informed semantic renderer v6 + layered editor.
 * Active stack: core + TopoDroid parser fix + renderer v2 + official branding + semantic SVG modules + corpus style v6 + corrected PDS logo + non-destructive editor + cloud persistence bridge.
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
  '<link rel="stylesheet" href="assets/sov-nacrt-editor-v6157.css?v=6157">' +
  '<script src="nacrt-core.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-tdr-fix.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-v2.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-branding.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-semantic-core.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-semantic-styles.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-semantic-layout.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-corpus-style-v6.js?v=6145at"></' + 'script>' +
  '<script src="nacrt-pds-logo-fix.js?v=6145at"></' + 'script>' +
  '<script src="assets/sov-nacrt-editor-v6157.js?v=6157"></' + 'script>' +
  '<script src="assets/sov-nacrt-editor-cloud-v6158.js?v=6158"></' + 'script>'
);
