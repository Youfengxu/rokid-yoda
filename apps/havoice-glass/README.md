# havoice-glass

The **on‑glasses companion** (CXR‑S) for [`../ha-voice`](../ha-voice). It runs *on the
glasses* in a CXR‑L **CustomApp** session and does two jobs:

1. **Trigger** — a **two‑finger touchpad tap** → sends `ptt_start` to the phone
   (the phone then streams the glasses mic, transcribes, and calls the orchestrator).
2. **Display** — renders status/reply text the phone pushes back, on the HUD
   (black bg / green text, sized for 480×640).

It does **not** do STT, networking, or talk to Home Assistant — all of that stays on
the phone. This app is intentionally tiny.

## How it's used

You don't install this by hand — the **phone** app (`ha-voice`) installs and starts it
over CXR‑L. But you must build its APK and hand it to the phone app:

```bash
cd apps/havoice-glass
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk \
   ../ha-voice/app/src/main/assets/glass.apk
```

The package **must** stay `com.rokidyoda.havoiceglass` (matches the phone's
`Config.GLASS_PACKAGE` / `GLASS_ENTRY`).

## Files

| File | Role |
|------|------|
| `MainActivity.kt` | HUD + CXR‑S `CXRServiceBridge`: subscribe (phone → HUD), send `ptt_start` |
| `RokidKeyReceiver.kt` | Ordered‑broadcast gesture handling; two‑finger tap = PTT |
| `Protocol.kt` | Custom‑command keys/events (mirrored in the phone's `Config.kt`) |

## Protocol

- Glasses → phone: `sendMessage("hv_glass", Caps{write("ptt_start")})`
- Phone → glasses: `subscribe("hv_phone")` → `Caps{ cmd, text }` where `cmd` ∈
  `status` | `reply`.

Full design: [../../docs/HA-VOICE.md](../../docs/HA-VOICE.md). SDK reference:
[../../docs/SDK-REFERENCE.md](../../docs/SDK-REFERENCE.md).

> Written against the verified CXR‑S API (`Rokid's CXR SDK samples, CXRSSDKSamples`) but not compiled
> here (no JDK in this environment). The Gradle wrapper jar may need generating with
> `gradle wrapper --gradle-version 8.13` or Android Studio.
