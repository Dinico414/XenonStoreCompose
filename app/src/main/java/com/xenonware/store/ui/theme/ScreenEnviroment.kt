package com.xenonware.store.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.xenonware.store.viewmodel.LayoutType

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

@Composable
fun rememberAppBarExpandableState(
    layoutType: LayoutType,
    isLandscape: Boolean,
    appSize: IntSize
): Boolean {

    //Universal for Split-screen
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

    //Only Surface Duo
    fun isSurfaceDuoDevice(): Boolean {
        val model = Build.MODEL
        return model.contains("Surface Duo", ignoreCase = true) ||
                model.contains("Surface Duo 2", ignoreCase = true)
    }

    fun isSurfaceDuoSingleScreen(currentAppSize: IntSize): Boolean {
        val duo1Portrait = IntSize(1350, 1800)
        val duo2Portrait = IntSize(1344, 1892)

        return currentAppSize == duo1Portrait || currentAppSize == duo2Portrait
    }

    //true false return
    return when (layoutType) {
        LayoutType.COVER -> false
        LayoutType.SMALL -> false
        LayoutType.COMPACT -> {
            if (isSurfaceDuoDevice()) {
                !(isSurfaceDuoSingleScreen(appSize) && !isLandscape)
            }
            else {
                !isLandscape || !aspectRatioConditionMet
            }
        }
        LayoutType.MEDIUM -> true // Keep as true for MEDIUM
        LayoutType.EXPANDED -> true // Keep as true for EXPANDED
    }
}