# Roadmap

## 1.1 — Reliability and data safety

Persist/restorable tab sessions, Room migration tests, screenshot/page-save flows, per-site settings, expanded import validation, notification permission UX, download error recovery, and instrumented WebView tests.

## 1.2 — Browser platform

GeckoView flavor behind `engines/api`, runtime engine selection, profile-specific storage, content-blocking filter-list parser, enhanced reader extraction, and offline article storage.

## 2.0 — Native extension runtime

A separately maintained Chromium-based engine is required for Manifest V3 service workers, content scripts, extension storage, permissions, popups, updates, and Web Store package verification. GeckoView WebExtensionController can provide Gecko-side installation subject to Mozilla signing and API restrictions. Every API must have a compatibility test; static manifest analysis is not proof of runtime support.

## Later

Android Autofill/passkeys, optional encrypted sync, PWA shortcuts, advanced PDF annotation, parallel-part resumable downloader, automation rules with explicit grants, and local AI/translation models.

Security, data migration, memory/battery measurements, and truthful compatibility reporting take priority over feature count.
