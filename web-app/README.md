# Pattukal Web (test build)

Single-page web app served at `/app-test/`. Plays through an audio element fed by a stream from the same public servers (keeps playing in the background), with the YouTube IFrame player as the fallback when no stream can be fetched (layouts, themes and extras never touch the engine), finds songs through public Piped / Invidious servers, lyrics from LRCLIB.

## Files
- `index.html` – app (player core, home, search, library, Theme Studio, settings)
- `extras.js` – Music Galaxy and Buddy, lazy-loaded only when opened
- `halloween.js` – Halloween Edition, Gesture Music, Mystery Capsule, Time Fold, collections, liquid song-open effect (same features as the Android app 1.5.0)
- `sw.js` – network-first service worker (bumps `C` to refresh caches)

## Platform limits (not hidden)
- No audio-reactive visualizer, crossfade, gapless or forced streaming quality: YouTube's player exposes none of them.
- iPhone Safari may pause audio when the screen locks or the page is suspended; the page re-syncs when you return. Autoplay after a track change needs an earlier tap.
- Karaoke needs the Android app.
- Buddy is rule-based and runs on the device; there is no AI service.

## Not the same as Android (browser limits)
- Gesture Music: circle, wave, diagonal and heart work; writing a song name is a typed box because browsers have no handwriting recognition.
- Ambient sounds are generated with Web Audio and are off by default.
- Pattukal Blue app icon and the notification glyph are Android-only.
