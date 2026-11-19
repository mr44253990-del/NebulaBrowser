# Nebula 2.0 research and implementation notes

## Official Android guidance applied

- **WebView state:** Android's `WebView.saveState(Bundle)` / `restoreState(Bundle)` preserve navigation state. Nebula stores per-tab bundles in app-private files instead of Activity `savedInstanceState`, avoiding the shared ~1 MB Binder transaction limit and `TransactionTooLargeException` risk. Oversized/corrupt state falls back to the last URL.
- **Cookies:** normal tabs keep the persistent WebView cookie store and call `CookieManager.flush()` on lifecycle persistence. Private mode must not disable the process-wide cookie manager because that would log users out of normal tabs.
- **Website permissions:** `WebChromeClient` permission requests expose only camera/microphone resources understood by Nebula. The user must approve both the website prompt and Android runtime permission. Geolocation has a separate origin prompt. Unknown protected resources are denied.
- **Preview images:** tab cards capture the actual attached WebView into an app-private WebP preview; no remote screenshot service is used.

Sources:
- https://developer.android.com/develop/ui/views/layout/webapps/manage-state
- https://developer.android.com/reference/android/webkit/WebChromeClient
- https://developer.android.com/reference/android/webkit/PermissionRequest
- https://developer.android.com/reference/android/webkit/package-summary

## New feature candidates considered and implemented

1. Named tab groups, group filtering, and long-press tab actions.
2. Persistent shortcuts with add/remove/rearrange.
3. Actual tab previews and desktop-mode state per tab.
4. Built-in text/code, image, PDF, archive-list, and device-codec media viewers.
5. Developer console, JavaScript evaluator, console messages, page source, and local crash logs.
6. Runtime camera/microphone/location/notification controls.
7. AI presets for summary, explanation, fact extraction, Bengali translation, and research questions.

## Next candidates (not falsely marked complete)

- Cross-device encrypted sync, passkeys and Android Autofill service.
- Native GeckoView engine and separately tested WebExtension runtime.
- Offline article archive, advanced PDF annotation and parallel-part downloader.
- Strong per-profile cookie isolation using a properly separated engine/process architecture.
- On-device AI, streaming responses, local translation and content-blocking filter-list subscriptions.

Priority remains migration safety, session reliability, permission correctness, accessibility, and battery/memory measurements before additional surface features.
