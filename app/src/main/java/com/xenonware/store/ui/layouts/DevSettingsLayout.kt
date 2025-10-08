package com.xenonware.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DevSettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: com.xenonware.store.viewmodel.DevSettingsViewModel,
    layoutType: com.xenonware.store.viewmodel.LayoutType,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COVER -> {
                _root_ide_package_.com.xenonware.store.ui.layouts.dev_settings.DevCoverSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel
                )
            }
            _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.SMALL, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COMPACT, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.MEDIUM, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.EXPANDED -> {
                _root_ide_package_.com.xenonware.store.ui.layouts.dev_settings.DevDefaultSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                )
            }
        }
    }
}
