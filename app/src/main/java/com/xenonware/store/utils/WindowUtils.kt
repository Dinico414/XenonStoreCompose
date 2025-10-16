package com.xenonware.store.utils

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.xenonware.store.viewmodel.LayoutType

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
        LayoutType.MEDIUM -> true
        LayoutType.EXPANDED -> true
    }
}