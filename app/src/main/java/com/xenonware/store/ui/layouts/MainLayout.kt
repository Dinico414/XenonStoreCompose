package com.xenonware.store.ui.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import com.xenonware.store.ui.layouts.store.CompactStore
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel

@Composable
fun MainLayout(
    viewModel: StoreViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    appSize: IntSize,
) {
    when (layoutType) {
        LayoutType.COVER -> {
            if (isLandscape) {
                CompactStore(
                    viewModel = viewModel,
                    isLandscape = true,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            } else {
                CompactStore(
                    viewModel = viewModel,
                    isLandscape = false,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            }
        }

        LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
            if (isLandscape) {
                CompactStore(
                    viewModel = viewModel,
                    isLandscape = true,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            } else {
                CompactStore(
                    viewModel = viewModel,
                    isLandscape = false,
                    layoutType = layoutType,
                    onOpenSettings = onOpenSettings,
                    appSize = appSize
                )
            }
        }
    }
}

