package com.xenonware.store.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.unit.IntSize
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xenon.mylibrary.res.LanguageOption
import com.xenonware.store.R
import com.xenonware.store.data.SharedPreferenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemeSetting(val title: String, val nightModeFlag: Int) {
    LIGHT("Light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("Dark", AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM("System", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}

enum class LayoutType {
    COVER, SMALL, COMPACT, MEDIUM, EXPANDED
}

data class FormatOption(val displayName: String, val pattern: String)

private fun getCurrentDateTimeFormatted(pattern: String): String {
    return try {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.format(Date())
    } catch (_: IllegalArgumentException) {
        "Preview unavailable"
    }
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val sharedPreferenceManager = SharedPreferenceManager(application)

    val themeOptions = ThemeSetting.entries.toTypedArray()

    private val _showSignOutDialog = MutableStateFlow(false)
    val showSignOutDialog: StateFlow<Boolean> = _showSignOutDialog

    private val _persistedThemeIndexFlow = MutableStateFlow(sharedPreferenceManager.theme)
    val persistedThemeIndex: StateFlow<Int> = _persistedThemeIndexFlow.asStateFlow()

    private val _dialogPreviewThemeIndex = MutableStateFlow(sharedPreferenceManager.theme)
    val dialogPreviewThemeIndex: StateFlow<Int> = _dialogPreviewThemeIndex.asStateFlow()

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog: StateFlow<Boolean> = _showThemeDialog.asStateFlow()

    private val _currentThemeTitleFlow =
        MutableStateFlow(themeOptions.getOrElse(sharedPreferenceManager.theme) { themeOptions.first() }.title)
    val currentThemeTitle: StateFlow<String> = _currentThemeTitleFlow.asStateFlow()

    private val _blackedOutModeEnabled =
        MutableStateFlow(sharedPreferenceManager.blackedOutModeEnabled)
    val blackedOutModeEnabled: StateFlow<Boolean> = _blackedOutModeEnabled.asStateFlow()

    private val _showCoverSelectionDialog = MutableStateFlow(false)
    val showCoverSelectionDialog: StateFlow<Boolean> = _showCoverSelectionDialog.asStateFlow()

    private val _enableCoverTheme = MutableStateFlow(sharedPreferenceManager.coverThemeEnabled)
    val enableCoverTheme: StateFlow<Boolean> = _enableCoverTheme.asStateFlow()

    private val _currentLanguage = MutableStateFlow(getCurrentLocaleDisplayName())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<LanguageOption>>(emptyList())
    val availableLanguages: StateFlow<List<LanguageOption>> = _availableLanguages.asStateFlow()

    private val _selectedLanguageTagInDialog = MutableStateFlow(getAppLocaleTag())
    val selectedLanguageTagInDialog: StateFlow<String> = _selectedLanguageTagInDialog.asStateFlow()

    private val _checkForPreReleases = MutableStateFlow(sharedPreferenceManager.checkForPreReleases)
    val checkForPreReleases: StateFlow<Boolean> = _checkForPreReleases.asStateFlow()

    private val _showClearDataDialog = MutableStateFlow(false)
    val showClearDataDialog: StateFlow<Boolean> = _showClearDataDialog.asStateFlow()

    private val _showResetSettingsDialog = MutableStateFlow(false)
    val showResetSettingsDialog: StateFlow<Boolean> = _showResetSettingsDialog.asStateFlow()

    private val _showVersionDialog = MutableStateFlow(false)
    val showVersionDialog: StateFlow<Boolean> = _showVersionDialog.asStateFlow()

    private val _developerModeEnabled =
        MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val developerModeEnabled: StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

    // derived state
    val activeNightModeFlag: StateFlow<Int> = combine(
        _persistedThemeIndexFlow, _dialogPreviewThemeIndex, _showThemeDialog
    ) { persistedIndex, previewIndex, isDialogShowing ->
        val themeIndexToUse = if (isDialogShowing) previewIndex else persistedIndex
        themeOptions.getOrElse(themeIndexToUse) { themeOptions.first { it == ThemeSetting.SYSTEM } }.nightModeFlag
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = themeOptions.getOrElse(sharedPreferenceManager.theme) { themeOptions.first { it == ThemeSetting.SYSTEM } }.nightModeFlag
    )

    val availableDateFormats = listOf(
        FormatOption(
            "System Default (${getCurrentDateTimeFormatted(getSystemShortDatePattern())})",
            getSystemShortDatePattern()
        ),
        FormatOption("YY-MM-DD (${getCurrentDateTimeFormatted("yy-MM-dd")})", "yy-MM-dd"),
        FormatOption("DD/MM/YY (${getCurrentDateTimeFormatted("dd/MM/yy")})", "dd/MM/yy"),
        FormatOption("MM/DD/YY (${getCurrentDateTimeFormatted("MM/dd/yy")})", "MM/dd/yy"),
        FormatOption("DD.MM.YY (${getCurrentDateTimeFormatted("dd.MM.yy")})", "dd.MM.yy"),
    )

    // ── Tap counter / developer mode helpers ─────────────────────────────────────

    private var infoTileTapCount = 0
    private var singleTapJob: Job? = null
    private var resetTapsJob: Job? = null
    private val requiredTaps = 7
    private val tapTimeoutMillis = 500L
    private var lastMultiTapTime: Long = 0
    private val multiTapCooldownMillis = 500L

    private var currentToast: Toast? = null

    // ── init ─────────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            activeNightModeFlag.collect { nightMode ->
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        }
        viewModelScope.launch {
            _persistedThemeIndexFlow.collect { index ->
                _currentThemeTitleFlow.value =
                    themeOptions.getOrElse(index) { themeOptions.first() }.title
            }
        }
        updateCurrentLanguage()
        prepareLanguageOptions()
    }

    // ── Public functions (roughly in requested UI order) ─────────────────────────
    // Sign in / Sign out
    fun onSignOutClicked() {
        _showSignOutDialog.value = true
    }

    fun dismissSignOutDialog() {
        _showSignOutDialog.value = false
    }

    // Theme
    fun onThemeSettingClicked() {
        _dialogPreviewThemeIndex.value = _persistedThemeIndexFlow.value
        _showThemeDialog.value = true
    }

    fun onThemeOptionSelectedInDialog(index: Int) {
        if (index >= 0 && index < themeOptions.size) {
            _dialogPreviewThemeIndex.value = index
            _persistedThemeIndexFlow.value = index
        }
    }

    fun applySelectedTheme() {
        val indexToApply = _dialogPreviewThemeIndex.value
        if (indexToApply >= 0 && indexToApply < themeOptions.size) {
            sharedPreferenceManager.theme = indexToApply
            _persistedThemeIndexFlow.value = indexToApply
        }
        _showThemeDialog.value = false
    }

    fun dismissThemeDialog() {
        _showThemeDialog.value = false
        _dialogPreviewThemeIndex.value = sharedPreferenceManager.theme
        _persistedThemeIndexFlow.value = sharedPreferenceManager.theme
    }

    // Blacked out mode
    fun setBlackedOutEnabled(enabled: Boolean) {
        sharedPreferenceManager.blackedOutModeEnabled = enabled
        _blackedOutModeEnabled.value = enabled
    }

    // Cover mode
    fun onCoverThemeClicked() {
        _showCoverSelectionDialog.value = true
    }

    fun setCoverThemeEnabled(enabled: Boolean) {
        sharedPreferenceManager.coverThemeEnabled = enabled
        _enableCoverTheme.value = enabled
    }

    fun dismissCoverThemeDialog() {
        _showCoverSelectionDialog.value = false
    }

    fun saveCoverDisplayMetrics(displaySize: IntSize) {
        sharedPreferenceManager.coverDisplaySize = displaySize
        _enableCoverTheme.value = true
        sharedPreferenceManager.coverThemeEnabled = true
        _showCoverSelectionDialog.value = false
    }

    fun applyCoverTheme(displaySize: IntSize): Boolean {
        return sharedPreferenceManager.isCoverThemeApplied(displaySize)
    }

    // Language
    fun onLanguageSettingClicked(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open language settings", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            _selectedLanguageTagInDialog.value =
                sharedPreferenceManager.languageTag.ifEmpty { getAppLocaleTag() }
            _showLanguageDialog.value = true
        }
    }

    fun onLanguageSelectedInDialog(localeTag: String) {
        _selectedLanguageTagInDialog.value = localeTag
    }

    fun applySelectedLanguage() {
        val selectedTag = _selectedLanguageTagInDialog.value

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            setAppLocale(selectedTag)
            sharedPreferenceManager.languageTag = selectedTag
            viewModelScope.launch {
                delay(500)
                restartApplication(getApplication())
            }
        }

        _showLanguageDialog.value = false
        updateCurrentLanguage()
    }


    fun dismissLanguageDialog() {
        _showLanguageDialog.value = false
        _selectedLanguageTagInDialog.value = getAppLocaleTag()
    }

    fun refreshLanguage() {
        updateCurrentLanguage()
    }

    fun updateCurrentLanguage() {
        _currentLanguage.value = getCurrentLocaleDisplayName()
        _selectedLanguageTagInDialog.value = getAppLocaleTag()
        refreshDeveloperModeState()
    }

    // ReleaseType
    fun setCheckForPreReleases(enabled: Boolean) {
        sharedPreferenceManager.checkForPreReleases = enabled
        _checkForPreReleases.value = enabled
    }

    // Clear data
    fun onClearDataClicked() {
        _showClearDataDialog.value = true
    }

    fun confirmClearData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val activityManager =
                    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val success = activityManager.clearApplicationUserData()
                if (success) {
                    val defaultThemeIndex =
                        themeOptions.indexOfFirst { it == ThemeSetting.SYSTEM }.takeIf { it != -1 }
                            ?: ThemeSetting.SYSTEM.ordinal
                    sharedPreferenceManager.theme = defaultThemeIndex
                    _persistedThemeIndexFlow.value = defaultThemeIndex
                    _dialogPreviewThemeIndex.value = defaultThemeIndex
                    sharedPreferenceManager.blackedOutModeEnabled = false
                    _blackedOutModeEnabled.value = false
                    sharedPreferenceManager.coverThemeEnabled = false
                    _enableCoverTheme.value = false
                    sharedPreferenceManager.developerModeEnabled = false
                    _developerModeEnabled.value = false
                    sharedPreferenceManager.checkForPreReleases = false
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        setAppLocale("")
                    }
                    updateCurrentLanguage()
                    restartApplication(context)
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_clearing_data_failed),
                        Toast.LENGTH_LONG
                    ).show()
                    openAppInfo(context)
                }
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_clearing_data_permission),
                    Toast.LENGTH_LONG
                ).show()
                openAppInfo(context)
                e.printStackTrace()
            } finally {
                refreshDeveloperModeState()
                _showClearDataDialog.value = false
            }
        }
    }

    fun dismissClearDataDialog() {
        _showClearDataDialog.value = false
    }

    // Reset app / settings
    fun onResetSettingsClicked() {
        _showResetSettingsDialog.value = true
    }

    fun confirmResetSettings() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            sharedPreferenceManager.clearSettings()

            val defaultThemeIndex = ThemeSetting.SYSTEM.ordinal
            _persistedThemeIndexFlow.value = defaultThemeIndex
            _dialogPreviewThemeIndex.value = defaultThemeIndex
            _blackedOutModeEnabled.value = sharedPreferenceManager.blackedOutModeEnabled
            _enableCoverTheme.value = sharedPreferenceManager.coverThemeEnabled
            refreshDeveloperModeState()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                setAppLocale("")
            }
            updateCurrentLanguage()
            _showResetSettingsDialog.value = false
            delay(1000)
            restartApplication(context)
        }
    }

    fun dismissResetSettingsDialog() {
        _showResetSettingsDialog.value = false
    }

    // Info
    fun onInfoTileClicked() {
        val context = getApplication<Application>().applicationContext
        currentToast?.cancel()
        singleTapJob?.cancel()
        resetTapsJob?.cancel()

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastMultiTapTime < multiTapCooldownMillis) {
            if (_developerModeEnabled.value) {
                currentToast = Toast.makeText(
                    context,
                    context.getString(R.string.already_in_developer_mode),
                    Toast.LENGTH_SHORT
                )
                currentToast?.show()
            }
            return
        }

        infoTileTapCount++

        if (infoTileTapCount == 1) {
            singleTapJob = viewModelScope.launch {
                delay(tapTimeoutMillis)
                _showVersionDialog.value = true
                infoTileTapCount = 0
            }
        } else {
            lastMultiTapTime = currentTime

            if (_developerModeEnabled.value) {
                currentToast = Toast.makeText(
                    context,
                    context.getString(R.string.already_in_developer_mode),
                    Toast.LENGTH_SHORT
                )
                currentToast?.show()
                infoTileTapCount = 0
                return
            }

            if (infoTileTapCount >= requiredTaps) {
                enableDeveloperMode()
                currentToast = Toast.makeText(
                    context, context.getString(R.string.developer_mode_enabled), Toast.LENGTH_LONG
                )
                currentToast?.show()
                infoTileTapCount = 0
            } else {
                val tapsRemaining = requiredTaps - infoTileTapCount
                currentToast = Toast.makeText(
                    context,
                    context.getString(R.string.taps_remaining_to_developer, tapsRemaining),
                    Toast.LENGTH_SHORT
                )
                currentToast?.show()

                resetTapsJob = viewModelScope.launch {
                    delay(multiTapCooldownMillis)
                    infoTileTapCount = 0
                }
            }
        }
    }

    fun dismissVersionDialog() {
        _showVersionDialog.value = false
    }

    fun openImpressum(context: Context) {
        Toast.makeText(context, "Impressum: xenonware.com/impressum", Toast.LENGTH_LONG).show()
    }

    // Dev
    fun refreshDeveloperModeState() {
        _developerModeEnabled.value = sharedPreferenceManager.developerModeEnabled
    }

    // ── Private helper functions ─────────────────────────────────────────────────

    val systemShortTimePattern: String = getSystemShortTimePatternInternal()

    private fun getSystemShortTimePatternInternal(): String {
        return try {
            val dateTimeInstance = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()
            )
            if (dateTimeInstance is SimpleDateFormat) {
                val parts = dateTimeInstance.toPattern().split(" ")
                if (parts.size > 1) parts.getOrNull(1) ?: "HH:mm" else "HH:mm"
            } else {
                "HH:mm"
            }
        } catch (_: Exception) {
            "HH:mm"
        }
    }

    private fun refreshSettingsStates() {
        _developerModeEnabled.value = sharedPreferenceManager.developerModeEnabled
        _checkForPreReleases.value = sharedPreferenceManager.checkForPreReleases
    }


    private fun getCurrentLocaleDisplayName(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty || locales[0] == null) {
            val savedTag = sharedPreferenceManager.languageTag
            if (savedTag.isNotEmpty()) {
                val locale = Locale.forLanguageTag(savedTag)
                return locale.getDisplayName(locale)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val ctx = getApplication<Application>()
                val localeManager = ctx.getSystemService(android.app.LocaleManager::class.java)
                val appLocales = localeManager.applicationLocales
                if (!appLocales.isEmpty) {
                    val locale = appLocales[0]
                    return locale.getDisplayName(locale)
                }
            }
            return getApplication<Application>().getString(R.string.system_default)
        }
        val locale = locales[0]!!
        return locale.getDisplayName(locale)
    }

    private fun getAppLocaleTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) {
            val saved = sharedPreferenceManager.languageTag
            if (saved.isNotEmpty()) return saved

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val ctx = getApplication<Application>()
                val localeManager = ctx.getSystemService(android.app.LocaleManager::class.java)
                val appLocales = localeManager.applicationLocales
                if (!appLocales.isEmpty) return appLocales.toLanguageTags()
            }
            return ""
        }
        return locales.toLanguageTags()
    }

    private fun prepareLanguageOptions() {
        val application = getApplication<Application>()
        val languages = mutableListOf(
            LanguageOption(application.getString(R.string.system_default), "")
        )
        val en = Locale.forLanguageTag("en")
        languages.add(LanguageOption(en.getDisplayName(en), en.toLanguageTag()))
        val de = Locale.forLanguageTag("de")
        languages.add(LanguageOption(de.getDisplayName(de), de.toLanguageTag()))
        _availableLanguages.value = languages
    }

    private fun setAppLocale(localeTag: String) {
        val appLocale = if (localeTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(localeTag)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private fun getSystemShortDatePattern(): String {
        return try {
            val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
            if (dateFormat is SimpleDateFormat) {
                dateFormat.toPattern()
            } else {
                "MM/dd/yy"
            }
        } catch (_: Exception) {
            "MM/dd/yy"
        }
    }

    private fun restartApplication(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent?.component != null) {
            context.startActivity(Intent.makeRestartActivityTask(intent.component))
            Process.killProcess(Process.myPid())
        } else {
            Toast.makeText(
                context, context.getString(R.string.error_restarting_app), Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openAppInfo(context: Context) {
        try {
            currentToast?.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            currentToast =
                Toast.makeText(context, "Could not open app settings.", Toast.LENGTH_SHORT)
            currentToast?.show()
        }
    }

    private fun enableDeveloperMode() {
        sharedPreferenceManager.developerModeEnabled = true
        _developerModeEnabled.value = true
        singleTapJob?.cancel()
        resetTapsJob?.cancel()
        currentToast?.cancel()
    }

    // ── Factory ──────────────────────────────────────────────────────────────────

    class SettingsViewModelFactory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST") return SettingsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
