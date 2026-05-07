package iq.gov.smartkarbala.util

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("smart_karbala_prefs", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString("theme_mode", "system") ?: "system"
        set(v) = prefs.edit().putString("theme_mode", v).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(v) = prefs.edit().putBoolean("is_logged_in", v).apply()

    var userId: String
        get() = prefs.getString("user_id", "") ?: ""
        set(v) = prefs.edit().putString("user_id", v).apply()

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(v) = prefs.edit().putString("user_name", v).apply()

    var userNationalId: String
        get() = prefs.getString("national_id", "") ?: ""
        set(v) = prefs.edit().putString("national_id", v).apply()

    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric_enabled", false)
        set(v) = prefs.edit().putBoolean("biometric_enabled", v).apply()

    var lastWeatherUpdate: Long
        get() = prefs.getLong("last_weather_update", 0L)
        set(v) = prefs.edit().putLong("last_weather_update", v).apply()

    var cachedWeatherJson: String
        get() = prefs.getString("cached_weather", "") ?: ""
        set(v) = prefs.edit().putString("cached_weather", v).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(v) = prefs.edit().putBoolean("notifications_enabled", v).apply()

    var mapOfflineReady: Boolean
        get() = prefs.getBoolean("map_offline_ready", false)
        set(v) = prefs.edit().putBoolean("map_offline_ready", v).apply()

    fun logout() {
        val theme = themeMode
        prefs.edit().clear().apply()
        themeMode = theme
    }
}
