package dev.swingmusic.android

import android.content.Context
import androidx.core.content.edit

data class Session(
    val baseUrl: String,
    val accessToken: String,
    val refreshToken: String,
    val maxAgeSeconds: Long,
    val savedAtMillis: Long
) {
    val isUsable: Boolean
        get() = baseUrl.isNotBlank() && accessToken.isNotBlank()
}

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("swingmusic.session", Context.MODE_PRIVATE)

    fun load(): Session? {
        val baseUrl = prefs.getString(KEY_BASE_URL, null) ?: return null
        val access = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        return Session(
            baseUrl = baseUrl,
            accessToken = access,
            refreshToken = refresh,
            maxAgeSeconds = prefs.getLong(KEY_MAX_AGE, 0L),
            savedAtMillis = prefs.getLong(KEY_SAVED_AT, 0L)
        )
    }

    fun save(session: Session) {
        prefs.edit {
            putString(KEY_BASE_URL, session.baseUrl)
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putLong(KEY_MAX_AGE, session.maxAgeSeconds)
            putLong(KEY_SAVED_AT, session.savedAtMillis)
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String, maxAgeSeconds: Long) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_MAX_AGE, maxAgeSeconds)
            putLong(KEY_SAVED_AT, System.currentTimeMillis())
        }
    }

    fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_MAX_AGE = "max_age"
        private const val KEY_SAVED_AT = "saved_at"

        fun buildBaseUrl(hostInput: String, portInput: String, useHttps: Boolean): String {
            val trimmed = hostInput.trim().trimEnd('/')
            val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(trimmed)
            val scheme = if (useHttps) "https" else "http"
            val withScheme = if (hasScheme) trimmed else "$scheme://$trimmed"
            val uriLike = withScheme.substringAfter("://")
            val hostAlreadyHasPort = uriLike.substringBefore('/').contains(":")
            val normalized = if (portInput.isBlank() || hostAlreadyHasPort) {
                withScheme
            } else {
                "$withScheme:${portInput.trim()}"
            }
            return normalized.trimEnd('/') + "/"
        }
    }
}
