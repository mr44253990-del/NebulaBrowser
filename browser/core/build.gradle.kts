plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.nebula.browser.browser.core"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(libs.kotlinx.coroutines.android)
}
