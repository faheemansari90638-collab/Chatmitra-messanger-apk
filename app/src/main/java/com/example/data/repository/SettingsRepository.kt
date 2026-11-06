package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import com.example.data.model.HamzaAiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "chatmitra_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val USE_SYSTEM_THEME = booleanPreferencesKey("use_system_theme")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val LAST_SEEN_VISIBILITY = stringPreferencesKey("last_seen_visibility")
        val PROFILE_PHOTO_VISIBILITY = stringPreferencesKey("profile_photo_visibility")
        val READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val CHAT_WALLPAPER = stringPreferencesKey("chat_wallpaper")
        val LAST_BACKUP_TIMESTAMP = stringPreferencesKey("last_backup_timestamp")
        val LAST_BACKUP_SIZE = stringPreferencesKey("last_backup_size")
        val DATA_SAVER_WIFI = booleanPreferencesKey("data_saver_wifi")
        val DATA_SAVER_MOBILE = booleanPreferencesKey("data_saver_mobile")
        val LOW_DATA_CALLS = booleanPreferencesKey("low_data_calls")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val HIDE_SENSITIVE_NOTIF = booleanPreferencesKey("hide_sensitive_notif")
        val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")

        // Hamza AI Preferences
        val AI_TONE = stringPreferencesKey("ai_tone")
        val AI_RESPONSE_LENGTH = stringPreferencesKey("ai_response_length")
        val AI_EMOJI_LEVEL = stringPreferencesKey("ai_emoji_level")
        val AI_DEFAULT_LANG = stringPreferencesKey("ai_default_lang")
        val AI_POLISHING_STYLE = stringPreferencesKey("ai_polishing_style")
        val AI_CUSTOM_INSTRUCTIONS = stringPreferencesKey("ai_custom_instructions")
        val AI_GRAMMAR_EXPLANATION = booleanPreferencesKey("ai_grammar_explanation")
    }

    val hamzaAiConfigFlow: Flow<HamzaAiConfig> = context.dataStore.data.map { prefs ->
        HamzaAiConfig(
            tone = prefs[PreferencesKeys.AI_TONE] ?: "Friendly & Warm",
            responseLength = prefs[PreferencesKeys.AI_RESPONSE_LENGTH] ?: "Balanced",
            emojiLevel = prefs[PreferencesKeys.AI_EMOJI_LEVEL] ?: "Moderate",
            defaultLanguage = prefs[PreferencesKeys.AI_DEFAULT_LANG] ?: "English",
            smartPolishingStyle = prefs[PreferencesKeys.AI_POLISHING_STYLE] ?: "Casual & Friendly",
            customInstructions = prefs[PreferencesKeys.AI_CUSTOM_INSTRUCTIONS] ?: "",
            grammarExplanation = prefs[PreferencesKeys.AI_GRAMMAR_EXPLANATION] ?: true
        )
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isDarkMode = prefs[PreferencesKeys.DARK_MODE] ?: false,
            useSystemTheme = prefs[PreferencesKeys.USE_SYSTEM_THEME] ?: true,
            isAppLockEnabled = prefs[PreferencesKeys.APP_LOCK_ENABLED] ?: false,
            lockTimeout = prefs[PreferencesKeys.LOCK_TIMEOUT] ?: "immediately",
            pinHash = prefs[PreferencesKeys.PIN_HASH],
            biometricEnabled = prefs[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
            lastSeenVisibility = prefs[PreferencesKeys.LAST_SEEN_VISIBILITY] ?: "everyone",
            profilePhotoVisibility = prefs[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] ?: "everyone",
            readReceiptsEnabled = prefs[PreferencesKeys.READ_RECEIPTS] ?: true,
            fontSize = prefs[PreferencesKeys.FONT_SIZE] ?: "medium",
            chatWallpaper = prefs[PreferencesKeys.CHAT_WALLPAPER] ?: "default",
            mediaAutoDownloadWifi = prefs[PreferencesKeys.DATA_SAVER_WIFI] ?: true,
            mediaAutoDownloadMobile = prefs[PreferencesKeys.DATA_SAVER_MOBILE] ?: false,
            lowDataUsageCalls = prefs[PreferencesKeys.LOW_DATA_CALLS] ?: false,
            imageUploadQuality = prefs[PreferencesKeys.IMAGE_QUALITY] ?: "standard",
            hideSensitiveNotifications = prefs[PreferencesKeys.HIDE_SENSITIVE_NOTIF] ?: false,
            blockUnknownSenders = prefs[PreferencesKeys.BLOCK_UNKNOWN] ?: false,
            screenshotProtection = prefs[PreferencesKeys.SCREENSHOT_PROTECTION] ?: false
        )
    }

    suspend fun updateDataSaver(wifiAuto: Boolean, mobileAuto: Boolean, lowDataCalls: Boolean, imageQuality: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DATA_SAVER_WIFI] = wifiAuto
            prefs[PreferencesKeys.DATA_SAVER_MOBILE] = mobileAuto
            prefs[PreferencesKeys.LOW_DATA_CALLS] = lowDataCalls
            prefs[PreferencesKeys.IMAGE_QUALITY] = imageQuality
        }
    }

    suspend fun updateAdvancedPrivacy(hideNotif: Boolean, blockUnknown: Boolean, screenshotProtection: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HIDE_SENSITIVE_NOTIF] = hideNotif
            prefs[PreferencesKeys.BLOCK_UNKNOWN] = blockUnknown
            prefs[PreferencesKeys.SCREENSHOT_PROTECTION] = screenshotProtection
        }
    }

    suspend fun updateHamzaAiConfig(config: HamzaAiConfig) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.AI_TONE] = config.tone
            prefs[PreferencesKeys.AI_RESPONSE_LENGTH] = config.responseLength
            prefs[PreferencesKeys.AI_EMOJI_LEVEL] = config.emojiLevel
            prefs[PreferencesKeys.AI_DEFAULT_LANG] = config.defaultLanguage
            prefs[PreferencesKeys.AI_POLISHING_STYLE] = config.smartPolishingStyle
            prefs[PreferencesKeys.AI_CUSTOM_INSTRUCTIONS] = config.customInstructions
            prefs[PreferencesKeys.AI_GRAMMAR_EXPLANATION] = config.grammarExplanation
        }
    }

    suspend fun resetHamzaAiConfig() {
        updateHamzaAiConfig(HamzaAiConfig())
    }

    suspend fun setTheme(useSystem: Boolean, isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.USE_SYSTEM_THEME] = useSystem
            prefs[PreferencesKeys.DARK_MODE] = isDark
        }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String? = null, biometric: Boolean = false, timeout: String = "immediately") {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LOCK_ENABLED] = enabled
            prefs[PreferencesKeys.BIOMETRIC_ENABLED] = biometric
            prefs[PreferencesKeys.LOCK_TIMEOUT] = timeout
            if (pin != null) {
                prefs[PreferencesKeys.PIN_HASH] = hashPin(pin)
            }
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val currentHash = hashPin(pin)
        var matches = false
        context.dataStore.edit { prefs ->
            val stored = prefs[PreferencesKeys.PIN_HASH]
            matches = (stored == currentHash)
        }
        return matches
    }

    suspend fun updatePrivacy(lastSeen: String, profilePhoto: String, readReceipts: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_SEEN_VISIBILITY] = lastSeen
            prefs[PreferencesKeys.PROFILE_PHOTO_VISIBILITY] = profilePhoto
            prefs[PreferencesKeys.READ_RECEIPTS] = readReceipts
        }
    }

    suspend fun updateChatAppearance(fontSize: String, wallpaper: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FONT_SIZE] = fontSize
            prefs[PreferencesKeys.CHAT_WALLPAPER] = wallpaper
        }
    }

    fun calculateStorageUsage(): Map<String, String> {
        val cacheDir = context.cacheDir
        val filesDir = context.filesDir
        val cacheSize = getFolderSize(cacheDir)
        val filesSize = getFolderSize(filesDir)
        val total = cacheSize + filesSize

        return mapOf(
            "total" to formatBytes(total),
            "cache" to formatBytes(cacheSize),
            "media" to formatBytes(filesSize * 2 / 3),
            "chats" to formatBytes(filesSize / 3)
        )
    }

    fun clearCache() {
        context.cacheDir.deleteRecursively()
    }

    suspend fun recordBackup(size: String, timestamp: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_BACKUP_SIZE] = size
            prefs[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    private fun getFolderSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
