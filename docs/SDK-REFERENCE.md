# CXR SDK reference

API-level notes for the three Rokid CXR SDK tiers. Coordinates and versions are
from the community reverse-engineered docs and **drift with firmware** — confirm
against the official portal (open.rokid.com) once you have developer access.

## Maven

All artifacts publish to Rokid's Maven repo:

```kotlin
// settings.gradle.kts (dependencyResolutionManagement) or build.gradle repositories
maven { url = uri("https://maven.rokid.com/repository/maven-public/") }
```

| SDK | Coordinate | Min SDK | Notes |
|-----|-----------|---------|-------|
| CXR-M (mobile) | `com.rokid.cxr:client-m:1.0.8` | 28 (Android 9) | phone companion |
| CXR-S (on-glasses) | `com.rokid.cxr:cxr-service-bridge:1.0-SNAPSHOT` | 28 | bridge app |
| CXR-L (standalone) | `com.rokid.cxr:client-l:0.0.1` | 28 (target 28) | binds aiapp AIDL |

> `hello-hud` in this repo intentionally uses **no CXR dependency** — it's a pure
> Android HUD app so it builds even before you have portal/Maven access. Add the
> `cxr-service-bridge` dependency when you want phone↔glasses messaging.

---

## CXR-S — on-glasses bridge app

Entry point: **`CXRServiceBridge`**. Runs inside your APK on the glasses and
exchanges messages with the phone's CXR-M app.

### Sending messages

`sendMessage(String name, Caps args)` → returns `0` success, `-1` param error,
`-3` internal error. The `name` is a channel string that must be **agreed with the
mobile end**.

```kotlin
val bridge = CXRServiceBridge()

// structured message
val args = Caps()
args.write("send_message")
args.writeUInt32(5)
val rc = bridge.sendMessage("message_channel", args)

// message + binary payload (e.g. an image)
val data: ByteArray = /* ... */ byteArrayOf()
bridge.sendMessage("photo_channel", args, data, 0, data.size)
```

### Receiving messages / connection state

Register listeners (interfaces documented in the community reference):

- `ConnectionListener` — Android/iOS device connect/disconnect.
- `ARTCHealthListener` — Bluetooth data-channel health.
- `MessageListener` — inbound structured commands from the phone.

Pattern: mobile sends a `Caps` command → `CXRService` routes it → your
`MessageListener` fires → you process and reply via `bridge.sendMessage(...)`.

---

## CXR-M — mobile companion app

Entry point: **`CxrApi`** singleton (callback/listener architecture). Internally:

- `CxrController` — request routing / id management
- `BluetoothController` — BLE GATT + classic socket (SCO for audio)
- `WifiController` — Wi-Fi P2P discovery
- `FileController` — HTTP file sync (port 8848), APK upload

Capabilities: device discovery + pairing/reconnection, battery/network/device
status queries, file transfer, audio recording, photo capture, and bidirectional
`Caps` messaging with the glasses. Custom AI-workflow integration hooks let the
phone drive on-glasses AI flows.

Connection handshake (roughly): BLE GATT scan → filter by Rokid UUID → GATT
connect → BT pair → classic socket → MTU/handshake (`cxr-service.json`) → optional
Wi-Fi P2P for bulk file sync.

---

## CXR-L — standalone app

Extends **`ExternalAppClient`** (Android AIDL bound service); binds the
`com.rokid.sprite.aiapp` service (`IMediaStreamService`) to take over media
streaming and AI-app lifecycle — i.e. **replace** the stock launcher/AI app.

```kotlin
class CXRLink(context: Context) : ExternalAppClient(context)
```

Use when you want a fully custom experience rather than a companion to the stock
apps. More involved; needs the app to be provisioned as the AI app.

---

## `Caps` serialization format

Rokid's binary wire format, shared across all three SDKs. Supported types:

- Primitives: `boolean`, `int32`, `int64`, `float`, `double`
- `string` (UTF-8), `byte[]` blobs
- Nested `Caps` objects (recursive), lists/maps

Written positionally — reader and writer must agree on field order and the channel
`name`. Transported over BT classic socket (negotiated port), Wi-Fi Direct HTTP
(8848), and BT SCO for audio.

Common write/read calls seen in samples: `write(String)`, `writeUInt32(int)`,
`writeInt32/64`, `writeFloat/Double`, `writeBinary(byte[])`, with mirrored
`read*()` on the receiving side.

---

## Which model do I pick?

| You want to… | Use |
|--------------|-----|
| Put text/graphics on the HUD, use camera/mic/IMU on-device | **CXR-S** (on-glasses APK) — start with `apps/hello-hud` |
| Run heavy AI/LLM/vision on the phone, glasses as I/O | **CXR-M** (phone) + a thin CXR-S app |
| Build a voice/AI "agent" with skills | **AIUI Studio** on the portal |
| Replace the whole stock experience | **CXR-L** (standalone) |
