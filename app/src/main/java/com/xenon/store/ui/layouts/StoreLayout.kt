package com.xenon.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.xenon.store.ui.layouts.store.CompactStore
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.StoreViewModel

@Composable
fun StoreLayout(
    modifier: Modifier = Modifier,
    layoutType: LayoutType,
    onOpenSettings: () -> Unit,
    isLandscape: Boolean,
    appSize: IntSize,
    storeViewModel: StoreViewModel // Added ViewModel parameter
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            LayoutType.COVER -> {
                CompactStore(
                    onOpenSettings = onOpenSettings,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                    storeViewModel = storeViewModel // Pass ViewModel
                )
            }

            LayoutType.SMALL, LayoutType.COMPACT, LayoutType.MEDIUM, LayoutType.EXPANDED -> {
                CompactStore(
                    onOpenSettings = onOpenSettings,
                    layoutType = layoutType,
                    isLandscape = true, // This seems to be always true, might be an oversight if it should depend on the actual orientation
                    appSize = appSize,
                    storeViewModel = storeViewModel // Pass ViewModel
                )
            }
        }
    }
}
