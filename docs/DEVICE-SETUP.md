# Device setup — get code onto the glasses

Rokid Glasses are an Android 12 device, so the toolchain is standard Android +
ADB. The two non-obvious requirements: you must **enable ADB from the Rokid AI
phone app**, and you need the **5-pin developer cable** (the magnetic charging
cable carries no USB data).

## 1. Prerequisites (host machine)

- **JDK 17** (`brew install openjdk@17` on macOS)
- **Android SDK** — via Android Studio, or `sdkmanager`:
  - `platform-tools` (gives you `adb`)
  - `platforms;android-32` (target API 32)
  - `build-tools;34.0.0`
- Point `apps/hello-hud/local.properties` at your SDK:
  ```
  sdk.dir=/Users/youfeng/Library/Android/sdk
  ```
  (create the file if it doesn't exist; it's gitignored)

## 2. Enable developer mode on the glasses

1. Pair the glasses with the **Rokid AI** phone app (App Store / Play Store).
2. In the app, open the glasses' settings and **enable ADB / developer mode**.
   (Exact menu path varies by app version; look for "ADB", "Developer", or
   "Debugging".)
3. Accept any on-device "allow USB debugging" prompt once connected.

## 3. Connect the dev cable

- Use the **5-pin Rokid developer cable**, not the charging cable.
- Plug into your host, then:

```bash
adb devices
# List of devices attached
# <serial>   device        ← glasses are connected and authorized
```

If it shows `unauthorized`, re-check the on-glasses/app debugging prompt.
If nothing shows, the cable is likely the charging one, or ADB isn't enabled.

## 4. Build, install, launch

```bash
cd apps/hello-hud
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.rokidyoda.hellohud/.MainActivity
```

Or use the helper:

```bash
./scripts/deploy.sh          # builds hello-hud, installs, launches
```

## 5. Debugging on-device

```bash
adb logcat --pid=$(adb shell pidof -s com.rokidyoda.hellohud)   # app logs
adb shell dumpsys battery                                        # battery
adb shell getprop ro.build.fingerprint                          # firmware id
adb shell getprop ro.devicetypeid                               # variant UUID
adb exec-out screencap -p > hud.png                             # screenshot the HUD
```

## Display constraints — design accordingly

- **480×640 portrait, right eye only, ~240 dpi, green monochrome.**
- Use a **black background** and **bright, high-contrast** foreground.
- Large fonts (think ≥24sp for body, bigger for glanceable info).
- Keep it sparse — a few lines, big icons, no dense layouts or subtle color.
- No color information survives — don't encode meaning in hue.
- Keep the important content roughly centered / upper area; avoid edges.

## Common gotchas

- **"adb: no devices"** → wrong cable, or ADB not enabled in the Rokid AI app.
- **Install fails on minSdk** → glasses are Android 12 (API 31/32). Rokid's official
  sample uses `minSdk 31`; `hello-hud` matches. YodaOS-Sprite is **Android Go** —
  keep the app lean (memory-constrained).
- **App installs but nothing on HUD** → confirm the `am start` component name and
  that the activity theme is fullscreen/no-title (see `hello-hud`'s theme).
- **CXR Maven 401/404** → those artifacts may require portal access; `hello-hud`
  avoids them so you can build immediately.
