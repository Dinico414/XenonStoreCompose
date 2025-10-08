package com.xenonware.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.xenonware.store.ui.layouts.store.CompactStore
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel

@Composable
fun StoreLayout(
    modifier: Modifier = Modifier,
    layoutType: com.xenonware.store.viewmodel.LayoutType,
    onOpenSettings: () -> Unit,
    isLandscape: Boolean,
    appSize: IntSize,
    storeViewModel: com.xenonware.store.viewmodel.StoreViewModel // Added ViewModel parameter
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COVER -> {
                _root_ide_package_.com.xenonware.store.ui.layouts.store.CompactStore(
                    onOpenSettings = onOpenSettings,
                    layoutType = layoutType,
                    isLandscape = true,
                    appSize = appSize,
                    storeViewModel = storeViewModel // Pass ViewModel
                )
            }

            _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.SMALL, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COMPACT, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.MEDIUM, _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.EXPANDED -> {
                _root_ide_package_.com.xenonware.store.ui.layouts.store.CompactStore(
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
