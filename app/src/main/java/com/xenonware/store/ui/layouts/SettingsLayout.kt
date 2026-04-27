package com.xenonware.store.ui.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.xenonware.store.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.store.presentation.sign_in.SignInState
import com.xenonware.store.ui.layouts.settings.CoverSettings
import com.xenonware.store.ui.layouts.settings.DefaultSettings
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.SettingsViewModel

@Composable
fun SettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onNavigateToDeveloperOptions: () -> Unit,
    state: SignInState,
    googleAuthUiClient: GoogleAuthUiClient,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onConfirmSignOut: () -> Unit,
    appSize: IntSize,
    ) {
    when (layoutType) {
        LayoutType.COVER -> {
            CoverSettings(
                onNavigateBack = onNavigateBack,
                viewModel = viewModel,
                onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                state = state,
                googleAuthUiClient = googleAuthUiClient,
                onSignInClick = onSignInClick,
                onSignOutClick = onSignOutClick,
                onConfirmSignOut = onConfirmSignOut
            )
        }

        LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
            DefaultSettings(
                onNavigateBack = onNavigateBack,
                viewModel = viewModel,
                isLandscape = isLandscape,
                layoutType = layoutType,
                onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                state = state,
                googleAuthUiClient = googleAuthUiClient,
                onSignInClick = onSignInClick,
                onSignOutClick = onSignOutClick,
                appSize = appSize,
                onConfirmSignOut = onConfirmSignOut
            )
        }
    }
}
