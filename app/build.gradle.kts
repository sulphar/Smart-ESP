plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.universalesp32home"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.universalesp32home"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }
}
