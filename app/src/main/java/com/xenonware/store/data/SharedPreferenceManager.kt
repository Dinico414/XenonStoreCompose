package com.xenonware.store.data

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.unit.IntSize
import androidx.core.content.edit
import com.xenonware.store.viewmodel.ThemeSetting
import com.xenonware.store.viewmodel.classes.StoreItem
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

enum class InstallMethod {
    DEFAULT,
    SHIZUKU,
    ROOT
}

class SharedPreferenceManager(context: Context) {

    companion object {
        const val PREFS_NAME = "StorePrefs"
        const val KEY_IS_USER_LOGGED_IN = "is_user_logged_in"
        const val KEY_GOOGLE_USER_ID = "google_user_id"
        const val KEY_GOOGLE_USERNAME = "google_username"
        const val KEY_GOOGLE_EMAIL = "google_email"
        const val KEY_GOOGLE_PHOTO_URL = "google_photo_url"

        const val KEY_IS_GITHUB_LOGGED_IN = "is_github_logged_in"
        const val KEY_GITHUB_TOKEN = "github_token"
        const val KEY_GITHUB_USERNAME = "github_username"
        const val KEY_GITHUB_AVATAR_URL = "github_avatar_url"
        const val KEY_THEME = "app_theme"
        const val KEY_BLACKED_OUT_MODE = "blacked_out_mode_enabled"
        const val KEY_COVER_THEME_ENABLED = "cover_theme_enabled"
        const val KEY_COVER_DISPLAY_DIMENSION_1 = "cover_display_dimension_1"
        const val KEY_COVER_DISPLAY_DIMENSION_2 = "cover_display_dimension_2"
        const val KEY_LANGUAGE_TAG = "app_language_tag"
        const val KEY_DEVELOPER_MODE = "developer_mode_enabled"
        const val KEY_SHOW_DUMMY_PROFILE = "show_dummy_profile_enabled"
        const val KEY_ADD_BUTTON_STATE = "add_button_state_enabled"
        const val KEY_CHECK_FOR_PRE_RELEASES = "check_for_pre_releases"
        const val KEY_INSTALL_METHOD = "install_method"
        const val KEY_CUSTOM_STORE_ITEMS = "custom_store_items"
        const val KEY_CACHED_CLOUD_STORE_ITEMS = "cached_cloud_store_items"
        const val KEY_DOWNLOADED_FILES_UPDATED = "downloaded_files_updated"
    }

    private val prefsName = PREFS_NAME
    private val isUserLoggedInKey = KEY_IS_USER_LOGGED_IN
    private val googleUserIdKey = KEY_GOOGLE_USER_ID
    private val googleUsernameKey = KEY_GOOGLE_USERNAME
    private val googleEmailKey = KEY_GOOGLE_EMAIL
    private val googlePhotoUrlKey = KEY_GOOGLE_PHOTO_URL

    private val isGitHubLoggedInKey = KEY_IS_GITHUB_LOGGED_IN
    private val githubTokenKey = KEY_GITHUB_TOKEN
    private val githubUsernameKey = KEY_GITHUB_USERNAME
    private val githubAvatarUrlKey = KEY_GITHUB_AVATAR_URL
    private val themeKey = KEY_THEME
    private val blackedOutModeKey = KEY_BLACKED_OUT_MODE
    private val coverThemeEnabledKey = KEY_COVER_THEME_ENABLED
    private val coverDisplayDimension1Key = KEY_COVER_DISPLAY_DIMENSION_1
    private val coverDisplayDimension2Key = KEY_COVER_DISPLAY_DIMENSION_2
    private val languageTagKey = KEY_LANGUAGE_TAG
    private val developerModeKey = KEY_DEVELOPER_MODE
    private val showDummyProfileKey = KEY_SHOW_DUMMY_PROFILE
    private val addButtonStateKey = KEY_ADD_BUTTON_STATE
    private val checkForPreReleasesKey = KEY_CHECK_FOR_PRE_RELEASES
    private val installMethodKey = KEY_INSTALL_METHOD
    private val customStoreItemsKey = KEY_CUSTOM_STORE_ITEMS
    private val cachedCloudStoreItemsKey = KEY_CACHED_CLOUD_STORE_ITEMS

    internal val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    var isUserLoggedIn: Boolean
        get() = sharedPreferences.getBoolean(isUserLoggedInKey, false)
        set(value) = sharedPreferences.edit { putBoolean(isUserLoggedInKey, value) }

    var googleUserId: String
        get() = sharedPreferences.getString(googleUserIdKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(googleUserIdKey, value) }

    var googleUsername: String
        get() = sharedPreferences.getString(googleUsernameKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(googleUsernameKey, value) }

    var googleEmail: String
        get() = sharedPreferences.getString(googleEmailKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(googleEmailKey, value) }

    var googlePhotoUrl: String
        get() = sharedPreferences.getString(googlePhotoUrlKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(googlePhotoUrlKey, value) }

    fun clearGoogleUser() {
        sharedPreferences.edit {
            putBoolean(isUserLoggedInKey, false)
            remove(googleUserIdKey)
            remove(googleUsernameKey)
            remove(googleEmailKey)
            remove(googlePhotoUrlKey)
        }
    }

