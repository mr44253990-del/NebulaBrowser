plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.nebula.browser.engine.chromium"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":engines:api"))
    implementation(project(":core:domain"))
    implementation(libs.androidx.core-ktx)
    implementation(libs.kotlinx.coroutines.android)
}
