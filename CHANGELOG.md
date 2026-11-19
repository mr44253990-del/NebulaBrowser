# Nebula Browser 4.0

## Unreleased — 4.1.0

- Drawer: explicit close (X) button, search field, grouped sections, rounded sheet and version footer.
- Bottom bars: new tab (+) button with long-press for private tabs on Home; compact quick bar with back, forward, new tab, tabs and menu while browsing a page.
- AI: full-screen Nebula AI workspace with chat history, suggestion cards, quick prompts, consent-aware sending, conversation export and an agent timeline with progress, cancel and report export. The quick sheet gains a header, close button and Expand action.
- Tabs: grid/list toggle, swipe-left to close in list mode, and a page menu split from the tab counter.
- Diagnostics in Settings (from 4.0.1): crash log view, copy, share, clear and clean start.

## 4.0.1 — crash fix

- Removed the background WebView warm-up that created a WebView off the main thread during `Application.onCreate`; it could abort startup on some devices.
- Startup tab restore is now guarded per tab. A corrupt or outdated saved WebView snapshot is deleted and the URL reloads instead of crashing on every launch.
- Live WebViews are created lazily and capped at four at launch, which also removes a low-memory crash with many tabs.
- If the previous process died during restore, the next launch starts without snapshots and says so.
- WebView creation, desktop mode, state restore and debugging flags are all wrapped so a missing or updating System WebView degrades to a message.
- Download progress service and share intents no longer throw on restricted Android versions.
- Settings → **Diagnostics**: view, copy, share or clear the on-device crash report, and start clean.
- Release keep rules now cover the whole app package, so the R8-minified APK cannot lose app classes that Compose reaches indirectly.
- Version `4.0.1` (versionCode 7).

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
