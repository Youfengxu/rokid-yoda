# Rokid → Home Assistant voice control

Control Home Assistant from the Rokid glasses by **reusing your existing
orchestrator** (`:8100/run` with the `ha_control` tool). Chosen approach:
**private phone bridge, push‑to‑talk** — the phone is mic + brain + network, the
glasses are the display.

## Architecture

```
 ┌──────────── Rokid Glasses (YodaOS) ────────────┐
 │  HUD CustomView (black bg / green text)         │◄─ reply rendered here
 └───────────────────▲─────────────────────────────┘
        BLE link      │ CXR-L CustomView (customViewUpdate)
 ┌────────────────────┴────────────────────────────┐
 │  Phone: HA-Voice app (CXR-L, this repo)          │
 │   1. Auth via Rokid AI app → token               │
 │   2. Open CustomView session on glasses          │
 │   3. Push-to-talk → phone SpeechRecognizer → text│
 │   4. POST text ──────────────────────────────────┼──┐
 │   6. Render reply on HUD ◄───────────────────────┼──┤ (over phone's tailnet/LAN)
 └──────────────────────────────────────────────────┘  │
                                                        ▼
        orchestrator :8100/run  (k11-services 192.168.100.21)
           │  {"task": "...", "history": [...]}
           ├── Tool RAG selects ha_control
           └── ha_control ──► Home Assistant :8123 ──► reply text
```

Nothing new is exposed publicly; no self‑hosted STT is added (the phone's OS STT
is used); the entire orchestrator + `ha_control` stack is reused unchanged.

## Why this shape

From the **CXR‑L capability matrix** (verified on the portal, see
[SDK-REFERENCE.md](SDK-REFERENCE.md)): a **CustomView** session (no app installed
on the glasses) already grants **audio, photo, brightness/volume, and a rendered
HUD view**. "Custom command" needs CustomApp, which we don't need — we drive
everything from the phone. So CustomView is the minimal, no‑install path.

Push‑to‑talk uses the **phone's** microphone + `android.speech.SpeechRecognizer`
(reliable, no server round trip for STT). Capturing the **glasses'** mic instead
is possible later via the CXR‑L PCM audio stream (`AudioUsageViewModel` in the
official sample) — deferred to keep v1 simple.

## Backend contract (already live)

```sh
curl -s -X POST http://192.168.100.21:8100/run \
  -H 'Content-Type: application/json' \
  -d '{"task": "Answer concisely for a small display: turn on the study light"}'
```

- Host: `k11-services` — LAN `192.168.100.21:8100`, or tailnet
  `k11-services.border-balance.ts.net:8100` when the phone is on Tailscale (works
  away from home). The orchestrator is **not** publicly exposed; keep it that way.
- Body: `{"task": string, "history"?: [{role, content}]}`. Stateless; multi‑turn is
  driven by the client sending recent turns (orchestrator trims to 8 pairs).
- We reuse the g2‑agent trick of prefixing `"Answer concisely for a small display: "`
  so replies fit the 480×640 HUD (same as your G2 glasses do via :8094).

## CXR‑L API this app uses (from `client-l:1.0.4`)

| Step | Call |
|------|------|
| Auth | `AuthorizationHelper.requestAuthorization(act, [MICROPHONE], REQ)` → `parseAuthorizationResult` → `AuthResult.AuthSuccess.token` |
| Create session | `CXRLink(ctx).configCXRSession(CxrDefs.CXRSession(CUSTOMVIEW))` |
| Callbacks | `setCXRLinkCbk(ICXRLinkCbk)` — ready when `onCXRLConnected` && `onGlassBtConnected` |
| Connect | `link.connect(token)` |
| Open HUD | `link.setCXRCustomViewCbk(cbk)`; `link.customViewOpen(layoutJson)` |
| Update HUD | `link.customViewUpdate([{ "action":"update","id":"textView","props":{"text": "..."} }])` |
| Close | `link.customViewClose()` |

HUD layout JSON (root LinearLayout `#FF000000`, TextView id `textView` `#00FF00`) is
built in `Hud.kt`. See the official sample at
[`../vendor-sdk/CXRLSample`](../vendor-sdk/CXRLSample) for the full API surface
(audio PCM, photo, device control, CustomApp).

## Prerequisites

1. **Rokid AI app** (`com.rokid.sprite.aiapp`) or **Hi Rokid**
   (`com.rokid.sprite.global.aiapp`) installed on the phone and **paired with the
   glasses** — this is what issues the auth token and carries the BLE link.
2. Phone can reach the orchestrator: same Wi‑Fi as `192.168.100.21`, **or** phone
   on your tailnet (recommended for away‑from‑home).
3. Build with the `maven.rokid.com` repo (already in `settings.gradle.kts`).

## Flow in the app

1. Launch → check Rokid AI app installed → `Authorize` → token.
2. `Connect` → CustomView session opens; HUD shows "Ready".
3. Hold the **Talk** button → speak ("turn on the study light") → release.
4. STT text → `POST /run` → reply → `customViewUpdate` shows it on the HUD.
5. Recent turns kept in memory and sent as `history` for follow‑ups
   ("…and the kitchen too").

## Not in v1 (easy follow‑ups)

- Glasses‑mic capture via CXR‑L PCM stream (hands nearer to natural voice).
- A wake phrase on‑device (Option B+) instead of push‑to‑talk.
- Streaming/partial HUD updates; TTS read‑back via glasses audio.
- CustomApp mode if you later want on‑glasses buttons to trigger capture.
