plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rokidyoda.havoiceglass"
    compileSdk = 34

    defaultConfig {
        // This applicationId is the CustomApp package the phone installs/starts —
        // it must match Config.GLASS_PACKAGE in the ha-voice phone app.
        applicationId = "com.rokidyoda.havoiceglass"
        minSdk = 31
        targetSdk = 32
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Rokid on-glasses bridge — talks to the phone (CXR-L) over the CXR link.
    // Verified coordinate from the official CXR-S sample (vendor-sdk/CXRSSDKSamples).
    implementation("com.rokid.cxr:cxr-service-bridge:1.0-20250519.061355-45")
}
