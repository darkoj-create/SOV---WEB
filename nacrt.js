/**
 * SOV Nacrt loader — stable clean symbols build.
 * Active stack: core + TopoDroid parser fix + renderer v2 + clean branding + symbols polish.
 * Heavy v3/v4 cave colouring layers remain disabled.
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
  '<script src="nacrt-core.js?v=6145aq"></' + 'script>' +
  '<script src="nacrt-tdr-fix.js?v=6145aq"></' + 'script>' +
  '<script src="nacrt-v2.js?v=6145aq"></' + 'script>' +
  '<script src="nacrt-branding.js?v=6145aq"></' + 'script>' +
  '<script src="nacrt-symbols.js?v=6145aq"></' + 'script>'
);
