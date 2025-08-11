package com.xenon.store.ui.layouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xenon.store.R
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt


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
fun FlexibleTopAppBarLayout(
    modifier: Modifier = Modifier,
    title: @Composable (fontWeight: FontWeight, color: Color) -> Unit = { _, _ -> },
    navigationIcon: @Composable () -> Unit = {},
    navigationIconExtraContent: @Composable RowScope.() -> Unit = {},
    actionsIcon: @Composable RowScope.() -> Unit = {},
    secondaryActionIcon: @Composable RowScope.() -> Unit = {},
    collapsedTitleColor: Color = colorScheme.onSurface,
    expandedTitleColor: Color = colorScheme.primary,
    containerColor: Color = colorScheme.surfaceDim,
    navigationIconContentColor: Color = colorScheme.onSurface,
    actionIconContentColor: Color = colorScheme.onSurface,
    content: @Composable (paddingValues: PaddingValues) -> Unit,
) {
    val topAppBarState = rememberTopAppBarState()

    val scrollBehavior: TopAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            state = topAppBarState,
        )

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = containerColor,
        topBar = {
            val fraction = scrollBehavior.state.collapsedFraction

            val curFontWeight by remember(fraction) {
                derivedStateOf {
                    val expandedWeight = 1000
                    val collapsedWeight = 100
                    val interpolatedWeight = lerp(expandedWeight.toFloat(), collapsedWeight.toFloat(), fraction).roundToInt()
                    FontWeight(interpolatedWeight.coerceIn(1, 1000))
                }
            }

            val currentTitleColor by remember(fraction, expandedTitleColor, collapsedTitleColor) {
                derivedStateOf {
                    androidx.compose.ui.graphics.lerp(expandedTitleColor, collapsedTitleColor, fraction)
                }
            }

            LargeTopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        title(curFontWeight, currentTitleColor)
                    }
                },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        navigationIcon()
                        navigationIconExtraContent()
                    }
                },
                actions = {
                    actionsIcon()
                    secondaryActionIcon()
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    navigationIconContentColor = navigationIconContentColor,
                    titleContentColor = currentTitleColor,
                    actionIconContentColor = actionIconContentColor
                )
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}
