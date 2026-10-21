# Pattukal – Full iOS / Web Music App

Complete Progressive Web App music player that you can install on iPhone / iPad (Add to Home Screen).

## Features

- **Home** – Quick Mix cards + Trending + For You lists
- **Search** – Live filter across songs
- **Library** – Liked songs + Recently played (saved in localStorage)
- **Full-screen Player** – Progress, shuffle, repeat, like, next/prev
- **Mini Player** – Always available while browsing
- **Media Session** – Lock screen / Control Center controls on iOS
- **Offline shell** – Service Worker caches the app
- **Installable** – Works as standalone app after Add to Home Screen

## How to test on iPhone

1. Open the app in **Safari**:
   - After GitHub Pages deploy, or
   - Use a raw link / local server / tunnel pointing to the `ios/` folder
2. Tap **Share → Add to Home Screen**
3. Open the icon – it runs full-screen like a native app
4. Tap any song – it plays, mini player appears, full player opens

## Current music source

Demo tracks from Bensound (royalty-free) so playback works immediately without API keys.

Next upgrade path: connect real YouTube Music / Innertube search + streaming (requires backend proxy for CORS & reliability).

## Files

| File | Role |
|------|------|
| `index.html` | App shell + screens |
| `styles.css` | Liquid-glass UI |
| `app.js` | Player, queue, library, search logic |
| `sw.js` | Offline caching |
| `manifest.webmanifest` | PWA install |

## Local run

```bash
npx serve ios
```

Then open the URL in Safari on your device.

---

Branch: `ios`  
Full web app experience for testing on iOS.
