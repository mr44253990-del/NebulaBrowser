# Nebula Browser

A real, modular Android browser built with **Kotlin, Jetpack Compose, Material 3, Room, DataStore, Android Keystore, and Chromium System WebView**.

> **Honest extension support:** Nebula 1.0 includes a static CRX/XPI/ZIP compatibility analyzer. It does **not** claim to execute desktop Chrome or Firefox extensions. Full extension execution requires a native Chromium extension runtime and/or GeckoView engine, which is documented in the roadmap.

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
- Voice and QR features depend on capabilities available on the device.

## APK from GitHub Actions

Every push to `main`, pull request, or manual workflow dispatch runs unit tests and builds the debug APK. Download **`NebulaBrowser-debug-apk`** from the workflow run's **Artifacts** section. The APK is debug-signed for testing and is not a Play Store release.

To build manually:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

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

## Privacy and limitations

Nebula has no app server and no analytics. Web requests go to visited sites. AI requests go directly to the user's configured provider only after confirmation. See [PRIVACY.md](docs/PRIVACY.md).

This is a substantial 1.0 foundation, not the entire multi-year master plan. Unsupported features are listed transparently in [FEATURES.md](docs/FEATURES.md) and [ROADMAP.md](docs/ROADMAP.md).
