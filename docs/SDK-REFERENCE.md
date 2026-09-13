# CXR SDK reference (authoritative)

Verified against the **official Rokid developer portal** (open.rokid.com, logged
in) on 2026-07-08, and cross-checked against the official on-glasses sample project
in [`../Rokid's CXR SDK samples, CXRSSDKSamples/`](../Rokid's CXR SDK samples, CXRSSDKSamples). Where the
community reverse-engineered docs disagreed, the portal wins — see
[the taxonomy note](#official-taxonomy-vs-community-naming).

## The three official SDKs (YodaOS-Sprite)

| SDK | Runs on | Public? | Version | Purpose |
|-----|---------|---------|---------|---------|
| **CXR-L** | Phone (Android/iOS) | ✅ yes | 1.0.4 (2026-06-29) | Extend the **Rokid AI App**; push content to HUD, photo/audio/commands, device control |
| **CXR-M** | Phone (Android) | ❌ email `Glasses.BD@rokid.com` | 1.1.0 (2026-04-01) | Deeper mobile toolkit: stable link, real-time A/V, scene customization; pairs with CXR-S |
| **CXR-S / "Bare-metal"** | Glasses (APK) | ✅ sample download | sample 1.0.9 | Apps that run **directly on the glasses**: HUD, buttons, IMU, camera |

Portal SDK page: <https://open.rokid.com/sdk?lang=en> (Development Tools → SDK).
There's a separate **YodaOS-Master** track on the same page for the tethered
AR Lite / AR Studio pucks — not covered here.

### Which one do I want?

- **Content on the HUD driven by phone logic (AI assistant, translator, cards)**
  → **CXR-L**. You write a phone app; it talks through the Rokid AI App to render
  `CustomView`s on the glasses and run photo/audio/commands. No on-glasses install.
- **A real app installed and running on the glasses (games, timers, sensor apps,
  camera apps)** → **CXR-S / bare-metal**. This is what [`apps/hello-hud`](../apps/hello-hud)
  targets and what the downloaded sample demonstrates.
- **Deep, low-level phone↔glasses (custom pairing, live A/V pipelines)** → **CXR-M**
  (contact Rokid BD).

---

## CXR-S / bare-metal (on-glasses) — the verified API

> Bare-metal dev is "largely the same as Android app development" (official docs).
> YodaOS-Sprite is based on **Android Go** — respect Go memory constraints.

### Gradle setup (from the official sample)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.rokid.com/repository/maven-public/") }
        google()
        mavenCentral()
    }
}
```

```kotlin
// app/build.gradle.kts
android {
    compileSdk = 36
    defaultConfig {
        minSdk = 31        // glasses are Android 12; sample uses 31
        targetSdk = 36
    }
}
dependencies {
    implementation("com.rokid.cxr:cxr-service-bridge:1.0-20250519.061355-45")
    // + CameraX 1.3.1 for photo/video capture
}
```

Toolchain the sample pins: Gradle **8.13**, AGP **8.13.1**, Kotlin **2.0.21**,
Jetpack Compose (BOM 2024.09.00), Java 11.

### Messaging: `CXRServiceBridge` + `Caps`  (package `com.rokid.cxr`)

The glasses app talks to a paired phone via `CXRServiceBridge`. Verbatim pattern
from the sample's `selfCMD` demo:

```kotlin
import com.rokid.cxr.CXRServiceBridge
import com.rokid.cxr.Caps

val bridge = CXRServiceBridge()

// 1. Connection state
bridge.setStatusListener(object : CXRServiceBridge.StatusListener {
    override fun onConnected(addr: String?, type: Int) {}
    override fun onDisconnected() {}
    override fun onARTCStatus(quality: Float, ok: Boolean) {}   // data-channel health
})

// 2. Subscribe to inbound messages on a channel key you define
bridge.subscribe("rk_custom_client", object : CXRServiceBridge.MsgCallback {
    override fun onReceive(name: String?, args: Caps?, bytes: ByteArray?) {
        // args is a Caps; iterate with caps.size() / caps.at(i)
    }
})

