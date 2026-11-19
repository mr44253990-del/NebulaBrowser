# Architecture

Nebula separates browser UI, durable browser data, security, downloads, and rendering engines.

## Engine contract

`engines/api` defines `BrowserEngine`, `BrowserSession`, and typed `EngineEvent` values. `BrowserViewModel` owns session lifecycles and maps events into one immutable `BrowserUiState`. Compose renders state and sends user intents back to the view model.

The current implementation is Android System WebView, which uses Chromium. WebView is **not** a desktop Chromium embed and does not expose extension service workers, content-script isolation, extension storage, browser-action popups, or Chrome Web Store installation.

## Data

- Room: bookmarks, history, and download metadata.
- DataStore: non-secret preferences and custom search engines.
- SharedPreferences ciphertext + Android Keystore: password vault and AI API key.
- Android DownloadManager: resilient background HTTP(S) transfers and system notifications.

Database version 1 has explicit Room entities and indices. Future changes must add migrations and migration tests; destructive upgrades are not allowed. Only destructive *downgrades* use Room's fallback.

## Security boundaries

- SSL errors are cancelled, never ignored.
- Mixed content is disabled and safe browsing is enabled.
- Private sessions disable cookies, DOM storage, and database storage, do not write history, and clear WebView cache/history on close.
- Web permission and geolocation prompts are denied until a native, per-origin consent flow exists.
- Extension packages are parsed as data and never executed.
- AI page extraction and provider upload are separate actions with a consent screen.

## Adding GeckoView

Implement `BrowserEngine` and `BrowserSession` in a new `engines/gecko` module. Add a build flavor or runtime engine selector, map Gecko delegate events to `EngineEvent`, and isolate Gecko-specific WebExtension management behind a separate extension-runtime interface. Do not leak Gecko types into Compose or browser data modules.
