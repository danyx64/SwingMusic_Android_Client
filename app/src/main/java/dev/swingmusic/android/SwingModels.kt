package dev.swingmusic.android

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

enum class RowKind {
    FOLDER,
    TRACK,
    PLAYLIST,
    ALBUM,
    SETTINGS,
    MESSAGE
}

enum class CoverKind {
    NONE,
    TRACK,
    ALBUM,
    PLAYLIST
}

data class ArtistRef(
    val name: String,
    val hash: String = ""
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("artisthash", hash)

    companion object {
        fun fromJson(json: JSONObject): ArtistRef {
            return ArtistRef(
                name = json.optString("name", "Unknown artist"),
                hash = json.optString("artisthash", "")
            )
        }
    }
}

data class Track(
    val title: String,
    val trackHash: String,
    val filepath: String,
    val folder: String,
    val album: String,
    val albumHash: String,
    val image: String,
    val duration: Int,
    val bitrate: Int,
    val isFavorite: Boolean,
    val artists: List<ArtistRef>
) {
    val artistText: String
        get() = artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" }

    val durationText: String
        get() {
            val minutes = duration / 60
            val seconds = duration % 60
            return String.format(Locale.US, "%d:%02d", minutes, seconds)
        }

    fun toJson(): JSONObject {
        return JSONObject()
            .put("title", title)
            .put("trackhash", trackHash)
            .put("filepath", filepath)
            .put("folder", folder)
            .put("album", album)
            .put("albumhash", albumHash)
            .put("image", image)
            .put("duration", duration)
            .put("bitrate", bitrate)
            .put("is_favorite", isFavorite)
            .put("artists", JSONArray().also { array ->
                artists.forEach { array.put(it.toJson()) }
            })
    }

    companion object {
        fun fromJson(json: JSONObject): Track {
            val artistList = parseArtists(json.optJSONArray("artists"))
                .ifEmpty { parseArtists(json.optJSONArray("albumartists")) }

            return Track(
                title = json.optString("title", "Untitled"),
                trackHash = json.optString("trackhash"),
                filepath = json.optString("filepath"),
                folder = json.optString("folder"),
                album = json.optString("album"),
                albumHash = json.optString("albumhash"),
                image = json.optString("image"),
                duration = json.optDouble("duration", 0.0).roundToInt(),
                bitrate = json.optInt("bitrate", 0),
                isFavorite = json.optBoolean("is_favorite", false),
                artists = artistList
            )
        }

        fun listToJson(tracks: List<Track>): String {
            val array = JSONArray()
            tracks.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(serialized: String?): List<Track> {
            if (serialized.isNullOrBlank()) return emptyList()
            val array = JSONArray(serialized)
            return buildList {
                for (index in 0 until array.length()) {
                    add(fromJson(array.getJSONObject(index)))
                }
            }
        }

        private fun parseArtists(array: JSONArray?): List<ArtistRef> {
            if (array == null) return emptyList()
            return buildList {
                for (index in 0 until array.length()) {
                    val value = array.opt(index)
                    when (value) {
                        is JSONObject -> add(ArtistRef.fromJson(value))
                        is String -> add(ArtistRef(value))
                    }
                }
            }
        }
    }
}

data class FolderItem(
    val name: String,
    val path: String,
    val trackCount: Int,
    val folderCount: Int
) {
    companion object {
        fun fromJson(json: JSONObject): FolderItem {
            return FolderItem(
                name = json.optString("name", json.optString("path", "Folder")),
                path = json.optString("path"),
                trackCount = if (json.has("trackcount")) json.optInt("trackcount") else json.optInt("count"),
                folderCount = json.optInt("foldercount", 0)
            )
        }
    }
}

data class PlaylistItem(
    val id: String,
    val name: String,
    val count: Int,
    val image: String = "",
    val coverKind: CoverKind = CoverKind.NONE
) {
    companion object {
        fun fromJson(json: JSONObject): PlaylistItem {
            val customImage = cleanImage(json.optString("thumb"))
                .ifBlank { cleanImage(json.optString("image")) }
            val previewImage = firstPlaylistPreview(json.optJSONArray("images"))
            val image = customImage.ifBlank { previewImage }
            return PlaylistItem(
                id = json.opt("id")?.toString().orEmpty(),
                name = json.optString("name", "Playlist"),
                count = json.optInt("count", json.optInt("trackcount", 0)),
                image = image,
                coverKind = when {
                    customImage.isNotBlank() -> CoverKind.PLAYLIST
                    previewImage.isNotBlank() -> CoverKind.TRACK
                    else -> CoverKind.NONE
                }
            )
        }

        private fun firstPlaylistPreview(array: JSONArray?): String {
            if (array == null || array.length() == 0) return ""
            for (index in 0 until array.length()) {
                val value = when (val item = array.opt(index)) {
                    is JSONObject -> cleanImage(item.optString("image"))
                        .ifBlank { cleanImage(item.optString("thumb")) }
                    is String -> cleanImage(item)
                    else -> ""
                }
                if (value.isNotBlank()) return value
            }
            return ""
        }

        private fun cleanImage(value: String): String {
            return value.trim()
                .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) || it.equals("none", ignoreCase = true) }
                .orEmpty()
        }
    }
}

