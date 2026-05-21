package dev.swingmusic.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

enum class CoverSize(val apiName: String) {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large")
}

object CoverArt {
    fun url(baseUrl: String, kind: CoverKind, image: String, size: CoverSize): String? {
        if (baseUrl.isBlank() || image.isBlank() || kind == CoverKind.NONE) return null
        val base = baseUrl.trimEnd('/')
        val cleaned = image.trim().trimStart('/')
        val parts = cleaned.split("?", limit = 2)
        val encodedPath = Uri.encode(parts[0], "/")
        val query = parts.getOrNull(1)?.let { "?$it" }.orEmpty()
        val encoded = encodedPath + query
        return when (kind) {
            CoverKind.TRACK,
            CoverKind.ALBUM -> "$base/img/thumbnail/${size.apiName}/$encoded"
            CoverKind.PLAYLIST -> "$base/img/playlist/$encoded"
            CoverKind.NONE -> null
        }
    }

    fun rawUrl(baseUrl: String, image: String): String? {
        if (baseUrl.isBlank() || image.isBlank()) return null
        val trimmed = image.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        val base = baseUrl.trimEnd('/')
        val cleaned = trimmed.trimStart('/')
        val parts = cleaned.split("?", limit = 2)
        val encodedPath = Uri.encode(parts[0], "/")
        val query = parts.getOrNull(1)?.let { "?$it" }.orEmpty()
        return "$base/$encodedPath$query"
    }

    fun userUrls(baseUrl: String, image: String): List<String> {
        if (baseUrl.isBlank() || image.isBlank()) return emptyList()
        val trimmed = image.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return listOf(trimmed)

        val base = baseUrl.trimEnd('/')
        val cleaned = trimmed.trimStart('/')
        val parts = cleaned.split("?", limit = 2)
        val encodedPath = Uri.encode(parts[0], "/")
        val query = parts.getOrNull(1)?.let { "?$it" }.orEmpty()
        val encoded = encodedPath + query
        return listOfNotNull(
            rawUrl(baseUrl, image),
            "$base/img/user/$encoded",
            "$base/img/users/$encoded",
            "$base/users/$encoded",
            "$base/static/$encoded"
        ).distinct()
    }

    fun fetchBitmap(url: String, token: String): Bitmap? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "image/*")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        return connection.inputStream.use { BitmapFactory.decodeStream(it) }.also {
            connection.disconnect()
        }
    }
}

class CoverArtLoader {
    private val executor = Executors.newFixedThreadPool(4)
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun load(
        imageView: ImageView,
        fallbackView: TextView?,
        session: Session?,
        kind: CoverKind,
        image: String,
        large: Boolean = false
    ) {
        val size = if (large) CoverSize.MEDIUM else CoverSize.MEDIUM
        val url = session?.let { CoverArt.url(it.baseUrl, kind, image, size) }
        if (url == null) {
            showFallback(imageView, fallbackView)
            return
        }

        imageView.tag = url
        cache.get(url)?.let {
            fallbackView?.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setBackgroundColor(Color.TRANSPARENT)
            imageView.setImageBitmap(it)
            return
        }

        val token = session.accessToken
        showFallback(imageView, fallbackView)
        executor.execute {
            val bitmap = runCatching { CoverArt.fetchBitmap(url, token) }.getOrNull()
            imageView.post {
                if (imageView.tag != url || bitmap == null) return@post
                cache.put(url, bitmap)
                fallbackView?.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                imageView.setBackgroundColor(Color.TRANSPARENT)
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    fun loadUrl(
        imageView: ImageView,
        fallbackView: TextView?,
        session: Session?,
        image: String
    ) {
        val url = session?.let { CoverArt.rawUrl(it.baseUrl, image) }
        if (url == null) {
            showFallback(imageView, fallbackView)
            return
        }

        loadCandidates(imageView, fallbackView, listOf(url), session.accessToken)
    }

    fun loadUserImage(
        imageView: ImageView,
        fallbackView: TextView?,
        session: Session?,
        image: String
    ) {
        val urls = session?.let { CoverArt.userUrls(it.baseUrl, image) }.orEmpty()
        if (session == null || urls.isEmpty()) {
            showFallback(imageView, fallbackView)
            return
        }
        loadCandidates(imageView, fallbackView, urls, session.accessToken)
    }

    private fun loadCandidates(
        imageView: ImageView,
        fallbackView: TextView?,
        urls: List<String>,
        token: String
    ) {
        val marker = urls.joinToString("|")
        imageView.tag = marker
        urls.firstNotNullOfOrNull { url -> cache.get(url)?.let { url to it } }?.let { (_, bitmap) ->
            fallbackView?.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setBackgroundColor(Color.TRANSPARENT)
            imageView.setImageBitmap(bitmap)
            return
        }

        showFallback(imageView, fallbackView)
        executor.execute {
            var loadedUrl = ""
            val bitmap = urls.firstNotNullOfOrNull { url ->
                runCatching { CoverArt.fetchBitmap(url, token) }.getOrNull()?.also { loadedUrl = url }
            }
            imageView.post {
                if (imageView.tag != marker || bitmap == null) return@post
                cache.put(loadedUrl, bitmap)
                fallbackView?.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                imageView.setBackgroundColor(Color.TRANSPARENT)
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun showFallback(imageView: ImageView, fallbackView: TextView?) {
        imageView.setImageDrawable(null)
        imageView.setBackgroundResource(R.drawable.cover_background)
        imageView.visibility = if (fallbackView == null) View.VISIBLE else View.GONE
        fallbackView?.visibility = View.VISIBLE
    }
}
