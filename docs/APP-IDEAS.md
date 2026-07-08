# App ideas & prior art

What the community has already shipped on Rokid Glasses (from
[awesome-rokid](https://github.com/Anezium/awesome-rokid)), plus angles worth
exploring. Use this to avoid reinventing, or to find a gap.

## Already built (prior art)

**AI assistants** — photo capture on glasses → phone runs a vision/LLM model →
answer streamed back to HUD. Examples: RokidGlassAI, NeuroGlasses,
rokid__visual_agent, RokidAIAssistant, HelloRokid-v2 (business-card scanner),
Clawsses / openclaw-rokid, Rokid Claude (drive Claude on a Mac from the glasses).

**Translation** — rokid-ar-translator, rokid-spain-trip (travel), real-time AR
subtitles.

**Navigation** — rokid-ar-navigation, Rokid-GMaps / Rokid-Maps (turn-by-turn),
M365-Rokid-HUD (e-scooter telemetry).

**Media / utility** — Rokid Lyrics (synced lyrics), Rokid Live Studio (stream cam
to YouTube/Twitch), Rokid Relay (notification relay + voice reply), OverlayRec
(screenshot/record), Rokid-Scribe (voice notes → transcript/PDF), hubu (Garmin
workout metrics), Rokid Shell / rokid-ssh-terminal (files + SSH/tmux).

**Fun** — Rokid-DragonBallScouter (face-lock scouter HUD), RokidGames (retro),
Memora (language learning).

**Infra / tooling** — RokidBrew (app store), Rokid-APKs / EUNG SOFT WebUSB
installer (sideloading), GlassKit (dev suite), RokidAIGlassesUnityBridge (Unity).

## Good starter projects (buildable in a weekend)

1. **Glanceable notification HUD** — CXR-M phone app forwards notifications; a
   CXR-S app renders them big on the HUD. High utility, exercises the full bridge.
2. **Timer / stopwatch / pomodoro HUD** — pure on-glasses, no phone. Trivial CXR-S
   or even no-CXR app; great for learning the display + input.
3. **Teleprompter** — phone sends text, glasses auto-scroll. Practical, simple.
4. **Camera → caption** — press to capture, send image over CXR to a vision model
   on the phone, show the caption. The canonical "AI glasses" demo.
5. **Compass / heading HUD** — read the IMU, draw a heading. Pure sensor practice.

## Design principles for this display

- One idea per screen. It's a glance, not a page.
- Black bg, bright mono foreground, big type (see DEVICE-SETUP.md).
- Prefer voice/phone input over on-glasses touch for anything beyond a tap.
- Do heavy compute on the phone (CXR-M), keep the glasses app thin (CXR-S).