data class UserProfile(
    val id: Int,
    val username: String,
    val image: String = "",
    val roles: List<String> = emptyList()
) {
    val initials: String
        get() = username
            .split(Regex("\\s+|[._-]+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "U" }

    val roleText: String
        get() = roles.joinToString(", ").ifBlank { "user" }

    companion object {
        fun fromJson(json: JSONObject): UserProfile {
            return UserProfile(
                id = json.optInt("id", 0),
                username = json.optString("username", "User"),
                image = userImage(json),
                roles = parseRoles(json.opt("roles"))
            )
        }

        private fun userImage(json: JSONObject): String {
            val extra = json.optJSONObject("extra")
            return cleanString(json.optString("image"))
                .ifBlank { cleanString(json.optString("avatar")) }
                .ifBlank { cleanString(json.optString("picture")) }
                .ifBlank { cleanString(json.optString("profile_image")) }
                .ifBlank { cleanString(json.optString("profileImage")) }
                .ifBlank { cleanString(extra?.optString("image").orEmpty()) }
                .ifBlank { cleanString(extra?.optString("avatar").orEmpty()) }
        }

        private fun cleanString(value: String): String {
            return value.trim()
                .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) || it.equals("none", ignoreCase = true) }
                .orEmpty()
        }

        private fun parseRoles(value: Any?): List<String> {
            return when (value) {
                is JSONArray -> buildList {
                    for (index in 0 until value.length()) {
                        value.optString(index).takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
                is String -> value.trim('[', ']', '"')
                    .split(',', ' ')
                    .map { it.trim().trim('"', '\'') }
                    .filter { it.isNotBlank() }
                else -> emptyList()
            }
        }
    }
}

data class AlbumItem(
    val hash: String,
    val title: String,
    val artistText: String,
    val helpText: String,
    val image: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): AlbumItem {
            val artistText = buildString {
                val array = json.optJSONArray("albumartists")
                if (array != null) {
                    for (index in 0 until array.length()) {
                        val artist = array.optJSONObject(index)
                        if (artist != null) {
                            if (isNotEmpty()) append(", ")
                            append(artist.optString("name"))
                        }
                    }
                }
            }.ifBlank { "Album" }

            return AlbumItem(
                hash = json.optString("albumhash"),
                title = json.optString("title", "Untitled album"),
                artistText = artistText,
                helpText = json.optString("help_text", json.optString("date", "")),
                image = json.optString("image")
            )
        }
    }
}

data class LibraryRow(
    val kind: RowKind,
    val title: String,
    val subtitle: String = "",
    val meta: String = "",
    val image: String = "",
    val coverKind: CoverKind = CoverKind.NONE,
    val playable: Boolean = false,
    val folder: FolderItem? = null,
    val track: Track? = null,
    val playlist: PlaylistItem? = null,
    val album: AlbumItem? = null
)
