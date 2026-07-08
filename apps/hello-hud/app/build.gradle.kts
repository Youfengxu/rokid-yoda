plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rokidyoda.hellohud"
    // Glasses run Android 12 (API 32). Compile against 34, target 32.
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rokidyoda.hellohud"
        minSdk = 31          // matches Rokid's official CXR-S sample; glasses are Android 12
        targetSdk = 34
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // --- Rokid on-glasses (CXR-S) bridge: phone <-> glasses messaging ---
    // Verified coordinate from Rokid's official sample (needs the maven.rokid.com repo
    // in settings.gradle.kts). Uncomment to use CXRServiceBridge / Caps.
    // implementation("com.rokid.cxr:cxr-service-bridge:1.0-20250519.061355-45")
    // See ../../docs/SDK-REFERENCE.md and ../../vendor-sdk/CXRSSDKSamples for usage.
}
