package com.xenon.store.ui.res

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GoogleProfilBorder(
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    gapAngle: Float = 15f,
    angleChangeIntervalMillis: Long = 2000,
    sweepAnimationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
) {
    val googleColors = remember {
        listOf(
            Color(0xFF4285F4),
            Color(0xFFDB4437),
            Color(0xFFF4B400),
            Color(0xFF0F9D58)
        )
    }
    val numColors = googleColors.size

    val rotationAngleAnim = remember { Animatable(0f) }

    val sweepAngleAnimatables = remember {
        val initialAngles = generateRandomSweepAngles(numColors, gapAngle)
        initialAngles.map { Animatable(it) }
    }

    var targetSweepAnglesHolder by remember {
        mutableStateOf(sweepAngleAnimatables.map { it.value })
    }

    LaunchedEffect(Unit) {
        rotationAngleAnim.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(angleChangeIntervalMillis, numColors, gapAngle) {
        while (true) {
            delay(angleChangeIntervalMillis)
            targetSweepAnglesHolder = generateRandomSweepAngles(numColors, gapAngle)
        }
    }

    LaunchedEffect(targetSweepAnglesHolder) {
        coroutineScope {
            targetSweepAnglesHolder.forEachIndexed { index, targetAngle ->
                if (index < sweepAngleAnimatables.size) {
                    launch {
                        sweepAngleAnimatables[index].animateTo(
                            targetValue = targetAngle,
                            animationSpec = sweepAnimationSpec
                        )
                    }
                }
            }
        }
    }

    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val strokeWidthPx = with(density) { strokeWidth.toPx() }
        val canvasSize = size.minDimension
        val arcRadius = canvasSize / 2 - strokeWidthPx / 2
        val componentStrokeWidth = strokeWidthPx

        var currentStartAngle = rotationAngleAnim.value - 90f

        for (i in googleColors.indices) {
            val sweep = sweepAngleAnimatables[i].value
            if (sweep <= 0.1f) continue

            drawArc(
                color = googleColors[i],
                startAngle = currentStartAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(
                    (size.width - 2 * arcRadius - componentStrokeWidth) / 2,
                    (size.height - 2 * arcRadius - componentStrokeWidth) / 2
                ),
                size = Size(arcRadius * 2 + componentStrokeWidth, arcRadius * 2 + componentStrokeWidth),
                style = Stroke(width = componentStrokeWidth, cap = StrokeCap.Round)
            )
            currentStartAngle += sweep + gapAngle
        }
    }
}

private fun generateRandomSweepAngles(count: Int, gapAngle: Float): List<Float> {
    if (count <= 0) return emptyList()
    val totalGapAngles = count * gapAngle
    val totalSweepRequired = (360f - totalGapAngles).coerceAtLeast(0f)

    if (totalSweepRequired == 0f) {
        return List(count) { 0f }
    }

    val randomValues = List(count) { Random.nextFloat() * 90f + 10f }
    val sumRandomValues = randomValues.sum()

    if (sumRandomValues == 0f) {
        return List(count) { totalSweepRequired / count }
    }

    return randomValues.map { (it / sumRandomValues) * totalSweepRequired }
}
