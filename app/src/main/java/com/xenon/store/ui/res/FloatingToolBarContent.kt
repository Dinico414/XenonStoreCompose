package com.xenon.store.ui.res

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.xenon.store.R
import com.xenon.store.ui.dialogs.ShareStoreDialog
import com.xenon.store.ui.values.LargePadding
import com.xenon.store.ui.values.SmallElevation
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class ScrollState(
    val firstVisibleItemIndex: Int,
    val firstVisibleItemScrollOffset: Int,
    val isScrollInProgress: Boolean,
    val canScrollForward: Boolean,
)

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class, FlowPreview::class
)
@Composable
fun FloatingToolbarContent(
    hazeState: HazeState,
    onOpenSettings: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    currentSearchQuery: String,
    lazyListState: LazyListState,
    allowToolbarScrollBehavior: Boolean,
    // TODO: Add hasUpdate and onDownloadUpdateClick parameters here
    // hasUpdate: Boolean,
    // onDownloadUpdateClick: () -> Unit,
) {
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showActionIconsExceptSearch by rememberSaveable { mutableStateOf(true) }
    var canShowTextField by rememberSaveable { mutableStateOf(false) }
    var showShareDialog by rememberSaveable { mutableStateOf(false) } // Added state for dialog

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
     val localContext = LocalContext.current // Not directly needed now

    val iconsAlphaDuration = 500
    val iconGroupExitAnimationDuration = 100
    val iconsClearanceTime = iconsAlphaDuration + 200
    val textFieldExistenceDelay = iconsAlphaDuration - 100
    val textFieldAnimationDuration = 500

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    val startPadding = 16.dp
    val endPadding = 16.dp
    val internalStartPadding = 8.dp
    val internalEndPadding = 8.dp
    val iconSize = 48.dp
    val spaceBetweenToolbarAndFab = 8.dp
    val fabSize = 56.dp
    val totalSubtractionInDp =
        startPadding + internalStartPadding + iconSize + internalEndPadding + spaceBetweenToolbarAndFab + fabSize + endPadding

    val maxTextFieldWidth = (screenWidthDp - totalSubtractionInDp).coerceIn(0.dp, 280.dp)

    var toolbarVisibleState by rememberSaveable { mutableStateOf(true) }

    if (showShareDialog) {
        ShareStoreDialog(onDismissRequest = { showShareDialog = false })
    }

    LaunchedEffect(lazyListState, isSearchActive, allowToolbarScrollBehavior) {
        if (isSearchActive || !allowToolbarScrollBehavior) {
            toolbarVisibleState = true
        } else {
            var previousOffset = lazyListState.firstVisibleItemScrollOffset
            var previousIndex = lazyListState.firstVisibleItemIndex

            snapshotFlow {
                ScrollState(
                    firstVisibleItemIndex = lazyListState.firstVisibleItemIndex,
                    firstVisibleItemScrollOffset = lazyListState.firstVisibleItemScrollOffset,
                    isScrollInProgress = lazyListState.isScrollInProgress,
                    canScrollForward = lazyListState.canScrollForward
                )
            }
                .distinctUntilChanged()
                .map { currentState ->
                    val isAtBottom = !currentState.canScrollForward
                    val scrollingUp = if (currentState.firstVisibleItemIndex < previousIndex) {
                        true
                    } else if (currentState.firstVisibleItemIndex > previousIndex) {
                        false
                    } else {
                        currentState.firstVisibleItemScrollOffset < previousOffset
                    }
                    previousOffset = currentState.firstVisibleItemScrollOffset
                    previousIndex = currentState.firstVisibleItemIndex

                    Triple(scrollingUp, currentState.isScrollInProgress, isAtBottom)
                }
                .collect { (scrollingUp, isScrolling, isAtBottom) ->
                    if (isScrolling) {
                        toolbarVisibleState = scrollingUp
                    }
                    if (isAtBottom) {
                        toolbarVisibleState = true
                    }
                }
        }
    }
    LaunchedEffect(lazyListState, allowToolbarScrollBehavior, isSearchActive) {
        if (!isSearchActive && allowToolbarScrollBehavior) {
            snapshotFlow { lazyListState.isScrollInProgress }
                .debounce(2000L)
                .collect { isScrolling ->
                    if (!isScrolling) {
                        toolbarVisibleState = true
                    }
                }
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(iconsClearanceTime.toLong())
            showActionIconsExceptSearch = false
        } else {
            showActionIconsExceptSearch = true
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(textFieldExistenceDelay.toLong())
            canShowTextField = true
            focusRequester.requestFocus()
        } else {
            canShowTextField = false
        }
    }

    val bottomPaddingNavigationBar =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imePaddingValues = WindowInsets.ime.asPaddingValues()
    val imeHeight = imePaddingValues.calculateBottomPadding()

    val targetBottomPadding = remember(imeHeight, bottomPaddingNavigationBar, imePaddingValues) {
        if (imeHeight > bottomPaddingNavigationBar) {
            imeHeight + LargePadding
        } else {
            max(bottomPaddingNavigationBar, imePaddingValues.calculateTopPadding()) + LargePadding
        }
    }

    val animatedBottomPadding by animateDpAsState(
        targetValue = targetBottomPadding,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bottomPaddingAnimation"
    )


    val toolbarHeight = 64.dp
    val toolbarOffsetTarget =
        if (toolbarVisibleState) 0.dp else toolbarHeight + LargePadding + 50.dp

    val animatedToolbarOffset by animateDpAsState(
        targetValue = toolbarOffsetTarget, animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow
        ), label = "toolbarOffsetAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = animatedBottomPadding,
            ), contentAlignment = Alignment.Center
    ) {
        HorizontalFloatingToolbar(
            modifier = Modifier
                .height(toolbarHeight)
                .offset(y = animatedToolbarOffset),
            expanded = true,
            floatingActionButton = {
                Box(contentAlignment = Alignment.Center) {
                    val fabShape = FloatingActionButtonDefaults.shape
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val isHovered by interactionSource.collectIsHoveredAsState()

                    val fabIconTint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        colorScheme.onPrimaryContainer
                    } else {
                        colorScheme.onPrimary
                    }
                    val hazeThinColor = colorScheme.primary
                    val smallElevationPx = with(density) { SmallElevation.toPx() }
                    val baseShadowAlpha = 0.7f
                    val interactiveShadowAlpha = 0.9f
                    val currentShadowRadius =
                        if (isPressed || isHovered) smallElevationPx * 1.5f else smallElevationPx
                    val currentShadowAlpha =
                        if (isPressed || isHovered) interactiveShadowAlpha else baseShadowAlpha
                    val currentShadowColor = colorScheme.scrim.copy(alpha = currentShadowAlpha)
                    val currentYOffsetPx = with(density) { 1.dp.toPx() }

                    Canvas(
                        modifier = Modifier.size(
                            FloatingActionButtonDefaults.LargeIconSize + 24.dp + if (isPressed || isHovered) 8.dp else 5.dp
                        )
                    ) {
                        val outline = fabShape.createOutline(this.size, layoutDirection, density)
                        val composePath = Path().apply { addOutline(outline) }
                        drawIntoCanvas { canvas ->
                            val frameworkPaint = Paint().asFrameworkPaint().apply {
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = with(this@Canvas) { 0.5.dp.toPx() }
                                color = Color.Transparent.toArgb()
                                setShadowLayer(
                                    currentShadowRadius,
                                    0f,
                                    currentYOffsetPx,
                                    currentShadowColor.toArgb()
                                )
                            }
                            canvas.nativeCanvas.drawPath(
                                composePath.asAndroidPath(), frameworkPaint
                            )
                        }
                    }

                    val rotationAngle = remember { Animatable(0f) }
                    LaunchedEffect(isSearchActive) {
                        if (isSearchActive) {
                            delay(700)
                            rotationAngle.animateTo(
                                targetValue = 45f, animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                )
                            )
                        } else {
                            rotationAngle.animateTo(
                                targetValue = 0f, animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                )
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            onSearchQueryChanged("")
                            keyboardController?.hide()
                            isSearchActive = false

                        },
                        containerColor = Color.Transparent,
                        shape = fabShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            0.dp, 0.dp, 0.dp, 0.dp
                        ),
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .clip(FloatingActionButtonDefaults.shape)
                            .background(colorScheme.primary)
                            .hazeEffect(
                                state = hazeState,
                                style = HazeMaterials.ultraThin(hazeThinColor),
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = if (isSearchActive) stringResource(R.string.cancel) else stringResource(
                                R.string.add_task_description
                            ), tint = fabIconTint, modifier = Modifier.rotate(rotationAngle.value)
                        )
                    }
                }
            },
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(colorScheme.surfaceDim),
            contentPadding = FloatingToolbarDefaults.ContentPadding,
        ) {
            IconButton(onClick = {
                isSearchActive = true
            }) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search_tasks_description),
                    tint = colorScheme.onSurface
                )
            }
            Box(
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = showActionIconsExceptSearch && !isSearchActive,
                        enter = fadeIn(animationSpec = tween(durationMillis = 500)),
                        exit = shrinkHorizontally(
                            animationSpec = tween(
                                durationMillis = iconGroupExitAnimationDuration,
                                delayMillis = iconsClearanceTime
                            )
                        ) + fadeOut(
                            animationSpec = tween(
                                durationMillis = iconGroupExitAnimationDuration,
                                delayMillis = iconsClearanceTime
                            )
                        )
                    ) {
                        Row {
                            val iconAlphaTarget = if (isSearchActive) 0f else 1f

                            val updateIconAlpha by animateFloatAsState(
                                targetValue = iconAlphaTarget, animationSpec = tween(
                                    durationMillis = iconsAlphaDuration,
                                    delayMillis = if (isSearchActive) 0 else 0
                                ), label = "UpdateIconAlpha"
                            )
                            val hasUpdate = true
                            val onDownloadUpdateClick = { /* Placeholder */ }

                            if (hasUpdate) {
                                val downloadProgress = 0.0f

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                        .size(40.dp)
                                        .alpha(updateIconAlpha)
                                        .clip(RoundedCornerShape(100f))
                                        .background(colorScheme.primary)
                                        .clickable(
                                            enabled = !isSearchActive && showActionIconsExceptSearch,
                                            onClick = {
                                                onDownloadUpdateClick()
                                            }
                                        ),
                                        ) {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = "Download update",
                                            tint = colorScheme.onPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (downloadProgress > 0f && downloadProgress < 1f) {
                                            CircularProgressIndicator(
                                                progress = { downloadProgress },
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
                    }
                }

                val fraction by animateFloatAsState(
                    targetValue = if (canShowTextField) 1F else 0F,
                    animationSpec = tween(durationMillis = textFieldAnimationDuration)
                )
                XenonTextFieldV2(
                    value = currentSearchQuery,
                    enabled = canShowTextField,
                    onValueChange = {
                        onSearchQueryChanged(it)
                    },
                    modifier = Modifier
                        .width(maxTextFieldWidth.times(fraction))
                        .alpha(fraction * fraction)
                        .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                    })
                )
            }
        }
    }
}
