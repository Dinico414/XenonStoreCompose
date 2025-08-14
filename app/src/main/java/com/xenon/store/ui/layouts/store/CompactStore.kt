package com.xenon.store.ui.layouts.store

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
import com.xenon.store.ui.values.*
import com.xenon.store.viewmodel.DevSettingsViewModel
import com.xenon.store.viewmodel.LayoutType
import com.xenon.store.viewmodel.StoreViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.collectLatest


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
    val context = LocalContext.current
    val storeItems by storeViewModel.storeItems.collectAsState()
    val errorMessage by storeViewModel.error.collectAsState()
    val currentActionInfo by storeViewModel.currentActionInfo.collectAsState()

    val hazeState = rememberHazeState()
    val snackbarHostState = remember { SnackbarHostState() }
    var currentSearchQuery by remember { mutableStateOf("") }
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

    LaunchedEffect(Unit) {
        storeViewModel.error.collectLatest { errorMsg ->
            if (errorMsg != null) {
                snackbarHostState.showSnackbar(
                    message = errorMsg,
                    duration = SnackbarDuration.Long
                )
                storeViewModel.clearError()
            }
        }
    }
    LaunchedEffect(Unit) {
        storeViewModel.currentActionInfo.collectLatest { infoMsg ->
            if (infoMsg != null) {
                Toast.makeText(context, infoMsg, Toast.LENGTH_SHORT).show()
            }
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
                    currentSearchQuery = newQuery
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
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = errorMessage!!,
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
                            itemsIndexed(storeItems, key = { _, item -> item.packageName }) { _, storeItem ->
                                StoreItemCell(
                                    storeItem = storeItem,
                                    onInstall = { item ->
                                        storeViewModel.installApp(item, context)
                                    },
                                    onUninstall = { item ->
                                        storeViewModel.uninstallApp(item, context)
                                    },
                                    onOpen = { item ->
                                        storeViewModel.openApp(item, context)
                                    }
                                )
                            }
                        }
                    }
                }
            })
    }
}