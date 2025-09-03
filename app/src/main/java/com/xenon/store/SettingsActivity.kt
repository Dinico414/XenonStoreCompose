package com.xenon.store

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xenon.store.ui.layouts.SettingsLayout
import com.xenon.store.ui.theme.ScreenEnvironment
import com.xenon.store.viewmodel.SettingsViewModel


object SettingsDestinations {
    const val MAIN_SETTINGS_ROUTE = "main_settings"
    // DEVELOPER_OPTIONS_ROUTE is not used for NavHost here as it's a separate Activity
}

class SettingsActivity : ComponentActivity() {

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.SettingsViewModelFactory(application)
        )[SettingsViewModel::class.java]

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController() // This NavController is for within SettingsActivity if needed

            val activeNightMode by settingsViewModel.activeNightModeFlag.collectAsState()
            LaunchedEffect(activeNightMode) {
                AppCompatDelegate.setDefaultNightMode(activeNightMode)
            }

            val persistedAppThemeIndex by settingsViewModel.persistedThemeIndex.collectAsState()
            val blackedOutEnabled by settingsViewModel.blackedOutModeEnabled.collectAsState()
            val coverThemeEnabled by settingsViewModel.enableCoverTheme.collectAsState()
            val containerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = remember(containerSize, coverThemeEnabled) {
                settingsViewModel.applyCoverTheme(containerSize)
            }

            ScreenEnvironment(persistedAppThemeIndex, applyCoverTheme, blackedOutEnabled
            ) { layoutType, isLandscape ->

                val context = LocalContext.current // Context for launching DevSettingsActivity
                // Using a simple NavHost for the main settings content.
                // If DevSettings were a composable destination, it would be defined here.
                NavHost(
                    navController = navController, // Use the navController defined above
                    startDestination = SettingsDestinations.MAIN_SETTINGS_ROUTE
                ) {
                    composable(SettingsDestinations.MAIN_SETTINGS_ROUTE) {
                        SettingsLayout(
                            onNavigateBack = { finish() },
                            viewModel = settingsViewModel,
                            isLandscape = isLandscape,
                            layoutType = layoutType,
                            onNavigateToDeveloperOptions = {
                                val intent = Intent(context, DevSettingsActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                    }
                    // If DevSettingsActivity was a Composable destination, it would be:
                    // composable(SettingsDestinations.DEVELOPER_OPTIONS_ROUTE) { /* DevSettings Composable */ }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // These calls ensure that when the Activity resumes (e.g., after returning
        // from DevSettingsActivity), the ViewModel's state is updated from SharedPreferences
        // or other sources, which will then trigger UI recomposition if needed.
        settingsViewModel.updateCurrentLanguage() // This now also calls refreshDeveloperModeState()
    }
}
