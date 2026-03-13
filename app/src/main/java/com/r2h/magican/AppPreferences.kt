package com.r2h.magican

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val themeModeValue: Flow<String> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[PreferenceKeys.themeMode] ?: DEFAULT_THEME_MODE
    }

    val lastRoute: Flow<String?> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[PreferenceKeys.lastRoute]
    }

    suspend fun setThemeMode(value: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferenceKeys.themeMode] = value
        }
    }

    suspend fun setLastRoute(route: String) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[PreferenceKeys.lastRoute] = route
        }
    }

    private object PreferenceKeys {
        val themeMode = stringPreferencesKey("theme_mode")
        val lastRoute = stringPreferencesKey("last_route")
    }

    enum class ThemeModeValue { System, Dark, Light }

    companion object {
        const val DEFAULT_THEME_MODE = "system"

        fun String?.toThemeModeValue(): ThemeModeValue = when (this) {
            "dark"  -> ThemeModeValue.Dark
            "light" -> ThemeModeValue.Light
            else    -> ThemeModeValue.System
        }

        fun ThemeModeValue.toPreferenceString(): String = when (this) {
            ThemeModeValue.System -> "system"
            ThemeModeValue.Dark   -> "dark"
            ThemeModeValue.Light  -> "light"
        }
    }
}
