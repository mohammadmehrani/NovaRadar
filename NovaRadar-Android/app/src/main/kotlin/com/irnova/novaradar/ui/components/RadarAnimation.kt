package com.irnova.novaradar.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.irnova.novaradar.data.model.ScanResult
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.irnova.novaradar.ui.theme.RadarGreen

@Composable
fun RadarAnimation(
    isScanning: Boolean,
    topResults: List<ScanResult>,
    modifier: Modifier = Modifier
) {
    // یک پیاده‌سازی موقت برای جلوگیری از خطا در بیلد
    Canvas(modifier = modifier) {
        drawCircle(
            color = RadarGreen.copy(alpha = 0.2f),
            radius = size.minDimension / 2,
            style = Stroke(width = 2f)
        )
    }
}
