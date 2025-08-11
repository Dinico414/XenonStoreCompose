package com.xenon.store.ui.layouts.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xenon.store.R
import com.xenon.store.SharedPreferenceManager
import com.xenon.store.ui.layouts.ActivityScreen
import com.xenon.store.ui.res.DialogClearDataConfirmation
import com.xenon.store.ui.res.DialogCoverDisplaySelection
import com.xenon.store.ui.res.DialogLanguageSelection
import com.xenon.store.ui.res.DialogResetSettingsConfirmation
import com.xenon.store.ui.res.DialogThemeSelection
import com.xenon.store.ui.values.LargestPadding
import com.xenon.store.ui.values.MediumPadding
import com.xenon.store.ui.values.NoSpacing
import com.xenon.store.viewmodel.classes.SettingsItems
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlin.text.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultSettings(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    onNavigateToDeveloperOptions: () -> Unit,
) {
    val context = LocalContext.current

    val currentThemeTitle by viewModel.currentThemeTitle.collectAsState()
    val blackedOutEnabled by viewModel.blackedOutModeEnabled.collectAsState()
    val showThemeDialog by viewModel.showThemeDialog.collectAsState()
    val themeOptions = viewModel.themeOptions
    val dialogSelectedThemeIndex by viewModel.dialogPreviewThemeIndex.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val showClearDataDialog by viewModel.showClearDataDialog.collectAsState()
    val showResetSettingsDialog by viewModel.showResetSettingsDialog.collectAsState()
    val showCoverSelectionDialog by viewModel.showCoverSelectionDialog.collectAsState()
    val coverThemeEnabled by viewModel.enableCoverTheme.collectAsState()

    val showLanguageDialog by viewModel.showLanguageDialog.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val selectedLanguageTagInDialog by viewModel.selectedLanguageTagInDialog.collectAsState()

    val showDateTimeFormatDialog by viewModel.showDateTimeFormatDialog.collectAsState()
    val availableDateFormats = viewModel.availableDateFormats
    val selectedDateFormatInDialog by viewModel.selectedDateFormatInDialog.collectAsState()
    val selectedTimeFormatInDialog by viewModel.selectedTimeFormatInDialog.collectAsState()
    val currentFormattedDateTime by viewModel.currentFormattedDateTime.collectAsState()

    val systemTimePattern = remember { viewModel.systemShortTimePattern }
    val twentyFourHourTimePattern = "HH:mm"
    val twelveHourTimePattern = "h:mm a"

    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo = remember {
        try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (_: Exception) {
            null
        }
    }
    val appVersion = packageInfo?.versionName ?: "N/A"

    val containerSize = LocalWindowInfo.current.containerSize
    val applyCoverTheme = remember(containerSize, coverThemeEnabled) {
        viewModel.applyCoverTheme(containerSize)
    }

    val appThemeSetting = remember { SharedPreferenceManager(context) }.theme
    val themeOptionsFromVm = viewModel.themeOptions
    val isSystemCurrentlyDark = isSystemInDarkTheme()

    val useDarkTileBackground: Boolean = when {
        blackedOutEnabled -> true
        appThemeSetting < 0 || appThemeSetting >= themeOptionsFromVm.size -> isSystemCurrentlyDark
        else -> when (themeOptionsFromVm[appThemeSetting].nightModeFlag) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> isSystemCurrentlyDark
        }
    }

    val isAppBarCollapsible = when (layoutType) {
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
        else -> true
    }
    val hazeState = rememberHazeState()

    ActivityScreen(
        titleText = stringResource(id = R.string.settings),
        navigationIconStartPadding = MediumPadding,
        navigationIconPadding = MediumPadding,
        navigationIconSpacing = NoSpacing,
        navigationIconContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.navigate_back_description),
                modifier = Modifier.size(24.dp)
            )
        },
        onNavigationIconClick = onNavigateBack,
        hasNavigationIconExtraContent = false,
        appBarActions = {},
        // isAppBarCollapsible = isAppBarCollapsible,
        modifier = Modifier.hazeSource(hazeState),
        content = { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = LargestPadding,
                        end = LargestPadding,
                        top = LargestPadding,
                        bottom = WindowInsets.safeDrawing.asPaddingValues()
                            .calculateBottomPadding() + LargestPadding
                    )
            ) {
                SettingsItems(
                    viewModel = viewModel,
                    currentThemeTitle = currentThemeTitle,
                    applyCoverTheme = applyCoverTheme,
                    coverThemeEnabled = coverThemeEnabled,
                    currentLanguage = currentLanguage,
                    currentFormat = currentFormattedDateTime,
                    appVersion = appVersion,
                    onNavigateToDeveloperOptions = onNavigateToDeveloperOptions
                )
            }
        })

    if (showThemeDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeEffect(hazeState)
        ) {
            DialogThemeSelection(
                themeOptions = themeOptions,
                currentThemeIndex = dialogSelectedThemeIndex,
                onThemeSelected = { index -> viewModel.onThemeOptionSelectedInDialog(index) },
                onDismiss = { viewModel.dismissThemeDialog() },
                onConfirm = { viewModel.applySelectedTheme() })
        }
    }
    if (showCoverSelectionDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeEffect(hazeState)
        ) {
            DialogCoverDisplaySelection(onConfirm = {
                viewModel.saveCoverDisplayMetrics(
                    containerSize
                )
            }, onDismiss = { viewModel.dismissCoverThemeDialog() })
        }
    }
    if (showClearDataDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeEffect(hazeState)
        ) {
            DialogClearDataConfirmation(
                onConfirm = { viewModel.confirmClearData() },
                onDismiss = { viewModel.dismissClearDataDialog() })
        }
    }
    if (showResetSettingsDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeEffect(hazeState)
        ) {
            DialogResetSettingsConfirmation(
                onConfirm = { viewModel.confirmResetSettings() },
                onDismiss = { viewModel.dismissResetSettingsDialog() })
        }
    }
    if (showLanguageDialog && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeEffect(hazeState)
        ) {
            DialogLanguageSelection(
                availableLanguages = availableLanguages,
                currentLanguageTag = selectedLanguageTagInDialog,
                onLanguageSelected = { tag -> viewModel.onLanguageSelectedInDialog(tag) },
                onDismiss = { viewModel.dismissLanguageDialog() },
                onConfirm = { viewModel.applySelectedLanguage() })
        }
    }
}

