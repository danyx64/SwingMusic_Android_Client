package dev.swingmusic.android

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

enum class ThemeChoice(val storageValue: String, val label: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    OLED("oled", "OLED");

    companion object {
        fun fromStorage(value: String?): ThemeChoice {
            return entries.firstOrNull { it.storageValue == value } ?: DARK
        }
    }
}

enum class LanguageChoice(val storageValue: String, val label: String) {
    SYSTEM("system", "System"),
    ITALIAN("it", "Italiano"),
    ENGLISH("en", "English"),
    SPANISH("es", "Espanol"),
    FRENCH("fr", "Francais"),
    GERMAN("de", "Deutsch"),
    PORTUGUESE("pt", "Portugues");

    companion object {
        fun fromStorage(value: String?): LanguageChoice {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("swingmusic.preferences", Context.MODE_PRIVATE)

    fun themeChoice(): ThemeChoice = ThemeChoice.fromStorage(prefs.getString(KEY_THEME, null))

    fun saveTheme(choice: ThemeChoice) {
        prefs.edit { putString(KEY_THEME, choice.storageValue) }
    }

    fun accentHex(): String = prefs.getString(KEY_ACCENT, null)
        ?.let { normalizeAccent(it) }
        ?: DEFAULT_ACCENT

    fun accentColor(): Int = Color.parseColor(accentHex())

    fun saveAccent(hex: String) {
        normalizeAccent(hex)?.let { prefs.edit { putString(KEY_ACCENT, it) } }
    }

    fun languageChoice(): LanguageChoice = LanguageChoice.fromStorage(prefs.getString(KEY_LANGUAGE, null))

    fun saveLanguage(choice: LanguageChoice) {
        prefs.edit { putString(KEY_LANGUAGE, choice.storageValue) }
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_ACCENT = "accent"
        private const val KEY_LANGUAGE = "language"
        const val DEFAULT_ACCENT = "#F4F4F4"
        val RECOMMENDED_ACCENTS = listOf(
            "#F4F4F4",
            "#7DD3FC",
            "#86EFAC",
            "#F9A8D4",
            "#FDE68A",
            "#C4B5FD"
        )

        fun nightModeFor(choice: ThemeChoice): Int {
            return when (choice) {
                ThemeChoice.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeChoice.DARK,
                ThemeChoice.OLED -> AppCompatDelegate.MODE_NIGHT_YES
            }
        }

        fun normalizeAccent(value: String): String? {
            val clean = value.trim().removePrefix("#")
            if (!Regex("^[0-9a-fA-F]{6}$").matches(clean)) return null
            return "#${clean.uppercase()}"
        }
    }
}
