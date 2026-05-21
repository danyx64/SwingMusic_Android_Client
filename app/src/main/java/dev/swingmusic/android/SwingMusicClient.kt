package dev.swingmusic.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class HttpError(val code: Int, message: String) : Exception(message)

data class FolderResponse(
    val path: String,
    val folders: List<FolderItem>,
    val tracks: List<Track>
)

class SwingMusicClient(private val sessionStore: SessionStore) {
    private var session: Session? = sessionStore.load()

    fun currentSession(): Session? = session

    fun setSession(value: Session) {
        session = value
        sessionStore.save(value)
    }

    suspend fun login(baseUrl: String, username: String, password: String): Session {
        return withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("username", username)
                .put("password", password)
            val response = performRequest(
                url = URL(baseUrl + "auth/login"),
                method = "POST",
                body = payload,
                bearer = null
            )
            ensureSuccess(response)
            parseAuthResponse(baseUrl, JSONObject(response.body)).also { setSession(it) }
        }
    }

    suspend fun getFolder(
        folder: String,
        start: Int = 0,
        limit: Int = 100,
        tracksOnly: Boolean = false
    ): FolderResponse {
        val payload = JSONObject()
            .put("folder", folder)
            .put("start", start)
            .put("limit", limit)
            .put("tracks_only", tracksOnly)
            .put("sorttracksby", "default")
            .put("tracksort_reverse", false)
            .put("sortfoldersby", "name")
            .put("foldersort_reverse", false)

        val json = JSONObject(request("folder", "POST", payload))
        return FolderResponse(
            path = json.optString("path", folder),
            folders = json.optJSONArray("folders").toObjects { FolderItem.fromJson(it) },
            tracks = json.optJSONArray("tracks").toObjects { Track.fromJson(it) }
        )
    }

    suspend fun getAllTracks(): List<Track> {
        val roots = runCatching { getRootDirs() }.getOrDefault(emptyList())
        val tracks = roots.flatMap { root ->
            val encodedPath = encode(root)
            val json = JSONObject(request("folder/tracks/all?path=$encodedPath"))
            json.optJSONArray("tracks").toObjects { Track.fromJson(it) }
        }
        return tracks
            .distinctBy { it.trackHash.ifBlank { it.filepath } }
            .ifEmpty { getFolder("\$home", tracksOnly = true).tracks }
    }

    suspend fun searchTracks(query: String): List<Track> {
        val encoded = encode(query)
        val json = JSONObject(request("search/?q=$encoded&itemtype=tracks&start=0&limit=80"))
        return json.optJSONArray("results").toObjects { Track.fromJson(it) }
    }

    suspend fun getPlaylists(): List<PlaylistItem> {
        val json = JSONObject(request("playlists"))
        return json.optJSONArray("data").toObjects { PlaylistItem.fromJson(it) }
    }

    suspend fun getPlaylistTracks(playlistId: String): List<Track> {
        val json = JSONObject(request("playlists/$playlistId?start=0&limit=-1"))
        return json.optJSONArray("tracks").toObjects { Track.fromJson(it) }
    }

    suspend fun createPlaylist(name: String): PlaylistItem {
        val json = JSONObject(request("playlists/new", "POST", JSONObject().put("name", name)))
        return PlaylistItem.fromJson(json.getJSONObject("playlist"))
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackHash: String) {
        val payload = JSONObject()
            .put("itemtype", "tracks")
            .put("itemhash", trackHash)
            .put("sortoptions", JSONObject())
        request("playlists/$playlistId/add", "POST", payload)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackHash: String, index: Int) {
        val item = JSONObject()
            .put("trackhash", trackHash)
            .put("index", index)
        val payload = JSONObject().put("tracks", JSONArray().put(item))
        request("playlists/$playlistId/remove-tracks", "POST", payload)
    }

    suspend fun getAlbums(): List<AlbumItem> {
        val json = JSONObject(request("getall/albums?limit=80&start=0&sortby=created_date&reverse=1"))
        return json.optJSONArray("items").toObjects { AlbumItem.fromJson(it) }
    }

    suspend fun getAlbumTracks(albumHash: String): List<Track> {
        val json = JSONObject(request("album", "POST", JSONObject().put("albumhash", albumHash).put("albumlimit", 7)))
        return json.optJSONArray("tracks").toObjects { Track.fromJson(it) }
    }

    suspend fun toggleFavorite(track: Track) {
        val endpoint = if (track.isFavorite) "favorites/remove" else "favorites/add"
        val payload = JSONObject()
            .put("hash", track.trackHash)
            .put("type", "track")
        request(endpoint, "POST", payload)
    }

    suspend fun triggerScan() {
        request("notsettings/trigger-scan")
    }

    suspend fun getHomePaths(): List<String> {
        return getRootDirs()
    }

    private suspend fun getRootDirs(): List<String> {
        val json = JSONObject(request("notsettings/get-root-dirs"))
        return json.optJSONArray("dirs").toStrings()
    }

    suspend fun getCurrentUser(): UserProfile {
        val tokenUser = UserProfile.fromJson(JSONObject(request("auth/user")))
        return runCatching { getFreshUserProfile(tokenUser) }.getOrDefault(tokenUser)
    }

    suspend fun updateProfile(username: String, password: String): UserProfile {
        val payload = JSONObject()
        if (username.isNotBlank()) payload.put("username", username)
        if (password.isNotBlank()) payload.put("password", password)
        return UserProfile.fromJson(JSONObject(request("auth/profile/update", "PUT", payload)))
    }

    private suspend fun getFreshUserProfile(tokenUser: UserProfile): UserProfile {
        val users = JSONObject(request("auth/users?simplified=false"))
            .optJSONArray("users")
            .toObjects { UserProfile.fromJson(it) }
        return users.firstOrNull { it.id != 0 && it.id == tokenUser.id }
            ?: users.firstOrNull { it.username == tokenUser.username }
            ?: tokenUser
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        allowRefresh: Boolean = true
    ): String {
        return withContext(Dispatchers.IO) {
            val current = session ?: throw IllegalStateException("Missing Swing Music session")
            val response = performRequest(
                url = URL(current.baseUrl + path.trimStart('/')),
                method = method,
                body = body,
                bearer = current.accessToken
            )

            if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED && allowRefresh) {
                refreshTokens()
                return@withContext request(path, method, body, allowRefresh = false)
            }

            ensureSuccess(response)
            response.body
        }
    }

    private fun refreshTokens() {
        val current = session ?: throw IllegalStateException("Missing Swing Music session")
        if (current.refreshToken.isBlank()) throw HttpError(401, "Session expired")

        val response = performRequest(
            url = URL(current.baseUrl + "auth/refresh"),
            method = "POST",
            body = null,
            bearer = current.refreshToken
        )
        ensureSuccess(response)
        val updated = parseAuthResponse(current.baseUrl, JSONObject(response.body))
        setSession(updated)
    }

    private fun parseAuthResponse(baseUrl: String, json: JSONObject): Session {
        return Session(
            baseUrl = baseUrl,
            accessToken = json.optString("accesstoken"),
            refreshToken = json.optString("refreshtoken"),
            maxAgeSeconds = json.optLong("maxage", 0L),
            savedAtMillis = System.currentTimeMillis()
        )
    }

    private fun ensureSuccess(response: RawResponse) {
        if (response.code !in 200..299) {
            val message = runCatching {
                JSONObject(response.body).optString("msg")
                    .ifBlank { JSONObject(response.body).optString("error") }
            }.getOrDefault("")
            throw HttpError(response.code, message.ifBlank { "HTTP ${response.code}" })
        }
    }

    private fun performRequest(
        url: URL,
        method: String,
        body: JSONObject?,
        bearer: String?
    ): RawResponse {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (!bearer.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearer")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        if (body != null) {
            connection.outputStream.use { stream ->
                stream.write(body.toString().toByteArray(Charsets.UTF_8))
            }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }.orEmpty()
        connection.disconnect()
        return RawResponse(code, text)
    }

    private data class RawResponse(val code: Int, val body: String)

    companion object {
        fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    }
}

private fun <T> JSONArray?.toObjects(mapper: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(mapper(it)) }
        }
    }
}

private fun JSONArray?.toStrings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}
