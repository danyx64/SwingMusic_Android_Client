#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/swingmusic-gradle-home}"

cd "$ROOT_DIR"
./gradlew --no-daemon assembleDebug

echo
echo "APK: $ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
