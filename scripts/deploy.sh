#!/usr/bin/env bash
# Build, install, and launch an on-glasses app over ADB.
# Usage: ./scripts/deploy.sh [app-dir]   (defaults to apps/hello-hud)
set -euo pipefail

APP_DIR="${1:-apps/hello-hud}"
PKG="com.rokidyoda.hellohud"
ACTIVITY=".MainActivity"

cd "$(dirname "$0")/.."

if ! adb get-state >/dev/null 2>&1; then
  echo "No ADB device. Connect the glasses with the 5-pin DEV cable and enable" >&2
  echo "ADB in the Rokid AI app (see docs/DEVICE-SETUP.md)." >&2
  exit 1
fi

echo ">> Building $APP_DIR"
( cd "$APP_DIR" && ./gradlew assembleDebug )

APK="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
echo ">> Installing $APK"
adb install -r "$APK"

echo ">> Launching $PKG/$ACTIVITY"
adb shell am start -n "$PKG/$ACTIVITY"

echo ">> Done. Tailing logs (Ctrl-C to stop):"
adb logcat --pid="$(adb shell pidof -s "$PKG")"
