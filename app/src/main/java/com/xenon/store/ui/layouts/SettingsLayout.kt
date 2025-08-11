package com.xenon.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xenon.store.ui.layouts.settings.CoverSettings
import com.xenon.store.ui.layouts.settings.DefaultSettings
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.SettingsViewModel

@Composable
fun SettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onNavigateToDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
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
                    layoutType = layoutType,
                    isLandscape = isLandscape,
                    onNavigateToDeveloperOptions = onNavigateToDeveloperOptions
                )
            }
        }
    }
}
