# SwingDroid

Android app for listening to your selfhoster server of SwingMusic.
## Requirements

- An Android 8.0 or newer phone.
- A Swing Music server reachable from the phone.
- Your server address, port, username, and password.

## Installation

Download the latest APK from the GitHub **Releases** page and open it on your phone. If Android asks for permission, allow installation from unknown sources for the app you are using to open the APK.

## First Login

1. Open SwingDroid.
2. Enter the IP address or hostname of your Swing Music server.
3. Enter the port, usually `1970`.
4. Enable HTTPS only if your server is configured for it.
5. Enter your username and password.
6. Tap **Connect**.

## Home

Home opens the folder configured in **Settings** and shows its contents directly. You can change it from **Settings** using **Home path**.

Useful examples:

- `$home` uses the Home folder configured by your Swing Music server.
- `/music` opens that server-side folder directly.
- Any valid subfolder path shows the folders and tracks inside it.

## Playback

- Tap a track to play it.
- Use the mini player at the bottom for play/pause and seeking.
- Tap the mini player to open the full-screen player.
- The full-screen player includes seeking, shuffle, repeat, previous, and next.
- Remaining track time is shown on the right side of the full-screen player.

## Sections

- **Home**: folders and tracks from your configured Home path.
- **Playlists**: server playlists, with an option to create a new playlist.
- **Search**: search tracks in your library.
- **Albums**: albums available on the server.
- **Settings**: theme, language, color, Home path, library rescan, and account controls.

You can switch sections by tapping the top tabs or by swiping horizontally.

## Settings

In **Settings** you can:

- change the app theme and accent color;
- choose the interface language;
- edit the Home path;
- trigger a library rescan;
- update your account name or password;
- log out.

## Notes

- Playback depends on your Swing Music server session. If the server cannot be reached, lists and tracks may keep loading or show an error.
- Credentials are stored on the device so the app can keep your session active.
- HTTP is supported for local networks; HTTPS is recommended if your server is exposed outside your LAN.
