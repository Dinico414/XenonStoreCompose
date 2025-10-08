package com.xenonware.store.ui.theme

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
import com.xenonware.store.viewmodel.LayoutType

@Composable
fun ScreenEnvironment(
    themePreference: Int,
    coverTheme: Boolean,
    blackedOutModeEnabled: Boolean,
    content: @Composable (layoutType: com.xenonware.store.viewmodel.LayoutType, isLandscape: Boolean) -> Unit
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
            coverTheme -> _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COVER
            dimensionForLayout < 320.dp -> _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.SMALL
            dimensionForLayout < 600.dp -> _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COMPACT
            dimensionForLayout < 840.dp -> _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.MEDIUM
            else -> _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.EXPANDED
        }

        val appIsDarkTheme = when {
            layoutType == _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COVER -> true

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
                if (layoutType == _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COVER) Color.Black else MaterialTheme.colorScheme.surfaceDim
            val darkIconsForSystemBars =
                if (layoutType == _root_ide_package_.com.xenonware.store.viewmodel.LayoutType.COVER) false else !appIsDarkTheme

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