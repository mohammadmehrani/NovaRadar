package com.irnova.novaradar.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.irnova.novaradar.data.model.ScanResult
import com.irnova.novaradar.ui.theme.RadarGreen
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarAnimation(
    isScanning: Boolean,
    topResults: List<ScanResult>,
    modifier: Modifier = Modifier,
    radarColor: Color = RadarGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val scanPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // Draw background circles
        for (i in 1..4) {
            drawCircle(
                color = radarColor.copy(alpha = 0.1f * i),
                radius = radius * (i / 4f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Draw cross lines
        drawLine(
            color = radarColor.copy(alpha = 0.2f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = radarColor.copy(alpha = 0.2f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.dp.toPx()
        )

        // Draw Scanning Sweep (The rotating part)
        if (isScanning) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    0.5f to radarColor.copy(alpha = 0.3f),
                    1f to radarColor,
                    center = center
                ),
                startAngle = rotation,
                sweepAngle = 90f,
                useCenter = true,
                size = size
            )
        }

        // Draw Results as Points on Radar
        topResults.take(10).forEachIndexed { index, result ->
            val angle = (index * 36) % 360f // Spread points
            // Latency lower = closer to center
            val normalizedDistance = (result.latencyMs.coerceIn(50, 1000) / 1000f) * radius
            
            val x = center.x + normalizedDistance * cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = center.y + normalizedDistance * sin(Math.toRadians(angle.toDouble())).toFloat()

            val pointColor = when {
                result.latencyMs < 200 -> Color.Green
                result.latencyMs < 500 -> Color.Yellow
                else -> Color.Red
            }

            drawCircle(
                color = pointColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            
            // Pulse for scanned items
            if (isScanning) {
                drawCircle(
                    color = pointColor.copy(alpha = 1f - scanPulse),
                    radius = (4.dp.toPx() + (scanPulse * 15.dp.toPx())),
                    center = Offset(x, y),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
