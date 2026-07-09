# ha-voice

**Phone app** (CXR‑L) that lets your Rokid glasses control **Home Assistant** through
your existing homelab **orchestrator** (`:8100/run` → `ha_control`). You **two‑finger
tap the glasses touchpad** and speak; the answer shows on the glasses HUD and is read
aloud. Pairs with the on‑glasses app [`../havoice-glass`](../havoice-glass).

Full design + data flow: [../../docs/HA-VOICE.md](../../docs/HA-VOICE.md).

```
Two-finger tap (glasses) → glasses mic → phone Vosk STT → POST /run → ha_control → HA
                                              reply → glasses HUD + phone TTS
```

## What it does

- Authorizes via the **Rokid AI / Hi Rokid** app to get a session token.
- Opens a **CXR‑L CustomApp** session and **auto‑installs + starts** the on‑glasses
  companion (`havoice-glass`).
- On a **two‑finger touchpad tap** (relayed from the glasses), streams the **glasses
  mic** to the phone, transcribes on‑device (Vosk), and **auto‑stops on silence**.
- `POST`s the text to the orchestrator; sends the reply back to the glasses HUD and
  **reads it aloud**. Keeps recent turns for follow‑ups. An on‑screen "Talk" button is
  a fallback for the same flow.

## Mic source & TTS (`Config.kt`)

- `MIC_SOURCE = GLASSES` (default): captures the **glasses** mic via the CXR‑L PCM
  stream and transcribes it **on‑device with Vosk** (offline, private) — talk with
  the phone pocketed. Requires a Vosk model (below).
- `MIC_SOURCE = PHONE`: uses the phone mic + `SpeechRecognizer`. No model needed.
- `TTS_ENABLED = true`: reply is read aloud via the phone's `TextToSpeech`; you hear
  it in the glasses when they're the phone's active **Bluetooth audio** output.

### Vosk model (only for `MIC_SOURCE = GLASSES`)

Models are ~40 MB and gitignored. Download a small English model and unpack it into
`app/src/main/assets/vosk-model-en/` (see the `PLACEHOLDER.md` there):

```bash
cd app/src/main/assets
curl -LO https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
unzip vosk-model-small-en-us-0.15.zip
rsync -a vosk-model-small-en-us-0.15/ vosk-model-en/   # contents into vosk-model-en/
rm -rf vosk-model-small-en-us-0.15 vosk-model-small-en-us-0.15.zip
```

## Prerequisites

1. **Rokid AI app** (`com.rokid.sprite.aiapp`) or **Hi Rokid**
   (`com.rokid.sprite.global.aiapp`) installed on the phone and **paired with the
   glasses**.
2. Phone can reach the orchestrator — same Wi‑Fi as `192.168.100.21`, or on your
   tailnet. Set the endpoint in
   [`Config.kt`](app/src/main/java/com/rokidyoda/havoice/Config.kt)
   (`ORCHESTRATOR_URL`); the host is allow‑listed for cleartext in
   `res/xml/network_security_config.xml`.
3. Build against `maven.rokid.com` (already in `settings.gradle.kts`).

## Build & install

Order matters — the phone app bundles the glasses APK and the Vosk model:

```bash
# 1. Build the on-glasses companion and drop its APK into this app's assets
cd apps/havoice-glass && ./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk \
   ../ha-voice/app/src/main/assets/glass.apk
# 2. Add the Vosk model (see the assets/vosk-model-en/PLACEHOLDER.md)
# 3. Build + install the PHONE app (not the glasses)
cd ../ha-voice
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` must point at your Android SDK (`sdk.dir=…`). On first connect the
phone auto‑installs and starts `havoice-glass` on the glasses. See
`assets/glass.apk.README.md`.

## Files

| File | Role |
|------|------|
| `MainActivity.kt` | Compose control panel: auth → connect → tap‑to‑talk → speak reply |
| `GlassSession.kt` | CXR‑L CustomApp: install/start glass app, custom commands, audio |
| `SpeechInput.kt` | Phone `SpeechRecognizer` fallback (MIC_SOURCE = PHONE) |
| `GlassMic.kt` | Glasses‑mic PCM → offline Vosk STT + silence auto‑stop (GLASSES) |
| `Tts.kt` | Reads the reply aloud via phone `TextToSpeech` (→ glasses over BT) |
| `Orchestrator.kt` | `POST /run` client (task + history) |
| `Config.kt` | Orchestrator URL, glass package, protocol keys, silence timings, TTS |

## Verify the backend independently

```bash
curl -s -X POST http://192.168.100.21:8100/run \
  -H 'Content-Type: application/json' \
  -d '{"task":"Answer concisely for a small display: turn on the study light"}'
```

## Follow‑ups (see docs/HA-VOICE.md)

Glasses‑mic capture via CXR‑L PCM stream · on‑device wake phrase · TTS read‑back ·
CustomApp mode for on‑glasses button triggers.

> The CXR‑L API here is grounded in Rokid's official v1.0.4 sample at
> [`../../vendor-sdk/CXRLSample`](../../vendor-sdk/CXRLSample). No Gradle/JDK is
> installed in this scratch environment, so the app is written against the verified
> SDK API but not compiled here — build it in Android Studio or with the bundled
> `./gradlew`.
