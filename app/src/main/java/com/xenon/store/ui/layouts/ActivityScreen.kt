package com.xenon.store.ui.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement // Import for Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.material.ripple.rememberRipple // Deprecated
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple // Import for M3 ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.xenon.store.ui.values.LargerCornerRadius
import com.xenon.store.ui.values.LargestPadding
import com.xenon.store.ui.values.SmallPadding // Added for default value

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    titleText: String,
    hasNavigationIconExtraContent: Boolean = false,
    navigationIconContent: @Composable (() -> Unit)? = null,
    onNavigationIconClick: (() -> Unit)? = null,
    navigationIconExtraContent: @Composable RowScope.() -> Unit = {},
    appBarActions: @Composable RowScope.() -> Unit = {},
    appBarSecondaryActionIcon: @Composable RowScope.() -> Unit = {},
    collapsedAppBarTextColor: Color = MaterialTheme.colorScheme.onSurface,
    expandedAppBarTextColor: Color = MaterialTheme.colorScheme.primary,
    appBarNavigationIconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    appBarActionIconContentColor: Color = MaterialTheme.colorScheme.onSurface,
    screenBackgroundColor: Color = MaterialTheme.colorScheme.surfaceDim,
    contentBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentCornerRadius: Dp = LargerCornerRadius,
    buttonPadding: Dp = LargestPadding,
    navigationIconStartPadding: Dp = SmallPadding,
    navigationIconPadding: Dp = SmallPadding,
    navigationIconSpacing: Dp = SmallPadding,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
    dialogs: @Composable () -> Unit = {}
) {
    FlexibleTopAppBarLayout(
        title = { fontWeightFromAppBar, colorFromAppBar ->
            Text(
                text = titleText,
                fontFamily = QuicksandTitleVariable,
                color = colorFromAppBar,
                fontWeight = fontWeightFromAppBar
            )
        },
        navigationIcon = {
            if (navigationIconContent != null || navigationIconExtraContent != {}) {
                val iconButtonContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                val interactionSource = remember { MutableInteractionSource() }

                val currentHorizontalPadding = if (!hasNavigationIconExtraContent) {
                    buttonPadding
                } else {
                    buttonPadding / 2
                }

                var boxModifier = Modifier
                    .padding(horizontal = currentHorizontalPadding)
                    .clip(RoundedCornerShape(100.0f))
                    .background(iconButtonContainerColor)

                if (onNavigationIconClick != null) {
                    boxModifier = boxModifier.clickable(
                        onClick = onNavigationIconClick,
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    )
                }

                Box(
                    modifier = boxModifier,
                    contentAlignment = Alignment.CenterStart
                ) {
                    CompositionLocalProvider(LocalContentColor provides appBarNavigationIconContentColor) {
                        Row(
                            modifier = Modifier.padding(start = navigationIconStartPadding, end = navigationIconPadding).padding(vertical = navigationIconPadding),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(navigationIconSpacing)
                        ) {
                            if (navigationIconContent != null) {
                                navigationIconContent()
                            }
                            if (hasNavigationIconExtraContent) {
                                navigationIconExtraContent()
                            }}
                    }
                }
            }
        },
        navigationIconExtraContent = { },
        modifier = modifier,
        actionsIcon = appBarActions,
        secondaryActionIcon = appBarSecondaryActionIcon,
        collapsedTitleColor = collapsedAppBarTextColor,
        expandedTitleColor = expandedAppBarTextColor,
        containerColor = screenBackgroundColor,
        navigationIconContentColor = appBarNavigationIconContentColor,
        actionIconContentColor = appBarActionIconContentColor,
    ) { paddingValuesFromAppBar ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor)
                .padding(top = paddingValuesFromAppBar.calculateTopPadding())
                .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal).asPaddingValues())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(contentModifier)
                    .clip(
                        RoundedCornerShape(
                            topStart = contentCornerRadius,
                            topEnd = contentCornerRadius
                        )
                    )
                    .background(contentBackgroundColor)
            ) {
                content(paddingValuesFromAppBar)
            }
        }
        dialogs()
    }
}
