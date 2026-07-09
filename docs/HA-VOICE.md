# Rokid → Home Assistant voice control

Control Home Assistant from the Rokid glasses by **reusing your existing
orchestrator** (`:8100/run` with the `ha_control` tool). Trigger: a **two‑finger tap
on the glasses touchpad**; capture **auto‑stops on silence**. Fully private — no
public endpoint, no self‑hosted STT.

## Architecture (two apps, CustomApp mode)

```
 ┌──────────────── Rokid Glasses (YodaOS) ─────────────────┐
 │  havoice-glass (CXR-S app, apps/havoice-glass)          │
 │   • renders HUD (black / green)                         │
 │   • two-finger tap → sendMessage("ptt_start") ──────────┼─┐
 │   • shows status/reply from phone  ◄────────────────────┼─┼─┐
 └─────────────────────────▲───────────────────────────────┘ │ │
              CXR link (BLE, via Rokid AI app)                │ │ custom commands
 ┌─────────────────────────┴───────────────────────────────┐ │ │
 │  ha-voice (CXR-L phone app, apps/ha-voice)               │ │ │
 │   1. Auth → token; CustomApp session; install+start glass│◄┘ │
 │   2. on "ptt_start": stream glasses mic → Vosk STT ───────────┘
 │   3. POST text ──────────────────────────────────────────┼──┐
 │   4. reply → sendReply() to glass HUD + phone TTS         │  │
 └───────────────────────────────────────────────────────────┘  │ (phone tailnet/LAN)
                                                                 ▼
        orchestrator :8100/run  (k11-services 192.168.100.21)
           │  {"task": "...", "history": [...]}
           ├── Tool RAG selects ha_control
           └── ha_control ──► Home Assistant :8123 ──► reply text
```

## Why CustomApp (not CustomView)

To trigger from a **temple/touchpad gesture**, the glasses must send an event to the
phone. Per the CXR‑L capability matrix (see [SDK-REFERENCE.md](SDK-REFERENCE.md)):

> **Custom commands (glasses → phone) work only in a CustomApp session, not CustomView.**

So we install a tiny app on the glasses (`havoice-glass`) that catches the gesture and
relays it. CustomApp + app‑opened also grants **audio, photo, custom command** — so the
same session streams the glasses mic to the phone. Trade‑off vs. CustomView: an APK is
installed on the glasses (done automatically by the phone via `appUploadAndInstall`).

## Gesture & endpointing

- **Trigger**: `ACTION_TWO_FINGER_SINGLE_TAP` on the glasses touchpad (interceptable).
  Reserved gestures we avoid: touchpad **long‑press** (Rokid AI app), top‑button
  tap/long‑press (photo/video), button **double‑tap** (Back).
- **Auto‑stop**: no release needed. The phone finalizes on Vosk silence endpointing or
  a `SILENCE_MS` (1.5 s) quiet timer after the last words; `NO_SPEECH_MS` (6 s) cancels
  if nothing is said; `MAX_UTTERANCE_MS` (12 s) is a hard cap.

## STT & TTS

- **STT**: glasses mic → CXR‑L PCM (**16 kHz mono 16‑bit**) → **on‑device Vosk**
  (`GlassMic.kt`). Offline, nothing leaves the phone. Needs a Vosk model in the phone
  app's assets (see app README). `Config.MIC_SOURCE = PHONE` falls back to the phone mic.
- **TTS**: the reply is read aloud via the phone's `TextToSpeech` (`Tts.kt`). CXR‑L has
  **no** "play on glasses" API, so this comes out of the glasses only when they're the
  phone's active **Bluetooth audio** output.

## Backend contract (already live, unchanged)

```sh
curl -s -X POST http://192.168.100.21:8100/run \
  -H 'Content-Type: application/json' \
  -d '{"task": "Answer concisely for a small display: turn on the study light"}'
```

- Host `k11-services`: LAN `192.168.100.21:8100`, or tailnet
  `k11-services.border-balance.ts.net:8100`. Not publicly exposed — keep it that way.
- Body `{"task": string, "history"?: [{role, content}]}`; returns `{"result": "..."}`.
- We prefix `"Answer concisely for a small display: "` so replies fit the 480×640 HUD.

## CXR APIs used

**Phone (CXR‑L, `client-l:1.0.4`)** — `GlassSession.kt`:

