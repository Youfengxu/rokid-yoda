# Research notes — Rokid AI Glasses development

Consolidated from a web sweep on 2026-07-08. The official developer portal gates
SDKs behind registration, so the most concrete API detail comes from a
community reverse-engineering project. Verify specifics against the portal before
shipping.

## Platform facts

- **OS:** YodaOS-Sprite — an "intelligent glasses OS" built on Android 12 (API 32),
  deeply optimized across chip/driver/system/apps for all-day wearable battery life.
- **SoC:** Qualcomm Snapdragon AR1 / "Neo" (Kryo 300, `arm64-v8a`). GPU: Adreno.
- **Display:** JBD JBD4020 **Micro-LED, right eye only**, **480×640, ~240 dpi,
  green monochrome**. Design UI for this: black background, bright high-contrast
  text, big fonts, minimal detail.
- **Audio DSP:** NXP RT600 co-processor running iFlytek front-end + Rokid wake-word
  (KWS) engine (noise reduction, AEC).
- **IMU:** InvenSense ICM-4x6xx (accel/gyro, freefall/motion) over I3C.
- **Camera:** Camera2 HAL; ICP/EVA/VPU firmware for capture, face detect, video.
- **Build fingerprint (reference):**
  `Rokid/glasses/glasses:12/SKQ1.240613.001/1.12.009`
- **Reference device:** RV101 (domestic). Variants: RV102 (carrier),
  RV201/202 (Bolon AI Glasses), RV203 (display-less), overseas SKUs.

## Key system apps / packages (on YodaOS)

| Package | Role |
|---------|------|
| `com.rokid.cxrservice` | Glasses-side Bluetooth/data bridge (CXRService) |
| `com.rokid.os.sprite.launcher` | Home UI (camera, gallery, translation) |
| `com.rokid.os.sprite.assistserver` | Central hub: TTS, media, web server |
| `com.rokid.sprite.aiapp` | AI app service — bound by CXR-L standalone apps |
| `com.rokid.glass.ota` | OTA firmware updates |
| `com.rokid.os.master.screenstream` | Screen recording/streaming |

## The CXR SDK suite (Connected XR)

Three tiers of app model. All share the **`Caps`** binary serialization format
and communicate over BLE GATT + Bluetooth classic socket, with Wi-Fi Direct
(HTTP, port 8848) for high-bandwidth file sync.

```
Mobile Phone (Android 9+)          Rokid Glasses (YodaOS)
   ┌─────────────┐                    ┌──────────────┐
   │  CXR-M SDK  │◄── BLE / Wi-Fi ───►│  CXRService  │
   └─────────────┘     Direct         └──────┬───────┘
                                    ┌─────────┴─────────┐
                              ┌─────▼─────┐      ┌───────▼──────┐
                              │ CXR-S SDK │      │  CXR-L SDK   │
                              │ (bridge)  │      │ (standalone) │
                              └───────────┘      └──────────────┘
```

> ⚠️ **Naming correction (verified on the official portal 2026-07-08).** The
> reverse-engineered community docs mislabeled the tiers. The authoritative mapping:
>
> - **CXR-L** — *phone* app (Android/iOS, public v1.0.4). Works through the Rokid AI
>   App: auth token, `CustomView`/`CustomApp` sessions, push to HUD, photo/audio/
>   commands, device control.
> - **CXR-M** — *phone* app (gated, request from `Glasses.BD@rokid.com`, v1.1.0).
>   Deeper toolkit: stable link, real-time A/V, scene customization; pairs with CXR-S.
> - **CXR-S / "bare-metal"** — *on-glasses* APK. Runs directly on the glasses;
>   HUD, buttons, IMU, camera. Entry point `CXRServiceBridge` (+ `Caps`). Official
>   sample downloaded to [`../vendor-sdk/CXRSSDKSamples`](../vendor-sdk/CXRSSDKSamples).
>
> The class names the community found (`CXRServiceBridge`, `Caps`,
> `cxr-service-bridge`) are correct. Full verified API + Maven coords:
> [SDK-REFERENCE.md](SDK-REFERENCE.md).

## Developer portal / official resources

- **open.rokid.com** (redirected from ar.rokid.com/sdk) — the official AR platform.
  Registration + verification unlocks: AIUI Studio, developer forum, YodaOS SDK
  downloads. This is where you get *official, current* SDK artifacts and publish.
- **AIUI Studio** — an online IDE for building "AIUI Agents" (voice/AI skills)
  with a skills library and code assist. Relevant if your app is primarily a
  voice assistant / AI agent rather than a custom HUD app.

## Community resources (unofficial but very useful)

- **buildwithfenna/rokid-docs** — the single best reverse-engineered reference for
  the CXR SDKs, YodaOS internals, hardware, and decompiled system apps. MIT.
  <https://github.com/buildwithfenna/rokid-docs>
- **Anezium/awesome-rokid** — curated list of 40+ community apps, launchers,
  tools, and SDK wrappers. <https://github.com/Anezium/awesome-rokid>
- **cursive-team/rokid-apps** — clean example Android apps (Kotlin) incl. a
  minimal `HelloHUD`; documents the exact ADB build/install flow.
  <https://github.com/cursive-team/rokid-apps>
- **Rokid/glass-docs** — older official RokidGlass docs.
  <https://github.com/Rokid/glass-docs> · <https://rokid.github.io/glass-docs/>
- **GlassKit** — open-source dev suite for vision-enabled glasses apps.
- **RokidAIGlassesUnityBridge** — Unity Android plugin, if you want a game engine.

### What people have already built (for inspiration / prior art)

AI assistants (photo → vision model → HUD answer), real-time AR translation,
turn-by-turn navigation, teleprompter/lyrics, live streaming to YouTube/Twitch,
notification relay with voice reply, SSH terminal, retro games, language-learning
HUDs, a community app store (RokidBrew), and APK sideloaders. Full annotated list
in [APP-IDEAS.md](APP-IDEAS.md).

## Sources

- https://open.rokid.com/sprite?lang=en — official YodaOS-Sprite page (portal)
- https://ar.rokid.com/sdk?lang=en → redirects to https://open.rokid.com/
- https://github.com/buildwithfenna/rokid-docs — community CXR SDK + YodaOS docs
- https://github.com/Anezium/awesome-rokid — curated ecosystem list
- https://github.com/cursive-team/rokid-apps — example Kotlin apps + build flow
- https://github.com/Rokid/glass-docs — older official docs
- https://rokid.github.io/glass-docs/ — RokidGlass GitBook
- https://medium.com/@20x05zero/building-an-ai-powered-ar-assistant-for-rokid-glasses-camera-photo-analysis-and-voice-commands-b5788c79d51a — build walkthrough
