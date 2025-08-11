package com.xenon.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xenon.store.ui.layouts.store.CompactStore
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.StoreViewModel

@Composable
fun StoreLayout(
    viewModel: StoreViewModel,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    widthSizeClass: WindowWidthSizeClass,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            LayoutType.COVER -> {
                CompactStore(
                    onOpenSettings = onOpenSettings,
                    layoutType = layoutType,
                    isLandscape = true,
                    widthSizeClass = widthSizeClass
                )
            }

            LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
                CompactStore(
                    onOpenSettings = onOpenSettings,
                    layoutType = layoutType,
                    isLandscape = true,
                    widthSizeClass = widthSizeClass
                )
            }
        }
    }
}
