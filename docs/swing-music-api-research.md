# Swing Music API research

Date: 2026-05-18

## Sources checked

- Official site/docs: https://swingmx.com/
- Backend: https://github.com/swingmx/swingmusic
- Official Android client: https://github.com/swingmx/android
- Official web client: https://github.com/swingmx/webclient
- PyPI package page: https://pypi.org/project/swingmusic/

## Project facts

- Swing Music is a self-hosted music streaming server for local audio files.
- Default server URL is `http://<host>:1970`.
- The GitHub backend default branch currently reports `version.txt` as `2.1.4`; latest GitHub release seen: `v2.1.4` on 2026-01-18.
- PyPI currently shows `2.1.11` published on 2026-04-25, with project links back to Swing Music.
- Backend stack: Python, Flask/OpenAPI, `flask-jwt-extended`, `tinytag`, `ffmpeg-python`, `watchdog`.
- Official Android stack: Kotlin, Jetpack Compose, Hilt, Room, DataStore, Retrofit, Coil, Paging, Media3/ExoPlayer.

## Auth model

Swing Music uses JWT. Browser clients can use cookies; Android should use `Authorization: Bearer <access_token>` headers.

Important endpoints:

- `POST /auth/login`
  Body: `{"username":"...", "password":"..."}`
  Returns: `accesstoken`, `refreshtoken`, `maxage`, `msg`.

- `GET /auth/getpaircode`
  Requires logged-in web session. Returns `{"code":"xxxxxx"}`.

- `GET /auth/pair?code=<code>`
  Used by Android QR pairing. Returns the same token payload as login. Pair codes are single-use.

- `POST /auth/refresh`
  Header: `Authorization: Bearer <refresh_token>`.
  Returns fresh `accesstoken` and `refreshtoken`.

- `GET /auth/users`
  Public-ish login helper; may return visible users depending on settings.

- `GET /auth/user`
  Current logged-in user.

Una app Android robusta deve salvare `baseUrl`, `accessToken`, `refreshToken`, `maxAge`; refreshare prima della scadenza; aggiungere Bearer a tutte le API protette; e configurare anche ExoPlayer con Bearer per lo streaming.

## Core API map

All URLs are relative to base URL.

### Library browsing

- `POST /folder`
  Body fields: `folder`, `sorttracksby`, `tracksort_reverse`, `sortfoldersby`, `foldersort_reverse`, `start`, `limit`, `tracks_only`.
  Returns folders and tracks. Special roots include `$home`, `$playlist/<id>`, `$favorites`.

- `GET /folder/tracks/all?path=<folder>`
  Returns up to 300 tracks under a folder tree; used to build queues.

- `GET /getall/albums?limit=20&start=0&sortby=created_date&reverse=1`
- `GET /getall/artists?limit=20&start=0&sortby=created_date&reverse=1`
  Returns `{"items":[...], "total": n}`.

### Albums and artists

- `POST /album`
  Body: `{"albumhash":"...", "albumlimit":7}`
  Returns album `info`, `tracks`, `stats`, `extra`, `more_from`, `other_versions`, `copyright`.

- `GET /album/<albumhash>/tracks`
  Returns album tracks only.

- `GET /artist/<artisthash>?tracklimit=-1&albumlimit=7&all=true`
  Returns artist, tracks, albums, stats.

- `GET /artist/<artisthash>/tracks`
  Returns artist tracks.

- `GET /artist/<artisthash>/albums?albumlimit=7&all=false`
  Returns grouped albums, appearances, compilations, singles/EPs.

- `GET /artist/<artisthash>/similar?artistlimit=9`
  Returns similar artists.

### Search

- `GET /search/top?q=<query>&limit=5`
  Returns top result plus grouped tracks/albums/artists.

- `GET /search/?q=<query>&itemtype=tracks|albums|artists&start=0&limit=30`
  Returns `{"results":[...], "more": true|false}`.

### Streaming and images

- `GET /file/<trackhash>/legacy?filepath=<urlencoded filepath>`
  Streams original file. Protected by JWT. Android must pass `Authorization` through Media3 data source.

- `POST /file/silence`
  Body: `{"ending_file":"...", "starting_file":"..."}`
  Returns silence padding values in milliseconds.

- `GET /img/thumbnail/<imgpath>`
- `GET /img/thumbnail/<size>/<imgpath>`
  Sizes: `large`, `xsmall`, `small`, `medium`, `original`.

- `GET /img/artist/<imgpath>`
- `GET /img/artist/small/<imgpath>`
- `GET /img/artist/medium/<imgpath>`
- `GET /img/playlist/<imgpath>`

### Playlists, favorites, stats

- `GET /playlists`
- `POST /playlists/new` with `{"name":"..."}`
- `GET /playlists/<playlistid>?start=0&limit=50&no_tracks=false`
- `POST /playlists/<playlistid>/add`
  Body: `{"itemtype":"tracks|folder|album|artist", "itemhash":"...", "sortoptions":{...}}`
- `POST /playlists/<playlistid>/remove-tracks`
- `POST /playlists/save-item`

- `POST /favorites/add`
- `POST /favorites/remove`
  Body: `{"hash":"...", "type":"track|album|artist"}`
- `GET /favorites`
- `GET /favorites/tracks|albums|artists`
- `GET /favorites/check?hash=<hash>&type=<type>`

- `POST /logger/track/log`
  Body: `{"trackhash":"...", "timestamp": 1710000000, "duration": 42, "source": "al:<albumhash>"}`
  Server rejects durations under 5 seconds.

- `GET /logger/top-tracks|top-artists|top-albums?duration=week|month|year|alltime&limit=10&order_by=playcount|playduration`
- `GET /logger/stats`

### Settings and realtime

- `GET /notsettings/get-root-dirs`
- `POST /notsettings/add-root-dirs`
- `GET /notsettings/trigger-scan`
- `PUT /notsettings/update`

- `GET /events/stream`
  Server-Sent Events, used by web client for realtime events. Android can ignore initially or add later with OkHttp SSE.

## Android client architecture recommendation

- Network: Retrofit + OkHttp interceptor that injects Bearer token, refreshes on `401`, and normalizes base URLs.
- Storage: DataStore for tokens/settings, Room for queue/cache/recent state.
- Playback: Media3/ExoPlayer with `DefaultHttpDataSource.Factory().setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))`.
- Images: Coil with authenticated image loading if needed; current official web paths are under `/img`.
- Offline/cache phase later: Media3 cache plus database metadata snapshots.
- Initial features to build first: server URL login, QR pairing, folder browsing, albums/artists lists, search, playback queue, favorites, scrobble logging.

## Cautions

- Use `v2.1.4+` server minimum because `v2.1.4` fixed a directory-browser path traversal vulnerability.
- Do not rely on unauthenticated media URLs. The official Android client sets Bearer headers for ExoPlayer.
- The API is real but not a polished stable public standard like OpenSubsonic, so keep request/response DTOs tolerant to missing fields.
- Endpoint names like `/notsettings` and `/nothome` are intentional in current clients.
