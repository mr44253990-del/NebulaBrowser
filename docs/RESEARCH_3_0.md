# Nebula 3.0 deep research and safety design

## Desktop rendering

Android documents that WebView does not use a wide viewport by default and that viewport metadata determines CSS layout width. Server-side mobile detection also uses the User-Agent, while newer sites can inspect User-Agent Client Hints. Nebula therefore applies all of these per tab:

- Windows desktop User-Agent without the Android Mobile token.
- `UserAgentMetadata` with desktop platform and Desktop form factor when supported by the installed WebView.
- Wide viewport, overview mode, zoom support, normal layout algorithm, and 1280px viewport metadata.
- Reload and persistence in the tab database. Mobile mode restores Android metadata and `width=device-width`.

Sources: Android “Support different screens in web apps”, “Best practices for web apps”, `WebSettings`, `UserAgentMetadata`, and `WebSettingsCompat`.

## Resilient downloads

Android DownloadManager owns HTTP transfers, background execution, retries across connectivity changes, and system reboots. Nebula does not replace it with an unreliable in-process coroutine. A separate foreground monitor only reads system status and displays percentage, throughput, ETA, and network-wait state. Android 15's dataSync timeout callback stops the monitor promptly; the system transfer continues independently.

Sources: `DownloadManager`, `DownloadManager.Request`, Android background data-transfer options, foreground-service overview and timeout contracts.

## Immersive UI

`WindowInsetsControllerCompat` hides/shows system bars with transient-swipe behavior. Nebula keeps a floating menu, optional three-finger gesture, and opt-in narrow edge swipe so users are never trapped without browser controls. Auto-hide is separate from full immersive mode and can be disabled.

## PDF crash mitigation

`PdfRenderer` pages are opened/rendered/closed one at a time and the renderer closes on disposal. Rendering is bounded to 900px width and 4096px height, every operation is failure-guarded, and images use bounded dimensions rather than unconstrained scroll measurement. External PDF apps remain available through Open with.

## AI agent safety

The AI provider receives the user's planning prompt, not browsing history. It may propose only a strict allowlist: `open_urls`, `go_back`, `collect_pages`, `export_text`, and `export_pdf`. URLs must be HTTPS and are capped at 12; plans are capped at 16 steps. The user reviews the plan before execution. Collected text is exported locally and is not sent back for a second AI request. No arbitrary JavaScript, forms, purchases, account changes, credentials, hidden downloads, or unrestricted device actions are accepted.

This is a real safe automation workflow, not a claim that an LLM can reliably perform every possible action on every website. Dynamic sites, consent walls, CAPTCHAs, and pages with little readable text may produce incomplete source content; step details expose those outcomes.

## Userscripts vs browser extensions

Nebula userscripts are deliberately limited to `@grant none` and declared match patterns. They are not Chrome MV3/Firefox WebExtensions. Packages requiring GM APIs are installed disabled with an explanation. CRX/XPI static compatibility reports remain separate from installed userscript counts.
