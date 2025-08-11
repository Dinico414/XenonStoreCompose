package com.xenon.store.ui.layouts.store // Assuming you want to change the package name

// Import other necessary resources for your store app
// import com.xenon.store.ui.res.StoreItemCell // Example: if you have a store item cell
// import com.xenon.store.ui.res.StoreItemContent // Example: if you have store item content
// import com.xenon.store.viewmodel.StoreViewModel // Example: if you have a StoreViewModel
// import com.xenon.store.viewmodel.StoreViewModelFactory // Example
// import com.xenon.store.viewmodel.classes.StoreItem // Example
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.store.R
import com.xenon.store.ui.layouts.ActivityScreen
import com.xenon.store.ui.layouts.QuicksandTitleVariable
import com.xenon.store.ui.res.FloatingToolbarContent
import com.xenon.store.ui.res.GoogleProfilBorder
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

    val storeItemsWithHeaders = remember { mutableStateOf<List<Any>>(emptyList()) }

    @Suppress("UnusedVariable", "unused") val isAppBarCollapsible = when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> !isLandscape
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }

    val hazeState = rememberHazeState()


    val snackbarHostState = remember { SnackbarHostState() }

    val currentSearchQuery by remember { mutableStateOf("") }

    var appWindowSize by remember { mutableStateOf(IntSize.Zero) }

    val showDummyProfile by devSettingsViewModel.showDummyProfileState.collectAsState()
    val isDeveloperModeEnabled by devSettingsViewModel.devModeToggleState.collectAsState()

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                XenonSnackbar(
                    snackbarData = snackbarData,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
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
                    // storeViewModel.setSearchQuery(newQuery) // Example
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

            navigationIconStartPadding = SmallPadding,
            navigationIconPadding = if (isDeveloperModeEnabled && showDummyProfile) SmallPadding else MediumPadding,
            navigationIconSpacing = NoSpacing,

            navigationIconContent = {

            },

            hasNavigationIconExtraContent = isDeveloperModeEnabled && showDummyProfile,

            navigationIconExtraContent = {
                if (isDeveloperModeEnabled && showDummyProfile) {
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
            },

            appBarActions = {},

            appBarSecondaryActionIcon = {},

            content = { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ExtraLargeSpacing)
                ) {
                    if (storeItemsWithHeaders.value.isEmpty() && currentSearchQuery.isBlank()) {
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
                    } else if (storeItemsWithHeaders.value.isEmpty() && currentSearchQuery.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f), contentPadding = PaddingValues(
                                top = ExtraLargePadding,
                                bottom = scaffoldPadding.calculateBottomPadding() + MediumPadding
                            )
                        ) {
                            itemsIndexed(
                                items = storeItemsWithHeaders.value,
                            ) { index, item ->
                                when (item) {
                                    is String -> {
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            ),
                                            fontWeight = FontWeight.Thin,
                                            textAlign = TextAlign.Start,
                                            fontFamily = QuicksandTitleVariable,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = if (index == 0) 0.dp else LargestPadding,
                                                    bottom = SmallPadding,
                                                    start = SmallPadding,
                                                    end = LargestPadding
                                                )
                                        )
                                    }


                                }
                            }
                        }
                    }
                }
            })
    }
}
