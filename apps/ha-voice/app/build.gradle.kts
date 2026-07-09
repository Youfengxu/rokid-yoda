plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.rokidyoda.havoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rokidyoda.havoice"
        minSdk = 31          // matches Rokid's CXR-L sample; glasses/companion need Android 12
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Rokid CXR-L phone SDK — verified coordinate from the official v1.0.4 sample.
    implementation("com.rokid.cxr:client-l:1.0.4")

    // Offline on-device STT for the glasses-mic path (Config.MIC_SOURCE = GLASSES).
    // Feeds the CXR-L PCM stream to Vosk; nothing leaves the phone.
    implementation("com.alphacephei:vosk-android:0.3.47")

    // JSON is handled with org.json (bundled in Android) + HttpURLConnection —
    // no extra networking dependency needed.
}
