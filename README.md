# rokid-yoda

Development workspace for building apps for **Rokid AI Glasses** (YodaOS-Sprite).

This repo collects the research, reference docs, and a working starter app to get
you from zero to "text on the glasses HUD" as fast as possible.

> Status: scaffolding + research complete. `apps/hello-hud` is a buildable
> on-glasses Android app template. See [docs/](docs/) for the full SDK landscape.

---

## TL;DR — what you're building on

Rokid Glasses run **YodaOS-Sprite**, an Android 12 (API 32) derivative on a
Qualcomm Snapdragon AR1 (`arm64-v8a`). Apps are **normal Android APKs** — you
build with Gradle/Kotlin and install over ADB. There is no separate "glasses OS
API" language; you use the Android SDK plus Rokid's **CXR SDK** for glasses↔phone
communication.

| Fact | Value |
|------|-------|
| OS | YodaOS-Sprite (Android 12, API level 32) |
| SoC | Qualcomm Snapdragon AR1 (Kryo, `arm64-v8a`) |
| Display | JBD Micro-LED, **right eye only**, 480×640, ~240 dpi, **green monochrome** |
| Reference device | Rokid Glasses (OEM id RV101) |
| App format | Android APK (Kotlin/Java) |
| Install path | ADB over a **5-pin dev cable** (not the charging cable) |
| Cross-device comms | Rokid **CXR SDK** (BLE + Wi-Fi Direct, `Caps` serialization) |

Because the display is a **single green monochrome eye at 480×640**, design for
high contrast (black background, bright text), large type, and minimal chrome.

---

## The three ways to build (pick one)

Rokid's **CXR (Connected XR)** SDK suite defines three app models:

1. **On-glasses app (CXR-S)** — an APK that runs *on the glasses*. Draws the HUD,
   uses the camera/mic/IMU, and talks to a phone companion over the CXR bridge.
   → **Start here.** `apps/hello-hud` is this model. Simplest path to pixels.

2. **Mobile companion app (CXR-M)** — an Android/iOS phone app that pairs with the
   glasses, queries status, pushes files, and drives AI workflows. Most community
   "AI assistant" apps are phone-side: the glasses capture a photo, the phone runs
   the vision/LLM call, the answer comes back to the HUD.

3. **Standalone app (CXR-L)** — replaces Rokid's built-in launcher/AI app entirely
   by binding the `com.rokid.sprite.aiapp` AIDL service. Advanced; for full custom
   experiences.

See [docs/SDK-REFERENCE.md](docs/SDK-REFERENCE.md) for API details and Maven
coordinates for all three.

---

## Quickstart (on-glasses HUD app)

Prereqs: macOS with Android Studio (or `sdkmanager`), JDK 17, `adb`, and the
**Rokid 5-pin developer cable**.

```bash
# 1. Enable ADB on the glasses via the Rokid AI phone app (see docs/DEVICE-SETUP.md)
# 2. Connect the glasses with the dev cable, then:
adb devices                      # confirm the glasses show up
cd apps/hello-hud
./gradlew assembleDebug          # first build downloads Gradle + deps
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.rokidyoda.hellohud/.MainActivity
# → "Hello, Rokid" should appear on the HUD (right eye)
```

Full step-by-step, including cable and driver notes, is in
[docs/DEVICE-SETUP.md](docs/DEVICE-SETUP.md).

---

## Repo layout

```
rokid-yoda/
├── README.md                 ← you are here
├── docs/
│   ├── RESEARCH.md           ← consolidated web research + source links
│   ├── SDK-REFERENCE.md      ← CXR-S / CXR-M / CXR-L APIs, Maven coords, Caps format
│   ├── DEVICE-SETUP.md       ← enable ADB, dev cable, install/launch, debugging
│   └── APP-IDEAS.md          ← project ideas + what the community has already built
├── apps/
│   └── hello-hud/            ← buildable on-glasses starter app (Kotlin + Gradle)
└── scripts/
    └── deploy.sh             ← build + install + launch helper
```

---

## Important caveats (read before you sink time in)

- **Not an official SDK download from a single button.** Rokid's developer portal
  (open.rokid.com / ar.rokid.com) gates the SDK + docs behind developer
  registration. Much of the concrete API knowledge below comes from the
  community-maintained, reverse-engineered
  [buildwithfenna/rokid-docs](https://github.com/buildwithfenna/rokid-docs).
  **Register on the portal** to get official, current SDK artifacts and to publish.
- **Maven coordinates and versions drift** with firmware. Treat the numbers in
  [docs/SDK-REFERENCE.md](docs/SDK-REFERENCE.md) as a starting point, verify
  against the portal.
- **You need the physical dev cable.** The standard magnetic charging cable does
  not expose USB data / ADB.

---

## Sources

Full annotated source list is in [docs/RESEARCH.md](docs/RESEARCH.md).
