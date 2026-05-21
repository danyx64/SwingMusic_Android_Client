# SwingDroid 0.2.3

## Fixed

- Settings now behaves like its own section instead of leaving the previous tab selected.
- Closing Settings restores the previous section cleanly.
- The settings icon is highlighted while Settings is active.

# SwingDroid 0.2.2

## Fixed

- Fixed the “Home” tab being slightly clipped on the left edge.
- Added safe spacing and disabled clipping around horizontal chip rows.
- Reduced the selected-tab scale a little so animated tabs stay inside their bounds.
- Closed the search keyboard automatically when moving to other sections or settings.
- Hid the previous library/search list while Settings is open.

# SwingDroid 0.2.1

## Fixed

- Fixed seek bar thumb clipping at the start of the track.
- Fixed the same clipping in both the full-screen player and the bottom mini player.
- Increased the mini player seek area so the thumb has enough vertical room.

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
