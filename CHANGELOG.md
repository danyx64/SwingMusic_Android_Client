# Changelog

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
