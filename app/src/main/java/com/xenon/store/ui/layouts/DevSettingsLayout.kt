package com.xenon.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xenon.store.ui.layouts.dev_settings.DevCoverSettings
import com.xenon.store.ui.layouts.dev_settings.DevDefaultSettings
import com.xenon.store.viewmodel.DevSettingsViewModel
import com.xenon.store.viewmodel.LayoutType

@Composable
fun DevSettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: DevSettingsViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            LayoutType.COVER -> {
                DevCoverSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel
                )
            }
            LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
                DevDefaultSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = isLandscape
                )
            }
        }
    }
}
