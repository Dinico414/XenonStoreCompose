package com.xenonware.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.xenonware.store.ui.layouts.store.CompactStore
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel

@Composable
fun StoreLayout(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
    layoutType: LayoutType,
    isLandscape: Boolean,
    appSize: IntSize,
    storeViewModel: StoreViewModel,
    devSettingsViewModel: DevSettingsViewModel
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            LayoutType.COVER -> {
                CompactStore(
                    onOpenSettings = onOpenSettings,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                    storeViewModel = storeViewModel,
                    devSettingsViewModel = devSettingsViewModel
                )
            }

            LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
                CompactStore(
                    onOpenSettings = onOpenSettings,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                    storeViewModel = storeViewModel,
                    devSettingsViewModel = devSettingsViewModel
                )
            }
        }
    }
}
