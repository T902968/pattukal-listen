# Pattukal – Real YouTube Music Web App

Full Progressive Web App that can **search and play real YouTube / YouTube Music tracks**.

## How it works

- Search uses public **Piped** instances (privacy-friendly YouTube frontend APIs).
- When you tap a song it fetches a real audio stream and plays it.
- Same installable PWA experience on iPhone / iPad (Add to Home Screen).

## Features

- Live search for real songs
- Actual streaming playback
- Home / Trending loaded from real data
- Mini player + full-screen player
- Like + Recently played (localStorage)
- Media Session (lock screen controls)
- Offline app shell

## Test on iPhone

1. Open in **Safari**:
   ```
   https://raw.githack.com/T902968/pattukal-listen/ios/ios/index.html
   ```
2. Share → **Add to Home Screen**
3. Search any song and play

## Limitations (honest)

- Depends on public Piped instances (sometimes one is down → it auto-rotates).
- Not 100% identical to the Android Innertube client (no official YouTube login, no offline downloads yet, no Cast).
- Some tracks may fail if the instance is rate-limited or geo-blocked.

This is the practical way to get **real playback** in a pure web app without running your own backend.

## Next possible upgrades

- Self-hosted Piped / Invidious backend for reliability
- Lyrics
- Playlists & Google account sync
- Better error handling & instance health checks

Branch: `ios`
