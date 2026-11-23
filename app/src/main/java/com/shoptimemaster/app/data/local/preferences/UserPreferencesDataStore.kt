package com.shoptimemaster.app.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shop_time_prefs")

data class UserPrefs(
    val language: String = "bn",
    val useBengaliNumerals: Boolean = true,
    val darkTheme: Boolean = true,
    val gpsEnabled: Boolean = false,
    val gpsPaused: Boolean = false,
    val gpsAutoConfirm: Boolean = false,
    val gpsConfirmationDelaySec: Int = 60,
    val onboardingDone: Boolean = false,
    val locationPromptSeen: Boolean = false,
    val biometricEnabled: Boolean = false
)

class UserPreferencesDataStore(private val context: Context) {
    private object Keys {
        val LANG = stringPreferencesKey("language")
        val BN_NUM = booleanPreferencesKey("bn_numerals")
        val DARK = booleanPreferencesKey("dark_theme")
        val GPS_ON = booleanPreferencesKey("gps_enabled")
        val GPS_PAUSED = booleanPreferencesKey("gps_paused")
        val GPS_AUTO = booleanPreferencesKey("gps_auto_confirm")
        val GPS_DELAY = intPreferencesKey("gps_delay_sec")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
        val LOC_PROMPT = booleanPreferencesKey("location_prompt_seen")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
    }

    val prefsFlow: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            language = p[Keys.LANG] ?: "bn",
            useBengaliNumerals = p[Keys.BN_NUM] ?: true,
            darkTheme = p[Keys.DARK] ?: true,
            gpsEnabled = p[Keys.GPS_ON] ?: false,
            gpsPaused = p[Keys.GPS_PAUSED] ?: false,
            gpsAutoConfirm = p[Keys.GPS_AUTO] ?: false,
            gpsConfirmationDelaySec = p[Keys.GPS_DELAY] ?: 60,
            onboardingDone = p[Keys.ONBOARDED] ?: false,
            locationPromptSeen = p[Keys.LOC_PROMPT] ?: false,
            biometricEnabled = p[Keys.BIOMETRIC] ?: false
        )
    }

    suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setLanguage(lang: String) = update { it[Keys.LANG] = lang }
    suspend fun setBengaliNumerals(v: Boolean) = update { it[Keys.BN_NUM] = v }
    suspend fun setDarkTheme(v: Boolean) = update { it[Keys.DARK] = v }
    suspend fun setGpsEnabled(v: Boolean) = update { it[Keys.GPS_ON] = v }
    suspend fun setGpsPaused(v: Boolean) = update { it[Keys.GPS_PAUSED] = v }
    suspend fun setGpsAutoConfirm(v: Boolean) = update { it[Keys.GPS_AUTO] = v }
    suspend fun setGpsDelay(sec: Int) = update { it[Keys.GPS_DELAY] = sec }
    suspend fun setOnboardingDone(v: Boolean) = update { it[Keys.ONBOARDED] = v }
    suspend fun setLocationPromptSeen(v: Boolean) = update { it[Keys.LOC_PROMPT] = v }
    suspend fun setBiometricEnabled(v: Boolean) = update { it[Keys.BIOMETRIC] = v }
}
