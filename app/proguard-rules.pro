# Nebula Browser R8 rules.
#
# Release builds are minified, so a missing keep rule only shows up at runtime.
# The app, its shared models and the engine contract are kept intact; shrinking
# still removes unused libraries and resources.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

-keep class com.nebula.browser.** { *; }
-keep interface com.nebula.browser.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
