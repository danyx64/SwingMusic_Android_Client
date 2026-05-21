package dev.swingmusic.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.LruCache
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.core.app.NotificationCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject
import kotlin.random.Random

class PlaybackService : Service() {
    private var player: MediaPlayer? = null
    private var queue: List<Track> = emptyList()
    private var currentIndex: Int = -1
    private var baseUrl: String = ""
    private var accessToken: String = ""
    private var currentSource: String = "mobile"
    private var isPlaying: Boolean = false
    private var repeatMode: Int = REPEAT_OFF
    private var shuffleEnabled: Boolean = false
    private var accumulatedSeconds: Long = 0L
    private var lastStartedAtMillis: Long = 0L
    private var trackStartedAtEpoch: Long = 0L
    private lateinit var mediaSession: MediaSessionCompat
    private val mainHandler = Handler(Looper.getMainLooper())
    private val artExecutor = Executors.newSingleThreadExecutor()
    private val notificationArt = LruCache<String, Bitmap>(24)
    private val progressRunnable = object : Runnable {
        override fun run() {
            val track = queue.getOrNull(currentIndex)
            if (track != null) {
                broadcastState(track)
                updateMediaSession(track, isPlaying, notificationArt.get(track.image))
            }
            if (isPlaying) {
                mainHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        mediaSession = MediaSessionCompat(this, "SwingMusic").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    if (!isPlaying) toggle()
                }

                override fun onPause() {
                    if (isPlaying) toggle()
                }

                override fun onSkipToNext() {
                    next()
                }

                override fun onSkipToPrevious() {
                    previous()
                }

                override fun onStop() {
                    stopPlayback()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }
            })
            isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_QUEUE -> {
                queue = Track.listFromJson(intent.getStringExtra(EXTRA_QUEUE))
                currentIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                baseUrl = intent.getStringExtra(EXTRA_BASE_URL).orEmpty()
                accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN).orEmpty()
                currentSource = intent.getStringExtra(EXTRA_SOURCE).orEmpty().ifBlank { "mobile" }
                playAt(currentIndex)
            }

            ACTION_TOGGLE -> toggle()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_REPEAT_MODE -> cycleRepeatMode()
            ACTION_SHUFFLE -> toggleShuffle()
            ACTION_STOP -> stopPlayback()
            ACTION_SEEK -> seekTo(intent.getLongExtra(EXTRA_POSITION_MS, 0L))
            ACTION_REFRESH_STATE -> {
                val track = queue.getOrNull(currentIndex)
                if (track != null) broadcastState(track) else stopSelf()
            }
        }
        return START_STICKY
    }

    private fun playAt(index: Int) {
        if (queue.isEmpty() || index !in queue.indices || baseUrl.isBlank()) return
        stopProgressUpdates()
        finishCurrentLog()
        currentIndex = index
        val track = queue[index]

        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setWakeMode(this@PlaybackService, PowerManager.PARTIAL_WAKE_LOCK)
            setOnPreparedListener {
                it.start()
                this@PlaybackService.isPlaying = true
                accumulatedSeconds = 0L
                lastStartedAtMillis = System.currentTimeMillis()
                trackStartedAtEpoch = System.currentTimeMillis() / 1000L
                startForeground(NOTIFICATION_ID, buildNotification(track, true))
                requestNotificationArt(track)
                broadcastState(track)
                startProgressUpdates()
            }
            setOnCompletionListener {
                stopProgressUpdates()
                finishCurrentLog()
                handleCompletion(track)
            }
            setOnErrorListener { _, _, _ ->
                this@PlaybackService.isPlaying = false
                stopProgressUpdates()
                broadcastState(track)
                true
            }

            val uri = Uri.parse(streamUrl(track))
            val headers = mapOf("Authorization" to "Bearer $accessToken")
            setDataSource(this@PlaybackService, uri, headers)
            startForeground(NOTIFICATION_ID, buildNotification(track, false))
            requestNotificationArt(track)
            prepareAsync()
        }
        broadcastState(track)
    }

    private fun toggle() {
        val mediaPlayer = player ?: return
        val track = queue.getOrNull(currentIndex) ?: return
        if (mediaPlayer.isPlaying) {
            accumulatedSeconds += elapsedSinceLastStart()
            mediaPlayer.pause()
            isPlaying = false
            stopProgressUpdates()
        } else {
            lastStartedAtMillis = System.currentTimeMillis()
            mediaPlayer.start()
            isPlaying = true
            startProgressUpdates()
        }
        startForeground(NOTIFICATION_ID, buildNotification(track, isPlaying))
        requestNotificationArt(track)
        broadcastState(track)
    }

    private fun seekTo(positionMs: Long) {
        val mediaPlayer = player ?: return
        val track = queue.getOrNull(currentIndex) ?: return
        val durationMs = (track.duration.toLong() * 1000L).coerceAtLeast(0L)
        val target = if (durationMs > 0L) {
            positionMs.coerceIn(0L, durationMs)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        runCatching {
            mediaPlayer.seekTo(target, MediaPlayer.SEEK_CLOSEST)
        }.onFailure {
            runCatching { mediaPlayer.seekTo(target.toInt()) }
        }
        updateMediaSession(track, isPlaying, notificationArt.get(track.image))
        broadcastState(track)
    }

    private fun next() {
        when {
            queue.isEmpty() -> Unit
            shuffleEnabled && queue.size > 1 -> playAt(randomIndex())
            currentIndex < queue.lastIndex -> playAt(currentIndex + 1)
            repeatMode == REPEAT_ALL -> playAt(0)
        }
    }

    private fun previous() {
        when {
            queue.isEmpty() -> Unit
            shuffleEnabled && queue.size > 1 -> playAt(randomIndex())
            currentIndex > 0 -> playAt(currentIndex - 1)
            repeatMode == REPEAT_ALL -> playAt(queue.lastIndex)
        }
    }

    private fun handleCompletion(track: Track) {
        when {
            repeatMode == REPEAT_ONE -> playAt(currentIndex)
            shuffleEnabled && queue.size > 1 -> playAt(randomIndex())
            currentIndex < queue.lastIndex -> playAt(currentIndex + 1)
            repeatMode == REPEAT_ALL && queue.isNotEmpty() -> playAt(0)
            else -> {
                isPlaying = false
                stopProgressUpdates()
                broadcastState(track)
                stopForeground(STOP_FOREGROUND_DETACH)
            }
        }
    }

    private fun randomIndex(): Int {
        if (queue.size <= 1) return currentIndex.coerceAtLeast(0)
        var next = currentIndex
        while (next == currentIndex) {
            next = Random.nextInt(queue.size)
        }
        return next
    }

    private fun cycleRepeatMode() {
        repeatMode = when (repeatMode) {
            REPEAT_OFF -> REPEAT_ALL
            REPEAT_ALL -> REPEAT_ONE
            else -> REPEAT_OFF
        }
        queue.getOrNull(currentIndex)?.let {
            startForeground(NOTIFICATION_ID, buildNotification(it, isPlaying))
            requestNotificationArt(it)
            broadcastState(it)
        }
    }

    private fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
        queue.getOrNull(currentIndex)?.let {
            startForeground(NOTIFICATION_ID, buildNotification(it, isPlaying))
            requestNotificationArt(it)
            broadcastState(it)
        }
    }

    private fun stopPlayback() {
        finishCurrentLog()
        player?.release()
        player = null
        isPlaying = false
        stopProgressUpdates()
        queue.getOrNull(currentIndex)?.let { broadcastState(it) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun streamUrl(track: Track): String {
        return baseUrl.trimEnd('/') + "/file/${track.trackHash}/legacy?filepath=" +
            SwingMusicClient.encode(track.filepath)
    }

    private fun elapsedSinceLastStart(): Long {
        if (!isPlaying || lastStartedAtMillis == 0L) return 0L
        return ((System.currentTimeMillis() - lastStartedAtMillis) / 1000L).coerceAtLeast(0L)
    }

    private fun currentPositionMs(): Long {
        return runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)
    }

    private fun startProgressUpdates() {
        mainHandler.removeCallbacks(progressRunnable)
        if (isPlaying) {
            mainHandler.postDelayed(progressRunnable, PROGRESS_INTERVAL_MS)
        }
    }

    private fun stopProgressUpdates() {
        mainHandler.removeCallbacks(progressRunnable)
    }

    private fun finishCurrentLog() {
        val track = queue.getOrNull(currentIndex) ?: return
        val played = accumulatedSeconds + elapsedSinceLastStart()
        accumulatedSeconds = 0L
        lastStartedAtMillis = 0L
        if (played < 5L || baseUrl.isBlank() || accessToken.isBlank()) return

        Thread {
            runCatching {
                val payload = JSONObject()
                    .put("trackhash", track.trackHash)
                    .put("timestamp", if (trackStartedAtEpoch == 0L) System.currentTimeMillis() / 1000L else trackStartedAtEpoch)
                    .put("duration", played)
                    .put("source", currentSource)
                val connection = (URL(baseUrl + "logger/track/log").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $accessToken")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                connection.inputStream.close()
                connection.disconnect()
            }
        }.start()
    }

    private fun buildNotification(track: Track, playing: Boolean): Notification {
        val art = notificationArt.get(track.image)
        updateMediaSession(track, playing, art)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (playing) {
            NotificationCompat.Action(R.drawable.ic_pause, "Pause", serviceAction(ACTION_TOGGLE, 1))
        } else {
            NotificationCompat.Action(R.drawable.ic_play, "Play", serviceAction(ACTION_TOGGLE, 1))
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(track.title)
            .setContentText(track.artistText)
            .setSubText(track.album)
            .setLargeIcon(art)
            .setContentIntent(contentIntent)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(R.drawable.ic_skip_previous, "Previous", serviceAction(ACTION_PREVIOUS, 2))
            .addAction(playPauseAction)
            .addAction(R.drawable.ic_skip_next, "Next", serviceAction(ACTION_NEXT, 3))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateMediaSession(track: Track, playing: Boolean, art: Bitmap?) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artistText)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration.toLong() * 1000L)
            .apply {
                if (art != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                    putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, art)
                }
            }
            .build()
        mediaSession.setMetadata(metadata)

        val actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SEEK_TO
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val position = currentPositionMs()
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, position, if (playing) 1f else 0f)
                .build()
        )
        mediaSession.isActive = true
    }

    private fun requestNotificationArt(track: Track) {
        if (track.image.isBlank() || notificationArt.get(track.image) != null) return
        val imageKey = track.image
        val url = CoverArt.url(baseUrl, CoverKind.TRACK, imageKey, CoverSize.MEDIUM) ?: return
        val token = accessToken
        artExecutor.execute {
            val bitmap = runCatching { CoverArt.fetchBitmap(url, token) }.getOrNull() ?: return@execute
            notificationArt.put(imageKey, bitmap)
            if (queue.getOrNull(currentIndex)?.image == imageKey) {
                val trackNow = queue.getOrNull(currentIndex) ?: return@execute
                val notification = buildNotification(trackNow, isPlaying)
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun broadcastState(track: Track) {
        val intent = Intent(ACTION_STATE)
            .setPackage(packageName)
            .putExtra(EXTRA_TITLE, track.title)
            .putExtra(EXTRA_ARTIST, track.artistText)
            .putExtra(EXTRA_ALBUM, track.album)
            .putExtra(EXTRA_IMAGE, track.image)
            .putExtra(EXTRA_DURATION, track.duration)
            .putExtra(EXTRA_POSITION_MS, currentPositionMs())
            .putExtra(EXTRA_PLAYING, isPlaying)
            .putExtra(EXTRA_INDEX, currentIndex)
            .putExtra(EXTRA_QUEUE_SIZE, queue.size)
            .putExtra(EXTRA_REPEAT_MODE, repeatMode)
            .putExtra(EXTRA_SHUFFLE, shuffleEnabled)
        sendBroadcast(intent)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.playback_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        finishCurrentLog()
        stopProgressUpdates()
        player?.release()
        player = null
        mediaSession.release()
        artExecutor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY_QUEUE = "dev.swingmusic.android.PLAY_QUEUE"
        const val ACTION_TOGGLE = "dev.swingmusic.android.TOGGLE"
        const val ACTION_NEXT = "dev.swingmusic.android.NEXT"
        const val ACTION_PREVIOUS = "dev.swingmusic.android.PREVIOUS"
        const val ACTION_REPEAT_MODE = "dev.swingmusic.android.REPEAT_MODE"
        const val ACTION_SHUFFLE = "dev.swingmusic.android.SHUFFLE"
        const val ACTION_STOP = "dev.swingmusic.android.STOP"
        const val ACTION_SEEK = "dev.swingmusic.android.SEEK"
        const val ACTION_REFRESH_STATE = "dev.swingmusic.android.REFRESH_STATE"
        const val ACTION_STATE = "dev.swingmusic.android.STATE"

        const val EXTRA_QUEUE = "queue"
        const val EXTRA_INDEX = "index"
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ALBUM = "album"
        const val EXTRA_IMAGE = "image"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_POSITION_MS = "position_ms"
        const val EXTRA_PLAYING = "playing"
        const val EXTRA_QUEUE_SIZE = "queue_size"
        const val EXTRA_REPEAT_MODE = "repeat_mode"
        const val EXTRA_SHUFFLE = "shuffle"

        private const val CHANNEL_ID = "swingmusic.playback"
        private const val NOTIFICATION_ID = 41
        private const val PROGRESS_INTERVAL_MS = 1000L
        private const val REPEAT_OFF = 0
        private const val REPEAT_ALL = 1
        private const val REPEAT_ONE = 2

        fun playIntent(
            context: Context,
            queue: List<Track>,
            index: Int,
            session: Session,
            source: String
        ): Intent {
            return Intent(context, PlaybackService::class.java)
                .setAction(ACTION_PLAY_QUEUE)
                .putExtra(EXTRA_QUEUE, Track.listToJson(queue))
                .putExtra(EXTRA_INDEX, index)
                .putExtra(EXTRA_BASE_URL, session.baseUrl)
                .putExtra(EXTRA_ACCESS_TOKEN, session.accessToken)
                .putExtra(EXTRA_SOURCE, source)
        }
    }
}
