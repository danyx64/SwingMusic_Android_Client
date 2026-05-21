# SwingMusic Android

Android client for a self-hosted Swing Music server.

## Current features

- Login with custom host/IP, port, and HTTP/HTTPS toggle.
- JWT login against `POST /auth/login`.
- Folder browser using `POST /folder`.
- Track search using Swing Music search API.
- Albums list and album playback.
- Playlist list, playlist opening, playlist creation, add track to playlist, remove track from playlist.
- Foreground background playback service using Android `MediaPlayer`.
- Authenticated streaming with `Authorization: Bearer <token>`.
- Playback notifications with previous, play/pause, and next.
- Seekable playback bar with elapsed time and remaining time.
- Basic scrobble/play logging to `POST /logger/track/log`.
- Manual library scan trigger via `/notsettings/trigger-scan`.

## Build

This project expects the local Android SDK currently configured in `local.properties`.

```bash
tools/build_debug_apk.sh
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Test from Linux without an emulator

You cannot run the full native Android UI and background media service on plain Linux without an Android runtime. The practical no-emulator options are:

- Use a real Android phone with USB or wireless debugging:

```bash
tools/install_debug_apk.sh --build
```

- Test the Swing Music server, login, library APIs, playlists, albums, search, and stream URL from Linux:

```bash
SWING_BASE_URL=http://127.0.0.1:1970 \
SWING_USER=your-user \
SWING_PASS=your-password \
python3 tools/swing_api_smoke_test.py --query beatles
```

Add `--probe-stream` to fetch the first bytes of one authenticated track stream. Add `--insecure` only when testing a self-signed HTTPS certificate.

- Waydroid can run APKs in an Android container on Linux, so it avoids a classic emulator, but it is still an Android runtime and media/network behavior can differ from a real phone.

## Swing Music compatibility notes

- Recommended server version: `v2.1.4+`.
- Default server port: `1970`.
- Cleartext HTTP is enabled for LAN/self-hosted setups.
- Media streaming requires Bearer auth headers; the app sends them from the playback service.
