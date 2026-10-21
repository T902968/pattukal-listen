# Pattukal iOS Web App (PWA)

This folder contains a complete **Progressive Web App** designed for iOS users.

## How to test on iPhone / iPad

1. Open the page in **Safari** (required for full Add to Home Screen support).
2. Tap the **Share** button (square with upward arrow).
3. Scroll down and tap **Add to Home Screen**.
4. Confirm the name "Pattukal" and tap **Add**.
5. The icon appears on your Home Screen. Opening it launches full-screen like a native app.

## Features

- Standalone display mode (no Safari chrome)
- Offline support via Service Worker
- Apple-specific meta tags & status bar styling
- Safe-area support for modern iPhones
- Liquid-glass visual style matching Pattukal branding
- Android APK download still available
- Auto-updates release info from GitHub

## Local preview

Serve the `ios/` folder with any static server, e.g.:

```bash
npx serve ios
```

Then open the URL in Safari on your device (or use a tunnel).

## Deploy

You can point GitHub Pages (or any static host) to the `ios/` directory, or merge into main and update the existing deploy workflow.

---

Branch: `ios`  
Created for iOS testing by request.
