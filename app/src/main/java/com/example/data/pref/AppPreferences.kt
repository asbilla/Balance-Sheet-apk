package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _webAppUrlFlow = MutableStateFlow(getWebAppUrl())
    val webAppUrlFlow: StateFlow<String> = _webAppUrlFlow.asStateFlow()

    fun getWebAppUrl(): String {
        return prefs.getString(KEY_WEB_APP_URL, "")?.trim() ?: ""
    }

    fun setWebAppUrl(url: String) {
        val trimmed = url.trim()
        prefs.edit().putString(KEY_WEB_APP_URL, trimmed).apply()
        _webAppUrlFlow.value = trimmed
    }

    fun isConfigured(): Boolean {
        return getWebAppUrl().isNotEmpty()
    }

    fun clearUrl() {
        prefs.edit().remove(KEY_WEB_APP_URL).apply()
        _webAppUrlFlow.value = ""
    }

    companion object {
        private const val PREF_NAME = "business_reporting_prefs"
        private const val KEY_WEB_APP_URL = "google_apps_script_url"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
