# Nebula Browser R8 rules.
#
# The engine contract and the shared core models are reached through generated
# Room code, reflection-free JSON parsing and the WebView bridge, so keep them
# intact. Compose, OkHttp, Room and coroutines all ship their own consumer rules.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.nebula.browser.core.** { *; }
-keep interface com.nebula.browser.core.** { *; }
-keep class com.nebula.browser.engine.** { *; }
-keep interface com.nebula.browser.engine.** { *; }

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep the download service and receiver referenced from the manifest.
-keep class com.nebula.browser.browser.DownloadProgressService { *; }
-keep class com.nebula.browser.browser.DownloadCompleteReceiver { *; }
