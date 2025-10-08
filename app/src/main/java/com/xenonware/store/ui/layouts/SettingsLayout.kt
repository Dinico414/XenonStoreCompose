package com.xenonware.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xenonware.store.ui.layouts.settings.CoverSettings
import com.xenonware.store.ui.layouts.settings.DefaultSettings
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.SettingsViewModel

@Composable
fun SettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: com.xenonware.store.viewmodel.SettingsViewModel,
    isLandscape: Boolean,
    layoutType: com.xenonware.store.viewmodel.LayoutType,
    onNavigateToDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COVER -> {
                _root_ide_package_.com.xenonware.store.ui.layouts.settings.CoverSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                    onNavigateToDeveloperOptions = onNavigateToDeveloperOptions
                )
            }

            _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.SMALL, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COMPACT, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.MEDIUM, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.EXPANDED -> {
                _root_ide_package_.com.xenonware.store.ui.layouts.settings.DefaultSettings(
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
