package com.xenon.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xenon.store.R
import com.xenon.store.ui.values.LargestPadding
import kotlin.math.sqrt

@OptIn(ExperimentalTextApi::class)
val QuicksandTitleVariable = FontFamily(
    Font(
        R.font.quicksand_variable_font_wght,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsingAppBarLayout(
    modifier: Modifier = Modifier,
    collapsedHeight: Dp = 64.dp,
    title: @Composable (fraction: Float) -> Unit = { _ -> },
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleAlignment: Alignment = Alignment.CenterStart,
    navigationIconAlignment: Alignment.Vertical = Alignment.Top,
    expandable: Boolean = true,
    expandedContainerColor: Color = MaterialTheme.colorScheme.surfaceDim,
    collapsedContainerColor: Color = MaterialTheme.colorScheme.surfaceDim,
    navigationIconContentColor: Color = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable (paddingValues: PaddingValues) -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val expandedHeight = remember(expandable) {
        if (expandable) (screenHeight / 100) * 30 else collapsedHeight
    }

    val scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            rememberTopAppBarState()
        )

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {},
                collapsedHeight = collapsedHeight,
                expandedHeight = expandedHeight,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = expandedContainerColor,
                    scrolledContainerColor = collapsedContainerColor,
                    navigationIconContentColor = navigationIconContentColor,
                    titleContentColor = Color.Transparent,
                    actionIconContentColor = actionIconContentColor
                ),
                scrollBehavior = scrollBehavior
            )

            val fraction = if (expandable) scrollBehavior.state.collapsedFraction else 1f
            val curHeight = collapsedHeight.times(fraction) +
                    expandedHeight.times(1 - fraction)
            val offset = curHeight - collapsedHeight
            var boxWidth by remember { mutableIntStateOf(0) }
            val titlePadding = sqrt(fraction) * (boxWidth / LocalDensity.current.density)

            CenterAlignedTopAppBar(
                expandedHeight = curHeight,
                title = {
                    // navigationIcon
                    Box(
                        modifier = Modifier
                            .height(curHeight)
                            .then(
                                when (navigationIconAlignment) {
                                    Alignment.Top -> Modifier.padding(bottom = offset)
                                    Alignment.Bottom -> Modifier.padding(top = offset)
                                    else -> Modifier
                                }
                            )
                            .onGloballyPositioned { layoutCoordinates ->
                                if(layoutCoordinates.size.width != boxWidth)
                                    boxWidth = layoutCoordinates.size.width
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        navigationIcon()
                    }
                    // title
                    Box(
                        contentAlignment = titleAlignment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                when (titleAlignment) {
                                    Alignment.Center, Alignment.CenterStart, Alignment.CenterEnd ->
                                        Modifier.height(curHeight)
                                    Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd ->
                                        Modifier.padding(top = offset)
                                    else -> Modifier
                                }
                                    .padding(start = titlePadding.dp + 8.dp)
                            ),
                    ) {
                        title(fraction)
                    }
                    // actions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(curHeight)
                            .then(
                                when (navigationIconAlignment) {
                                    Alignment.Top -> Modifier.padding(bottom = offset)
                                    Alignment.Bottom -> Modifier.padding(top = offset)
                                    else -> Modifier
                                }
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row {
                            actions()
                            Spacer(modifier = Modifier.width(LargestPadding))
                        }
                    }
                },
                navigationIcon = {},
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = navigationIconContentColor,
                    actionIconContentColor = actionIconContentColor
                ),
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}