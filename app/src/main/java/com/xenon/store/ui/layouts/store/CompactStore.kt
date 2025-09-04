package com.xenon.store.ui.layouts.store

import android.widget.Toast
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    onOpenSettings: () -> Unit,
    isLandscape: Boolean,
    appSize: IntSize,

    ) {
    val context = LocalContext.current
    val storeItems by storeViewModel.storeItems.collectAsState()
    val errorMessage by storeViewModel.error.collectAsState()
    val currentActionInfo by storeViewModel.currentActionInfo.collectAsState()

    val density = LocalDensity.current
    val appWidthDp = with(density) { appSize.width.toDp() }
    val appHeightDp = with(density) { appSize.height.toDp() }

    val currentAspectRatio = if (isLandscape) {
        appWidthDp / appHeightDp
    } else {
        appHeightDp / appWidthDp
    }

    val aspectRatioConditionMet = if (isLandscape) {
        currentAspectRatio > 0.5625f
    } else {
        currentAspectRatio < 1.77f
    }

    val isAppBarCollapsible = when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape || !aspectRatioConditionMet
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }

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
    val lazyListState = rememberLazyListState()


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
                onSearchQueryChanged = { newQuery ->
                    currentSearchQuery = newQuery
                },
                lazyListState = lazyListState,
                allowToolbarScrollBehavior = !isAppBarCollapsible
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
            expandable = isAppBarCollapsible,

            navigationIconStartPadding = if (shouldShowNavigationElements) SmallPadding else 0.dp,
            navigationIconPadding = if (shouldShowNavigationElements) {
                if (isDeveloperModeEnabled && showDummyProfile) SmallPadding else MediumPadding
            } else {
                0.dp
            },
            navigationIconSpacing = if (shouldShowNavigationElements) NoSpacing else 0.dp,

            navigationIcon = {},

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
          actions = {},
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
                            state = lazyListState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                top = LargestPadding,
                                bottom = scaffoldPadding.calculateBottomPadding() + MediumPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(MediumPadding)
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