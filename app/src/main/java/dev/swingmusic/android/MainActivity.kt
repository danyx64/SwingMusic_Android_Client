package dev.swingmusic.android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val REQUEST_NOTIFICATIONS = 41
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var appPreferences: AppPreferences
    private lateinit var sessionStore: SessionStore
    private lateinit var client: SwingMusicClient
    private lateinit var adapter: LibraryAdapter
    private val coverArtLoader = CoverArtLoader()

    private lateinit var rootPanel: View
    private lateinit var loginPanel: ScrollView
    private lateinit var appPanel: LinearLayout
    private lateinit var hostInput: TextInputEditText
    private lateinit var portInput: TextInputEditText
    private lateinit var usernameInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var httpsSwitch: MaterialSwitch
    private lateinit var loginButton: MaterialButton
    private lateinit var loginError: TextView
    private lateinit var pathText: TextView
    private lateinit var searchPanel: LinearLayout
    private lateinit var queryInput: TextInputEditText
    private lateinit var runSearchButton: MaterialButton
    private lateinit var newPlaylistButton: MaterialButton
    private lateinit var playCollectionButton: ImageButton
    private lateinit var progress: LinearProgressIndicator
    private lateinit var listActions: LinearLayout
    private lateinit var libraryList: RecyclerView
    private lateinit var nowPlayingTitle: TextView
    private lateinit var nowPlayingSubtitle: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var miniCoverImage: ImageView
    private lateinit var miniCoverFallback: TextView
    private lateinit var miniSeekBar: SeekBar
    private lateinit var playerBar: LinearLayout

    private lateinit var foldersButton: MaterialButton
    private lateinit var playlistsButton: MaterialButton
    private lateinit var searchButton: MaterialButton
    private lateinit var albumsButton: MaterialButton
    private lateinit var settingsButton: ImageButton
    private lateinit var settingsPanel: LinearLayout
    private lateinit var themeLightButton: MaterialButton
    private lateinit var themeDarkButton: MaterialButton
    private lateinit var themeOledButton: MaterialButton
    private lateinit var languageSystemButton: MaterialButton
    private lateinit var languageItalianButton: MaterialButton
    private lateinit var languageEnglishButton: MaterialButton
    private lateinit var languageSpanishButton: MaterialButton
    private lateinit var languageFrenchButton: MaterialButton
    private lateinit var languageGermanButton: MaterialButton
    private lateinit var languagePortugueseButton: MaterialButton
    private lateinit var accentDefaultButton: MaterialButton
    private lateinit var accentSkyButton: MaterialButton
    private lateinit var accentMintButton: MaterialButton
    private lateinit var accentPinkButton: MaterialButton
    private lateinit var accentGoldButton: MaterialButton
    private lateinit var accentVioletButton: MaterialButton
    private lateinit var customColorButton: MaterialButton
    private lateinit var homePathInput: TextInputEditText
    private lateinit var saveHomePathButton: MaterialButton
    private lateinit var rescanButton: MaterialButton
    private lateinit var accountNameInput: TextInputEditText
    private lateinit var accountPasswordInput: TextInputEditText
    private lateinit var saveAccountButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    private lateinit var playerSheet: LinearLayout
    private lateinit var closePlayerButton: ImageButton
    private lateinit var sheetCoverImage: ImageView
    private lateinit var sheetCoverFallback: TextView
    private lateinit var sheetTitle: TextView
    private lateinit var sheetArtist: TextView
    private lateinit var sheetAlbum: TextView
    private lateinit var sheetMeta: TextView
    private lateinit var sheetSeekBar: SeekBar
    private lateinit var sheetElapsedText: TextView
    private lateinit var sheetRemainingText: TextView
    private lateinit var sheetPlayPauseButton: ImageButton
    private lateinit var shuffleButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton

    private var currentFolder = "\$home"
    private var selectedPlaylistId: String? = null
    private var selectedAlbum: AlbumItem? = null
    private var currentQueue: List<Track> = emptyList()
    private var currentSource = "mobile"
    private var currentTrackImage: String = ""
    private var currentTrackTitle: String = ""
    private var currentDurationSeconds: Int = 0
    private var currentPositionMs: Long = 0L
    private var currentIsPlaying: Boolean = false
    private var userIsSeeking: Boolean = false
    private var progressTickerJob: Job? = null
    private var repeatMode: Int = 0
    private var shuffleEnabled: Boolean = false
    private var activeTabButton: MaterialButton? = null
    private var sectionBeforeSettings: MaterialButton? = null
    private var homePathChangedInSettings = false
    private var currentPathLabel: String = "Home"
    private var currentUser: UserProfile? = null
    private var pendingSectionEnterDirection: Int? = null
    private var sectionAnimationToken = 0
    private var contentLoadGeneration = 0
    private var activeLoadingRequests = 0
    private var sectionIsTransitioning = false
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swipeStartTime = 0L
    private var trackingSectionSwipe = false
    private var draggingSectionSwipe = false
    private var sectionChildTouchCanceled = false
    private var suppressRowClicksUntil = 0L
    private var lastSearchQuery = ""
    private var lastSearchResults: List<Track> = emptyList()
    private var hasSearchRun = false
    private var searchRequestVersion = 0
    private var searchDebounceJob: Job? = null
    private val folderHistory = mutableListOf<String>()
    private val sectionEase by lazy { PathInterpolator(0.18f, 0f, 0f, 1f) }
    private val sectionSettleEase = DecelerateInterpolator(1.8f)
    private val sectionExitEase = AccelerateInterpolator(1.15f)

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != PlaybackService.ACTION_STATE) return
            val previousImage = currentTrackImage
            currentTrackTitle = intent.getStringExtra(PlaybackService.EXTRA_TITLE) ?: "Nothing playing"
            val artist = intent.getStringExtra(PlaybackService.EXTRA_ARTIST).orEmpty()
            val album = intent.getStringExtra(PlaybackService.EXTRA_ALBUM).orEmpty()
            currentTrackImage = intent.getStringExtra(PlaybackService.EXTRA_IMAGE).orEmpty()
            val duration = intent.getIntExtra(PlaybackService.EXTRA_DURATION, 0)
            val positionMs = intent.getLongExtra(PlaybackService.EXTRA_POSITION_MS, 0L)
            val index = intent.getIntExtra(PlaybackService.EXTRA_INDEX, -1)
            val queueSize = intent.getIntExtra(PlaybackService.EXTRA_QUEUE_SIZE, 0)
            repeatMode = intent.getIntExtra(PlaybackService.EXTRA_REPEAT_MODE, repeatMode)
            shuffleEnabled = intent.getBooleanExtra(PlaybackService.EXTRA_SHUFFLE, shuffleEnabled)
            val playing = intent.getBooleanExtra(PlaybackService.EXTRA_PLAYING, false)
            nowPlayingTitle.text = currentTrackTitle
            nowPlayingSubtitle.text = artist
            sheetTitle.text = currentTrackTitle
            sheetArtist.text = artist
            sheetAlbum.text = album.ifBlank { "Unknown album" }
            sheetMeta.text = buildPlayerMeta(duration, index, queueSize)
            val icon = if (playing) R.drawable.ic_pause else R.drawable.ic_play
            playPauseButton.setImageResource(icon)
            sheetPlayPauseButton.setImageResource(icon)
            updateProgressViews(duration, positionMs, playing)
            updateModeButtons()
            if (currentTrackImage != previousImage) {
                loadCurrentCover()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return if (trackSectionSwipe(event)) {
            true
        } else {
            super.dispatchTouchEvent(event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)
        AppCompatDelegate.setDefaultNightMode(AppPreferences.nightModeFor(appPreferences.themeChoice()))
        setContentView(R.layout.activity_main)

        sessionStore = SessionStore(this)
        client = SwingMusicClient(sessionStore)
        bindViews()
        setupList()
        setupSeekBars()
        setupActions()
        setupBackNavigation()
        registerPlaybackReceiver()
        applyThemeChrome()
        requestNotificationPermission()

        val session = sessionStore.load()
        if (session?.isUsable == true) {
            showApp(session)
            loadHomeTracks()
        } else {
            showLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::client.isInitialized && ::accountNameInput.isInitialized && client.currentSession()?.isUsable == true) {
            loadUserProfile()
            sendPlaybackAction(PlaybackService.ACTION_REFRESH_STATE)
        }
    }

    private fun bindViews() {
        rootPanel = findViewById(R.id.rootPanel)
        loginPanel = findViewById(R.id.loginPanel)
        appPanel = findViewById(R.id.appPanel)
        hostInput = findViewById(R.id.hostInput)
        portInput = findViewById(R.id.portInput)
        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        httpsSwitch = findViewById(R.id.httpsSwitch)
        loginButton = findViewById(R.id.loginButton)
        loginError = findViewById(R.id.loginError)
        pathText = findViewById(R.id.pathText)
        searchPanel = findViewById(R.id.searchPanel)
        queryInput = findViewById(R.id.queryInput)
        runSearchButton = findViewById(R.id.runSearchButton)
        newPlaylistButton = findViewById(R.id.newPlaylistButton)
        playCollectionButton = findViewById(R.id.playCollectionButton)
        progress = findViewById(R.id.progress)
        listActions = findViewById(R.id.listActions)
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle)
        nowPlayingSubtitle = findViewById(R.id.nowPlayingSubtitle)
        playPauseButton = findViewById(R.id.playPauseButton)
        miniCoverImage = findViewById(R.id.miniCoverImage)
        miniCoverFallback = findViewById(R.id.miniCoverFallback)
        miniSeekBar = findViewById(R.id.miniSeekBar)
        playerBar = findViewById(R.id.playerBar)

        foldersButton = findViewById(R.id.foldersButton)
        playlistsButton = findViewById(R.id.playlistsButton)
        searchButton = findViewById(R.id.searchButton)
        albumsButton = findViewById(R.id.albumsButton)
        settingsButton = findViewById(R.id.settingsButton)
        settingsPanel = findViewById(R.id.settingsPanel)
        themeLightButton = findViewById(R.id.themeLightButton)
        themeDarkButton = findViewById(R.id.themeDarkButton)
        themeOledButton = findViewById(R.id.themeOledButton)
        languageSystemButton = findViewById(R.id.languageSystemButton)
        languageItalianButton = findViewById(R.id.languageItalianButton)
        languageEnglishButton = findViewById(R.id.languageEnglishButton)
        languageSpanishButton = findViewById(R.id.languageSpanishButton)
        languageFrenchButton = findViewById(R.id.languageFrenchButton)
        languageGermanButton = findViewById(R.id.languageGermanButton)
        languagePortugueseButton = findViewById(R.id.languagePortugueseButton)
        accentDefaultButton = findViewById(R.id.accentDefaultButton)
        accentSkyButton = findViewById(R.id.accentSkyButton)
        accentMintButton = findViewById(R.id.accentMintButton)
        accentPinkButton = findViewById(R.id.accentPinkButton)
        accentGoldButton = findViewById(R.id.accentGoldButton)
        accentVioletButton = findViewById(R.id.accentVioletButton)
        customColorButton = findViewById(R.id.customColorButton)
        homePathInput = findViewById(R.id.homePathInput)
        saveHomePathButton = findViewById(R.id.saveHomePathButton)
        rescanButton = findViewById(R.id.rescanButton)
        accountNameInput = findViewById(R.id.accountNameInput)
        accountPasswordInput = findViewById(R.id.accountPasswordInput)
        saveAccountButton = findViewById(R.id.saveAccountButton)
        logoutButton = findViewById(R.id.logoutButton)

        playerSheet = findViewById(R.id.playerSheet)
        closePlayerButton = findViewById(R.id.closePlayerButton)
        sheetCoverImage = findViewById(R.id.sheetCoverImage)
        sheetCoverFallback = findViewById(R.id.sheetCoverFallback)
        sheetTitle = findViewById(R.id.sheetTitle)
        sheetArtist = findViewById(R.id.sheetArtist)
        sheetAlbum = findViewById(R.id.sheetAlbum)
        sheetMeta = findViewById(R.id.sheetMeta)
        sheetSeekBar = findViewById(R.id.sheetSeekBar)
        sheetElapsedText = findViewById(R.id.sheetElapsedText)
        sheetRemainingText = findViewById(R.id.sheetRemainingText)
        sheetPlayPauseButton = findViewById(R.id.sheetPlayPauseButton)
        shuffleButton = findViewById(R.id.shuffleButton)
        repeatButton = findViewById(R.id.repeatButton)
        prevButton = findViewById(R.id.prevButton)
        nextButton = findViewById(R.id.nextButton)
    }

    private fun setupList() {
        adapter = LibraryAdapter(
            onClick = ::handleRowClick,
            onLongClick = ::handleRowLongClick,
            onPlayClick = ::handleRowPlayClick,
            canAcceptInput = ::canAcceptRowInput
        )
        libraryList = findViewById(R.id.libraryList)
        libraryList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator().apply {
                supportsChangeAnimations = false
                addDuration = 170L
                removeDuration = 90L
                moveDuration = 180L
                changeDuration = 130L
            }
        }
    }

    private fun setupSeekBars() {
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (currentDurationSeconds <= 0) return
                userIsSeeking = true
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val position = progress.toLong()
                currentPositionMs = position
                syncSeekBars(position, seekBar)
                updateTimeLabels(position, currentDurationSeconds)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (currentDurationSeconds <= 0) {
                    userIsSeeking = false
                    return
                }
                val position = seekBar.progress.toLong()
                currentPositionMs = position
                userIsSeeking = false
                syncSeekBars(position, seekBar)
                updateTimeLabels(position, currentDurationSeconds)
                sendSeekAction(position)
            }
        }

        listOf(miniSeekBar, sheetSeekBar).forEach {
            it.max = DEFAULT_SEEK_MAX
            it.progress = 0
            it.isEnabled = false
            it.setOnSeekBarChangeListener(listener)
        }
        updateTimeLabels(0L, 0)
    }

    private fun setupActions() {
        loginButton.setOnClickListener { login() }
        foldersButton.setOnClickListener { navigateToSection(0) }
        playlistsButton.setOnClickListener { navigateToSection(1) }
        searchButton.setOnClickListener { navigateToSection(2, focusSearch = true) }
        albumsButton.setOnClickListener { navigateToSection(3) }
        settingsButton.setOnClickListener { showSettings() }
        runSearchButton.setOnClickListener {
            searchDebounceJob?.cancel()
            runSearch(closeInput = true)
        }
        queryInput.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || enterPressed) {
                searchDebounceJob?.cancel()
                runSearch(closeInput = true)
                true
            } else {
                false
            }
        }
        queryInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(text: Editable?) {
                if (!searchPanel.isVisible) return
                val query = text?.toString().orEmpty().trim()
                if (query.isBlank()) {
                    clearSearchResults()
                } else if ((hasSearchRun && query != lastSearchQuery) || !runSearchButton.isEnabled) {
                    searchRequestVersion++
                    lastSearchQuery = ""
                    lastSearchResults = emptyList()
                    hasSearchRun = false
                    currentQueue = emptyList()
                    currentSource = "se"
                    playCollectionButton.isVisible = false
                    runSearchButton.isEnabled = true
                    setPathLabel("Search")
                    submitRows(messageRow("Press Go to search"))
                }
                scheduleSearch(query)
            }
        })
        newPlaylistButton.setOnClickListener { createPlaylistDialog() }
        playCollectionButton.setOnClickListener { playCurrentQueue() }
        saveHomePathButton.setOnClickListener { saveHomePath() }
        rescanButton.setOnClickListener { triggerScan() }
        logoutButton.setOnClickListener { logout() }
        saveAccountButton.setOnClickListener { saveAccount() }
        homePathInput.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (actionId == EditorInfo.IME_ACTION_DONE || enterPressed) {
                saveHomePath()
                true
            } else {
                false
            }
        }
        playPauseButton.setOnClickListener { sendPlaybackAction(PlaybackService.ACTION_TOGGLE) }
        sheetPlayPauseButton.setOnClickListener { sendPlaybackAction(PlaybackService.ACTION_TOGGLE) }
        prevButton.setOnClickListener { sendPlaybackAction(PlaybackService.ACTION_PREVIOUS) }
        nextButton.setOnClickListener { sendPlaybackAction(PlaybackService.ACTION_NEXT) }
        shuffleButton.setOnClickListener { sendPlaybackAction(PlaybackService.ACTION_SHUFFLE) }
        repeatButton.setOnClickListener { sendPlaybackAction(PlaybackService.ACTION_REPEAT_MODE) }
        closePlayerButton.setOnClickListener { playerSheet.isVisible = false }
        playerBar.setOnClickListener { openPlayerSheet() }
        themeLightButton.setOnClickListener { setThemeChoice(ThemeChoice.LIGHT) }
        themeDarkButton.setOnClickListener { setThemeChoice(ThemeChoice.DARK) }
        themeOledButton.setOnClickListener { setThemeChoice(ThemeChoice.OLED) }
        languageSystemButton.setOnClickListener { setLanguageChoice(LanguageChoice.SYSTEM) }
        languageItalianButton.setOnClickListener { setLanguageChoice(LanguageChoice.ITALIAN) }
        languageEnglishButton.setOnClickListener { setLanguageChoice(LanguageChoice.ENGLISH) }
        languageSpanishButton.setOnClickListener { setLanguageChoice(LanguageChoice.SPANISH) }
        languageFrenchButton.setOnClickListener { setLanguageChoice(LanguageChoice.FRENCH) }
        languageGermanButton.setOnClickListener { setLanguageChoice(LanguageChoice.GERMAN) }
        languagePortugueseButton.setOnClickListener { setLanguageChoice(LanguageChoice.PORTUGUESE) }
        accentDefaultButton.setOnClickListener { setAccent(AppPreferences.DEFAULT_ACCENT) }
        accentSkyButton.setOnClickListener { setAccent(AppPreferences.RECOMMENDED_ACCENTS[1]) }
        accentMintButton.setOnClickListener { setAccent(AppPreferences.RECOMMENDED_ACCENTS[2]) }
        accentPinkButton.setOnClickListener { setAccent(AppPreferences.RECOMMENDED_ACCENTS[3]) }
        accentGoldButton.setOnClickListener { setAccent(AppPreferences.RECOMMENDED_ACCENTS[4]) }
        accentVioletButton.setOnClickListener { setAccent(AppPreferences.RECOMMENDED_ACCENTS[5]) }
        customColorButton.setOnClickListener { showCustomColorDialog() }
    }

    private fun trackSectionSwipe(event: MotionEvent): Boolean {
        if (!canSwipeSections()) {
            trackingSectionSwipe = false
            draggingSectionSwipe = false
            sectionChildTouchCanceled = false
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val touchingSearchInput = searchPanel.isVisible && isTouchInside(queryInput, event)
                trackingSectionSwipe = !sectionIsTransitioning &&
                    !isTouchInside(playerBar, event) &&
                    !touchingSearchInput
                draggingSectionSwipe = false
                sectionChildTouchCanceled = false
                swipeStartX = event.rawX
                swipeStartY = event.rawY
                swipeStartTime = event.eventTime
                if (trackingSectionSwipe) {
                    resetSectionMotionState()
                } else {
                    cancelSectionMotionAnimations()
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!trackingSectionSwipe || sectionIsTransitioning) return false
                val dx = event.rawX - swipeStartX
                val dy = event.rawY - swipeStartY
                if (!draggingSectionSwipe && abs(dx) > dp(14) && abs(dx) > abs(dy) * 1.2f) {
                    draggingSectionSwipe = true
                    suppressRowClicks()
                    cancelChildTouch(event)
                }
                if (draggingSectionSwipe) {
                    applySwipeDrag(dx)
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP -> {
                if (!trackingSectionSwipe) return false
                val dx = event.rawX - swipeStartX
                val dy = event.rawY - swipeStartY
                val elapsed = event.eventTime - swipeStartTime
                val velocity = if (elapsed > 0L) abs(dx) / elapsed.toFloat() else 0f
                val wasDragging = draggingSectionSwipe
                trackingSectionSwipe = false
                draggingSectionSwipe = false
                val isSectionSwipe = elapsed <= 900L &&
                    (abs(dx) >= dp(76) || velocity > 0.55f) &&
                    abs(dx) > abs(dy) * 1.35f

                if (isSectionSwipe) {
                    suppressRowClicks()
                    cancelChildTouch(event)
                    switchSectionBySwipe(if (dx < 0) 1 else -1)
                    return true
                }

                if (wasDragging) {
                    suppressRowClicks()
                    settleSwipeDrag()
                    return true
                }
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                val wasDragging = draggingSectionSwipe
                trackingSectionSwipe = false
                draggingSectionSwipe = false
                sectionChildTouchCanceled = false
                settleSwipeDrag()
                return wasDragging
            }
        }
        return false
    }

    private fun cancelChildTouch(event: MotionEvent) {
        if (sectionChildTouchCanceled) return
        sectionChildTouchCanceled = true
        val cancel = MotionEvent.obtain(event).apply {
            action = MotionEvent.ACTION_CANCEL
        }
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
    }

    private fun suppressRowClicks() {
        suppressRowClicksUntil = SystemClock.uptimeMillis() + 520L
    }

    private fun canAcceptRowInput(): Boolean {
        return SystemClock.uptimeMillis() >= suppressRowClicksUntil &&
            !draggingSectionSwipe &&
            !sectionIsTransitioning
    }

    private fun canSwipeSections(): Boolean {
        return ::appPanel.isInitialized &&
            appPanel.isVisible &&
            !loginPanel.isVisible &&
            !settingsPanel.isVisible &&
            !playerSheet.isVisible &&
            activeLoadingRequests == 0
    }

    private fun nextContentLoadGeneration(): Int {
        contentLoadGeneration += 1
        return contentLoadGeneration
    }

    private fun isCurrentContentLoad(generation: Int): Boolean {
        return generation == contentLoadGeneration
    }

    private fun switchSectionBySwipe(direction: Int) {
        val sections = listOf(foldersButton, playlistsButton, searchButton, albumsButton)
        val current = sections.indexOf(activeTabButton).takeIf { it >= 0 } ?: 0
        val target = (current + direction).coerceIn(sections.indices)
        if (target == current) {
            settleSwipeDrag()
            return
        }

        rootPanel.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        navigateToSection(target, focusSearch = false)
    }

    private fun isTouchInside(view: View, event: MotionEvent): Boolean {
        if (!view.isVisible) return false
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height
        return event.rawX >= left && event.rawX <= right && event.rawY >= top && event.rawY <= bottom
    }

    private fun showKeyboard(view: View) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View = queryInput) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun closeSearchInput() {
        queryInput.clearFocus()
        hideKeyboard()
    }

    private fun closeKeyboard(view: View) {
        view.clearFocus()
        hideKeyboard(view)
    }

    private fun navigateToSection(target: Int, focusSearch: Boolean = false) {
        if (target != 2) {
            searchRequestVersion++
            searchDebounceJob?.cancel()
            closeSearchInput()
        }
        if (settingsPanel.isVisible) {
            settingsPanel.isVisible = false
            sectionBeforeSettings = null
            updateSettingsButtonState()
        }
        setLibraryContentVisible(true)
        val current = currentSectionIndex()
        resetSectionMotionState()
        if (target != current) {
            prepareSectionTransition(if (target > current) 1 else -1)
        } else {
            settleSwipeDrag()
            pulseTab(sectionButtons()[target])
        }

        when (target) {
            0 -> loadHomeTracks()
            1 -> loadPlaylists()
            2 -> showSearch(focusInput = focusSearch)
            3 -> loadAlbums()
        }
    }

    private fun currentSectionIndex(): Int {
        return sectionButtons().indexOf(activeTabButton).takeIf { it >= 0 } ?: 0
    }

    private fun sectionButtons(): List<MaterialButton> {
        return listOf(foldersButton, playlistsButton, searchButton, albumsButton)
    }

    private fun prepareSectionTransition(direction: Int) {
        pendingSectionEnterDirection = direction.coerceIn(-1, 1).takeIf { it != 0 }
        sectionIsTransitioning = true
        resetSectionMotionState()
        animateContentOut(direction)
    }

    private fun sectionAnimatedViews(): List<View> {
        return listOf(searchPanel, listActions, progress, libraryList)
    }

    private fun sectionResetViews(): List<View> {
        return listOf(searchPanel, listActions, pathText, newPlaylistButton, playCollectionButton, progress, libraryList)
    }

    private fun cancelSectionMotionAnimations() {
        sectionResetViews().forEach { it.animate().cancel() }
    }

    private fun resetSectionMotionState() {
        cancelSectionMotionAnimations()
        sectionResetViews().forEach { view ->
            view.alpha = 1f
            view.translationX = 0f
            view.translationY = 0f
        }
    }

    private fun applySwipeDrag(dx: Float) {
        val current = currentSectionIndex()
        val atStart = current == 0 && dx > 0
        val atEnd = current == sectionButtons().lastIndex && dx < 0
        val resistance = if (atStart || atEnd) 0.12f else 0.28f
        val drag = dx * resistance
        val fade = (1f - (abs(dx) / (resources.displayMetrics.widthPixels * 2.2f))).coerceIn(0.78f, 1f)
        sectionAnimatedViews().filter { it.isVisible }.forEach { view ->
            view.translationX = drag
            view.alpha = fade
        }
        activeTabButton?.let { tab ->
            val scale = (selectedTabScale() - abs(dx) / (resources.displayMetrics.widthPixels * 18f))
                .coerceIn(0.98f, selectedTabScale())
            tab.scaleX = scale
            tab.scaleY = scale
        }
    }

    private fun settleSwipeDrag() {
        val visibleViews = sectionAnimatedViews().filter { it.isVisible }
        visibleViews.forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate()
                .translationX(0f)
                .alpha(1f)
                .setStartDelay((index * 10L).coerceAtMost(40L))
                .setDuration(180L)
                .setInterpolator(sectionSettleEase)
                .withEndAction {
                    if (index == visibleViews.lastIndex) resetSectionMotionState()
                }
                .start()
        }
        if (visibleViews.isEmpty()) resetSectionMotionState()
        activeTabButton?.animate()
            ?.scaleX(selectedTabScale())
            ?.scaleY(selectedTabScale())
            ?.setDuration(160L)
            ?.setInterpolator(sectionSettleEase)
            ?.start()
    }

    private fun animateContentOut(direction: Int) {
        val distance = (resources.displayMetrics.widthPixels * 0.18f).coerceAtMost(dp(120).toFloat())
        sectionAnimatedViews().filter { it.isVisible }.forEachIndexed { index, view ->
            view.animate().cancel()
            view.animate()
                .translationX(-direction * distance)
                .alpha(0f)
                .setStartDelay((index * 8L).coerceAtMost(32L))
                .setDuration(105L)
                .setInterpolator(sectionExitEase)
                .start()
        }
    }

    private fun animateContentIn(direction: Int) {
        val token = ++sectionAnimationToken
        val distance = (resources.displayMetrics.widthPixels * 0.16f).coerceAtMost(dp(108).toFloat())
        val visibleViews = sectionAnimatedViews().filter { it.isVisible }
        visibleViews.forEachIndexed { index, view ->
            view.animate().cancel()
            view.translationX = direction * distance
            view.alpha = 0f
            view.animate()
                .translationX(0f)
                .alpha(1f)
                .setStartDelay((index * 18L).coerceAtMost(72L))
                .setDuration(260L)
                .setInterpolator(sectionEase)
                .withEndAction {
                    if (index == visibleViews.lastIndex && token == sectionAnimationToken) {
                        sectionIsTransitioning = false
                        resetSectionMotionState()
                    }
                }
                .start()
        }
        if (visibleViews.isEmpty()) {
            sectionIsTransitioning = false
            resetSectionMotionState()
        } else {
            rootPanel.postDelayed({
                if (token == sectionAnimationToken) {
                    sectionIsTransitioning = false
                    resetSectionMotionState()
                }
            }, 380L)
        }
    }

    private fun animateContentRefresh() {
        libraryList.animate().cancel()
        libraryList.translationX = 0f
        libraryList.alpha = 0.86f
        libraryList.translationY = dp(8).toFloat()
        libraryList.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180L)
            .setInterpolator(sectionSettleEase)
            .withEndAction {
                libraryList.alpha = 1f
                libraryList.translationX = 0f
                libraryList.translationY = 0f
            }
            .start()
    }

    private fun pulseTab(tab: MaterialButton) {
        tab.animate().cancel()
        tab.scaleX = 0.98f
        tab.scaleY = 0.98f
        tab.animate()
            .scaleX(selectedTabScale())
            .scaleY(selectedTabScale())
            .setDuration(180L)
            .setInterpolator(sectionSettleEase)
            .start()
    }

    private fun selectedTabScale(): Float = 1.02f

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!navigateBackInsideLibrary()) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun login() {
        val host = hostInput.text?.toString().orEmpty().trim()
        val port = portInput.text?.toString().orEmpty().trim()
        val username = usernameInput.text?.toString().orEmpty().trim()
        val password = passwordInput.text?.toString().orEmpty()

        if (host.isBlank() || username.isBlank()) {
            showLoginError("Server and username are required")
            return
        }

        val baseUrl = SessionStore.buildBaseUrl(host, port, httpsSwitch.isChecked)
        loginButton.isEnabled = false
        loginError.isVisible = false

        scope.launch {
            try {
                progress.isVisible = true
                val session = client.login(baseUrl, username, password)
                usernameInput.setText(username)
                showApp(session)
                loadHomeTracks()
            } catch (error: Exception) {
                showLoginError(loginErrorMessage(error))
            } finally {
                progress.isVisible = false
                loginButton.isEnabled = true
            }
        }
    }

    private fun showLoginError(message: String) {
        loginError.text = message
        loginError.isVisible = true
    }

    private fun loginErrorMessage(error: Exception): String {
        val message = error.message ?: "Login failed"
        return if (message.contains("User not found", ignoreCase = true)) {
            "User not found. Check the Swing Music username; spaces are now removed before login."
        } else {
            message
        }
    }

    private fun showLogin() {
        loginPanel.isVisible = true
        appPanel.isVisible = false
        loginButton.isEnabled = true
        loginError.isVisible = false
    }

    private fun showApp(session: Session) {
        loginPanel.isVisible = false
        appPanel.isVisible = true
        client.setSession(session)
        adapter.setSession(session)
        renderUserProfile(
            UserProfile(
                id = 0,
                username = usernameInput.text?.toString()?.trim().takeUnless { it.isNullOrBlank() } ?: "Swing Music",
                roles = listOf("connected")
            )
        )
        loadUserProfile()
        loadCurrentCover()
    }

    private fun loadUserProfile() {
        scope.launch {
            try {
                updateUserProfile(client.getCurrentUser())
            } catch (_: Exception) {
                // Older or locked-down servers can omit this endpoint; keep the provisional header.
            }
        }
    }

    private fun updateUserProfile(user: UserProfile) {
        currentUser = user
        renderUserProfile(user)
    }

    private fun renderUserProfile(user: UserProfile) {
        if (!accountNameInput.hasFocus()) {
            accountNameInput.setText(user.username)
        }
    }

    private fun saveAccount() {
        val loadedUser = currentUser
        if (loadedUser == null || loadedUser.id == 0) {
            toast("Account is still loading")
            return
        }

        val username = accountNameInput.text?.toString().orEmpty().trim()
        val password = accountPasswordInput.text?.toString().orEmpty()
        if (username.isBlank()) {
            toast("Name is required")
            return
        }
        if (username == loadedUser.username && password.isBlank()) {
            toast("Nothing to save")
            return
        }

        saveAccountButton.isEnabled = false
        scope.launch {
            try {
                runLoading {
                    val updated = client.updateProfile(username, password)
                    accountPasswordInput.setText("")
                    updateUserProfile(updated)
                    loadUserProfile()
                    toast("Account updated")
                }
            } finally {
                saveAccountButton.isEnabled = true
            }
        }
    }

    private fun loadFolder(folder: String, pushHistory: Boolean) {
        val loadGeneration = nextContentLoadGeneration()
        selectedPlaylistId = null
        selectedAlbum = null
        selectedTab(foldersButton)
        searchPanel.isVisible = false
        settingsPanel.isVisible = false
        newPlaylistButton.isVisible = false
        playCollectionButton.isVisible = false
        if (pushHistory) folderHistory.add(currentFolder)
        currentFolder = folder
        clearRowsForLoading()

        scope.launch {
            runLoading(shouldConsumePendingTransition = { isCurrentContentLoad(loadGeneration) }) {
                val response = client.getFolder(folder)
                if (!isCurrentContentLoad(loadGeneration)) return@runLoading
                currentSource = "fo:${response.path}"
                currentQueue = response.tracks
                playCollectionButton.isVisible = currentQueue.isNotEmpty()
                setPathLabel(displayFolderPath(response.path))
                submitRows(buildRows(response))
            }
        }
    }

    private fun loadHomeTracks() {
        val loadGeneration = nextContentLoadGeneration()
        val homePath = appPreferences.homePath()
        homePathChangedInSettings = false
        selectedPlaylistId = null
        selectedAlbum = null
        selectedTab(foldersButton)
        searchPanel.isVisible = false
        settingsPanel.isVisible = false
        newPlaylistButton.isVisible = false
        playCollectionButton.isVisible = false
        setPathLabel(displayFolderPath(homePath))
        currentFolder = homePath
        folderHistory.clear()
        currentSource = "fo:$homePath"
        clearRowsForLoading()

        scope.launch {
            runLoading(shouldConsumePendingTransition = { isCurrentContentLoad(loadGeneration) }) {
                val response = client.getFolder(homePath)
                if (!isCurrentContentLoad(loadGeneration)) return@runLoading
                currentSource = "fo:${response.path}"
                currentQueue = response.tracks
                playCollectionButton.isVisible = currentQueue.isNotEmpty()
                setPathLabel(displayFolderPath(response.path))
                submitRows(buildRows(response))
            }
        }
    }

    private fun loadPlaylists() {
        val loadGeneration = nextContentLoadGeneration()
        selectedPlaylistId = null
        selectedAlbum = null
        selectedTab(playlistsButton)
        searchPanel.isVisible = false
        settingsPanel.isVisible = false
        newPlaylistButton.isVisible = true
        playCollectionButton.isVisible = false
        setPathLabel("Playlists")
        currentQueue = emptyList()
        currentSource = "pl"
        clearRowsForLoading()

        scope.launch {
            runLoading(shouldConsumePendingTransition = { isCurrentContentLoad(loadGeneration) }) {
                val playlists = client.getPlaylists()
                if (!isCurrentContentLoad(loadGeneration)) return@runLoading
                submitRows(
                    playlists.map {
                        LibraryRow(
                            kind = RowKind.PLAYLIST,
                            title = it.name,
                            subtitle = "${it.count} tracks",
                            meta = if (it.count > 0) "play" else "",
                            image = it.image,
                            coverKind = it.coverKind,
                            playable = it.count > 0,
                            playlist = it
                        )
                    }.ifEmpty { messageRow("No playlists yet") }
                )
            }
        }
    }

    private fun loadPlaylist(playlist: PlaylistItem) {
        val loadGeneration = nextContentLoadGeneration()
        selectedPlaylistId = playlist.id
        selectedAlbum = null
        selectedTab(playlistsButton)
        searchPanel.isVisible = false
        settingsPanel.isVisible = false
        newPlaylistButton.isVisible = true
        playCollectionButton.isVisible = false
        setPathLabel(playlist.name)
        currentSource = "pl:${playlist.id}"
        clearRowsForLoading()

        scope.launch {
            runLoading(shouldConsumePendingTransition = { isCurrentContentLoad(loadGeneration) }) {
                currentQueue = client.getPlaylistTracks(playlist.id)
                if (!isCurrentContentLoad(loadGeneration)) return@runLoading
                playCollectionButton.isVisible = currentQueue.isNotEmpty()
                submitRows(trackRows(currentQueue).ifEmpty { messageRow("This playlist is empty") })
            }
        }
    }

    private fun clearSearchResults() {
        nextContentLoadGeneration()
        searchDebounceJob?.cancel()
        if (!hasSearchRun && lastSearchQuery.isBlank() && lastSearchResults.isEmpty() && currentQueue.isEmpty() && currentPathLabel == "Search") {
            return
        }
        searchRequestVersion++
        lastSearchQuery = ""
        lastSearchResults = emptyList()
        hasSearchRun = false
        currentQueue = emptyList()
        currentSource = "se"
        playCollectionButton.isVisible = false
        runSearchButton.isEnabled = true
        setPathLabel("Search")
        submitRows(messageRow("Search your music"))
    }

    private fun scheduleSearch(query: String) {
        if (query == lastSearchQuery && hasSearchRun) return
        searchDebounceJob?.cancel()
        searchDebounceJob = scope.launch {
            delay(520L)
            val currentQuery = queryInput.text?.toString().orEmpty().trim()
            if (searchPanel.isVisible && currentQuery == query) {
                runSearch(closeInput = false)
            }
        }
    }

    private fun showSearch(focusInput: Boolean) {
        nextContentLoadGeneration()
        selectedPlaylistId = null
        selectedAlbum = null
        selectedTab(searchButton)
        searchPanel.isVisible = true
        settingsPanel.isVisible = false
        newPlaylistButton.isVisible = false
        currentSource = if (lastSearchQuery.isBlank()) "se" else "se:$lastSearchQuery"
        setPathLabel(if (lastSearchQuery.isBlank()) "Search" else "Search: $lastSearchQuery")
        currentQueue = lastSearchResults
        playCollectionButton.isVisible = currentQueue.isNotEmpty()
        val rows = when {
            lastSearchQuery.isBlank() -> messageRow("Search your music")
            lastSearchResults.isNotEmpty() -> trackRows(lastSearchResults)
            hasSearchRun -> messageRow("No tracks found")
            else -> messageRow("Search your music")
        }
        submitRows(rows)
        if (focusInput) {
            queryInput.requestFocus()
            queryInput.post { showKeyboard(queryInput) }
        } else {
            queryInput.clearFocus()
            hideKeyboard()
        }
    }

    private fun runSearch(closeInput: Boolean = true) {
        val query = queryInput.text?.toString().orEmpty().trim()
        if (query.isBlank()) {
            if (closeInput) {
                hideKeyboard()
                queryInput.clearFocus()
            }
            clearSearchResults()
            return
        }
        if (query == lastSearchQuery && hasSearchRun) {
            if (closeInput) {
                hideKeyboard()
                queryInput.clearFocus()
            }
            currentQueue = lastSearchResults
            playCollectionButton.isVisible = currentQueue.isNotEmpty()
            setPathLabel("Search: $query")
            submitRows(trackRows(currentQueue).ifEmpty { messageRow("No tracks found") })
            return
        }
        selectedPlaylistId = null
        selectedAlbum = null
        currentSource = "se:$query"
        val loadGeneration = nextContentLoadGeneration()
        val requestVersion = ++searchRequestVersion
        if (closeInput) {
            hideKeyboard()
            queryInput.clearFocus()
        }
        playCollectionButton.isVisible = false
        setPathLabel("Search: $query")
        submitRows(messageRow("Searching..."))
        runSearchButton.isEnabled = false

        scope.launch {
            var completed = false
            var stale = false
            try {
                runLoading(shouldConsumePendingTransition = {
                    isCurrentContentLoad(loadGeneration) && requestVersion == searchRequestVersion
                }) {
                    val results = client.searchTracks(query)
                    val typedQuery = queryInput.text?.toString().orEmpty().trim()
                    if (!isCurrentContentLoad(loadGeneration) || requestVersion != searchRequestVersion || typedQuery != query) {
                        stale = true
                        return@runLoading
                    }
                    completed = true
                    lastSearchQuery = query
                    lastSearchResults = results
                    hasSearchRun = true
                    currentQueue = results
                    playCollectionButton.isVisible = currentQueue.isNotEmpty()
                    setPathLabel("Search: $query")
                    submitRows(trackRows(currentQueue).ifEmpty { messageRow("No tracks found") })
                }
            } finally {
                if (requestVersion == searchRequestVersion) {
                    runSearchButton.isEnabled = true
                    if (!completed && !stale) {
                        currentQueue = emptyList()
                        playCollectionButton.isVisible = false
                        submitRows(messageRow("Search failed"))
                    }
                }
            }
        }
    }

    private fun loadAlbums() {
        val loadGeneration = nextContentLoadGeneration()
        selectedPlaylistId = null
        selectedAlbum = null
        selectedTab(albumsButton)
        searchPanel.isVisible = false
        settingsPanel.isVisible = false
        newPlaylistButton.isVisible = false
        playCollectionButton.isVisible = false
        setPathLabel("Albums")
        currentQueue = emptyList()
        clearRowsForLoading()

        scope.launch {
            runLoading(shouldConsumePendingTransition = { isCurrentContentLoad(loadGeneration) }) {
                val albums = client.getAlbums()
                if (!isCurrentContentLoad(loadGeneration)) return@runLoading
                submitRows(
                    albums.map {
                        LibraryRow(
                            kind = RowKind.ALBUM,
                            title = it.title,
                            subtitle = it.artistText,
                            meta = it.helpText,
                            image = it.image,
                            coverKind = CoverKind.ALBUM,
                            playable = true,
                            album = it
                        )
                    }.ifEmpty { messageRow("No albums found") }
                )
            }
        }
    }

    private fun loadAlbumTracks(album: AlbumItem) {
        val loadGeneration = nextContentLoadGeneration()
        selectedAlbum = album
        selectedPlaylistId = null
        selectedTab(albumsButton)
        searchPanel.isVisible = false
        settingsPanel.isVisible = false
        newPlaylistButton.isVisible = false
        playCollectionButton.isVisible = false
        setPathLabel(album.title)
        currentSource = "al:${album.hash}"
        clearRowsForLoading()

        scope.launch {
            runLoading(shouldConsumePendingTransition = { isCurrentContentLoad(loadGeneration) }) {
                currentQueue = client.getAlbumTracks(album.hash)
                if (!isCurrentContentLoad(loadGeneration)) return@runLoading
                playCollectionButton.isVisible = currentQueue.isNotEmpty()
                submitRows(trackRows(currentQueue).ifEmpty { messageRow("No tracks in album") })
            }
        }
    }

    private fun showSettings() {
        closeSearchInput()
        val openingSettings = !settingsPanel.isVisible
        if (openingSettings) {
            sectionBeforeSettings = activeTabButton
            searchPanel.isVisible = false
            settingsPanel.isVisible = true
            setLibraryContentVisible(false)
            newPlaylistButton.isVisible = false
            playCollectionButton.isVisible = false
            pathText.text = "Settings"
            clearSelectedTab()
            loadUserProfile()
            loadHomePathSetting()
        } else {
            settingsPanel.isVisible = false
            setLibraryContentVisible(true)
            restoreSectionAfterSettings()
        }
        updateSettingsButtonState()
        updateThemeButtons()
        updateLanguageButtons()
        updateAccentButtons()
    }

    private fun setLibraryContentVisible(visible: Boolean) {
        listActions.isVisible = visible
        libraryList.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    private fun restoreSectionAfterSettings() {
        val restoredTab = sectionBeforeSettings ?: activeTabButton ?: foldersButton
        sectionBeforeSettings = null
        if (restoredTab == foldersButton && homePathChangedInSettings) {
            loadHomeTracks()
            return
        }
        selectedTab(restoredTab)
        searchPanel.isVisible = restoredTab == searchButton
        if (!searchPanel.isVisible) {
            closeSearchInput()
        }
        newPlaylistButton.isVisible = restoredTab == playlistsButton
        playCollectionButton.isVisible = currentQueue.isNotEmpty()
        pathText.text = currentPathLabel
    }

    private fun loadHomePathSetting() {
        if (!homePathInput.hasFocus()) {
            homePathInput.setText(appPreferences.homePath())
        }
    }

    private fun saveHomePath() {
        val path = AppPreferences.normalizeHomePath(homePathInput.text?.toString().orEmpty())
        val previousPath = appPreferences.homePath()
        homePathInput.setText(path)
        homePathInput.setSelection(homePathInput.text?.length ?: 0)
        closeKeyboard(homePathInput)

        if (path == previousPath) {
            toast("Nothing to save")
            return
        }

        appPreferences.saveHomePath(path)
        homePathChangedInSettings = true
        toast("Home path saved")
    }

    private fun buildRows(response: FolderResponse): List<LibraryRow> {
        val folderRows = response.folders.map {
            LibraryRow(
                kind = RowKind.FOLDER,
                title = it.name,
                subtitle = it.path,
                meta = if (it.trackCount > 0) "${it.trackCount}" else "",
                folder = it
            )
        }
        return (folderRows + trackRows(response.tracks)).ifEmpty { messageRow("Nothing here") }
    }

    private fun displayFolderPath(path: String): String {
        return if (path == "\$home") "Home" else path
    }

    private fun setPathLabel(label: String) {
        currentPathLabel = label
        if (!settingsPanel.isVisible) {
            pathText.text = label
        }
    }

    private fun trackRows(tracks: List<Track>): List<LibraryRow> {
        return tracks.map {
            LibraryRow(
                kind = RowKind.TRACK,
                title = it.title,
                subtitle = it.artistText.ifBlank { it.album },
                meta = it.durationText,
                image = it.image,
                coverKind = CoverKind.TRACK,
                playable = true,
                track = it
            )
        }
    }

    private fun messageRow(message: String): List<LibraryRow> {
        return listOf(LibraryRow(kind = RowKind.MESSAGE, title = message))
    }

    private fun clearRowsForLoading() {
        resetSectionMotionState()
        adapter.submit(emptyList())
        libraryList.scrollToPosition(0)
    }

    private fun submitRows(rows: List<LibraryRow>) {
        val enterDirection = pendingSectionEnterDirection
        pendingSectionEnterDirection = null
        resetSectionMotionState()
        adapter.submit(rows)
        libraryList.scrollToPosition(0)
        if (enterDirection != null) {
            animateContentIn(enterDirection)
        } else {
            animateContentRefresh()
        }
    }

    private fun handleRowClick(row: LibraryRow, position: Int) {
        when (row.kind) {
            RowKind.FOLDER -> row.folder?.let { loadFolder(it.path, pushHistory = true) }
            RowKind.PLAYLIST -> row.playlist?.let { loadPlaylist(it) }
            RowKind.ALBUM -> row.album?.let { loadAlbumTracks(it) }
            RowKind.TRACK -> row.track?.let { playTrack(it, position) }
            RowKind.SETTINGS -> showSettings()
            RowKind.MESSAGE -> Unit
        }
    }

    private fun handleRowPlayClick(row: LibraryRow, position: Int) {
        when (row.kind) {
            RowKind.TRACK -> row.track?.let { playTrack(it, position) }
            RowKind.PLAYLIST -> row.playlist?.let { playPlaylist(it) }
            RowKind.ALBUM -> row.album?.let { playAlbum(it) }
            else -> Unit
        }
    }

    private fun handleRowLongClick(row: LibraryRow, position: Int) {
        if (row.kind == RowKind.TRACK && row.track != null) {
            trackActions(row.track, position)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun playTrack(track: Track, rowPosition: Int) {
        val session = client.currentSession() ?: return
        val queue = currentQueue.ifEmpty { listOf(track) }
        val trackIndex = queue.indexOfFirst { it.trackHash == track.trackHash }
            .takeIf { it >= 0 }
            ?: 0
        val intent = PlaybackService.playIntent(
            context = this,
            queue = queue,
            index = trackIndex,
            session = session,
            source = currentSource
        )
        currentTrackTitle = track.title
        currentTrackImage = track.image
        nowPlayingTitle.text = track.title
        nowPlayingSubtitle.text = track.artistText
        sheetTitle.text = track.title
        sheetArtist.text = track.artistText
        sheetAlbum.text = track.album.ifBlank { "Unknown album" }
        sheetMeta.text = buildPlayerMeta(track.duration, trackIndex, queue.size)
        updateProgressViews(track.duration, 0L, playing = false)
        loadCurrentCover()
        ContextCompat.startForegroundService(this, intent)
        openPlayerSheet()
    }

    private fun playCurrentQueue() {
        if (currentQueue.isEmpty()) return
        playTrack(currentQueue.first(), 0)
    }

    private fun playPlaylist(playlist: PlaylistItem) {
        scope.launch {
            runLoading {
                val tracks = client.getPlaylistTracks(playlist.id)
                if (tracks.isEmpty()) {
                    toast("This playlist is empty")
                    return@runLoading
                }
                currentQueue = tracks
                currentSource = "pl:${playlist.id}"
                playTrack(tracks.first(), 0)
            }
        }
    }

    private fun playAlbum(album: AlbumItem) {
        scope.launch {
            runLoading {
                val tracks = client.getAlbumTracks(album.hash)
                if (tracks.isEmpty()) {
                    toast("No tracks in album")
                    return@runLoading
                }
                currentQueue = tracks
                currentSource = "al:${album.hash}"
                playTrack(tracks.first(), 0)
            }
        }
    }

    private fun trackActions(track: Track, position: Int) {
        val dialog = AlertDialog.Builder(this).create()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(14))
        }
        val title = TextView(this).apply {
            text = track.title
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.ink))
            textSize = 18f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = track.artistText
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.muted))
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        container.addView(title)
        container.addView(subtitle, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        fun addAction(label: String, primary: Boolean = false, action: () -> Unit) {
            container.addView(
                MaterialButton(this).apply {
                    text = label
                    isAllCaps = false
                    letterSpacing = 0f
                    gravity = Gravity.CENTER_VERTICAL
                    cornerRadius = dp(16)
                    backgroundTintList = ColorStateList.valueOf(if (primary) accentColor() else Color.TRANSPARENT)
                    setTextColor(if (primary) onAccentColor(accentColor()) else ContextCompat.getColor(this@MainActivity, R.color.ink))
                    strokeColor = ColorStateList.valueOf(if (primary) accentColor() else edgeColor())
                    strokeWidth = dp(1)
                    setOnClickListener {
                        dialog.dismiss()
                        action()
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(46)
                ).apply {
                    topMargin = dp(10)
                }
            )
        }
        addAction("Play now", primary = true) { playTrack(track, position) }
        addAction("Add to playlist") { addToPlaylistDialog(track) }
        addAction(if (track.isFavorite) "Remove favorite" else "Add favorite") { toggleFavorite(track) }
        val playlistId = selectedPlaylistId
        if (playlistId != null) {
            addAction("Remove from this playlist") { removeFromPlaylist(track, position) }
        }
        dialog.setView(container)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.surface_alt)))
        }
        dialog.show()
    }

    private fun addToPlaylistDialog(track: Track) {
        scope.launch {
            runLoading {
                val playlists = client.getPlaylists()
                if (playlists.isEmpty()) {
                    toast("Create a playlist first")
                    return@runLoading
                }
                val names = playlists.map { it.name }.toTypedArray()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Add to playlist")
                    .setItems(names) { _, which ->
                        scope.launch {
                            runLoading {
                                client.addTrackToPlaylist(playlists[which].id, track.trackHash)
                                toast("Added to ${playlists[which].name}")
                            }
                        }
                    }
                    .show()
            }
        }
    }

    private fun createPlaylistDialog() {
        val input = EditText(this).apply {
            hint = "Playlist name"
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("New playlist")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    scope.launch {
                        runLoading {
                            client.createPlaylist(name)
                            loadPlaylists()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeFromPlaylist(track: Track, position: Int) {
        val playlistId = selectedPlaylistId ?: return
        scope.launch {
            runLoading {
                client.removeTrackFromPlaylist(playlistId, track.trackHash, position)
                currentQueue = currentQueue.filterIndexed { index, item ->
                    !(index == position && item.trackHash == track.trackHash)
                }
                submitRows(trackRows(currentQueue).ifEmpty { messageRow("This playlist is empty") })
                toast("Removed")
            }
        }
    }

    private fun toggleFavorite(track: Track) {
        scope.launch {
            runLoading {
                client.toggleFavorite(track)
                toast(if (track.isFavorite) "Removed favorite" else "Added favorite")
            }
        }
    }

    private fun triggerScan() {
        scope.launch {
            runLoading {
                client.triggerScan()
                toast("Scan triggered")
            }
        }
    }

    private fun logout() {
        sendPlaybackAction(PlaybackService.ACTION_STOP)
        activeLoadingRequests = 0
        progress.isVisible = false
        sessionStore.clear()
        adapter.setSession(null)
        currentQueue = emptyList()
        currentUser = null
        currentTrackTitle = ""
        currentTrackImage = ""
        updateProgressViews(0, 0L, playing = false)
        searchDebounceJob?.cancel()
        lastSearchQuery = ""
        lastSearchResults = emptyList()
        hasSearchRun = false
        searchRequestVersion++
        currentPathLabel = "Home"
        submitRows(emptyList())
        showLogin()
    }

    private suspend fun runLoading(
        shouldConsumePendingTransition: () -> Boolean = { true },
        block: suspend () -> Unit
    ) {
        try {
            showLoading()
            block()
        } catch (error: Exception) {
            toast(error.message ?: "Something went wrong")
        } finally {
            hideLoading()
            if (pendingSectionEnterDirection != null && shouldConsumePendingTransition()) {
                val direction = pendingSectionEnterDirection ?: 1
                pendingSectionEnterDirection = null
                animateContentIn(direction)
            } else if (pendingSectionEnterDirection == null && !sectionIsTransitioning) {
                resetSectionMotionState()
            }
        }
    }

    private fun showLoading() {
        activeLoadingRequests += 1
        progress.isVisible = true
    }

    private fun hideLoading() {
        activeLoadingRequests = (activeLoadingRequests - 1).coerceAtLeast(0)
        progress.isVisible = activeLoadingRequests > 0
    }

    private fun selectedTab(active: MaterialButton) {
        activeTabButton = active
        val buttons = sectionButtons()
        val accent = accentColor()
        val onAccent = onAccentColor(accent)
        val ink = ContextCompat.getColor(this, R.color.ink)
        val edge = edgeColor()
        buttons.forEach { button ->
            button.animate().cancel()
            val selected = button == active
            button.backgroundTintList = ColorStateList.valueOf(if (selected) accent else Color.TRANSPARENT)
            button.setTextColor(if (selected) onAccent else ink)
            button.strokeColor = ColorStateList.valueOf(if (selected) Color.TRANSPARENT else edge)
            button.strokeWidth = dp(1)
            button.elevation = if (selected) dp(1).toFloat() else 0f
            button.animate()
                .scaleX(if (selected) selectedTabScale() else 1f)
                .scaleY(if (selected) selectedTabScale() else 1f)
                .translationY(if (selected) -dp(1).toFloat() else 0f)
                .setDuration(170L)
                .setInterpolator(sectionSettleEase)
                .start()
        }
    }

    private fun clearSelectedTab() {
        activeTabButton = null
        val ink = ContextCompat.getColor(this, R.color.ink)
        val edge = edgeColor()
        sectionButtons().forEach { button ->
            button.animate().cancel()
            button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            button.setTextColor(ink)
            button.strokeColor = ColorStateList.valueOf(edge)
            button.strokeWidth = dp(1)
            button.elevation = 0f
            button.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(140L)
                .setInterpolator(sectionSettleEase)
                .start()
        }
    }

    private fun sendPlaybackAction(action: String) {
        startService(Intent(this, PlaybackService::class.java).setAction(action))
    }

    private fun sendSeekAction(positionMs: Long) {
        startService(
            Intent(this, PlaybackService::class.java)
                .setAction(PlaybackService.ACTION_SEEK)
                .putExtra(PlaybackService.EXTRA_POSITION_MS, positionMs)
        )
    }

    private fun openPlayerSheet() {
        if (currentTrackTitle.isBlank() || currentTrackTitle == "Nothing playing") return
        playerSheet.isVisible = true
        loadCurrentCover()
    }

    private fun loadCurrentCover() {
        val session = client.currentSession()
        coverArtLoader.load(miniCoverImage, miniCoverFallback, session, CoverKind.TRACK, currentTrackImage)
        coverArtLoader.load(sheetCoverImage, sheetCoverFallback, session, CoverKind.TRACK, currentTrackImage, large = true)
    }

    private fun buildPlayerMeta(duration: Int, index: Int, queueSize: Int): String {
        val parts = mutableListOf<String>()
        if (duration > 0) parts.add(formatDuration(duration))
        if (index >= 0 && queueSize > 0) parts.add("${index + 1} of $queueSize")
        parts.add(
            when (repeatMode) {
                1 -> "Repeat all"
                2 -> "Repeat one"
                else -> "Repeat off"
            }
        )
        if (shuffleEnabled) parts.add("Shuffle")
        return parts.joinToString("  -  ")
    }

    private fun updateProgressViews(duration: Int, positionMs: Long, playing: Boolean) {
        currentDurationSeconds = duration.coerceAtLeast(0)
        val durationMs = durationMs()
        currentPositionMs = if (durationMs > 0L) {
            positionMs.coerceIn(0L, durationMs)
        } else {
            0L
        }
        currentIsPlaying = playing

        val max = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().takeIf { it > 0 } ?: DEFAULT_SEEK_MAX
        listOf(miniSeekBar, sheetSeekBar).forEach {
            if (it.max != max) it.max = max
            it.isEnabled = durationMs > 0L
        }
        if (!userIsSeeking) {
            syncSeekBars(currentPositionMs, null)
            updateTimeLabels(currentPositionMs, currentDurationSeconds)
        }

        if (playing && durationMs > 0L) {
            startProgressTicker()
        } else {
            stopProgressTicker()
        }
    }

    private fun startProgressTicker() {
        if (progressTickerJob?.isActive == true) return
        progressTickerJob = scope.launch {
            while (currentIsPlaying) {
                delay(PROGRESS_TICK_MS)
                if (!currentIsPlaying || userIsSeeking || currentDurationSeconds <= 0) continue
                val durationMs = durationMs()
                val nextPosition = (currentPositionMs + PROGRESS_TICK_MS).coerceAtMost(durationMs)
                currentPositionMs = nextPosition
                syncSeekBars(nextPosition, null)
                updateTimeLabels(nextPosition, currentDurationSeconds)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    private fun syncSeekBars(positionMs: Long, source: SeekBar?) {
        val progressValue = positionMs.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        listOf(miniSeekBar, sheetSeekBar).forEach {
            if (it != source && it.progress != progressValue) {
                it.progress = progressValue
            }
        }
    }

    private fun updateTimeLabels(positionMs: Long, duration: Int) {
        val elapsedSeconds = (positionMs / 1000L).toInt().coerceAtLeast(0)
        val remainingSeconds = (duration - elapsedSeconds).coerceAtLeast(0)
        sheetElapsedText.text = formatDuration(elapsedSeconds)
        sheetRemainingText.text = "-${formatDuration(remainingSeconds)}"
    }

    private fun durationMs(): Long {
        return currentDurationSeconds.toLong() * 1000L
    }

    private fun formatDuration(duration: Int): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun updateModeButtons() {
        val active = accentColor()
        val inactive = ContextCompat.getColor(this, R.color.ink_alt)
        val activeIcon = onAccentColor(active)
        val inactiveIcon = ContextCompat.getColor(this, R.color.ink)
        shuffleButton.backgroundTintList = ColorStateList.valueOf(if (shuffleEnabled) active else inactive)
        repeatButton.backgroundTintList = ColorStateList.valueOf(if (repeatMode > 0) active else inactive)
        shuffleButton.setColorFilter(if (shuffleEnabled) activeIcon else inactiveIcon)
        repeatButton.setColorFilter(if (repeatMode > 0) activeIcon else inactiveIcon)
    }

    private fun setThemeChoice(choice: ThemeChoice) {
        appPreferences.saveTheme(choice)
        AppCompatDelegate.setDefaultNightMode(AppPreferences.nightModeFor(choice))
        recreate()
    }

    private fun setLanguageChoice(choice: LanguageChoice) {
        appPreferences.saveLanguage(choice)
        updateLanguageButtons()
        toast("Language: ${choice.label}")
    }

    private fun setAccent(hex: String) {
        appPreferences.saveAccent(hex)
        applyAccentChrome()
    }

    private fun showCustomColorDialog() {
        var selectedColor = accentColor()
        val preview = View(this).apply {
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.player_background)
            backgroundTintList = ColorStateList.valueOf(selectedColor)
        }
        val picker = ColorPickerView(this).apply {
            setColor(selectedColor)
            onColorChanged = { color ->
                selectedColor = color
                preview.backgroundTintList = ColorStateList.valueOf(color)
            }
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), 0)
            addView(
                picker,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(330)
                )
            )
            addView(
                preview,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(34)
                ).apply {
                    topMargin = dp(8)
                }
            )
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Custom color")
            .setView(container)
            .setPositiveButton("Apply") { _, _ -> setAccent(colorToHex(selectedColor)) }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.surface_alt)))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.ink))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this, R.color.ink))
        }
        dialog.show()
    }

    private fun updateThemeButtons() {
        val buttons = mapOf(
            ThemeChoice.LIGHT to themeLightButton,
            ThemeChoice.DARK to themeDarkButton,
            ThemeChoice.OLED to themeOledButton
        )
        val selectedChoice = appPreferences.themeChoice()
        val accent = accentColor()
        val onAccent = onAccentColor(accent)
        val ink = ContextCompat.getColor(this, R.color.ink)
        val edge = edgeColor()
        buttons.forEach { (choice, button) ->
            val selected = choice == selectedChoice
            button.backgroundTintList = ColorStateList.valueOf(if (selected) accent else Color.TRANSPARENT)
            button.setTextColor(if (selected) onAccent else ink)
            button.strokeColor = ColorStateList.valueOf(if (selected) Color.TRANSPARENT else edge)
            button.strokeWidth = dp(if (selected) 2 else 1)
        }
    }

    private fun applyThemeChrome() {
        val surface = surfaceColor()
        window.statusBarColor = surface
        window.navigationBarColor = surface
        rootPanel.setBackgroundColor(surface)
        appPanel.setBackgroundColor(surface)
        playerSheet.setBackgroundColor(surface)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val lightFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
            window.decorView.systemUiVisibility = if (appPreferences.themeChoice() == ThemeChoice.LIGHT) lightFlags else 0
        }
        applyAccentChrome()
    }

    private fun applyAccentChrome() {
        val accent = accentColor()
        val onAccent = onAccentColor(accent)
        val ink = ContextCompat.getColor(this, R.color.ink)
        val edge = edgeColor()
        val secondarySurface = secondarySurfaceColor()
        adapter.setAccent(accent, onAccent)
        adapter.setThemeColors(secondarySurface, edge)

        playerBar.backgroundTintList = null
        playerBar.background = ContextCompat.getDrawable(this, playerBackgroundDrawable())
        settingsPanel.backgroundTintList = ColorStateList.valueOf(settingsSurfaceColor())

        listOf(miniCoverFallback, sheetCoverFallback).forEach { it.setTextColor(accent) }

        updateSettingsButtonState()
        listOf(playPauseButton, sheetPlayPauseButton, prevButton, nextButton).forEach {
            it.backgroundTintList = ColorStateList.valueOf(accent)
            it.setColorFilter(onAccent)
        }
        closePlayerButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ink_alt))
        closePlayerButton.setColorFilter(ink)
        listOf(miniSeekBar, sheetSeekBar).forEach {
            it.progressTintList = ColorStateList.valueOf(accent)
            it.thumbTintList = ColorStateList.valueOf(accent)
            it.progressBackgroundTintList = ColorStateList.valueOf(edge)
        }

        listOf(loginButton, runSearchButton, saveHomePathButton, saveAccountButton).forEach {
            it.backgroundTintList = ColorStateList.valueOf(accent)
            it.setTextColor(onAccent)
            it.strokeColor = ColorStateList.valueOf(accent)
        }
        playCollectionButton.backgroundTintList = ColorStateList.valueOf(accent)
        playCollectionButton.setColorFilter(onAccent)
        listOf(newPlaylistButton, rescanButton, logoutButton).forEach {
            it.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            it.setTextColor(accent)
            it.strokeColor = ColorStateList.valueOf(edge)
            it.strokeWidth = dp(1)
        }

        activeTabButton?.let { selectedTab(it) }
        updateThemeButtons()
        updateLanguageButtons()
        updateAccentButtons()
        updateModeButtons()
    }

    private fun updateSettingsButtonState() {
        if (!::settingsButton.isInitialized) return
        val settingsActive = ::settingsPanel.isInitialized && settingsPanel.isVisible
        settingsButton.backgroundTintList = null
        settingsButton.setColorFilter(if (settingsActive) accentColor() else ContextCompat.getColor(this, R.color.ink))
        settingsButton.alpha = if (settingsActive) 1f else 0.72f
    }

    private fun updateLanguageButtons() {
        val choices = listOf(
            LanguageChoice.SYSTEM to languageSystemButton,
            LanguageChoice.ITALIAN to languageItalianButton,
            LanguageChoice.ENGLISH to languageEnglishButton,
            LanguageChoice.SPANISH to languageSpanishButton,
            LanguageChoice.FRENCH to languageFrenchButton,
            LanguageChoice.GERMAN to languageGermanButton,
            LanguageChoice.PORTUGUESE to languagePortugueseButton
        )
        val current = appPreferences.languageChoice()
        val accent = accentColor()
        val onAccent = onAccentColor(accent)
        val ink = ContextCompat.getColor(this, R.color.ink)
        val edge = edgeColor()
        choices.forEach { (choice, button) ->
            val selected = choice == current
            button.backgroundTintList = ColorStateList.valueOf(if (selected) accent else Color.TRANSPARENT)
            button.setTextColor(if (selected) onAccent else ink)
            button.strokeColor = ColorStateList.valueOf(if (selected) Color.TRANSPARENT else edge)
            button.strokeWidth = dp(if (selected) 2 else 1)
        }
    }

    private fun updateAccentButtons() {
        val choices = listOf(
            AppPreferences.DEFAULT_ACCENT to accentDefaultButton,
            AppPreferences.RECOMMENDED_ACCENTS[1] to accentSkyButton,
            AppPreferences.RECOMMENDED_ACCENTS[2] to accentMintButton,
            AppPreferences.RECOMMENDED_ACCENTS[3] to accentPinkButton,
            AppPreferences.RECOMMENDED_ACCENTS[4] to accentGoldButton,
            AppPreferences.RECOMMENDED_ACCENTS[5] to accentVioletButton
        )
        val current = appPreferences.accentHex()
        val edge = edgeColor()
        val selectedStroke = selectionRingColor()
        choices.forEach { (hex, button) ->
            val color = if (hex == AppPreferences.DEFAULT_ACCENT) defaultAccentForTheme() else Color.parseColor(hex)
            val selected = hex == current
            button.backgroundTintList = ColorStateList.valueOf(color)
            button.setTextColor(onAccentColor(color))
            button.strokeColor = ColorStateList.valueOf(if (selected) selectedStroke else edge)
            button.strokeWidth = dp(if (selected) 2 else 1)
        }

        val customSelected = choices.none { it.first == current }
        val customBg = if (customSelected) accentColor() else Color.TRANSPARENT
        customColorButton.backgroundTintList = ColorStateList.valueOf(customBg)
        customColorButton.setTextColor(
            if (customSelected) onAccentColor(customBg) else ContextCompat.getColor(this, R.color.ink)
        )
        customColorButton.strokeColor = ColorStateList.valueOf(if (customSelected) selectedStroke else edge)
        customColorButton.strokeWidth = dp(if (customSelected) 2 else 1)
    }

    private fun accentColor(): Int {
        val stored = appPreferences.accentHex()
        return if (stored == AppPreferences.DEFAULT_ACCENT) defaultAccentForTheme() else Color.parseColor(stored)
    }

    private fun defaultAccentForTheme(): Int {
        return if (appPreferences.themeChoice() == ThemeChoice.LIGHT) Color.BLACK else Color.WHITE
    }

    private fun selectionRingColor(): Int {
        return if (appPreferences.themeChoice() == ThemeChoice.LIGHT) Color.BLACK else Color.WHITE
    }

    private fun surfaceColor(): Int {
        return if (appPreferences.themeChoice() == ThemeChoice.OLED) {
            Color.BLACK
        } else {
            ContextCompat.getColor(this, R.color.surface)
        }
    }

    private fun secondarySurfaceColor(): Int {
        return if (appPreferences.themeChoice() == ThemeChoice.OLED) {
            Color.rgb(8, 8, 8)
        } else {
            ContextCompat.getColor(this, R.color.surface_alt)
        }
    }

    private fun settingsSurfaceColor(): Int {
        return if (appPreferences.themeChoice() == ThemeChoice.OLED) {
            Color.rgb(5, 5, 5)
        } else {
            ContextCompat.getColor(this, R.color.glass)
        }
    }

    private fun edgeColor(): Int {
        return if (appPreferences.themeChoice() == ThemeChoice.OLED) {
            Color.rgb(58, 58, 58)
        } else {
            ContextCompat.getColor(this, R.color.edge)
        }
    }

    private fun playerBackgroundDrawable(): Int {
        return if (appPreferences.themeChoice() == ThemeChoice.OLED) {
            R.drawable.player_background_oled
        } else {
            R.drawable.player_background
        }
    }

    private fun onAccentColor(color: Int): Int {
        val luminance = (
            0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)
            ) / 255.0
        return if (luminance > 0.58) Color.BLACK else Color.WHITE
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun registerPlaybackReceiver() {
        val filter = IntentFilter(PlaybackService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(playbackReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(playbackReceiver, filter)
        }
    }

    private fun navigateBackInsideLibrary(): Boolean {
        return when {
            playerSheet.isVisible -> {
                playerSheet.isVisible = false
                true
            }
            settingsPanel.isVisible -> {
                settingsPanel.isVisible = false
                setLibraryContentVisible(true)
                restoreSectionAfterSettings()
                updateSettingsButtonState()
                true
            }
            searchPanel.isVisible -> {
                loadHomeTracks()
                true
            }
            selectedPlaylistId != null -> {
                loadPlaylists()
                true
            }
            selectedAlbum != null -> {
                loadAlbums()
                true
            }
            folderHistory.isNotEmpty() -> {
                val previous = folderHistory.removeAt(folderHistory.lastIndex)
                loadFolder(previous, pushHistory = false)
                true
            }
            else -> false
        }
    }

    companion object {
        private const val DEFAULT_SEEK_MAX = 1000
        private const val PROGRESS_TICK_MS = 500L
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(playbackReceiver) }
        scope.cancel()
        super.onDestroy()
    }
}