| Step | Call |
|------|------|
| Auth | `AuthorizationHelper.requestAuthorization(act, [MICROPHONE], REQ)` → `AuthResult.AuthSuccess.token` |
| Session | `CXRLink(ctx).configCXRSession(CxrDefs.CXRSession(CUSTOMAPP, GLASS_PACKAGE))`; `connect(token)` |
| Install/start glass app | `appIsInstalled` / `appUploadAndInstall(apk)` / `appStart(entry)` via `IGlassAppCbk` |
| Custom cmd (recv) | `setCXRCustomCmdCbk{ onCustomCmdResult(key, payload) }` → `Caps.fromBytes` |
| Custom cmd (send) | `sendCustomCmd(PHONE_TO_GLASS_KEY, Caps{write(cmd); write(text)})` |
| Glasses mic | `setCXRAudioCbk(cbk)`; `startAudioStream(1)` / `stopAudioStream()` |

**Glasses (CXR‑S, `cxr-service-bridge`)** — `havoice-glass/MainActivity.kt`:

| Step | Call |
|------|------|
| Bridge | `CXRServiceBridge()`; `setStatusListener(...)` |
| Recv from phone | `subscribe(PHONE_TO_GLASS_KEY, MsgCallback{ onReceive(name, args, bytes) })` |
| Send to phone | `sendMessage(GLASS_TO_PHONE_KEY, Caps{write("ptt_start")})` |
| Gesture | `RokidKeyReceiver` (ordered broadcast) → `TWO_FINGER_SINGLE_TAP` |

Protocol constants live in `havoice-glass/Protocol.kt` and are mirrored in the phone's
`Config.kt` (`GLASS_TO_PHONE_KEY="hv_glass"`, `PHONE_TO_GLASS_KEY="hv_phone"`, …).

## Build & install choreography

1. Build the glasses app → copy its APK to the phone app's assets as `glass.apk`
   (see `apps/ha-voice/app/src/main/assets/glass.apk.README.md`).
2. Add the Vosk model to `apps/ha-voice/.../assets/vosk-model-en/` (phone STT).
3. Install the **phone** app. On first connect it auto‑installs `havoice-glass` onto the
   glasses and starts it.

## Smoke test first (echo/ping, `Config.DEBUG_ECHO = true`)

Before wiring STT/orchestrator, verify the glasses↔phone custom-command channel. With
`DEBUG_ECHO = true` (default) the phone **echoes a pong** on tap instead of listening —
no Vosk model, mic, or orchestrator needed. Only the glass APK is required.

```bash
adb logcat -s HaVoice HaVoiceGlass   # watch both sides
```

1. Install the phone app, authorize, wait for **Glasses app running** ✅ (auto-install).
2. **Two-finger tap** the glasses touchpad → glass HUD shows `Tap #1 sent…` then
   **`pong #1`**. That single round-trip proves both directions:
   glasses→phone (`ptt_start`) and phone→glasses (`sendReply`).
3. Tap **Ping glasses HUD** on the phone → glass HUD shows `ping #n` (phone→glasses only).
4. Logcat shows `ptt_start received (#n)` (phone) and `from phone: cmd=reply …` (glasses).

If the pong never reaches the HUD, the **custom-command name routing** is the thing to
adjust (`GLASS_TO_PHONE_KEY` / `PHONE_TO_GLASS_KEY` in `Protocol.kt` + `Config.kt`) —
that's the one interop detail the samples left fuzzy. Once this works, set
`DEBUG_ECHO = false` for the real voice flow.

## Prerequisites

1. **Rokid AI app** (`com.rokid.sprite.aiapp`) / **Hi Rokid** paired with the glasses
   (issues the token + carries the link).
2. Phone reaches the orchestrator (home Wi‑Fi or tailnet).
3. Build both apps against `maven.rokid.com` (already in `settings.gradle.kts`).

## Not yet (easy follow‑ups)

- Wake‑phrase instead of tap (CXR‑L surfaces `onGlassAiInterrupt`, the glasses' own wake).
- Streaming/partial HUD updates as the reply generates.
- On‑glasses replies with richer layout, or a "cancel" gesture mid‑capture.

## Needs on‑device iteration (can't verify without hardware)

- The exact CXR‑L↔CXR‑S custom‑command name routing (mirrored from the samples).
- The `appUploadAndInstall` install flow / signing policy for the glass APK.
- Whether the glasses expose a Bluetooth **A2DP** route so TTS plays through them.
- Vosk accuracy on the glasses' far‑field mic; tune `SILENCE_MS` to taste.
