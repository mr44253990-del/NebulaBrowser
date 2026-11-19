# Privacy notes

Nebula contains no analytics SDK, account system, advertising SDK, or Nebula-operated backend.

- **Browsing:** sites receive normal network requests. HTTPS-first only affects typed domains; links can still use HTTP. Android System WebView safe-browsing behavior is controlled by the platform provider.
- **History/bookmarks:** stored locally in Room. Private tab URLs are not inserted into history.
- **Private tabs:** DOM storage and Web SQL storage are disabled. WebView cache and history are cleared when destroyed. Cookies are process-wide in Android WebView and are not presented as strongly isolated; private mode is not a separate OS/browser profile.
- **Passwords and AI key:** encrypted using AES-GCM and an Android Keystore-generated AES key. The key is non-exportable. Device compromise, rooted devices, screenshots, and clipboard access remain outside the app's complete control.
- **AI:** disabled by default. Opening the assistant does not upload a page. After extraction, a dialog identifies the page and maximum text size. Confirming sends the title, URL, text, and prompt directly to the configured OpenAI-compatible HTTPS endpoint.
- **Downloads:** stored in `Download/Nebula` through Android DownloadManager. Other apps with appropriate user-granted storage access may read downloaded files.
- **Extensions:** imported packages are read locally for static manifest inspection. Package code is not executed.

Do not enter production secrets into a debug build on an untrusted device.
