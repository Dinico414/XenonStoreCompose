package com.xenon.store.ui.layouts.store

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.store.R
import com.xenon.store.ui.layouts.ActivityScreen
import com.xenon.store.ui.res.FloatingToolbarContent
import com.xenon.store.ui.res.GoogleProfilBorder
import com.xenon.store.ui.res.StoreItemCell
import com.xenon.store.ui.res.XenonSnackbar
import com.xenon.store.ui.values.ExtraLargePadding
import com.xenon.store.ui.values.ExtraLargeSpacing
import com.xenon.store.ui.values.LargestPadding
import com.xenon.store.ui.values.MediumPadding
import com.xenon.store.ui.values.NoSpacing
import com.xenon.store.ui.values.SmallPadding
import com.xenon.store.viewmodel.DevSettingsViewModel
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.StoreViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CompactStore(
    storeViewModel: StoreViewModel = viewModel(),
    devSettingsViewModel: DevSettingsViewModel = viewModel(),
    layoutType: LayoutType,
    isLandscape: Boolean,
    onOpenSettings: () -> Unit,
    widthSizeClass: WindowWidthSizeClass,
) {
    val storeItems by storeViewModel.storeItems.collectAsState()
    val error by storeViewModel.error.collectAsState()

    val hazeState = rememberHazeState()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentSearchQuery by remember { mutableStateOf("") }
    var appWindowSize by remember { mutableStateOf(IntSize.Zero) }

    val showDummyProfile by devSettingsViewModel.showDummyProfileState.collectAsState()
    val isDeveloperModeEnabled by devSettingsViewModel.devModeToggleState.collectAsState()

    val shouldShowNavigationElements by remember(isDeveloperModeEnabled, showDummyProfile) {
        derivedStateOf {
            val isMainIconPresent = false
            val isExtraIconPresent = isDeveloperModeEnabled && showDummyProfile
            isMainIconPresent || isExtraIconPresent
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                XenonSnackbar(
                    snackbarData = snackbarData,
                    modifier = Modifier.padding(horizontal = LargestPadding, vertical = 12.dp)
                )
            }
        },
        bottomBar = {
            FloatingToolbarContent(
                hazeState = hazeState,
                onOpenSettings = onOpenSettings,
                currentSearchQuery = currentSearchQuery,
                widthSizeClass = widthSizeClass,
                layoutType = layoutType,
                onSearchQueryChanged = { newQuery ->
                    // Handle search query changes
                },
                appSize = appWindowSize
            )
        },
    ) { scaffoldPadding ->
        ActivityScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding()
                .hazeSource(hazeState)
                .onSizeChanged { newSize ->
                    appWindowSize = newSize
                },
            titleText = stringResource(id = R.string.app_name),

            navigationIconStartPadding = if (shouldShowNavigationElements) SmallPadding else 0.dp,
            navigationIconPadding = if (shouldShowNavigationElements) {
                if (isDeveloperModeEnabled && showDummyProfile) SmallPadding else MediumPadding
            } else {
                0.dp
            },
            navigationIconSpacing = if (shouldShowNavigationElements) NoSpacing else 0.dp,

            navigationIconContent = null,

            hasNavigationIconExtraContent = if (shouldShowNavigationElements) {
                isDeveloperModeEnabled && showDummyProfile
            } else {
                false
            },

            navigationIconExtraContent = if (shouldShowNavigationElements && isDeveloperModeEnabled && showDummyProfile) {
                {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        GoogleProfilBorder(
                            modifier = Modifier.size(32.dp),
                        )
                        Image(
                            painter = painterResource(id = R.mipmap.default_icon),
                            contentDescription = stringResource(R.string.open_navigation_menu),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            } else {
                {}
            },

            appBarActions = {},
            appBarSecondaryActionIcon = {},

            content = { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ExtraLargeSpacing)
                ) {
                    if (error != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (storeItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.nothing_in_store_yet),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                top = LargestPadding,
                                bottom = scaffoldPadding.calculateBottomPadding() + MediumPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(LargestPadding)
                        ) {
                            itemsIndexed(storeItems) { index, storeItem ->
                                StoreItemCell(
                                    storeItem = storeItem,
                                    onInstall = { /* Handle install */ },
                                    onUninstall = { /* Handle uninstall */ },
                                    onOpen = { /* Handle open */ }
                                )
                            }
                        }
                    }
                }
            })
    }
}
