#!/usr/bin/env python3
import argparse
import getpass
import json
import os
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request


class SmokeError(Exception):
    pass


def normalize_base_url(args):
    if args.base_url:
        return args.base_url.strip().rstrip("/") + "/"

    host = (args.host or os.environ.get("SWING_HOST") or "127.0.0.1").strip().rstrip("/")
    port = (args.port or os.environ.get("SWING_PORT") or "1970").strip()
    use_https = args.https or os.environ.get("SWING_HTTPS") in {"1", "true", "TRUE", "yes"}
    scheme = "https" if use_https else "http"
    has_scheme = "://" in host
    with_scheme = host if has_scheme else f"{scheme}://{host}"
    host_part = with_scheme.split("://", 1)[1].split("/", 1)[0]
    if port and ":" not in host_part:
        with_scheme = f"{with_scheme}:{port}"
    return with_scheme.rstrip("/") + "/"


def error_message(body):
    try:
        parsed = json.loads(body)
        return parsed.get("msg") or parsed.get("error") or parsed.get("message") or body
    except json.JSONDecodeError:
        return body


def request_json(base_url, path, method="GET", body=None, token=None, context=None, timeout=15):
    url = urllib.parse.urljoin(base_url, path.lstrip("/"))
    headers = {
        "Accept": "application/json",
        "User-Agent": "SwingMusic Android local smoke test",
    }
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode("utf-8")
    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout, context=context) as response:
            raw = response.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        raise SmokeError(f"{method} {path}: HTTP {error.code} {error_message(raw)}") from error
    except urllib.error.URLError as error:
        raise SmokeError(f"{method} {path}: {error.reason}") from error

    if not raw:
        return {}
    try:
        return json.loads(raw)
    except json.JSONDecodeError as error:
        raise SmokeError(f"{method} {path}: risposta non JSON: {raw[:160]}") from error


def mask_token(value):
    if not value:
        return "(vuoto)"
    return value[:8] + "..." + value[-6:] if len(value) > 18 else value[:4] + "..."


def title_of_track(track):
    title = track.get("title") or "Untitled"
    artists = track.get("artists") or track.get("albumartists") or []
    names = []
    for artist in artists:
        if isinstance(artist, dict):
            name = artist.get("name")
        else:
            name = str(artist)
        if name:
            names.append(name)
    return f"{title} - {', '.join(names)}" if names else title


def stream_url_for(base_url, track):
    track_hash = track.get("trackhash") or ""
    filepath = track.get("filepath") or ""
    if not track_hash or not filepath:
        return ""
    query = urllib.parse.urlencode({"filepath": filepath})
    return urllib.parse.urljoin(
        base_url,
        f"file/{urllib.parse.quote(track_hash, safe='')}/legacy?{query}",
    )


def probe_stream(url, token, context):
    headers = {
        "Authorization": f"Bearer {token}",
        "Range": "bytes=0-2047",
        "User-Agent": "SwingMusic Android local smoke test",
    }
    request = urllib.request.Request(url, headers=headers, method="GET")
    try:
        with urllib.request.urlopen(request, timeout=15, context=context) as response:
            chunk = response.read(64)
            return response.status, len(chunk), response.headers.get("Content-Type", "")
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", errors="replace")
        raise SmokeError(f"stream probe: HTTP {error.code} {error_message(raw)}") from error
    except urllib.error.URLError as error:
        raise SmokeError(f"stream probe: {error.reason}") from error


def main():
    parser = argparse.ArgumentParser(
        description="Verifica da Linux login e API principali di un server Swing Music."
    )
    parser.add_argument("--base-url", default=os.environ.get("SWING_BASE_URL"))
    parser.add_argument("--host", default=None)
    parser.add_argument("--port", default=None)
    parser.add_argument("--https", action="store_true")
    parser.add_argument("--username", default=os.environ.get("SWING_USER"))
    parser.add_argument("--password", default=os.environ.get("SWING_PASS"))
    parser.add_argument("--query", default=os.environ.get("SWING_QUERY", "a"))
    parser.add_argument("--probe-stream", action="store_true")
    parser.add_argument("--insecure", action="store_true", help="accetta certificati HTTPS self-signed")
    args = parser.parse_args()

    username = args.username or input("Swing username: ").strip()
    password = args.password or getpass.getpass("Swing password: ")
    base_url = normalize_base_url(args)
    context = ssl._create_unverified_context() if args.insecure else None

    print(f"Server: {base_url}")
    auth = request_json(
        base_url,
        "auth/login",
        method="POST",
        body={"username": username, "password": password},
        context=context,
    )
    access_token = auth.get("accesstoken") or ""
    refresh_token = auth.get("refreshtoken") or ""
    if not access_token:
        raise SmokeError("login riuscito ma accesstoken mancante nella risposta")
    print(f"Login: OK, token {mask_token(access_token)}")
    print(f"Refresh token: {'presente' if refresh_token else 'assente'}")

    folder = request_json(
        base_url,
        "folder",
        method="POST",
        body={
            "folder": "$home",
            "start": 0,
            "limit": 10,
            "tracks_only": False,
            "sorttracksby": "default",
            "tracksort_reverse": False,
            "sortfoldersby": "name",
            "foldersort_reverse": False,
        },
        token=access_token,
        context=context,
    )
    folders = folder.get("folders") or []
    folder_tracks = folder.get("tracks") or []
    print(f"Folder home: OK, {len(folders)} cartelle, {len(folder_tracks)} tracce nel primo blocco")

    playlists = request_json(base_url, "playlists", token=access_token, context=context)
    playlist_items = playlists.get("data") or []
    print(f"Playlist: OK, {len(playlist_items)} trovate")

    albums = request_json(
        base_url,
        "getall/albums?limit=5&start=0&sortby=created_date&reverse=1",
        token=access_token,
        context=context,
    )
    album_items = albums.get("items") or []
    print(f"Albums: OK, {len(album_items)} letti")

    query = args.query.strip()
    search_tracks = []
    if query:
        search_path = "search/?" + urllib.parse.urlencode(
            {"q": query, "itemtype": "tracks", "start": 0, "limit": 5}
        )
        search = request_json(base_url, search_path, token=access_token, context=context)
        search_tracks = search.get("results") or []
        print(f"Search '{query}': OK, {len(search_tracks)} tracce")

    track = next((item for item in folder_tracks + search_tracks if isinstance(item, dict)), None)
    if track:
        print(f"Prima traccia testabile: {title_of_track(track)}")
        stream_url = stream_url_for(base_url, track)
        print(f"Stream URL: {stream_url}")
        if args.probe_stream:
            status, size, content_type = probe_stream(stream_url, access_token, context)
            print(f"Stream probe: HTTP {status}, {size} byte letti, content-type {content_type or 'n/d'}")
    else:
        print("Stream URL: nessuna traccia disponibile per costruire un test")

    print("Smoke test completato.")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nInterrotto.", file=sys.stderr)
        sys.exit(130)
    except SmokeError as error:
        print(f"Errore: {error}", file=sys.stderr)
        sys.exit(1)
