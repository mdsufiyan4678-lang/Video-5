package com.example.universavideodownloader.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isPremium = MutableStateFlow(isPremium())
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _wifiOnly = MutableStateFlow(isWifiOnly())
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(isNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun isPremium(): Boolean {
        return prefs.getBoolean(KEY_IS_PREMIUM, false)
    }

    fun setPremium(premium: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PREMIUM, premium).apply()
        _isPremium.value = premium
    }

    fun isWifiOnly(): Boolean {
        return prefs.getBoolean(KEY_WIFI_ONLY, false)
    }

    fun setWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
        _wifiOnly.value = enabled
    }

    fun isNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun getDownloadDirectoryName(): String {
        return "UniversalVideoDownloader"
    }

    enum class ThemeMode {
        SYSTEM,
        LIGHT,
        DARK
    }

    companion object {
        private const val PREFS_NAME = "universal_video_downloader_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_IS_PREMIUM = "key_is_premium"
        private const val KEY_WIFI_ONLY = "key_wifi_only"
        private const val KEY_NOTIFICATIONS = "key_notifications"

        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
