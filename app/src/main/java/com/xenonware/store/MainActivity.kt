package com.xenonware.store

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.ui.layouts.MainLayout
import com.xenonware.store.ui.theme.ScreenEnvironment
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferenceManager: SharedPreferenceManager
    private lateinit var viewModel: StoreViewModel
    private lateinit var devSettingsViewModel: DevSettingsViewModel

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean = false
    private var lastAppliedBlackedOutMode: Boolean = false

    private val SETTINGS_REQUEST_CODE = 1001

    // Listener für die Shizuku-Berechtigung
    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.showToast("Shizuku permission granted!")
        } else {
            viewModel.showToast("Shizuku permission denied.")
        }
    }
    private val installResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
            val packageName = intent?.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
            val message = intent?.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            if (status == PackageInstaller.STATUS_SUCCESS) {
                viewModel.showToast("Installation of $packageName successful!")
                viewModel.handlePackageChanged(packageName ?: return)
            } else {
                viewModel.showToast("Installation of $packageName failed: $message")
                viewModel.handlePackageChanged(packageName ?: return)
            }
        }
    }


    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        sharedPreferenceManager = SharedPreferenceManager(applicationContext)
        viewModel = ViewModelProvider(this).get(StoreViewModel::class.java)
        devSettingsViewModel = ViewModelProvider(this).get(DevSettingsViewModel::class.java)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                devSettingsViewModel.customAppsUpdated.collect {
                    viewModel.onCustomAppsUpdated()
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
            val currentContainerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = sharedPreferenceManager.isCoverThemeApplied(currentContainerSize)

            ScreenEnvironment(
                themePreference = lastAppliedTheme,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = lastAppliedBlackedOutMode
            ) { layoutType, isLandscape ->
                XenonApp(
                    viewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = isLandscape,
                    appSize = currentContainerSize,
                    onOpenSettings = {
                        val intent = Intent(currentContext, SettingsActivity::class.java)
                        startActivityForResult(intent, SETTINGS_REQUEST_CODE)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchAndRefreshAppList(useCache = false)
        viewModel.verifyAndRefreshPendingInstallations()

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SETTINGS_REQUEST_CODE && resultCode == RESULT_OK) {
            viewModel.fetchAndRefreshAppList(useCache = false)
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
fun XenonApp(
    viewModel: StoreViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {
    MainLayout(
        viewModel = viewModel,
        isLandscape = isLandscape,
        layoutType = layoutType,
        onOpenSettings = onOpenSettings,
        appSize = appSize,
    )
}