package iq.gov.smartkarbala

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import iq.gov.smartkarbala.data.db.AppDatabase
import iq.gov.smartkarbala.util.PrefsManager

class SmartKarbalaApp : Application() {

    companion object {
        lateinit var instance: SmartKarbalaApp
            private set

        fun context(): Context = instance.applicationContext
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        applyThemePreference()
    }

    private fun applyThemePreference() {
        val prefs = PrefsManager(this)
        val mode = when (prefs.themeMode) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
