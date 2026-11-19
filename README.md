# Nebula Browser

A real, modular Android browser built with **Kotlin, Jetpack Compose, Material 3, Room, DataStore, Android Keystore, and Chromium System WebView**.

> **Honest extension support:** Nebula includes a static CRX/XPI/ZIP compatibility analyzer. It does **not** claim to execute desktop Chrome or Firefox extensions. Full extension execution requires a native Chromium extension runtime and/or GeckoView engine, which is documented in the roadmap.

## Working features

- Chromium WebView browsing with HTTPS-first input, certificate-error blocking, safe browsing, mixed-content blocking, external-app links, file upload, and page progress.
- Multiple tabs, private tabs, pin/duplicate/restore/close-others, and five workspaces.
- Smart omnibox: URL detection, search keywords (`gh`, `w`, `d`, etc.), arithmetic, and common unit conversions.
- Customizable new-tab page, quick links, recent history, bottom/top address bar, dark/light theme, and 3–5 column grid.
- Room-backed history and bookmarks, search, visit counts, deletion, and Netscape HTML bookmark import/export.
- Android system background downloads with notifications, progress sync, cancel, retry, remove, open, Wi-Fi-only policy, and public Downloads storage.
- AES-GCM password vault using a non-exportable Android Keystore key, strong biometric unlock, secure password generation, and credential search.
- Native advertising/tracker host filtering with a real per-session blocked-request counter.
- Reader mode with selectable text, adjustable typography, reading-time estimate, and Android text-to-speech.
- Optional OpenAI-compatible page assistant with HTTPS endpoint validation, encrypted API key, model/temperature controls, and an explicit page-sharing consent dialog.
- Find in page, desktop user agent, page sharing, QR generation/scanning, voice search, and system Print/Save-as-PDF.
- Dynamic Material You theming with six curated accents, a shared motion system with a reduce-motion switch, and a filterable Command Center that reaches every tool.
- Screenshot capture, save page as Markdown or HTML, copy article text, clear cookies/site data, and copy download links. Exports land in `Download/Nebula`.
- Voice and QR features depend on capabilities available on the device.

## APK from GitHub Actions

Every push to `main`, pull request, or manual workflow dispatch runs unit tests and builds two APKs:

- **`NebulaBrowser-debug-apk`** — the fast debug build for testing.
- **`NebulaBrowser-release-apk`** — R8-minified and resource-shrunk. It is signed with the debug key so it stays installable, and is not a Play Store release.

Download either from the workflow run's **Artifacts** section. The run page also reports both APK sizes.

To build manually:

```bash
./gradlew testDebugUnitTest assembleDebug assembleRelease
```

Outputs: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release.apk`.

## Project structure

```text
app/                    Compose UI, Android integration, AI and extension analysis
core/domain/           Shared models and smart omnibox parser
core/database/         Room entities, DAOs, repositories
core/datastore/        Persistent browser and AI preferences
core/security/         Keystore encryption and password vault
browser/core/          Android background download controller
engines/api/           Engine-neutral session contract and events
engines/chromium/      Chromium System WebView implementation
.github/workflows/     Automatic test + APK artifact pipeline
docs/                  Architecture, privacy, features, and roadmap
```

The UI communicates with `BrowserEngine`/`BrowserSession`, not directly with WebView. A Gecko engine can therefore be added without rewriting browser data modules.


## Nebula 2.0 update

- Responsive purple-neon/glass design across Home, Workspaces, Settings, libraries, and tools.
- Persistent normal tabs and WebView navigation state, real page previews, named tab groups, and long-press tab controls.
- Add/remove/rearrange home shortcuts and per-tab desktop mode.
- Built-in text/code, image, PDF, ZIP listing, and media file viewers plus FileProvider open/share.
- Origin + Android runtime permission prompts for camera, microphone, and location.
- Developer Mode with Chromium remote debugging, JS evaluator, page console/source, and local crash reports.
- Drawer opens only from the menu button, preventing accidental website swipe gestures; the drawer itself is scrollable.

See [the 2.0 research notes](docs/RESEARCH_2_0.md) for Android platform guidance and future candidates.


## Nebula 2.1 readability and compact-toolbar update

- Explicit high-contrast text, placeholder, icon, button and card colors for dark and light themes.
- Dark system status/navigation bars prevent white-on-white system text over light webpages.
- Webpages use one compact 52dp address toolbar instead of separate address and navigation bars; the 58dp navigation dock remains on Home/Tabs.
- Premium nebula clouds, cyan/purple orbital waves, and a layered 3D planet mark.


## Nebula 3.0 platform update

- Desktop mode now combines desktop UA, desktop UA Client Hints/form-factor, a 1280px viewport, wide viewport/overview settings, and per-tab persistence.
- System-resilient downloads with live percentage/speed/ETA notifications, network-wait status, elapsed time, filters, and multi-select deletion.
- Bounded, crash-resistant PDF page rendering and safer text/image/archive/media viewers.
- Immersive display, auto-hide toolbar, floating menu, optional edge gesture, three-finger menu, and selectable background styles.
- Real bundled brand vectors, most-visited Home ranking, and persistent ranked search suggestions.
- Searchable tabs, drag-to-group handles, in-browser group dock, and same-group tab creation.
- Installed `@grant none` userscripts with declared site permissions, separate from static CRX/XPI reports.
- Approved safe AI research plans with real-time steps, up to 12 HTTPS sources, and local TXT/PDF report export.

The AI agent is intentionally not an unrestricted arbitrary-action system. See the 3.0 research notes.

## Nebula 4.0 → 4.1 experience update

- Material You dynamic color on Android 12+, six curated accents, and theme-aware glass surfaces, chips and buttons.
- A shared motion system (duration tokens, spring press feedback, pulsing mark, animated progress) with a system-friendly **Reduce motion** switch.
- A searchable drawer with grouped sections and an explicit close button.
- New tab buttons on both bottom bars, with long-press for a private tab.
- A full-screen Nebula AI workspace: chat history, quick prompts, export, and a research-agent timeline with progress, cancel and report export.
- A tab switcher with grid/list modes, swipe-to-close and per-tab pin, group and workspace actions.
- Command Center: a filterable sheet that reaches every screen and tool, opened from the bolt button on Home or the drawer.
- Home shows a time-of-day greeting, live tab/bookmark counts and a live download card.
- New page tools: screenshot, save as Markdown, save as HTML, copy article text, clear cookies/site data, and copy download links.
- Release builds are R8-minified with resource shrinking and published next to the debug APK by CI.
- Startup is faster and safer: live WebViews are restored lazily (four at a time) and a startup guard recovers from a previous crash instead of repeating it.
- Launcher shortcuts jump straight to a new tab, a private tab, bookmarks or the tab switcher.
- Version 4.1.0 (versionCode 8).

## Privacy and limitations

Nebula has no app server and no analytics. Web requests go to visited sites. AI requests go directly to the user's configured provider only after confirmation. See [PRIVACY.md](docs/PRIVACY.md).

This is a substantial 2.0 foundation, not the entire multi-year master plan. Unsupported features are listed transparently in [FEATURES.md](docs/FEATURES.md) and [ROADMAP.md](docs/ROADMAP.md).
