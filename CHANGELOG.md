# Nebula Browser 4.0

## Added

- **Dynamic theming** — Material You wallpaper palette on Android 12+, six curated
  accents (violet, cyan, magenta, lime, amber, cobalt), and theme-aware glass,
  borders and typography.
- **Motion system** — shared duration/easing tokens, press feedback on buttons,
  pulsing brand mark, animated download progress and an optional drifting
  background. "Reduce motion" disables all of it.
- **Command Center** — one filterable sheet that reaches every screen and tool.
  Open it from the bolt button on Home or from the drawer.
- **Home refresh** — time-of-day greeting with live tab/bookmark counts and a
  live download card with animated progress.
- **New page tools** — screenshot of the visible viewport, save page as
  Markdown, save page as HTML, copy the whole article to the clipboard, and
  clear all cookies and site data. Everything lands in `Download/Nebula`.
- **Download shortcuts** — copy a download link from the downloads screen.
- **Optimized release APK** — R8 minification and resource shrinking, debug-signed
  in CI so the artifact stays installable.
- **Faster first page** — the System WebView provider is warmed on a background
  looper thread during startup, and a Compose stability configuration lets Home
  and tab lists skip recomposition of the shared model classes.

## Changed

- Version `4.0.0` (versionCode 6).
- Glass cards, chips, buttons and highlights now follow the selected accent
  instead of a fixed violet.
- Settings gained a **Theme & motion** section and show the real version number.
- Android predictive back is enabled.
- CI builds and publishes both the debug and the optimized release APK, with a
  size summary in the run page.

## Fixed

- Repaired the multi-line string literals in the safe agent that broke Kotlin
  compilation and cascaded into 200+ errors.
- Added the missing Compose imports (`LazyRow`, `Color`, `ArrowForward`,
  `Refresh`, `MoreVert`, `sp`) that failed the APK workflow.

## Not in this release

Chrome Web Store extension execution, cross-device sync, passkeys, a Gecko
engine flavor, split view and full-page screenshot stitching. See
[`docs/FEATURES.md`](docs/FEATURES.md) for the honest status table.
