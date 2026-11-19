# Feature status

## Implemented in 1.0

Browsing, progress, navigation, file upload, external schemes, tabs, workspaces, private browsing restrictions, history, bookmarks + HTML interchange, downloads, password vault, search engine manager, smart calculations/conversions, native host filtering, reader/TTS, PDF print, voice search, QR tools, desktop mode, static extension reports, and consent-based remote AI.

## Deliberately not represented as working

- Chrome Web Store installation or execution of CRX extensions.
- Firefox XPI execution, Gecko rendering, native messaging, or extension background workers.
- Cross-device sync, passkeys, payment/address autofill, Android Autofill service.
- Custom parallel-part downloader, scheduling, and user-controlled pause/resume. Android DownloadManager may pause transfers for OS/network reasons.
- Full PDF annotation/editing, offline web archive, service-worker/PWA installation UI.
- Per-origin camera/microphone/location grants; these are securely denied.
- Complete filter-list compatibility (the native blocker uses a built-in host suffix set).
- On-device AI, streaming responses, local translation models.
- Full cookie/storage isolation equivalent to a separate browser profile. Android WebView shares process infrastructure.

See the roadmap for implementation order.
