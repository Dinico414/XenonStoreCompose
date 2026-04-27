package com.xenonware.store.ui.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.xenonware.store.ui.layouts.dev_settings.DevCoverSettings
import com.xenonware.store.ui.layouts.dev_settings.DevDefaultSettings
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.LayoutType

@Composable
fun DevSettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: DevSettingsViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    appSize: IntSize,
    ) {
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
                isLandscape = isLandscape,
                appSize = appSize,
                layoutType = layoutType,
            )
        }
    }
}

