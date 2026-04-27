package com.xenonware.store

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.identity.Identity
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.store.presentation.sign_in.SignInEvent
import com.xenonware.store.presentation.sign_in.SignInViewModel
import com.xenonware.store.ui.layouts.MainLayout
import com.xenonware.store.ui.theme.ScreenEnvironment
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: StoreViewModel
    private lateinit var devSettingsViewModel: DevSettingsViewModel
    private val signInViewModel: SignInViewModel by viewModels {
        SignInViewModel.SignInViewModelFactory(application)
    }

    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

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

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(currentContext, "Notifications disabled. You won't see update alerts.", Toast.LENGTH_SHORT).show()
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(currentContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                signInViewModel.signInEvent.collect { event ->
                    if (event is SignInEvent.SignedInSuccessfully) {
                        viewModel.onSignedIn()
                    }
                }
            }
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
                        currentContext.startActivity(intent)
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
        lifecycleScope.launch {
            val user = googleAuthUiClient.getSignedInUser()
            val isSignedIn = user != null

            sharedPreferenceManager.isUserLoggedIn = isSignedIn
            signInViewModel.updateSignInState(isSignedIn)

            if (isSignedIn) {
                viewModel.onSignedIn()
            }
        }
        viewModel.fetchAndRefreshAppList(useCache = false)
        viewModel.verifyAndRefreshPendingInstallations()

        val currentThemePref = sharedPreferenceManager.theme
        val currentCoverThemeEnabledSetting = sharedPreferenceManager.coverThemeEnabled
        val currentBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        if (currentThemePref != lastAppliedTheme ||
            currentCoverThemeEnabledSetting != lastAppliedCoverThemeEnabled ||
            currentBlackedOutMode != lastAppliedBlackedOutMode
        ) {
            if (currentThemePref != lastAppliedTheme) {
                updateAppCompatDelegateTheme(currentThemePref)
            }

            lastAppliedTheme = currentThemePref
            lastAppliedCoverThemeEnabled = currentCoverThemeEnabledSetting
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

    override fun attachBaseContext(newBase: Context) {
        var context = newBase
        val prefs = SharedPreferenceManager(newBase)
        val savedTag = prefs.languageTag
        if (savedTag.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = Locale.forLanguageTag(savedTag)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            context = newBase.createConfigurationContext(config)
        }
        super.attachBaseContext(ContextWrapper(context))
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