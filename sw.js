// Flip 7 – Offline-Speicher der Web-App (GitHub Pages).
// • Seite: kommt sofort aus dem Speicher (auch ohne Internet am Spieltisch). Im Hintergrund wird
//   die neueste index.html geholt – sie gilt ab dem nächsten Start, nie mitten im Spiel.
//   Für ein Update der Seite muss hier nichts geändert werden.
// • Schriften: die Kopien aus android/assets/fonts, genau wie in der APK.
const CACHE = 'flip7-v39';
const FONTS = 'android/assets/fonts/';
const CORE = [
  './',
  'manifest.webmanifest',
  'icons/apple-touch-icon.png',
  'icons/icon-192.png',
  'icons/icon-512.png',
  FONTS + 'fonts.css',
  FONTS + '8vIH7w4qzmVxm25L9G78HEZnMg.woff2',
  FONTS + '8vIH7w4qzmVxm2BL9G78HEY.woff2',
  FONTS + '8vIH7w4qzmVxm2NL9G78HEZnMg.woff2',
  FONTS + 'aFTR7PB1QTsUX8KYvumzEY2tbYf-Vlh3uA.woff2',
  FONTS + 'aFTR7PB1QTsUX8KYvumzEYOtbYf-Vlg.woff2',
  FONTS + 'aFTU7PB1QTsUX8KYthSQBK6PYK3EXw.woff2',
  FONTS + 'aFTU7PB1QTsUX8KYthqQBK6PYK0.woff2',
];

self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE)
      .then(c => c.addAll(CORE.map(u => new Request(u, { cache: 'reload' }))))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k.startsWith('flip7-') && k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  // Google-Schriften → mitgelieferte Kopien (wie die APK)
  if (url.hostname === 'fonts.googleapis.com') { e.respondWith(local(FONTS + 'fonts.css')); return; }
  if (url.hostname === 'fonts.gstatic.com') { e.respondWith(local(FONTS + url.pathname.split('/').pop())); return; }
  if (url.origin !== self.location.origin) return;
  if (req.mode === 'navigate') { e.respondWith(page(e)); return; }
  e.respondWith(caches.match(req, { ignoreSearch: true }).then(r => r || fetch(req)));
});

// Seite: sofort aus dem Speicher, frische Version für den nächsten Start im Hintergrund holen
async function page(e) {
  const cache = await caches.open(CACHE);
  const cached = await cache.match('./');
  const fresh = fetch('./', { cache: 'no-cache' }).then(r => {
    if (r.ok) cache.put('./', r.clone());
    return r;
  });
  if (cached) {
    e.waitUntil(fresh.catch(() => {}));
    return cached;
  }
  return fresh.catch(() => new Response('Flip 7 ist offline – bitte einmal mit Internet öffnen.',
    { status: 503, headers: { 'Content-Type': 'text/plain; charset=utf-8' } }));
}

function local(path) {
  const u = new URL(path, self.registration.scope).href;
  return caches.match(u).then(r => r || fetch(u));
}
