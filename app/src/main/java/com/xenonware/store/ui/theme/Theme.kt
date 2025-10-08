package com.xenonware.store.ui.theme

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
import com.xenonware.store.ui.theme.backgroundDark
import com.xenonware.store.ui.theme.backgroundLight
import com.xenonware.store.ui.theme.errorContainerDark
import com.xenonware.store.ui.theme.errorContainerLight
import com.xenonware.store.ui.theme.errorDark
import com.xenonware.store.ui.theme.errorLight
import com.xenonware.store.ui.theme.inverseErrorContainerDark
import com.xenonware.store.ui.theme.inverseErrorContainerLight
import com.xenonware.store.ui.theme.inverseErrorDark
import com.xenonware.store.ui.theme.inverseErrorLight
import com.xenonware.store.ui.theme.inverseOnErrorContainerDark
import com.xenonware.store.ui.theme.inverseOnErrorContainerLight
import com.xenonware.store.ui.theme.inverseOnErrorDark
import com.xenonware.store.ui.theme.inverseOnErrorLight
import com.xenonware.store.ui.theme.inverseOnSurfaceDark
import com.xenonware.store.ui.theme.inverseOnSurfaceLight
import com.xenonware.store.ui.theme.inversePrimaryDark
import com.xenonware.store.ui.theme.inversePrimaryLight
import com.xenonware.store.ui.theme.inverseSurfaceDark
import com.xenonware.store.ui.theme.inverseSurfaceLight
import com.xenonware.store.ui.theme.onBackgroundDark
import com.xenonware.store.ui.theme.onBackgroundLight
import com.xenonware.store.ui.theme.onErrorContainerDark
import com.xenonware.store.ui.theme.onErrorContainerLight
import com.xenonware.store.ui.theme.onErrorDark
import com.xenonware.store.ui.theme.onErrorLight
import com.xenonware.store.ui.theme.onPrimaryContainerDark
import com.xenonware.store.ui.theme.onPrimaryContainerLight
import com.xenonware.store.ui.theme.onPrimaryDark
import com.xenonware.store.ui.theme.onPrimaryLight
import com.xenonware.store.ui.theme.onSecondaryContainerDark
import com.xenonware.store.ui.theme.onSecondaryContainerLight
import com.xenonware.store.ui.theme.onSecondaryDark
import com.xenonware.store.ui.theme.onSecondaryLight
import com.xenonware.store.ui.theme.onSurfaceDark
import com.xenonware.store.ui.theme.onSurfaceLight
import com.xenonware.store.ui.theme.onSurfaceVariantDark
import com.xenonware.store.ui.theme.onSurfaceVariantLight
import com.xenonware.store.ui.theme.onTertiaryContainerDark
import com.xenonware.store.ui.theme.onTertiaryContainerLight
import com.xenonware.store.ui.theme.onTertiaryDark
import com.xenonware.store.ui.theme.onTertiaryLight
import com.xenonware.store.ui.theme.outlineDark
import com.xenonware.store.ui.theme.outlineLight
import com.xenonware.store.ui.theme.outlineVariantDark
import com.xenonware.store.ui.theme.outlineVariantLight
import com.xenonware.store.ui.theme.primaryContainerDark
import com.xenonware.store.ui.theme.primaryContainerLight
import com.xenonware.store.ui.theme.primaryDark
import com.xenonware.store.ui.theme.primaryLight
import com.xenonware.store.ui.theme.scrimDark
import com.xenonware.store.ui.theme.scrimLight
import com.xenonware.store.ui.theme.secondaryContainerDark
import com.xenonware.store.ui.theme.secondaryContainerLight
import com.xenonware.store.ui.theme.secondaryDark
import com.xenonware.store.ui.theme.secondaryLight
import com.xenonware.store.ui.theme.surfaceBrightDark
import com.xenonware.store.ui.theme.surfaceBrightLight
import com.xenonware.store.ui.theme.surfaceContainerDark
import com.xenonware.store.ui.theme.surfaceContainerHighDark
import com.xenonware.store.ui.theme.surfaceContainerHighLight
import com.xenonware.store.ui.theme.surfaceContainerHighestDark
import com.xenonware.store.ui.theme.surfaceContainerHighestLight
import com.xenonware.store.ui.theme.surfaceContainerLight
import com.xenonware.store.ui.theme.surfaceContainerLowDark
import com.xenonware.store.ui.theme.surfaceContainerLowLight
import com.xenonware.store.ui.theme.surfaceContainerLowestDark
import com.xenonware.store.ui.theme.surfaceContainerLowestLight
import com.xenonware.store.ui.theme.surfaceDark
import com.xenonware.store.ui.theme.surfaceDimDark
import com.xenonware.store.ui.theme.surfaceDimLight
import com.xenonware.store.ui.theme.surfaceLight
import com.xenonware.store.ui.theme.surfaceVariantDark
import com.xenonware.store.ui.theme.surfaceVariantLight
import com.xenonware.store.ui.theme.tertiaryContainerDark
import com.xenonware.store.ui.theme.tertiaryContainerLight
import com.xenonware.store.ui.theme.tertiaryDark
import com.xenonware.store.ui.theme.tertiaryLight

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
