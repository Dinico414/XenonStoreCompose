package com.xenonware.store.ui.layouts

import androidx.compose.runtime.Composable
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
) {
    when (layoutType) {
        LayoutType.COVER -> {
            CoverSettings(
                onNavigateBack = onNavigateBack,
                viewModel = viewModel,
                onNavigateToDeveloperOptions = onNavigateToDeveloperOptions
            )
        }

        LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
            DefaultSettings(
                onNavigateBack = onNavigateBack,
                viewModel = viewModel,
                isLandscape = isLandscape,
                layoutType = layoutType,
                onNavigateToDeveloperOptions = onNavigateToDeveloperOptions
            )
        }
    }
}
