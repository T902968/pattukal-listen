# Pattukal Web (test build)

Single-page web app served at `/app-test/`. Plays through the YouTube IFrame player (the only audio engine; layouts, themes and extras never touch it), finds songs through public Piped / Invidious servers, lyrics from LRCLIB.

## Files
- `index.html` – app (player core, home, search, library, Theme Studio, settings)
- `extras.js` – Music Galaxy and Buddy, lazy-loaded only when opened
- `sw.js` – network-first service worker (bumps `C` to refresh caches)

## Platform limits (not hidden)
- No audio-reactive visualizer, crossfade, gapless or forced streaming quality: YouTube's player exposes none of them.
- iPhone Safari may pause audio when the screen locks or the page is suspended; the page re-syncs when you return. Autoplay after a track change needs an earlier tap.
- Karaoke needs the Android app.
- Buddy is rule-based and runs on the device; there is no AI service.
