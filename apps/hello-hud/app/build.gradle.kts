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
        minSdk = 28          // CXR SDKs require >= 28; glasses are 32
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
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // --- Rokid glasses <-> phone bridge (uncomment once you have portal access) ---
    // implementation("com.rokid.cxr:cxr-service-bridge:1.0-SNAPSHOT")
    // See ../../docs/SDK-REFERENCE.md for the CXRServiceBridge API.
}
