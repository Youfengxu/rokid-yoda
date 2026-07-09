#!/usr/bin/env bash
# Build the on-glasses app, bundle it into the phone app, build the phone app, and
# (if a phone is connected) install it. Verified toolchain: JDK 17 + Android cmdline-tools.
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="$ANDROID_HOME/platform-tools/adb"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo ">> Building glasses app (havoice-glass)"
( cd "$ROOT/apps/havoice-glass" && ./gradlew assembleDebug --no-daemon -q )

cp "$ROOT/apps/havoice-glass/app/build/outputs/apk/debug/app-debug.apk" \
   "$ROOT/apps/ha-voice/app/src/main/assets/glass.apk"
echo ">> Bundled glass.apk into the phone app"

echo ">> Building phone app (ha-voice)"
( cd "$ROOT/apps/ha-voice" && ./gradlew assembleDebug --no-daemon -q )

APK="$ROOT/apps/ha-voice/app/build/outputs/apk/debug/app-debug.apk"
echo ">> Phone APK: $APK"

if "$ADB" get-state >/dev/null 2>&1; then
  echo ">> Installing to the connected phone"
  "$ADB" install -r "$APK"
  echo ">> Installed. Open 'HA Voice (Rokid)' on your phone."
else
  echo ">> No phone detected. Enable USB debugging, plug it in, then run:"
  echo "     $ADB install -r \"$APK\""
fi