// 3. Send a structured message. Returns 0 = success, -1 = failure.
val cap = Caps().apply {
    write("key")        // string field
    writeInt32(42)      // int field
}
val rc = bridge.sendMessage("rk_custom_key", cap)
```

Notes:
- Channel `name`s are **contracts you agree on with the phone side** — the send key
  (`rk_custom_key`) and subscribe key (`rk_custom_client`) are app-defined strings.
- `sendMessage(name, caps)` returns `Int`: `0` success, `-1` failure.

### `Caps` serialization

Positional binary format. Write in order, read by index.

```kotlin
// write
val c = Caps().apply { write("hello"); writeInt32(7) }
// read
c.size()                    // field count
val v = c.at(0)             // Caps.Value
v.type()                    // Caps.Value.TYPE_STRING, TYPE_INT32, ...
v.string; v.int; v.long; v.float; v.double; v.`object`; v.binary
```

Value types seen: `TYPE_STRING, TYPE_INT32, TYPE_UINT32, TYPE_INT64, TYPE_UINT64,
TYPE_FLOAT, TYPE_DOUBLE, TYPE_OBJECT` (nested Caps), `TYPE_BINARY` (`.binary.data`,
`.binary.length`).

### Hardware buttons & touchpad — ordered broadcasts

Not in the community docs. YodaOS broadcasts input events as **ordered
broadcasts**; register a `BroadcastReceiver` with high priority and call
`abortBroadcast()` to consume an event (prevent system default). From the sample's
`keys` demo:

```kotlin
enum class KeyType(val action: String) {
    CLICK("com.android.action.ACTION_SPRITE_BUTTON_CLICK"),
    BUTTON_DOWN("com.android.action.ACTION_SPRITE_BUTTON_DOWN"),
    BUTTON_UP("com.android.action.ACTION_SPRITE_BUTTON_UP"),
    DOUBLE_CLICK("com.android.action.ACTION_SPRITE_BUTTON_DOUBLE_CLICK"), // = Back
    AI_START("com.android.action.ACTION_AI_START"),                      // touchpad long-press
    LONG_PRESS("com.android.action.ACTION_SPRITE_BUTTON_LONG_PRESS"),
    TWO_FINGER_SINGLE_TAP("com.android.action.ACTION_TWO_FINGER_SINGLE_TAP"),
    TWO_FINGER_DOUBLE_TAP("com.android.action.ACTION_TWO_FINGER_DOUBLE_TAP"),
    TWO_FINGER_SWIPE_FORWARD("com.android.action.ACTION_TWO_FINGER_SWIPE_FORWARD"),
    TWO_FINGER_SWIPE_BACK("com.android.action.ACTION_TWO_FINGER_SWIPE_BACK"),
    SETTINGS_KEY("com.android.action.ACTION_SETTINGS_KEY"),
}

registerReceiver(keyReceiver, IntentFilter().apply {
    KeyType.values().forEach { addAction(it.action) }
    priority = 100
})
// in onReceive: handle, then abortBroadcast() to swallow it
```

Fixed system gestures you **cannot** override (defined by YodaOS-Sprite):
long-press right-temple touchpad = enter Rokid AI app; double-tap button = Back;
tap top button = photo; long-press top button = record video; wake words trigger
features.

### Other capabilities in the sample

- **Camera / video**: CameraX 1.3.1 (`camera-core/camera2/lifecycle/video/view`).
- **Audio capture**: `RECORD_AUDIO`; sample defines an audio channel `0x6000FC`.
- **BLE GATT server** on the glasses: `BLUETOOTH_CONNECT` + `BLUETOOTH_ADVERTISE`
  (Android 12 runtime perms).

---

## CXR-L (phone) — official summary

Runs on the **phone**; works with Rokid Glasses **through the Rokid AI App**
(a.k.a. "Hi Rokid"). Handles auth, session, and pushing experiences to the HUD.

Typical flow (from the official CXR-L docs):
1. Integrate the SDK; guide the user to install/launch the Rokid AI App.
2. Obtain a **token** via authorization.
3. Establish a **`CustomView`** or **`CustomApp`** session; keep the link alive.
4. Complete **scene building** on the glasses (glasses reach working state).
5. Use **photo capture, audio, custom commands** once the scene is ready.
6. Use **device control** (brightness / volume) when linked.

Docs (login-gated, SPA): CXR-L → *Documentation* button on the SDK page.
Left-nav: Introduction · Quick Start · Development Flow & State-Machine · Terms ·
Feature Development · Version History.

## CXR-M (phone) — gated

"A mobile development toolkit for building Android apps that work with Rokid
Glasses — stable connections, data communication, real-time audio/video, and scene
customization; can be used with the on-device CXR-S SDK." **Not publicly
available**; request from **`Glasses.BD@rokid.com`**. Latest 1.1.0 (2026-04-01).

---

## Official taxonomy vs community naming

The reverse-engineered [buildwithfenna/rokid-docs](https://github.com/buildwithfenna/rokid-docs)
labeled the tiers differently. Trust the portal:

| Concept | Official portal | Community docs called it |
|---------|-----------------|--------------------------|
| Phone app via Rokid AI App | **CXR-L** (public) | "CXR-L standalone" (approx.) |
| Deeper phone toolkit | **CXR-M** (gated) | "CXR-M mobile companion" |
| On-glasses app | **CXR-S / bare-metal** | "CXR-S bridge" |

The class names the community found (`CXRServiceBridge`, `Caps`, the
`cxr-service-bridge` artifact) **are correct** — confirmed by the official sample.

## Source URLs

- SDK landing (login): <https://open.rokid.com/sdk?lang=en>
- On-glasses/bare-metal docs: `custom.rokid.com/.../pc/us/2f8aac88fc6747448a353e2327bc7c30.html`
- CXR-L docs: `custom.rokid.com/.../pc/us/663f26766e7348059905815bc022e1f7.html`
- CXR-S sample zip: <https://rokid-ota.oss-cn-hangzhou.aliyuncs.com/toB/Document/CXR/1.0.9/CXRSSDKSamples.zip>
- Design guidelines: <https://t.rokid.com/0w0opp8x> (redirects to a CN SPA)
- Maven: `https://maven.rokid.com/repository/maven-public/`
- Android Go constraints: <https://developer.android.com/guide/topics/androidgo>
