package com.xenonware.store.ui.layouts.store

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.FloatingToolbarContent
import com.xenon.mylibrary.res.GoogleProfilBorderNoGoogle
import com.xenon.mylibrary.res.SpannedModeFAB
import com.xenon.mylibrary.res.XenonSnackbar
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargePadding
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.store.R
import com.xenonware.store.ui.res.DialogGitHubApps
import com.xenonware.store.ui.res.DialogShareSelector
import com.xenonware.store.ui.res.StoreItemCell
import com.xenonware.store.ui.theme.extendedMaterialColorScheme
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.flow.collectLatest


@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CompactStore(
    viewModel: StoreViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    appSize: IntSize,
    onOpenSettings: () -> Unit,
) {
    DeviceConfigProvider(appSize = appSize) {
        val deviceConfig = LocalDeviceConfig.current
        val context = LocalContext.current
        val storeItems by viewModel.storeItems.collectAsState()
        val devSettingsViewModel: DevSettingsViewModel = viewModel()

        val hazeState = rememberHazeState()
        val snackbarHostState = remember { SnackbarHostState() }
        var currentSearchQuery by remember { mutableStateOf("") }
        var isSearchActive by rememberSaveable { mutableStateOf(false) }
        var showShareDialog by rememberSaveable { mutableStateOf(false) }
        var showGitHubDialog by remember { mutableStateOf(false) }

        // State variables for GitHub dialog inputs
        var ownerInput by rememberSaveable { mutableStateOf("") }
        var repoInput by rememberSaveable { mutableStateOf("") }
        var packageNameInput by rememberSaveable { mutableStateOf("") }
        var gitHubPATInput by rememberSaveable { mutableStateOf("") }


        val isAddButtonEnabled by devSettingsViewModel.addButtonState.collectAsState()
        val showDummyProfile by devSettingsViewModel.showDummyProfileState.collectAsState()
        val isDeveloperModeEnabled by devSettingsViewModel.devModeToggleState.collectAsState()

        val shouldShowNavigationElements by remember(isDeveloperModeEnabled, showDummyProfile) {
            derivedStateOf {
                val isMainIconPresent = false
                val isExtraIconPresent = isDeveloperModeEnabled && showDummyProfile
                isMainIconPresent || isExtraIconPresent
            }
        }
        val xenonStoreUpdateInfo by viewModel.xenonStoreUpdateInfo.collectAsState()
        val xenonStoreDownloadProgress by viewModel.xenonStoreDownloadProgress.collectAsState()

        val lazyListState = rememberLazyListState()

        val configuration = LocalConfiguration.current
        val appHeight = configuration.screenHeightDp.dp
        val isAppBarExpandable = when (layoutType) {
            LayoutType.COVER -> false
            LayoutType.SMALL -> false
            LayoutType.COMPACT -> !isLandscape && appHeight >= 460.dp
            LayoutType.MEDIUM -> true
            LayoutType.EXPANDED -> true
        }

        LaunchedEffect(Unit) {
            viewModel.error.collectLatest { errorMsg ->
                if (errorMsg != null) {
                    snackbarHostState.showSnackbar(
                        message = errorMsg, duration = SnackbarDuration.Long
                    )
                    viewModel.clearError()
                }
            }
        }
        LaunchedEffect(Unit) {
            viewModel.currentActionInfo.collectLatest { infoMsg ->
                if (infoMsg != null) {
                }
            }
        }

        if (showShareDialog) {
            DialogShareSelector(onDismissRequest = { showShareDialog = false })
        }

        if (showGitHubDialog) {
            DialogGitHubApps(
                onDismissRequest = { showGitHubDialog = false },
                onConfirm = {
                    viewModel.addGitHubRepoConfig(
                        owner = ownerInput,
                        repo = repoInput,
                        packageName = packageNameInput,
                        gitHubPAT = if (gitHubPATInput.isEmpty()) null else gitHubPATInput,
                        isUpdate = false
                    )
                    showGitHubDialog = false
                    // Clear input fields after confirmation
                    ownerInput = ""
                    repoInput = ""
                    packageNameInput = ""
                    gitHubPATInput = ""
                },
                owner = ownerInput,
                onOwnerChange = { ownerInput = it },
                repo = repoInput,
                onRepoChange = { repoInput = it },
                packageName = packageNameInput,
                onPackageNameChange = { packageNameInput = it },
                gitHubPAT = gitHubPATInput,
                onGitHubPATChange = { gitHubPATInput = it })
        }


        fun resetGitHubDialogState() {

        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                    XenonSnackbar(
                        snackbarData = snackbarData, modifier = Modifier.padding(
                            horizontal = 16.dp, vertical = 12.dp
                        )
                    )
                }
            },
            bottomBar = {
                val bottomPaddingNavigationBar =
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val imePaddingValues = WindowInsets.ime.asPaddingValues()
                val imeHeight = imePaddingValues.calculateBottomPadding()

                val targetBottomPadding =
                    remember(imeHeight, bottomPaddingNavigationBar, imePaddingValues) {
                        val calculatedPadding = if (imeHeight > bottomPaddingNavigationBar) {
                            imeHeight + LargePadding
                        } else {
                            max(
                                bottomPaddingNavigationBar, imePaddingValues.calculateTopPadding()
                            ) + LargePadding
                        }
                        max(calculatedPadding, 0.dp)
                    }

                val animatedBottomPadding by animateDpAsState(
                    targetValue = targetBottomPadding, animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow
                    ), label = "bottomPaddingAnimation"
                )
                FloatingToolbarContent(
                    hazeState = hazeState,
                    onSearchQueryChanged = { newQuery ->
                        currentSearchQuery = newQuery
                        viewModel.setSearchQuery(newQuery)
                    },
                    currentSearchQuery = currentSearchQuery,
                    lazyListState = lazyListState,
                    allowToolbarScrollBehavior = !isAppBarExpandable,
                    isSelectedColor = extendedMaterialColorScheme.inverseErrorContainer,
                    selectedNoteIds = emptyList(),
                    onClearSelection = { },
                    isAddModeActive = false,
                    onAddModeToggle = {
                        resetGitHubDialogState()
                        showGitHubDialog = true
                    },
                    isSearchActive = isSearchActive,
                    onIsSearchActiveChange = { isSearchActive = it },
                    defaultContent = { iconsAlphaDuration, showActionIconsExceptSearch ->
                        Row {
                            val updateButtonAnimationDuration = 300
                            val iconAlphaTarget = if (isSearchActive) 0f else 1f

                            val updateIconAlpha by animateFloatAsState(
                                targetValue = iconAlphaTarget, animationSpec = tween(
                                    durationMillis = iconsAlphaDuration,
                                    delayMillis = if (isSearchActive) 0 else 0
                                ), label = "FilterIconAlpha"
                            )
                            AnimatedVisibility(
                                visible = xenonStoreUpdateInfo != null,
                                enter = fadeIn(animationSpec = tween(durationMillis = updateButtonAnimationDuration)) + scaleIn(
                                    animationSpec = tween(durationMillis = updateButtonAnimationDuration),
                                    initialScale = 0.8f,
                                    transformOrigin = TransformOrigin.Center
                                ),
                                exit = fadeOut(animationSpec = tween(durationMillis = updateButtonAnimationDuration)) + scaleOut(
                                    animationSpec = tween(durationMillis = updateButtonAnimationDuration),
                                    targetScale = 0.8f,
                                    transformOrigin = TransformOrigin.Center
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .alpha(updateIconAlpha)
                                            .clip(RoundedCornerShape(100f))
                                            .background(colorScheme.primary)
                                            .clickable(
                                                enabled = !isSearchActive && showActionIconsExceptSearch,
                                                onClick = {
                                                    viewModel.downloadAndInstallXenonStoreUpdate(
                                                        context
                                                    )
                                                }),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = stringResource(R.string.update),
                                            tint = colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (xenonStoreDownloadProgress > 0f && xenonStoreDownloadProgress < 1f) {
                                            CircularProgressIndicator(
                                                progress = { xenonStoreDownloadProgress },
                                                modifier = Modifier.size(36.dp),
                                                color = colorScheme.onPrimary,
                                                trackColor = Color.Transparent,
                                                strokeWidth = 5.dp
                                            )
                                        }
                                    }
                                }
                            }

                            val shareIconAlpha by animateFloatAsState(
                                targetValue = iconAlphaTarget, animationSpec = tween(
                                    durationMillis = iconsAlphaDuration,
                                    delayMillis = if (isSearchActive) 100 else 0
                                ), label = "FilterIconAlpha"
                            )
                            IconButton(
                                onClick = { showShareDialog = true },
                                modifier = Modifier.alpha(shareIconAlpha),
                                enabled = !isSearchActive && showActionIconsExceptSearch
                            ) {
                                Icon(
                                    Icons.Rounded.Share,
                                    contentDescription = stringResource(R.string.share_store_action),
                                    tint = colorScheme.onSurface
                                )
                            }
                            val settingsIconAlpha by animateFloatAsState(
                                targetValue = iconAlphaTarget, animationSpec = tween(
                                    durationMillis = iconsAlphaDuration,
                                    delayMillis = if (isSearchActive) 200 else 0
                                ), label = "SettingsIconAlpha"
                            )
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.alpha(settingsIconAlpha),
                                enabled = !isSearchActive && showActionIconsExceptSearch
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = colorScheme.onSurface
                                )
                            }
                        }
                    },
                    isFabEnabled = isAddButtonEnabled,
                    isSpannedMode = deviceConfig.isSpannedMode,
                    fabOnLeftInSpannedMode = deviceConfig.fabOnLeft,
                    spannedModeHingeGap = deviceConfig.hingeGapDp,
                    spannedModeFab = {
                        SpannedModeFAB(
                            hazeState = hazeState,
                            onClick = deviceConfig.toggleFabSide,
                            modifier = Modifier.padding(bottom = animatedBottomPadding),
                        )
                    })
            },


            ) { scaffoldPadding ->
            ActivityScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding()
                    .hazeSource(hazeState)
                    .onSizeChanged { newSize ->
                    },
                titleText = stringResource(id = R.string.app_name),
                expandable = isAppBarExpandable,
                navigationIconStartPadding = if (shouldShowNavigationElements) SmallPadding else 0.dp,
                navigationIconPadding = if (shouldShowNavigationElements) {
                        if (isDeveloperModeEnabled && showDummyProfile) SmallPadding else MediumPadding
                    } else {
                        0.dp
                    },
                navigationIconSpacing = if (shouldShowNavigationElements) NoSpacing else 0.dp,
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
                            GoogleProfilBorderNoGoogle(
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
                navigationIcon = {},
                actions = {},
                content = { _ ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = ExtraLargeSpacing)
                    ) {
                        if (storeItems.isEmpty()) {
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
                                itemsIndexed(
                                    storeItems,
                                    key = { _, item -> item.packageName }) { _, storeItem ->
                                    StoreItemCell(storeItem = storeItem, onInstall = { item ->
                                        viewModel.installApp(item, context)
                                    }, onUninstall = { item ->
                                        viewModel.uninstallApp(item, context)
                                    }, onOpen = { item ->
                                        viewModel.openApp(item, context)
                                    })
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
