package com.xenonware.store.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme.Companion.expressive
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

data class ExtendedMaterialColorScheme(
    val inverseError: Color,
    val inverseOnError: Color,
    val inverseErrorContainer: Color,
    val inverseOnErrorContainer: Color,
)

val LocalExtendedMaterialColorScheme = staticCompositionLocalOf<ExtendedMaterialColorScheme> {
    error("No ExtendedMaterialColorScheme provided. Did you forget to wrap your Composable in XenonStoreTheme?")
}

val extendedMaterialColorScheme: ExtendedMaterialColorScheme
    @Composable @ReadOnlyComposable get() = LocalExtendedMaterialColorScheme.current


private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight
)

fun Color.decreaseBrightness(factor: Float): Color {
    val hsv = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsv)

    hsv[2] = hsv[2] * factor.coerceIn(0f, 1f)

    return Color(ColorUtils.HSLToColor(hsv))
}
fun ColorScheme.toBlackedOut(): ColorScheme {
    return this.copy(
        background = surfaceDimDark.decreaseBrightness(0.5f),
        surfaceContainer = Color.Black,
        surfaceBright = surfaceDimDark
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StoreTheme(
    darkTheme: Boolean,
    useBlackedOutDarkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val baseColorScheme: ColorScheme = if (darkTheme) {
        val baseDarkScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicDarkColorScheme(context)
        } else {
            DarkColorScheme
        }
        if (useBlackedOutDarkTheme) {
            baseDarkScheme.toBlackedOut()
        } else {
            baseDarkScheme
        }
    } else {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicLightColorScheme(context)
        } else {
            LightColorScheme
        }
    }

    val extendedColorScheme = remember(darkTheme) {
        if (darkTheme) {
            ExtendedMaterialColorScheme(
                inverseError = inverseErrorDark,
                inverseOnError = inverseOnErrorDark,
                inverseErrorContainer = inverseErrorContainerDark,
                inverseOnErrorContainer = inverseOnErrorContainerDark
            )
        } else {
            ExtendedMaterialColorScheme(
                inverseError = inverseErrorLight,
                inverseOnError = inverseOnErrorLight,
                inverseErrorContainer = inverseErrorContainerLight,
                inverseOnErrorContainer = inverseOnErrorContainerLight
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalExtendedMaterialColorScheme provides extendedColorScheme) {
        MaterialTheme(
            colorScheme = baseColorScheme, typography = Typography, motionScheme = expressive(), content = content
        )
    }
}
