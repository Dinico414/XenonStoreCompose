package com.xenon.store.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
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
import com.xenon.store.ui.theme.backgroundDark
import com.xenon.store.ui.theme.backgroundLight
import com.xenon.store.ui.theme.errorContainerDark
import com.xenon.store.ui.theme.errorContainerLight
import com.xenon.store.ui.theme.errorDark
import com.xenon.store.ui.theme.errorLight
import com.xenon.store.ui.theme.inverseErrorContainerDark
import com.xenon.store.ui.theme.inverseErrorContainerLight
import com.xenon.store.ui.theme.inverseErrorDark
import com.xenon.store.ui.theme.inverseErrorLight
import com.xenon.store.ui.theme.inverseOnErrorContainerDark
import com.xenon.store.ui.theme.inverseOnErrorContainerLight
import com.xenon.store.ui.theme.inverseOnErrorDark
import com.xenon.store.ui.theme.inverseOnErrorLight
import com.xenon.store.ui.theme.inverseOnSurfaceDark
import com.xenon.store.ui.theme.inverseOnSurfaceLight
import com.xenon.store.ui.theme.inversePrimaryDark
import com.xenon.store.ui.theme.inversePrimaryLight
import com.xenon.store.ui.theme.inverseSurfaceDark
import com.xenon.store.ui.theme.inverseSurfaceLight
import com.xenon.store.ui.theme.onBackgroundDark
import com.xenon.store.ui.theme.onBackgroundLight
import com.xenon.store.ui.theme.onErrorContainerDark
import com.xenon.store.ui.theme.onErrorContainerLight
import com.xenon.store.ui.theme.onErrorDark
import com.xenon.store.ui.theme.onErrorLight
import com.xenon.store.ui.theme.onPrimaryContainerDark
import com.xenon.store.ui.theme.onPrimaryContainerLight
import com.xenon.store.ui.theme.onPrimaryDark
import com.xenon.store.ui.theme.onPrimaryLight
import com.xenon.store.ui.theme.onSecondaryContainerDark
import com.xenon.store.ui.theme.onSecondaryContainerLight
import com.xenon.store.ui.theme.onSecondaryDark
import com.xenon.store.ui.theme.onSecondaryLight
import com.xenon.store.ui.theme.onSurfaceDark
import com.xenon.store.ui.theme.onSurfaceLight
import com.xenon.store.ui.theme.onSurfaceVariantDark
import com.xenon.store.ui.theme.onSurfaceVariantLight
import com.xenon.store.ui.theme.onTertiaryContainerDark
import com.xenon.store.ui.theme.onTertiaryContainerLight
import com.xenon.store.ui.theme.onTertiaryDark
import com.xenon.store.ui.theme.onTertiaryLight
import com.xenon.store.ui.theme.outlineDark
import com.xenon.store.ui.theme.outlineLight
import com.xenon.store.ui.theme.outlineVariantDark
import com.xenon.store.ui.theme.outlineVariantLight
import com.xenon.store.ui.theme.primaryContainerDark
import com.xenon.store.ui.theme.primaryContainerLight
import com.xenon.store.ui.theme.primaryDark
import com.xenon.store.ui.theme.primaryLight
import com.xenon.store.ui.theme.scrimDark
import com.xenon.store.ui.theme.scrimLight
import com.xenon.store.ui.theme.secondaryContainerDark
import com.xenon.store.ui.theme.secondaryContainerLight
import com.xenon.store.ui.theme.secondaryDark
import com.xenon.store.ui.theme.secondaryLight
import com.xenon.store.ui.theme.surfaceBrightDark
import com.xenon.store.ui.theme.surfaceBrightLight
import com.xenon.store.ui.theme.surfaceContainerDark
import com.xenon.store.ui.theme.surfaceContainerHighDark
import com.xenon.store.ui.theme.surfaceContainerHighLight
import com.xenon.store.ui.theme.surfaceContainerHighestDark
import com.xenon.store.ui.theme.surfaceContainerHighestLight
import com.xenon.store.ui.theme.surfaceContainerLight
import com.xenon.store.ui.theme.surfaceContainerLowDark
import com.xenon.store.ui.theme.surfaceContainerLowLight
import com.xenon.store.ui.theme.surfaceContainerLowestDark
import com.xenon.store.ui.theme.surfaceContainerLowestLight
import com.xenon.store.ui.theme.surfaceDark
import com.xenon.store.ui.theme.surfaceDimDark
import com.xenon.store.ui.theme.surfaceDimLight
import com.xenon.store.ui.theme.surfaceLight
import com.xenon.store.ui.theme.surfaceVariantDark
import com.xenon.store.ui.theme.surfaceVariantLight
import com.xenon.store.ui.theme.tertiaryContainerDark
import com.xenon.store.ui.theme.tertiaryContainerLight
import com.xenon.store.ui.theme.tertiaryDark
import com.xenon.store.ui.theme.tertiaryLight

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
            colorScheme = baseColorScheme, typography = Typography, content = content
        )
    }
}
