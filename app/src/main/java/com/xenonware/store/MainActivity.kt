package com.xenonware.store

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
import com.xenonware.store.ui.layouts.StoreLayout
import com.xenonware.store.ui.theme.ScreenEnvironment
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel

// Import Toast and related Compose UI components
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var devSettingsViewModel: DevSettingsViewModel

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean = false
    private var lastAppliedBlackedOutMode: Boolean = false

    // Define a request code for SettingsActivity
    private val SETTINGS_REQUEST_CODE = 1001


    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        sharedPreferenceManager = SharedPreferenceManager(applicationContext)
        storeViewModel = ViewModelProvider(this).get(StoreViewModel::class.java)
        devSettingsViewModel = ViewModelProvider(this).get(DevSettingsViewModel::class.java)

        // Observe custom app updates
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                devSettingsViewModel.customAppsUpdated.collect {
                    storeViewModel.onCustomAppsUpdated()
                }
            }
        }

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

//            val toastMsg by storeViewModel.toastMessage.collectAsState()
//
//            // Show toast when toastMsg changes
//            LaunchedEffect(toastMsg) {
//                toastMsg?.let {
//                    Toast.makeText(currentContext, it, Toast.LENGTH_SHORT).show()
//                    storeViewModel.clearToastMessage() // Clear the message after showing
//                }
//            }

            ScreenEnvironment(
                lastAppliedTheme,
                lastAppliedCoverThemeEnabled,
                lastAppliedBlackedOutMode,

                ) {
                layoutType, isLandscape ->
                XenonStoreApp(
                    layoutType = layoutType,
                    onOpenSettings = {
                        val intent = Intent(currentContext, SettingsActivity::class.java)
                        // Start activity for result
                        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
                    },
                    isLandscape = isLandscape,
                    appSize = currentContainerSize,
                    storeViewModel = storeViewModel,
                    devSettingsViewModel = devSettingsViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh app list on every resume
        storeViewModel.fetchAndRefreshAppList(useCache = false)
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

    // Handle the result from SettingsActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            // Settings were exited, refresh the app list
            storeViewModel.fetchAndRefreshAppList(useCache = false)
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
    storeViewModel: StoreViewModel,
    devSettingsViewModel: DevSettingsViewModel
) {
    StoreLayout(
        layoutType = layoutType,
        onOpenSettings = onOpenSettings,
        modifier = Modifier.fillMaxSize(),
        isLandscape = isLandscape,
        appSize = appSize,
        storeViewModel = storeViewModel,
        devSettingsViewModel = devSettingsViewModel
    )
}
