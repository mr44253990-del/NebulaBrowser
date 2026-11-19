# Feature status

Nebula reports what the code actually does. Everything in **Working** runs in the
current APK; **Limited** works with the documented constraints; **Not supported**
is deliberately absent.

## Interface

| Feature | Status | Notes |
| --- | --- | --- |
| Material You dynamic color | Working | Android 12+; falls back to the Nebula palette |
| Curated accents | Working | Violet, cyan, magenta, lime, amber, cobalt |
| Reduce motion | Working | Disables transitions, pulse and background drift |
| Command Center | Working | Filterable list of every screen and tool |
| Drawer | Working | Grouped sections, search field, explicit close button, swipe to close |
| Bottom bars | Working | New tab (+) with long-press private tab; compact quick bar while browsing |
| Launcher shortcuts | Working | New tab, private tab, bookmarks and tab switcher |
| Home greeting and live cards | Working | Visit counts, active downloads |
| Five background styles | Working | `nebula`, `aurora`, `midnight`, `minimal`, plus light theme |
| Adaptive tablet layout | Not supported | Phone layout only |

## Browsing

| Feature | Status | Notes |
| --- | --- | --- |
| Chromium System WebView engine | Working | HTTPS-first, mixed-content blocked, safe browsing |
| Tabs, private tabs, groups, workspaces | Working | Group colours, drag-to-group, restore closed tab |
| Tab switcher views | Working | Grid and list, search, swipe-left to close in list mode |
| Session restore | Working | `WebView.saveState` snapshots per tab |
| Smart omnibox | Working | URLs, `gh`/`w`/`d` keywords, maths, unit conversion |
| Reader mode + text-to-speech | Working | Adjustable type size and reading time |
| Find in page, desktop UA, print/PDF | Working | System print service |
| QR generate and scan | Working | Camera permission required for scanning |
| Voice search | Working | Depends on the device speech service |

## Tools added in 3.1

| Feature | Status | Notes |
| --- | --- | --- |
| Screenshot | Working | Visible viewport, PNG, `Download/Nebula` |
| Save page as Markdown | Working | Title, URL and readable text |
| Save page as HTML | Working | `documentElement.outerHTML` |
| Copy page text | Working | Whole readable article to the clipboard |
| Clear cookies and site data | Working | Clears WebView cookies, storage and cache |
| Download copy-link | Working | Other actions: open, share, retry, cancel, remove |
| Full-page scrolled screenshot | Not supported | Needs scroll-and-stitch capture |

## Data and privacy

| Feature | Status | Notes |
| --- | --- | --- |
| History, bookmarks, HTML import/export | Working | Room-backed |
| Download manager | Working | Android `DownloadManager`, Wi-Fi-only policy, notifications |
| Password vault | Working | AES-GCM with a non-exportable Keystore key, biometric unlock |
| Tracker/ad host blocking | Limited | Built-in host suffix list, not a full filter-list engine |
| Per-site permission prompts | Limited | Camera, microphone and location only |
| Per-site settings and site-data manager | Not supported | Planned for 3.2 |
| Cross-device sync, passkeys | Not supported | No account exists |

## AI

| Feature | Status | Notes |
| --- | --- | --- |
| Page assistant | Working | OpenAI-compatible endpoint, explicit consent dialog |
| Full-screen AI workspace | Working | Chat history, quick prompts, copy and transcript export |
| Research agent | Working | Safe action plan, progress timeline, cancel, exports text/PDF reports |
| Streaming responses | Not supported | Single request/response |
| On-device local model | Not supported | Planned |

## Extensions

| Feature | Status | Notes |
| --- | --- | --- |
| Manifest/CRX/XPI/ZIP analyzer | Working | Static analysis only |
| Userscripts (`@grant none`) | Working | Injected per declared `@match` |
| Web Store installation or MV3 execution | Not supported | Requires a Chromium extension runtime |

## Performance

| Item | Status |
| --- | --- |
| R8 minification and resource shrinking | Enabled for release |
| Debug-signed release artifact in CI | Enabled |
| Gradle build cache and parallel builds | Enabled |
| Lazy tab-session restore (max 4 live WebViews at launch) | Enabled |
| Startup crash recovery and guarded WebView creation | Enabled |
| Compose stability configuration for shared models | Enabled |
| Baseline profiles / startup macrobenchmarks | Not yet |
| Room paging for large histories | Not yet |
