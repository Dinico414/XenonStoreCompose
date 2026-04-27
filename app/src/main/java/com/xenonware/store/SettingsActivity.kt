package com.xenonware.store

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.xenonware.store.data.SharedPreferenceManager
import com.xenonware.store.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.store.presentation.sign_in.SignInViewModel
import com.xenonware.store.ui.layouts.SettingsLayout
import com.xenonware.store.ui.theme.ScreenEnvironment
import com.xenonware.store.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.util.Locale


object SettingsDestinations {
    const val MAIN_SETTINGS_ROUTE = "main_settings"
}

class SettingsActivity : ComponentActivity() {

    private val sharedPreferenceManager by lazy { SharedPreferenceManager(application) }

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var signInViewModel: SignInViewModel

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsViewModel = ViewModelProvider(
            this,
            SettingsViewModel.SettingsViewModelFactory(application)
        )[SettingsViewModel::class.java]

        signInViewModel = ViewModelProvider(
            this,
            SignInViewModel.SignInViewModelFactory(application)
        )[SignInViewModel::class.java]
        settingsViewModel.refreshLanguage()
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

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

            ScreenEnvironment(
                persistedAppThemeIndex, applyCoverTheme, blackedOutEnabled
            ) { layoutType, isLandscape ->

                val context = LocalContext.current
                val state by signInViewModel.state.collectAsStateWithLifecycle()

                val oneTapLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { result ->
                        if (result.resultCode == RESULT_OK) {
                            lifecycleScope.launch {
                                val signInResult = googleAuthUiClient.signInWithIntent(
                                    intent = result.data ?: return@launch
                                )
                                signInViewModel.onSignInResult(signInResult)
                            }
                        }
                    }
                )

                val traditionalSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = { result ->
                        if (result.resultCode == RESULT_OK) {
                            lifecycleScope.launch {
                                val signInResult = googleAuthUiClient.signInWithTraditionalIntent(
                                    intent = result.data ?: return@launch
                                )
                                signInViewModel.onSignInResult(signInResult)
                            }
                        }
                    }
                )
                NavHost(
                    navController = navController,
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
                            },
                            state = state,
                            googleAuthUiClient = googleAuthUiClient,
                            onSignInClick = {
                                lifecycleScope.launch {
                                    val signInResult = googleAuthUiClient.signIn()
                                    if (signInResult != null) {
                                        oneTapLauncher.launch(IntentSenderRequest.Builder(signInResult.pendingIntent.intentSender).build())
                                    } else {
                                        traditionalSignInLauncher.launch(googleAuthUiClient.getTraditionalSignInIntent())
                                    }
                                }
                            },
                            onSignOutClick = {
                                settingsViewModel.onSignOutClicked()
                            },
                            onConfirmSignOut = {
                                lifecycleScope.launch {
                                    googleAuthUiClient.signOut()
                                    sharedPreferenceManager.isUserLoggedIn = false
                                    settingsViewModel.dismissSignOutDialog()
                                    signInViewModel.resetState()
                                }
                            },
                            appSize = containerSize
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.updateCurrentLanguage()
        settingsViewModel.refreshDeveloperModeState()
        lifecycleScope.launch {
            val user = googleAuthUiClient.getSignedInUser()
            val isSignedIn = user != null
            sharedPreferenceManager.isUserLoggedIn = isSignedIn
            signInViewModel.updateSignInState(isSignedIn)
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
}