    var isGitHubLoggedIn: Boolean
        get() = sharedPreferences.getBoolean(isGitHubLoggedInKey, false)
        set(value) = sharedPreferences.edit { putBoolean(isGitHubLoggedInKey, value) }

    var githubToken: String
        get() = sharedPreferences.getString(githubTokenKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(githubTokenKey, value) }

    var githubUsername: String
        get() = sharedPreferences.getString(githubUsernameKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(githubUsernameKey, value) }

    var githubAvatarUrl: String
        get() = sharedPreferences.getString(githubAvatarUrlKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(githubAvatarUrlKey, value) }

    var theme: Int
        get() = sharedPreferences.getInt(themeKey, ThemeSetting.SYSTEM.ordinal)
        set(value) = sharedPreferences.edit { putInt(themeKey, value) }

    val themeFlag: Array<Int> = arrayOf(
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )

    var blackedOutModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(blackedOutModeKey, false)
        set(value) = sharedPreferences.edit { putBoolean(blackedOutModeKey, value) }

    var coverThemeEnabled: Boolean
        get() = sharedPreferences.getBoolean(coverThemeEnabledKey, false)
        set(value) = sharedPreferences.edit { putBoolean(coverThemeEnabledKey, value) }

    var coverDisplaySize: IntSize
        get() {
            val dim1 = sharedPreferences.getInt(coverDisplayDimension1Key, 0)
            val dim2 = sharedPreferences.getInt(coverDisplayDimension2Key, 0)
            return IntSize(dim1, dim2)
        }
        set(value) {
            sharedPreferences.edit {
                putInt(coverDisplayDimension1Key, min(value.width, value.height))
                putInt(coverDisplayDimension2Key, max(value.width, value.height))
            }
        }

    var languageTag: String
        get() = sharedPreferences.getString(languageTagKey, "") ?: ""
        set(value) = sharedPreferences.edit { putString(languageTagKey, value) }

    var checkForPreReleases: Boolean
        get() = sharedPreferences.getBoolean(checkForPreReleasesKey, false)
        set(value) = sharedPreferences.edit { putBoolean(checkForPreReleasesKey, value) }

    var developerModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(developerModeKey, false)
        set(value) = sharedPreferences.edit { putBoolean(developerModeKey, value) }

    var addButtonEnabled: Boolean
        get() = sharedPreferences.getBoolean(addButtonStateKey, false)
        set(value) = sharedPreferences.edit { putBoolean(addButtonStateKey, value) }

    var installMethod: InstallMethod
        get() {
            val methodName = sharedPreferences.getString(installMethodKey, InstallMethod.DEFAULT.name)
            return try {
                InstallMethod.valueOf(methodName ?: InstallMethod.DEFAULT.name)
            } catch (_: IllegalArgumentException) {
                InstallMethod.DEFAULT
            }
        }
        set(value) = sharedPreferences.edit { putString(installMethodKey, value.name) }

    fun isCoverThemeApplied(currentDisplaySize: IntSize): Boolean {
        if (!coverThemeEnabled) return false
        val storedDimension1 = sharedPreferences.getInt(coverDisplayDimension1Key, 0)
        val storedDimension2 = sharedPreferences.getInt(coverDisplayDimension2Key, 0)
        if (storedDimension1 == 0 || storedDimension2 == 0) return false
        val currentDimension1 = min(currentDisplaySize.width, currentDisplaySize.height)
        val currentDimension2 = max(currentDisplaySize.width, currentDisplaySize.height)
        return currentDimension1 == storedDimension1 && currentDimension2 == storedDimension2
    }

    fun saveCustomStoreItems(items: List<StoreItem>) {
        val jsonString = json.encodeToString(items)
        sharedPreferences.edit { putString(customStoreItemsKey, jsonString) }
    }

    fun loadCustomStoreItems(): List<StoreItem> {
        val jsonString = sharedPreferences.getString(customStoreItemsKey, null)
        return if (!jsonString.isNullOrEmpty()) {
            try {
                json.decodeFromString<List<StoreItem>>(jsonString)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveCachedCloudStoreItems(items: List<StoreItem>) {
        val jsonString = json.encodeToString(items)
        sharedPreferences.edit { putString(cachedCloudStoreItemsKey, jsonString) }
    }

    fun loadCachedCloudStoreItems(): List<StoreItem> {
        val jsonString = sharedPreferences.getString(cachedCloudStoreItemsKey, null)
        return if (!jsonString.isNullOrEmpty()) {
            try {
                json.decodeFromString<List<StoreItem>>(jsonString)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun clearSettings() {
        sharedPreferences.edit {
            putInt(themeKey, ThemeSetting.SYSTEM.ordinal)
            putBoolean(coverThemeEnabledKey, false)
            remove(coverDisplayDimension1Key)
            remove(coverDisplayDimension2Key)
            putBoolean(blackedOutModeKey, false)
            putBoolean(developerModeKey, false)
            putBoolean(showDummyProfileKey, false)
            putBoolean(checkForPreReleasesKey, false)
            putString(installMethodKey, InstallMethod.DEFAULT.name)
        }
    }
}