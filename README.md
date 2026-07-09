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

Rokid's **CXR (Connected XR)** SDK suite defines three app models. Naming below is
the **official** portal naming (verified while logged in — it differs from the
community reverse-engineered docs; see [SDK-REFERENCE.md](docs/SDK-REFERENCE.md)):

1. **On-glasses app — CXR-S / "bare-metal"** — an APK that runs *on the glasses*.
   Draws the HUD; uses the camera/mic/IMU/buttons; can message a phone over the
   CXR bridge. → **Start here.** `apps/hello-hud` is this model, and Rokid's own
   sample is in [`vendor-sdk/CXRSSDKSamples`](vendor-sdk/CXRSSDKSamples). Simplest
   path to pixels on the glasses.

2. **Phone app — CXR-L** (public) — runs on the phone and works *through the Rokid
   AI App* to authenticate, open a `CustomView`/`CustomApp` session, push content
   to the HUD, and run photo/audio/custom commands + device control. Best for
   "AI assistant / translator / cards" ideas where the phone does the heavy lifting
   and no on-glasses install is needed.

3. **Phone app — CXR-M** (gated) — a deeper mobile toolkit (stable link, real-time
   A/V, scene customization) that pairs with CXR-S. Not a public download — request
   from `Glasses.BD@rokid.com`.

See [docs/SDK-REFERENCE.md](docs/SDK-REFERENCE.md) for the verified API,
Maven coordinates, and code samples.

### Project: voice-control Home Assistant

Two apps that reuse an existing homelab **orchestrator** (`:8100/run` with an
`ha_control` tool) to control Home Assistant, fully private (no public endpoint):
- [`apps/ha-voice`](apps/ha-voice) — **CXR-L phone app**: does auth, STT (on-device
  Vosk), the orchestrator call, and TTS.
- [`apps/havoice-glass`](apps/havoice-glass) — **CXR-S on-glasses app**: a two-finger
  touchpad tap starts listening; it renders the reply on the HUD.

Flow: two-finger tap → glasses mic → phone Vosk STT (auto-stops on silence) →
orchestrator → reply on the glasses HUD + read aloud. Design + data flow in
[docs/HA-VOICE.md](docs/HA-VOICE.md).

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
│   ├── RESEARCH.md           ← consolidated research + source links
│   ├── SDK-REFERENCE.md      ← verified CXR-S/L/M APIs, Maven coords, Caps, buttons
│   ├── DEVICE-SETUP.md       ← enable ADB, dev cable, install/launch, debugging
│   └── APP-IDEAS.md          ← project ideas + what the community has already built
├── apps/
│   ├── hello-hud/            ← buildable on-glasses starter app (Kotlin + Gradle)
│   ├── ha-voice/            ← PHONE app: voice-control Home Assistant via your orchestrator
│   └── havoice-glass/      ← on-GLASSES companion: two-finger-tap trigger + HUD for ha-voice
├── vendor-sdk/
│   ├── CXRSSDKSamples/       ← Rokid's OFFICIAL on-glasses (CXR-S) sample project
│   └── CXRLSample/          ← Rokid's OFFICIAL phone (CXR-L) sample project
└── scripts/
    └── deploy.sh             ← build + install + launch helper
```

> `vendor-sdk/` holds official Rokid materials pulled from the logged-in developer
> portal on 2026-07-08 — kept for local reference, not for redistribution.

---

## Important caveats (read before you sink time in)

- **The SDK + docs live behind a login** at [open.rokid.com](https://open.rokid.com)
  (Development Tools → SDK). The on-glasses **CXR-S sample is a public download**
  (already pulled into `vendor-sdk/`); **CXR-L** docs are login-gated; **CXR-M** is
  request-only via `Glasses.BD@rokid.com`. The API in
  [docs/SDK-REFERENCE.md](docs/SDK-REFERENCE.md) is verified against the official
  sample. The community [buildwithfenna/rokid-docs](https://github.com/buildwithfenna/rokid-docs)
  is still handy for YodaOS internals, but its SDK *naming* is off — trust the portal.
- **Maven coordinates and versions drift** with firmware. The verified coordinate
  (`com.rokid.cxr:cxr-service-bridge:1.0-20250519.061355-45`) is what Rokid's
  current sample uses; re-check the portal over time.
- **You need the physical dev cable.** The standard magnetic charging cable does
  not expose USB data / ADB.

---

## Sources

Full annotated source list is in [docs/RESEARCH.md](docs/RESEARCH.md).
