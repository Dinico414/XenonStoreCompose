package com.xenonware.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.ViewModelProvider
import com.xenonware.store.ui.layouts.DevSettingsLayout
import com.xenonware.store.ui.theme.ScreenEnvironment
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.SettingsViewModel

class DevSettingsActivity : ComponentActivity() {

    private lateinit var devSettingsViewModel: com.xenonware.store.viewmodel.DevSettingsViewModel
    private lateinit var mainSettingsViewModel: com.xenonware.store.viewmodel.SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainSettingsViewModel = ViewModelProvider(
            this,
            _root_ide_package_.com.xenonware.store.viewmodel.SettingsViewModel.SettingsViewModelFactory(application)
        )[_root_ide_package_.com.xenonware.store.viewmodel.SettingsViewModel::class.java]

        devSettingsViewModel = ViewModelProvider(this)[_root_ide_package_.com.xenonware.store.viewmodel.DevSettingsViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            val activeNightMode by mainSettingsViewModel.activeNightModeFlag.collectAsState()
            LaunchedEffect(activeNightMode) {
                AppCompatDelegate.setDefaultNightMode(activeNightMode)
            }

            val persistedAppThemeIndex by mainSettingsViewModel.persistedThemeIndex.collectAsState()
            val blackedOutEnabled by mainSettingsViewModel.blackedOutModeEnabled.collectAsState()
            val coverThemeEnabled by mainSettingsViewModel.enableCoverTheme.collectAsState()
            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = mainSettingsViewModel.applyCoverTheme(containerSize)


            _root_ide_package_.com.xenonware.store.ui.theme.ScreenEnvironment(
                persistedAppThemeIndex, applyCoverTheme, blackedOutEnabled
            ) { layoutType, isLandscape ->
                _root_ide_package_.com.xenonware.store.ui.layouts.DevSettingsLayout(
                    onNavigateBack = { finish() },
                    viewModel = devSettingsViewModel,
                    layoutType = layoutType
                )
            }
        }
    }
}
