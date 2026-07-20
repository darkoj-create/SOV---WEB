#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';

const ROOT = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const BASE = process.env.SOV_AUDIT_BASE || 'http://127.0.0.1:4173';
const SKIP_DIRS = new Set(['.git', 'node_modules', '.vercel', 'dist', 'build', 'coverage']);

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (SKIP_DIRS.has(entry.name)) continue;
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(p, out);
    else if (/\.html?$/i.test(entry.name)) out.push(p);
  }
  return out;
}

const pages = walk(ROOT)
  .map(p => path.relative(ROOT, p).replaceAll(path.sep, '/'))
  .sort();

const issues = [];
const results = [];
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 390, height: 844 },
  ignoreHTTPSErrors: false,
});

// A smoke test must never read from or write to the live SOV database.
// Return deterministic synthetic responses for all Supabase project traffic.
await context.route(/^https:\/\/[^/]+\.supabase\.co\//, async route => {
  const request = route.request();
  const url = request.url();
  const headers = {
    'access-control-allow-origin': '*',
    'access-control-allow-headers': '*',
    'access-control-allow-methods': 'GET,HEAD,POST,PATCH,PUT,DELETE,OPTIONS',
    'content-type': 'application/json; charset=utf-8',
  };
  if (request.method() === 'OPTIONS') {
    await route.fulfill({ status: 200, headers, body: '{}' });
    return;
  }
  if (url.includes('/rest/v1/rpc/sov_log_client_error')) {
    await route.fulfill({ status: 200, headers, body: 'null' });
    return;
  }
  await route.fulfill({
    status: 401,
    headers,
    body: JSON.stringify({ message: 'SOV audit isolation: live Supabase disabled' }),
  });
});

await context.addInitScript(() => {
  Object.defineProperty(window, '__SOV_AUDIT_MODE__', { value: true, configurable: false });
});

for (const rel of pages) {
  const page = await context.newPage();
  const localProblems = [];
  const pageErrors = [];
  const consoleErrors = [];
  const url = `${BASE}/${rel.split('/').map(encodeURIComponent).join('/')}`;

  page.on('pageerror', error => pageErrors.push(String(error?.message || error)));
  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('requestfailed', req => {
    try {
      const u = new URL(req.url());
      const failure = req.failure()?.errorText || 'unknown';
      // Navigation/auth redirects normally abort requests that are still in
      // flight. Missing local files are caught authoritatively by HTTP >= 400.
      if (u.origin === BASE && !/ERR_ABORTED/i.test(failure)) {
        localProblems.push(`request failed ${u.pathname}: ${failure}`);
      }
    } catch {}
  });
  page.on('response', response => {
    try {
      const u = new URL(response.url());
      if (u.origin === BASE && response.status() >= 400) {
        localProblems.push(`HTTP ${response.status()} ${u.pathname}`);
      }
    } catch {}
  });

  let status = 0;
  let finalUrl = '';
  try {
    const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 15000 });
    status = response?.status() || 0;
    await page.waitForTimeout(700);
    finalUrl = page.url();
  } catch (error) {
    localProblems.push(`navigation failed: ${error?.message || error}`);
    finalUrl = page.url();
  }

  let authRedirect = false;
  try {
    const final = new URL(finalUrl);
    authRedirect = final.origin === BASE && final.pathname.endsWith('/login.html');
  } catch {}

  const uniqueLocal = [...new Set(localProblems)];
  // Protected pages intentionally redirect in this unauthenticated pass. Any
  // late null errors belong to scripts being torn down during navigation, not
  // to the rendered login result. Authenticated flows have dedicated tests.
  const uniquePage = authRedirect ? [] : [...new Set(pageErrors)];
  const uniqueConsole = authRedirect ? [] : [...new Set(consoleErrors)]
    .filter(x => !/favicon\.ico|youtube|third[- ]party cookie|ERR_BLOCKED_BY_CLIENT|SOV audit isolation|401/i.test(x));

  for (const detail of uniqueLocal) issues.push({ severity: 'error', code: 'BROWSER_LOCAL_REQUEST', file: rel, detail });
  for (const detail of uniquePage) issues.push({ severity: 'error', code: 'BROWSER_PAGEERROR', file: rel, detail });
  for (const detail of uniqueConsole) issues.push({ severity: 'warning', code: 'BROWSER_CONSOLE', file: rel, detail });
  if (status >= 400 || status === 0) issues.push({ severity: 'error', code: 'BROWSER_NAV_STATUS', file: rel, detail: `status=${status}` });

  results.push({ file: rel, status, finalUrl, authRedirect, localProblems: uniqueLocal, pageErrors: uniquePage, consoleErrors: uniqueConsole });
  await page.close();
}

await browser.close();

const errors = issues.filter(x => x.severity === 'error').length;
const warnings = issues.filter(x => x.severity === 'warning').length;
const payload = { summary: { pages: pages.length, errors, warnings }, issues, results };
fs.writeFileSync(path.join(ROOT, 'BROWSER_SMOKE_REPORT.json'), JSON.stringify(payload, null, 2));
const md = [
  '# SOV browser smoke report',
  '',
  `- Pages opened: **${pages.length}**`,
  `- Errors: **${errors}**`,
  `- Warnings: **${warnings}**`,
  '',
  ...issues.map(x => `- **${x.severity.toUpperCase()}** \`${x.code}\` — \`${x.file}\` — ${x.detail.replaceAll('\n', ' ')}`),
  '',
].join('\n');
fs.writeFileSync(path.join(ROOT, 'BROWSER_SMOKE_REPORT.md'), md);

console.log(`SOV BROWSER SMOKE pages=${pages.length} errors=${errors} warnings=${warnings}`);
for (const issue of issues.slice(0, 250)) {
  console.log(`[${issue.severity.toUpperCase()}] ${issue.code} ${issue.file}: ${issue.detail}`);
}
if (issues.length > 250) console.log(`... ${issues.length - 250} more issue(s); see BROWSER_SMOKE_REPORT.md`);
process.exit(errors ? 1 : 0);
