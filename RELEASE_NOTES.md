# SwingDroid 0.2.0

## Added

- Seekable playback bar in the mini player and full player.
- Elapsed time and remaining time in the full player.
- MediaSession seek support for Android/system playback controls.

## Improved

- Playback progress updates now avoid reloading cover art every tick.
- Full-player cover art is capped on short screens so the controls stay visible.
- Release builds now use minification and resource shrinking.
- Kotlin builds run in-process to avoid daemon issues with newer system JDKs.

## Tested

- Built debug and optimized release APKs successfully.
- Installed and launched version `0.2.0` on an ADB-connected device.
- Dragged the seek bar on-device and confirmed playback position/remaining time updated.
