# hello-hud

The minimal on-glasses app: draws one bright line of text on the Rokid HUD.
Proves your build → install → launch loop works before you add SDK complexity.

- **Pure Android/Kotlin** — no CXR dependency, so it builds without portal access.
- Fullscreen, black background, big green text (matches the 480×640 mono display).

## Build & run

```bash
# from repo root, with glasses connected via the dev cable + ADB enabled:
../../scripts/deploy.sh
# or manually:
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.rokidyoda.hellohud/.MainActivity
```

First build needs `local.properties` with `sdk.dir=...` (see
[../../docs/DEVICE-SETUP.md](../../docs/DEVICE-SETUP.md)).

## Where to go next

- Change the text: `app/src/main/res/values/strings.xml`.
- Receive text from a phone: add the CXR-S bridge dependency in
  `app/build.gradle.kts` and follow [../../docs/SDK-REFERENCE.md](../../docs/SDK-REFERENCE.md).
- Use the camera/IMU: standard Android Camera2 / SensorManager APIs.

## Note on the Gradle wrapper

If `gradlew` / `gradle/wrapper/gradle-wrapper.jar` is missing, generate it once
with a system Gradle:

```bash
gradle wrapper --gradle-version 8.9
```

or open the project in Android Studio, which will create it for you.
