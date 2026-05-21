# Changelog

## 0.2.4

- Added a local editable Home path setting.
- Changed the Home tab to open the saved folder path directly instead of showing all tracks.
- Reloaded Home after saving a new local path from Settings.
- Cleaned up cover clipping and reduced artwork corner rounding.
- Replaced the full-player shuffle icon with a clearer line-style version.
- Fixed library row spacing so track durations cannot collide with play buttons.
- Cleaned up Settings text fields when focused.
- Removed the Home shuffle shortcut so the section action always starts normal playback.
- Fixed library rows drawing over `/music`, the section play button, or the mini player while scrolling.
- Fixed swipe/tab transitions leaving content faded or shifted after fast gestures.
- Hid stale rows while a new tab is loading.
- Removed the prefilled local IP from the login screen for public releases.
- Rewrote the README as an end-user usage guide.

## 0.2.3

- Treated Settings as its own navigation state.
- Cleared the selected top tab while Settings is open.
- Highlighted the settings icon while Settings is active.
- Restored the previous section cleanly when closing Settings.

## 0.2.2

- Fixed top navigation chips being clipped when the selected tab scales.
- Added clipping-safe spacing to horizontal chip rows.
- Reduced selected-tab scale slightly to keep the UI crisp at screen edges.
- Dismissed search keyboard when moving to other tabs or settings.
- Hid stale library rows while the settings panel is open.

## 0.2.1

- Fixed seek bar thumb clipping in the mini player and full player.
- Added a little more vertical room to the mini player progress control.

## 0.2.0

- Added seekable playback bars in the mini player and full player.
- Added elapsed and remaining time labels for the current track.
- Added MediaSession seek support so system controls can jump within a song.
- Reduced repeated cover-art refreshes during playback progress updates.
- Enabled release minification and resource shrinking.

## 0.1.1

- Initial Android client with login, browsing, search, playlists, albums, playback notifications, and basic play logging.
