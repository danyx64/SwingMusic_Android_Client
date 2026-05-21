#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [[ "${1:-}" == "--build" || ! -f "$APK" ]]; then
  "$ROOT_DIR/tools/build_debug_apk.sh"
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "adb non trovato. Installa Android platform-tools o aggiungi adb al PATH." >&2
  exit 1
fi

if [[ ! -f "$APK" ]]; then
  echo "APK non trovato: $APK" >&2
  exit 1
fi

echo "Dispositivi collegati:"
adb devices
echo
adb install -r "$APK"
