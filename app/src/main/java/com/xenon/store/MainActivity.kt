package com.xenon.store

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.xenon.store.ui.layouts.StoreLayout
import com.xenon.store.ui.theme.ScreenEnvironment
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var storeViewModel: StoreViewModel

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean = false
    private var lastAppliedBlackedOutMode: Boolean = false

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        sharedPreferenceManager = SharedPreferenceManager(applicationContext)
        storeViewModel = ViewModelProvider(this).get(StoreViewModel::class.java)

        val initialThemePref = sharedPreferenceManager.theme
        val initialCoverThemeEnabled = sharedPreferenceManager.coverThemeEnabled
        val initialBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        updateAppCompatDelegateTheme(initialThemePref)

        lastAppliedTheme = initialThemePref
        lastAppliedCoverThemeEnabled = initialCoverThemeEnabled
        lastAppliedBlackedOutMode = initialBlackedOutMode

        setContent {
            val currentContext = LocalContext.current
            val currentContainerSize = LocalWindowInfo.current.containerSize // Use LocalWindowInfo

            ScreenEnvironment(
                lastAppliedTheme,
                lastAppliedCoverThemeEnabled,
                lastAppliedBlackedOutMode,

                ) { layoutType, isLandscape ->
                XenonStoreApp(
                    layoutType = layoutType,
                    onOpenSettings = {
                        val intent = Intent(currentContext, SettingsActivity::class.java)
                        currentContext.startActivity(intent)
                    },
                    isLandscape = isLandscape,
                    appSize = currentContainerSize,
                    storeViewModel = storeViewModel // Pass ViewModel to Composable if needed there
                    )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        storeViewModel.verifyAndRefreshPendingInstallations()

        val currentThemePref = sharedPreferenceManager.theme
        val currentCoverThemeEnabled = sharedPreferenceManager.coverThemeEnabled
        val currentBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        if (currentThemePref != lastAppliedTheme ||
            currentCoverThemeEnabled != lastAppliedCoverThemeEnabled ||
            currentBlackedOutMode != lastAppliedBlackedOutMode
        ) {
            if (currentThemePref != lastAppliedTheme) {
                updateAppCompatDelegateTheme(currentThemePref)
            }

            lastAppliedTheme = currentThemePref
            lastAppliedCoverThemeEnabled = currentCoverThemeEnabled
            lastAppliedBlackedOutMode = currentBlackedOutMode

            recreate()
        }
    }

    private fun updateAppCompatDelegateTheme(themePref: Int) {
        if (themePref >= 0 && themePref < sharedPreferenceManager.themeFlag.size) {
            AppCompatDelegate.setDefaultNightMode(sharedPreferenceManager.themeFlag[themePref])
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

@Composable
fun XenonStoreApp(
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    isLandscape: Boolean = false,
    appSize: IntSize,
    storeViewModel: StoreViewModel // Added ViewModel parameter
    ) {
    StoreLayout(
        layoutType = layoutType,
        onOpenSettings = onOpenSettings,
        modifier = Modifier.fillMaxSize(),
        isLandscape = isLandscape,
        appSize = appSize,
        storeViewModel = storeViewModel // Pass ViewModel to StoreLayout
    )
}
