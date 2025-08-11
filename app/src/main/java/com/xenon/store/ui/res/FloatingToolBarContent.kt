package com.xenon.store.ui.res

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xenon.store.R
import com.xenon.store.ui.values.LargePadding
import com.xenon.store.ui.values.SmallElevation
import com.xenon.store.viewmodel.LayoutType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalHazeMaterialsApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun FloatingToolbarContent(
    hazeState: HazeState,
    onOpenSettings: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    currentSearchQuery: String,
    widthSizeClass: WindowWidthSizeClass,
    layoutType: LayoutType,
    appSize: IntSize,
) {
    val localContext = LocalContext.current
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var canShowTextField by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val iconsAlphaDuration = 500
    val iconGroupExitAnimationDuration = 100
    val iconsClearanceTime = iconsAlphaDuration + 200
    val textFieldExistenceDelay = (iconsClearanceTime + iconsAlphaDuration).toLong()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val density = LocalDensity.current
    val appWidthDp = with(density) { appSize.width.toDp() }
    val appHeightDp = with(density) { appSize.height.toDp() }

    val startPadding = 16.dp
    val endPadding = 16.dp
    val internalStartPadding = 8.dp
    val internalEndPadding = 8.dp
    val iconSize = 48.dp

    val fabSpace = 56.dp + 8.dp

    val baseScreenWidthDp =
        if ((appWidthDp == 561.dp && appHeightDp == 748.dp) || (appWidthDp == 748.dp && appHeightDp == 561.dp)) {
            appWidthDp
        } else {
            screenWidthDp
        }

    val totalSubtractionForSearchActive =
        startPadding + internalStartPadding + iconSize + internalEndPadding + endPadding + fabSpace
    val totalSubtractionForSearchInactive =
        startPadding + internalStartPadding + (iconSize * 3) + internalEndPadding + endPadding

    val calculatedMaxWidth = if (isSearchActive) {
        baseScreenWidthDp - totalSubtractionForSearchActive
    } else {
        baseScreenWidthDp - totalSubtractionForSearchInactive
    }


    LaunchedEffect(isSearchActive) {
            delay(iconsClearanceTime.toLong() - iconGroupExitAnimationDuration)
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(textFieldExistenceDelay)
            canShowTextField = true
            delay(50)
        } else {
            canShowTextField = false
            keyboardController?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding() + LargePadding,
            ), contentAlignment = Alignment.Center
    ) {
        if (isSearchActive) {
            HorizontalFloatingToolbar(
                modifier = Modifier.height(64.dp),
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
                            val outline =
                                fabShape.createOutline(this.size, layoutDirection, density)
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
                        FloatingActionButton(
                            onClick = {
                                onSearchQueryChanged("")
                                isSearchActive = false // This will trigger recomposition to the other branch
                            },
                            containerColor = Color.Transparent, // Consider making this solid if haze isn't perfect
                            shape = fabShape,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .clip(FloatingActionButtonDefaults.shape)
                                .background(colorScheme.primary) // Ensure background for haze
                                .hazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.ultraThin(hazeThinColor),
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = fabIconTint,
                            )
                        }
                    }
                },
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(colorScheme.surfaceDim),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Search Icon (still clickable to potentially re-initiate search, though current logic closes it)
                    IconButton(onClick = { /* isSearchActive is already true */ }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_tasks_description),
                            tint = colorScheme.onSurface
                        )
                    }

                    // TextField is only shown when canShowTextField is true
                    // and canShowTextField is controlled by isSearchActive with a delay
                    AnimatedVisibility(visible = canShowTextField) {
                        val maxWidth = when {
                            layoutType == LayoutType.MEDIUM -> 280.dp
                            layoutType == LayoutType.EXPANDED && widthSizeClass == WindowWidthSizeClass.Expanded -> 280.dp
                            else -> if (calculatedMaxWidth > 0.dp) calculatedMaxWidth else 0.dp // Use calculatedMaxWidth
                        }
                        XenonTextFieldV2(
                            value = currentSearchQuery,
                            onValueChange = {
                                onSearchQueryChanged(it)
                            },
                            modifier = Modifier
                                .widthIn(max = maxWidth)
                                .focusRequester(focusRequester)
                                .weight(1f, fill = false),
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
        } else {
            // Layout WHEN SEARCH IS NOT ACTIVE (only action icons)
            HorizontalFloatingToolbar(
                modifier = Modifier.height(64.dp), // Ensure consistent height
                expanded = true,
                // NO floatingActionButton when search is not active
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors(colorScheme.surfaceDim),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        isSearchActive = true // This will trigger recomposition to the other branch
                    }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.search_tasks_description),
                            tint = colorScheme.onSurface
                        )
                    }
                    // Action icons are always visible in this branch (showActionIconsExceptSearch will be true)
                    // You can keep AnimatedVisibility for their individual fade in/out if desired,
                    // but the group's visibility is controlled by the outer if/else.
                    Row {
                        IconButton(
                            onClick = { Toast.makeText(localContext, "Coming soon", Toast.LENGTH_SHORT).show() }
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = stringResource(R.string.sort_tasks_description),
                                tint = colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { Toast.makeText(localContext, "Coming soon", Toast.LENGTH_SHORT).show() }
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = stringResource(R.string.filter_tasks_description),
                                tint = colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.settings),
                                tint = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
