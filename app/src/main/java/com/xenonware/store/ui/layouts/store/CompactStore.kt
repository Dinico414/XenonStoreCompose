package com.xenonware.store.ui.layouts.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.FloatingToolbarContent
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.store.R
import com.xenonware.store.ui.res.DialogGitHubApps
import com.xenonware.store.ui.res.DialogShareSelector
import com.xenonware.store.ui.res.GoogleProfilBorder
import com.xenonware.store.ui.res.StoreItemCell
import com.xenonware.store.ui.res.XenonSnackbar
import com.xenonware.store.viewmodel.DevSettingsViewModel
import com.xenonware.store.viewmodel.LayoutType
import com.xenonware.store.viewmodel.StoreViewModel
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
    storeViewModel: StoreViewModel,
    devSettingsViewModel: DevSettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    appSize: IntSize,
    onOpenSettings: () -> Unit,


    ) {
    val context = LocalContext.current
    val storeItems by storeViewModel.storeItems.collectAsState()

//    val isAppBarCollapsible = rememberAppBarExpandableState(layoutType, isLandscape, appSize)

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
    val xenonStoreUpdateInfo by storeViewModel.xenonStoreUpdateInfo.collectAsState()
    val xenonStoreDownloadProgress by storeViewModel.xenonStoreDownloadProgress.collectAsState()

    val lazyListState = rememberLazyListState()



    val coverLayout = layoutType == LayoutType.COVER

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

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


    LaunchedEffect(Unit) {
        storeViewModel.error.collectLatest { errorMsg ->
            if (errorMsg != null) {
                snackbarHostState.showSnackbar(
                    message = errorMsg, duration = SnackbarDuration.Long
                )
                storeViewModel.clearError()
            }
        }
    }
    LaunchedEffect(Unit) {
        storeViewModel.currentActionInfo.collectLatest { infoMsg ->
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
                storeViewModel.addGitHubRepoConfig(
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
            onGitHubPATChange = { gitHubPATInput = it }
        )
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
           FloatingToolbarContent(
                hazeState = hazeState,
                onSearchQueryChanged = { newQuery ->
                    currentSearchQuery = newQuery
                    storeViewModel.setSearchQuery(newQuery)
                },
                currentSearchQuery = currentSearchQuery,
                lazyListState = lazyListState,
                allowToolbarScrollBehavior = !isAppBarCollapsible,
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
                                modifier = Modifier.fillMaxHeight()
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
                                                storeViewModel.downloadAndInstallXenonStoreUpdate(context)
                                            }
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
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
                                Icons.Filled.Share,
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
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings),
                                tint = colorScheme.onSurface
                            )
                        }
                    }
                },
               isFabEnabled = isAddButtonEnabled,
               )
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
                    }
                    else {
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
                                storeItems, key = { _, item -> item.packageName }) { _, storeItem ->
                                StoreItemCell(storeItem = storeItem, onInstall = { item ->
                                    storeViewModel.installApp(item, context)
                                }, onUninstall = { item ->
                                    storeViewModel.uninstallApp(item, context)
                                }, onOpen = { item ->
                                    storeViewModel.openApp(item, context)
                                })
                            }
                        }
                    }
                }
            })
    }
}
