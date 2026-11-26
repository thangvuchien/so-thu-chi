plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "vn.sothuchi"
    compileSdk = 35
    defaultConfig {
        applicationId = "vn.sothuchi"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
