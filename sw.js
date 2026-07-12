/* SOV PWA service worker — Korak 1.
 * Pravila:
 * - Supabase / Apps Script / ne-GET nikad ne keširamo.
 * - HTML: network-first, fallback cache, fallback offline.html.
 * - assets: stale-while-revalidate.
 * - kill-switch: sw-disable.flag vraća 200 => SW se odjavljuje.
 */
const SW_VERSION = '6.1.46a-pwa-step1';
const CACHE_PREFIX = 'sov-pwa';
const SHELL_CACHE = `${CACHE_PREFIX}-shell-${SW_VERSION}`;
const HTML_CACHE = `${CACHE_PREFIX}-html-${SW_VERSION}`;
const ASSET_CACHE = `${CACHE_PREFIX}-assets-${SW_VERSION}`;

const PRECACHE_URLS = [
  '/',
  '/index.html',
  '/login.html',
  '/offline.html',
  '/manifest.webmanifest',
  '/assets/site.css',
  '/assets/mobile.css',
  '/assets/supabase-config.js',
  '/assets/auth.js',
  '/assets/sov-foundation-v55822.css',
  '/assets/sov-shell-v55825.css',
  '/assets/sov-polish-v55826.css',
  '/assets/sov-wow-v6.css',
  '/assets/sov-wow-app-v6.css',
  '/assets/sov-typography-v6144g.css',
  '/assets/sov-foundation-v55822.js',
  '/assets/sov-shell-v55825.js',
  '/assets/sov-polish-v55826.js',
  '/assets/sov-wow-v6.js',
  '/assets/sov-version.js',
  '/assets/pwa-register.js',
  '/assets/sov-logo.png',
  '/assets/icons/sov-icon-192.png',
  '/assets/icons/sov-icon-512.png',
  '/assets/icons/sov-icon-maskable-192.png',
  '/assets/icons/sov-icon-maskable-512.png'
];

function isBlockedRequest(request) {
  if (request.method !== 'GET') return true;
  const url = new URL(request.url);
  const host = url.hostname.toLowerCase();
  if (host.endsWith('.supabase.co')) return true;
  if (host === 'script.google.com' || host.endsWith('.script.google.com')) return true;
  return false;
}

function isHtmlRequest(request) {
  const url = new URL(request.url);
  if (request.mode === 'navigate') return true;
  if (request.headers.get('accept') && request.headers.get('accept').includes('text/html')) return true;
  return url.origin === self.location.origin && url.pathname.endsWith('.html');
}

function isSameOriginAsset(request) {
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return false;
  return /\.(css|js|png|jpg|jpeg|webp|svg|gif|ico|woff2?|ttf|json|webmanifest)$/i.test(url.pathname);
}

async function maybeDisableSelf() {
  try {
    const res = await fetch('/sw-disable.flag?ts=' + Date.now(), {cache: 'no-store'});
    if (res && res.ok) {
      const keys = await caches.keys();
      await Promise.all(keys.filter(k => k.startsWith(CACHE_PREFIX)).map(k => caches.delete(k)));
      await self.registration.unregister();
      const clients = await self.clients.matchAll({type: 'window'});
      clients.forEach(client => client.navigate(client.url));
      return true;
    }
  } catch (_) {
    // Nema kill-switch datoteke ili nema mreže — normalno stanje.
  }
  return false;
}

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL_CACHE);
    await Promise.allSettled(PRECACHE_URLS.map(url => cache.add(new Request(url, {cache: 'reload'}))));
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys
      .filter(k => k.startsWith(CACHE_PREFIX) && ![SHELL_CACHE, HTML_CACHE, ASSET_CACHE].includes(k))
      .map(k => caches.delete(k)));
    await self.clients.claim();
    await maybeDisableSelf();
  })());
});

self.addEventListener('message', event => {
  if (event.data && event.data.type === 'SOV_SKIP_WAITING') {
    self.skipWaiting();
  }
});

async function networkFirstHtml(request) {
  const cache = await caches.open(HTML_CACHE);
  try {
    const fresh = await fetch(request, {cache: 'no-store'});
    if (fresh && fresh.ok) cache.put(request, fresh.clone());
    return fresh;
  } catch (_) {
    const cached = await cache.match(request) || await caches.match(request);
    return cached || await caches.match('/offline.html');
  }
}

async function staleWhileRevalidate(request) {
  const cache = await caches.open(ASSET_CACHE);
  const cached = await cache.match(request);
  const freshPromise = fetch(request).then(response => {
    if (response && response.ok) cache.put(request, response.clone());
    return response;
  }).catch(() => null);
  return cached || freshPromise || fetch(request);
}

self.addEventListener('fetch', event => {
  const request = event.request;
  if (isBlockedRequest(request)) return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (isHtmlRequest(request)) {
    event.respondWith(networkFirstHtml(request));
    return;
  }
  if (isSameOriginAsset(request)) {
    event.respondWith(staleWhileRevalidate(request));
  }
});
