package com.xenon.store

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.unit.IntSize
import androidx.core.content.edit
import com.xenon.store.viewmodel.ThemeSetting
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

class SharedPreferenceManager(context: Context) {

    private val prefsName = "StorePrefs"
    private val themeKey = "app_theme"
    private val coverThemeEnabledKey = "cover_theme_enabled"
    private val coverDisplayDimension1Key = "cover_display_dimension_1"
    private val coverDisplayDimension2Key = "cover_display_dimension_2"
    private val taskListKey = "task_list_json"
    private val drawerTodoItemsKey = "drawer_todo_items_json"
    private val blackedOutModeKey = "blacked_out_mode_enabled"
    private val dateFormatKey = "date_format_key"
    private val timeFormatKey = "time_format_key"
    private val developerModeKey = "developer_mode_enabled"
    // New key for the dummy profile setting
    private val showDummyProfileKey = "show_dummy_profile_enabled"
    private val checkForPreReleasesKey = "check_for_pre_releases" // Added key

    internal val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val defaultDateFormat = "yyyy-MM-dd"
    private val defaultTimeFormat = "HH:mm"

    var theme: Int
        get() = sharedPreferences.getInt(themeKey, ThemeSetting.SYSTEM.ordinal)
        set(value) = sharedPreferences.edit { putInt(themeKey, value) }

    val themeFlag: Array<Int> = arrayOf(
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )

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


    var blackedOutModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(blackedOutModeKey, false)
        set(value) = sharedPreferences.edit { putBoolean(blackedOutModeKey, value) }

    var dateFormat: String
        get() = sharedPreferences.getString(dateFormatKey, defaultDateFormat) ?: defaultDateFormat
        set(value) = sharedPreferences.edit { putString(dateFormatKey, value) }

    var timeFormat: String
        get() = sharedPreferences.getString(timeFormatKey, defaultTimeFormat) ?: defaultTimeFormat
        set(value) = sharedPreferences.edit { putString(timeFormatKey, value) }

    var developerModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(developerModeKey, false)
        set(value) = sharedPreferences.edit { putBoolean(developerModeKey, value) }

    var showDummyProfileEnabled: Boolean
        get() = sharedPreferences.getBoolean(showDummyProfileKey, false)
        set(value) = sharedPreferences.edit { putBoolean(showDummyProfileKey, value) }

    var checkForPreReleases: Boolean // Added property
        get() = sharedPreferences.getBoolean(checkForPreReleasesKey, false)
        set(value) = sharedPreferences.edit { putBoolean(checkForPreReleasesKey, value) }


    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    fun getString(key: String, defaultValue: String?): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun putString(key: String, value: String?) {
        sharedPreferences.edit { putString(key, value) }
    }


    fun isCoverThemeApplied(currentDisplaySize: IntSize): Boolean {
        if (!coverThemeEnabled) return false
        val storedDimension1 = sharedPreferences.getInt(coverDisplayDimension1Key, 0)
        val storedDimension2 = sharedPreferences.getInt(coverDisplayDimension2Key, 0)
        if (storedDimension1 == 0 || storedDimension2 == 0) return false
        val currentDimension1 = min(currentDisplaySize.width, currentDisplaySize.height)
        val currentDimension2 = max(currentDisplaySize.width, currentDisplaySize.height)
        return currentDimension1 == storedDimension1 && currentDimension2 == storedDimension2
    }

    fun clearSettings() {
        sharedPreferences.edit {
            putInt(themeKey, ThemeSetting.SYSTEM.ordinal)
            putBoolean(coverThemeEnabledKey, false)
            remove(coverDisplayDimension1Key)
            remove(coverDisplayDimension2Key)
            putBoolean(blackedOutModeKey, false)
            putString(dateFormatKey, defaultDateFormat)
            putString(timeFormatKey, defaultTimeFormat)
            putBoolean(developerModeKey, false)
            putBoolean(showDummyProfileKey, false)
            putBoolean(checkForPreReleasesKey, false) // Added reset
        }
    }
}
