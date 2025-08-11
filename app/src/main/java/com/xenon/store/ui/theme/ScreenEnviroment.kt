package com.xenon.store.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenon.store.viewmodel.LayoutType

@Composable
fun ScreenEnvironment(
    themePreference: Int,
    coverTheme: Boolean,
    blackedOutModeEnabled: Boolean,
    content: @Composable (layoutType: LayoutType, isLandscape: Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useDarkTheme = when (themePreference) {
        0 -> false // Light
        1 -> true  // Dark
        else -> isSystemInDarkTheme() // System
    }
    val useDynamicColor = true

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = this.maxWidth
        val screenHeight = this.maxHeight
        val dimensionForLayout = if (isLandscape) screenHeight else screenWidth

        val layoutType = when {
            coverTheme -> LayoutType.COVER
            dimensionForLayout < 320.dp -> LayoutType.SMALL
            dimensionForLayout < 600.dp -> LayoutType.COMPACT
            dimensionForLayout < 840.dp -> LayoutType.MEDIUM
            else -> LayoutType.EXPANDED
        }

        val appIsDarkTheme = when {
            layoutType == LayoutType.COVER -> true

            else -> when (themePreference) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }
        }

        StoreTheme(
            darkTheme = useDarkTheme,
            useBlackedOutDarkTheme = if (useDarkTheme) blackedOutModeEnabled else false,
            dynamicColor = useDynamicColor
        ) {
            val systemUiController = rememberSystemUiController()
            val view = LocalView.current

            val systemBarColor =
                if (layoutType == LayoutType.COVER) Color.Black else MaterialTheme.colorScheme.surfaceDim
            val darkIconsForSystemBars =
                if (layoutType == LayoutType.COVER) false else !appIsDarkTheme

            if (!view.isInEditMode) {
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = systemBarColor, darkIcons = darkIconsForSystemBars
                    )
                    systemUiController.setNavigationBarColor(
                        color = Color.Transparent,
                        darkIcons = darkIconsForSystemBars,
                        navigationBarContrastEnforced = false
                    )
                }
            }
            content(layoutType, isLandscape)
        }
    }
}