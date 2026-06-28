package com.novaradar.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.novaradar.app.ui.components.StatWidget
import com.novaradar.app.ui.components.Wc
import com.novaradar.app.ui.components.WidgetCard
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.viewmodel.AppLanguage
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.AliveIp
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val tlsPorts = setOf(443, 2053, 2083, 2087, 2096, 8443)

@Composable
fun RadarScreen(viewModel: NovaRadarViewModel) {
    val context = LocalContext.current
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedCount by viewModel.scannedCount.collectAsState()
    val aliveCount by viewModel.aliveCount.collectAsState()
    val deadCount by viewModel.deadCount.collectAsState()
    val eta by viewModel.etaValue.collectAsState()
    val subnetScanning by viewModel.currentScanningSubnet.collectAsState()
    val allIps by viewModel.allAliveIps.collectAsState()
    val recentProbes by viewModel.recentProbes.collectAsState()

    val isLight = theme == AppTheme.PRISM_LIGHT
    val subPagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var showConfigBuilder by remember { mutableStateOf(false) }
    var showFullscreenRadar by remember { mutableStateOf(false) }
    var cfgUuid by remember { mutableStateOf("") }
    var cfgSni by remember { mutableStateOf("nova2.altramax083.workers.dev") }
    var cfgNetwork by remember { mutableStateOf("ws") }
    var cfgSecurity by remember { mutableStateOf("tls") }
    var cfgPath by remember { mutableStateOf("/") }

    val infiniteTransition = rememberInfiniteTransition(label = "sweep")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )

    val sweepBrush = remember {
        Brush.sweepGradient(
            0.0f to Color.Transparent, 0.5f to Color.Transparent,
            0.7f to Wc.primary.copy(alpha = 0.02f),
            0.85f to Wc.primary.copy(alpha = 0.20f),
            0.95f to Wc.primary.copy(alpha = 0.50f),
            1.0f to Wc.primary.copy(alpha = 0.85f)
        )
    }

    // Persistent random positions for top 10 IPs (stable across recompositions)
    val dotPositions = remember(allIps.take(10).hashCode()) {
        allIps.map { alive ->
            // Seed based on IP:port for consistency
            val seed = "${alive.ip}:${alive.port}".hashCode()
            val rng = Random(seed)
            // Random angle 0-360 and distance based on ping (closer for lower ping)
            val angle = rng.nextFloat() * 360f
            val normalizedPing = (alive.ping.coerceAtMost(1000).toFloat() / 1000f).coerceIn(0.05f, 0.95f)
            // Lower ping = closer to center (0.1 to 0.85 of outer radius)
            val distance = 0.1f + (1f - normalizedPing) * 0.75f
            val pa = rng.nextFloat() * 360f // phase offset for fade cycle
            DotPos(angle, distance, pa)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .padding(bottom = 88.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Sub-tabs
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("tab_radar" to 0, "tab_results" to 1).forEach { (key, idx) ->
                    val active = subPagerState.currentPage == idx
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (active) Wc.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable(remember { MutableInteractionSource() }, null) { coroutineScope.launch { subPagerState.animateScrollToPage(idx) } }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(
                                Localization.get(key, lang),
                                fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) Wc.primary else Color.Gray
                            )
                            if (idx == 1 && aliveCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                    Text("$aliveCount", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalPager(
                state = subPagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> ScannerTab(isScanning, scannedCount, aliveCount, deadCount, eta, subnetScanning, allIps, recentProbes, animatedAngle, sweepBrush, dotPositions, isLight, showFullscreenRadar, { showFullscreenRadar = it })
                    1 -> ResultsTab(viewModel, isScanning, aliveCount, allIps, context, lang, isLight, showConfigBuilder, { showConfigBuilder = it }, cfgUuid, { cfgUuid = it }, cfgSni, { cfgSni = it }, cfgNetwork, { cfgNetwork = it }, cfgSecurity, { cfgSecurity = it }, cfgPath, { cfgPath = it })
                }
            }
        }
    }

    // Fullscreen radar dialog
    if (showFullscreenRadar) {
        FullscreenRadarDialog(allIps, dotPositions, animatedAngle, sweepBrush, isLight, isScanning) { showFullscreenRadar = false }
    }
}

private data class DotPos(val angle: Float, val distance: Float, val phase: Float)

@Composable
private fun ScannerTab(
    isScanning: Boolean, scannedCount: Int, aliveCount: Int, deadCount: Int,
    eta: String, subnetScanning: String, allIps: List<AliveIp>, recentProbes: List<String>,
    animatedAngle: Float, sweepBrush: Brush, dotPositions: List<DotPos>,
    isLight: Boolean, showFullscreenRadar: Boolean, onShowFullscreen: (Boolean) -> Unit
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Radar widget — fixed height, not scrollable
        WidgetCard(isLightTheme = isLight, borderColor = Wc.primary.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp, max = 320.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().clip(CircleShape)
                    .background(if (isLight) Color(0xFFF0FDF4) else Color(0xFF021708))
                    .border(1.5.dp, Wc.primary.copy(alpha = if (isLight) 0.3f else 0.5f), CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null) { onShowFullscreen(true) },
                contentAlignment = Alignment.Center
            ) {
                RadarCanvas(allIps, dotPositions, animatedAngle, sweepBrush, isScanning)
            }
        }

        // Status bar
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(if (isLight) Color.White.copy(alpha = 0.5f) else Color(0xFF0D1219).copy(alpha = 0.5f))
                .border(0.5.dp, Wc.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (isScanning) Wc.success else Wc.success.copy(alpha = 0.3f)))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isScanning) subnetScanning.ifEmpty { "SCANNING..." } else "STANDBY",
                        fontFamily = FontFamily.Monospace, fontSize = 9.sp,
                        color = (if (isScanning) Wc.success else Wc.successLight).copy(alpha = 0.8f)
                    )
                }
                Text("ETA $eta", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Wc.warning.copy(alpha = 0.7f))
            }
        }

        // 2x2 stat grid — compact
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatWidget(Modifier.weight(1f), "SCANNED", "$scannedCount", Wc.primary, Wc.primary, isLight)
            StatWidget(Modifier.weight(1f), "ALIVE", "$aliveCount", Wc.success, Wc.success, isLight)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatWidget(Modifier.weight(1f), "DEAD", "$deadCount", Wc.error, Wc.error, isLight)
            StatWidget(Modifier.weight(1f), "ETA", eta, Wc.warning, Wc.warning, isLight)
        }

        // Probe feed — scrollable within remaining space
        WidgetCard(isLightTheme = isLight, borderColor = Wc.primary.copy(alpha = 0.08f), modifier = Modifier.weight(1f)) {
            Text("PROBE FEED", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Wc.primary.copy(alpha = 0.5f), letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
            if (recentProbes.isEmpty()) {
                Text("awaiting scan...", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Wc.primary.copy(alpha = 0.2f))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    recentProbes.take(5).forEach { entry ->
                        Text(entry, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = (if (isLight) Color(0xFF1A202C) else Wc.success).copy(alpha = 0.6f), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarCanvas(
    allIps: List<AliveIp>, dotPositions: List<DotPos>,
    animatedAngle: Float, sweepBrush: Brush, isScanning: Boolean
) {
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        val outer = r * 0.70f

        // Grid circles
        drawCircle(color = Wc.primary.copy(alpha = 0.25f), radius = r * 0.35f, style = Stroke(1.2f))
        drawCircle(color = Wc.primary.copy(alpha = 0.40f), radius = outer * 0.55f, style = Stroke(1.2f))
        drawCircle(color = Wc.primary.copy(alpha = 0.55f), radius = outer, style = Stroke(2.dp.toPx()))
        // Cross lines
        drawLine(color = Wc.primary.copy(alpha = 0.18f), start = Offset(c.x - outer, c.y), end = Offset(c.x + outer, c.y), strokeWidth = 1f)
        drawLine(color = Wc.primary.copy(alpha = 0.18f), start = Offset(c.x, c.y - outer), end = Offset(c.x, c.y + outer), strokeWidth = 1f)

        // Sweep gradient
        val sweepAlpha = if (isScanning) 1f else 0.15f
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(c.x, c.y)
            canvas.rotate(animatedAngle)
            drawCircle(brush = sweepBrush, radius = outer, alpha = sweepAlpha)
            canvas.restore()
        }

        // Sweep line
        val aRad = Math.toRadians(animatedAngle.toDouble())
        val ex = c.x + outer * cos(aRad).toFloat()
        val ey = c.y + outer * sin(aRad).toFloat()
        drawLine(color = Wc.primary.copy(alpha = 0.70f * sweepAlpha), start = c, end = Offset(ex, ey), strokeWidth = 2.5f)

        // Center dot
        drawCircle(color = Wc.primary, radius = 4.dp.toPx())
        drawCircle(color = Color.Transparent, radius = 8.dp.toPx(), style = Stroke(1.2f.dp.toPx()))

        // Draw top 10 dots with ping labels
        allIps.take(10).forEachIndexed { index, alive ->
            if (index < dotPositions.size) {
                val dp = dotPositions[index]
                val daRad = Math.toRadians(dp.angle.toDouble())
                val distPx = dp.distance * outer * 0.92f
                val dx = c.x + distPx * cos(daRad).toFloat()
                val dy = c.y + distPx * sin(daRad).toFloat()

                val dotColor = when { alive.ping < 200 -> Wc.successLight; alive.ping < 500 -> Wc.warning; else -> Wc.error }

                // Sweep highlight: dot is bright when sweep is near, fades behind
                val angleDiff = ((animatedAngle - dp.angle + 720f) % 360f) / 360f
                // angleDiff = 0 is just ahead of sweep, rises to 1 behind sweep
                val baseAlpha = if (isScanning) {
                    val highlight = (1f - angleDiff * 4f).coerceIn(0f, 1f) // bright ahead of sweep
                    val persist = (1f - ((angleDiff - 0.25f) * 2f).coerceIn(0f, 1f)) * 0.3f // fade behind
                    highlight + persist
                } else 0.2f
                val alpha = baseAlpha.coerceIn(0.05f, 1f)

                // Glow
                drawCircle(color = dotColor.copy(alpha = 0.25f * alpha), radius = 7.dp.toPx(), center = Offset(dx, dy))
                // Core dot
                drawCircle(color = dotColor.copy(alpha = alpha), radius = 3.5f.dp.toPx(), center = Offset(dx, dy))

                // Ping text label above dot
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb((alpha.coerceIn(0.3f, 1f) * 255).toInt(), 255, 255, 255)
                    textSize = 22f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                // If top 3, show ping; otherwise just show dot
                if (index < 5) {
                    drawContext.canvas.nativeCanvas.drawText("${alive.ping}", dx, dy - 10.dp.toPx(), paint)
                }
            }
        }
    }
}

@Composable
private fun FullscreenRadarDialog(
    allIps: List<AliveIp>, dotPositions: List<DotPos>,
    animatedAngle: Float, sweepBrush: Brush, isLight: Boolean, isScanning: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF8FAFC) else Color(0xFF0D1219))
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOP 10 TARGETS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Wc.primary, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                .background(if (isLight) Color(0xFFF0FDF4) else Color(0xFF021708))
                .border(1.5.dp, Wc.primary.copy(alpha = 0.4f), CircleShape)
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val outer = r * 0.72f
                    drawCircle(color = Wc.primary.copy(alpha = 0.20f), radius = r * 0.35f, style = Stroke(1.5f))
                    drawCircle(color = Wc.primary.copy(alpha = 0.35f), radius = outer * 0.55f, style = Stroke(1.5f))
                    drawCircle(color = Wc.primary.copy(alpha = 0.50f), radius = outer, style = Stroke(2.5f))
                    drawLine(color = Wc.primary.copy(alpha = 0.15f), start = Offset(c.x - outer, c.y), end = Offset(c.x + outer, c.y), strokeWidth = 1f)
                    drawLine(color = Wc.primary.copy(alpha = 0.15f), start = Offset(c.x, c.y - outer), end = Offset(c.x, c.y + outer), strokeWidth = 1f)
                    val sweepAlpha = if (isScanning) 1f else 0.15f
                    drawIntoCanvas { canvas ->
                        canvas.save(); canvas.translate(c.x, c.y); canvas.rotate(animatedAngle); drawCircle(brush = sweepBrush, radius = outer, alpha = sweepAlpha); canvas.restore()
                    }
                    val aRad = Math.toRadians(animatedAngle.toDouble())
                    val ex = c.x + outer * cos(aRad).toFloat()
                    val ey = c.y + outer * sin(aRad).toFloat()
                    drawLine(color = Wc.primary.copy(alpha = 0.70f * sweepAlpha), start = c, end = Offset(ex, ey), strokeWidth = 3f)
                    drawCircle(color = Wc.primary, radius = 5.dp.toPx())
                    drawCircle(color = Color.Transparent, radius = 9.dp.toPx(), style = Stroke(1.5f))
                    allIps.take(10).forEachIndexed { index, alive ->
                        if (index < dotPositions.size) {
                            val dp = dotPositions[index]
                            val daRad = Math.toRadians(dp.angle.toDouble())
                            val distPx = dp.distance * outer * 0.92f
                            val dx = c.x + distPx * cos(daRad).toFloat()
                            val dy = c.y + distPx * sin(daRad).toFloat()
                            val dotColor = when { alive.ping < 200 -> Wc.successLight; alive.ping < 500 -> Wc.warning; else -> Wc.error }
                            val angleDiff = ((animatedAngle - dp.angle + 720f) % 360f) / 360f
                            val baseAlpha = if (isScanning) {
                                ((1f - angleDiff * 4f).coerceIn(0f, 1f) + (1f - ((angleDiff - 0.25f) * 2f).coerceIn(0f, 1f)) * 0.3f)
                            } else 0.2f
                            val alpha = baseAlpha.coerceIn(0.05f, 1f)
                            drawCircle(color = dotColor.copy(alpha = 0.25f * alpha), radius = 8.dp.toPx(), center = Offset(dx, dy))
                            drawCircle(color = dotColor.copy(alpha = alpha), radius = 4.dp.toPx(), center = Offset(dx, dy))
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb((alpha.coerceIn(0.3f, 1f) * 255).toInt(), 255, 255, 255)
                                textSize = 26f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                            }
                            drawContext.canvas.nativeCanvas.drawText("${alive.ping}ms", dx, dy - 12.dp.toPx(), paint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Wc.primary)
                }
            }
            allIps.take(10).forEachIndexed { i, alive ->
                val col = when { alive.ping < 200 -> Wc.successLight; alive.ping < 500 -> Wc.warning; else -> Wc.error }
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${i + 1} ${alive.ip}:${alive.port}", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = if (isLight) Color(0xFF1A202C) else Wc.onSurfaceDark)
                    Text("${alive.ping}ms", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = col)
                }
            }
        }
    }
}
}

@Composable
private fun ResultsTab(
    viewModel: NovaRadarViewModel, isScanning: Boolean, aliveCount: Int, allIps: List<AliveIp>,
    context: android.content.Context, lang: AppLanguage, isLight: Boolean,
    showConfigBuilder: Boolean, onShowConfigBuilder: (Boolean) -> Unit,
    cfgUuid: String, onCfgUuid: (String) -> Unit,
    cfgSni: String, onCfgSni: (String) -> Unit,
    cfgNetwork: String, onCfgNetwork: (String) -> Unit,
    cfgSecurity: String, onCfgSecurity: (String) -> Unit,
    cfgPath: String, onCfgPath: (String) -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WidgetCard(isLightTheme = isLight) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (isScanning) Wc.warning else if (allIps.isNotEmpty()) Wc.success else Wc.error.copy(alpha = 0.4f)))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isScanning) "SCANNING..." else if (allIps.isNotEmpty()) "${allIps.size} TARGETS" else "NO TARGETS",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        color = if (isScanning) Wc.warning else if (allIps.isNotEmpty()) Wc.success else Wc.error.copy(alpha = 0.5f)
                    )
                    if (allIps.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text("Ø${allIps.map { it.ping }.average().toLong()}ms", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Wc.primary.copy(alpha = 0.6f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(Icons.Default.FormatListNumbered to { viewModel.copyTop10ToClipboard(context); Toast.makeText(context, Localization.get("copied_note", lang), Toast.LENGTH_SHORT).show() }, Icons.Default.ContentCopy to { viewModel.copyAllToClipboard(context) }, Icons.Default.Save to { viewModel.exportResultsToTxtFile(context) }, Icons.Default.Build to { onShowConfigBuilder(true) }).forEach { (icon, action) ->
                        IconButton(onClick = action, modifier = Modifier.size(24.dp)) { Icon(icon, null, tint = Wc.primary, modifier = Modifier.size(14.dp)) }
                    }
                }
            }
        }

        if (allIps.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Clash" to { viewModel.exportClash(context) }, "V2Ray" to { viewModel.exportV2Ray(context) }, "VLESS" to { viewModel.exportVLESS(context) }, "Sing-Box" to { viewModel.exportSingBox(context) }).forEach { (label, action) ->
                    Box(Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(6.dp)).background(Wc.success.copy(alpha = 0.12f)).border(0.5.dp, Wc.success.copy(alpha = 0.2f), RoundedCornerShape(6.dp)).clickable(remember { MutableInteractionSource() }, null, onClick = action), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Wc.success)
                    }
                }
            }
        }

        if (allIps.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isScanning) "AWAITING TARGETS" else "NO TARGETS", fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = (if (isScanning) Wc.warning else Wc.error).copy(alpha = 0.5f))
                    Spacer(Modifier.height(4.dp))
                    Text(if (isScanning) "IPs appear as verified" else "Start a scan", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray.copy(alpha = 0.5f))
                }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(if (isLight) Color(0xFFEDF2F7) else Color(0xFF1C2333)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("#", Modifier.width(20.dp), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("IP", Modifier.weight(1f), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("PING", Modifier.width(36.dp), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("HTTP", Modifier.width(32.dp), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("SPD", Modifier.width(32.dp), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(allIps, key = { _, ip -> "${ip.ip}:${ip.port}" }) { index, alive ->
                        val speedKey = "${alive.ip}:${alive.port}"
                        val speedStr = viewModel.speedResults.value[speedKey] ?: "--"
                        val pingColor = when { alive.ping < 200 -> Wc.success; alive.ping < 500 -> Wc.warning; else -> Wc.error }
                        val httpColor = when { alive.httpPing < 0 -> Color.Gray.copy(alpha = 0.3f); alive.httpPing < 300 -> Wc.success; alive.httpPing < 600 -> Wc.warning; else -> Wc.error }
                        Box(Modifier.fillMaxWidth().background(if (index % 2 == 0) Color.Transparent else Color.White.copy(alpha = 0.02f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", Modifier.width(20.dp), fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                Text(alive.ip, Modifier.weight(1f), fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = if (isLight) Color(0xFF1A202C) else Color(0xFFE8ECF4), maxLines = 1)
                                Text("${alive.ping}", Modifier.width(36.dp), fontSize = 7.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = pingColor)
                                Text(if (alive.httpPing > 0) "${alive.httpPing}" else "--", Modifier.width(32.dp), fontSize = 7.sp, fontFamily = FontFamily.Monospace, color = httpColor)
                                Row(Modifier.width(32.dp), verticalAlignment = Alignment.CenterVertically) { Text(speedStr, fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Wc.warning.copy(alpha = 0.8f)) }
                            }
                            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 8.dp).height(1.dp)) {
                                val bw = (1f - (alive.ping.coerceAtMost(1000).toFloat() / 1000f)).coerceIn(0.02f, 0.98f)
                                Box(Modifier.weight(bw).fillMaxHeight().background(pingColor.copy(alpha = 0.3f)))
                                Box(Modifier.weight(1f - bw).fillMaxHeight().background(Color.Gray.copy(alpha = 0.06f)))
                            }
                            Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(":${alive.port}", fontSize = 6.sp, fontFamily = FontFamily.Monospace, color = Wc.primary.copy(alpha = 0.4f))
                                if (alive.port in tlsPorts) { Box(Modifier.clip(RoundedCornerShape(2.dp)).background(Wc.primary.copy(alpha = 0.08f)).padding(horizontal = 3.dp)) { Text("TLS", fontSize = 5.sp, color = Wc.primary.copy(alpha = 0.5f)) } }
                                Box(Modifier.clip(RoundedCornerShape(2.dp)).background(Wc.primary.copy(alpha = 0.08f)).padding(horizontal = 3.dp)) { Text("Nova-${alive.novaId}", fontSize = 5.sp, color = Wc.primary.copy(alpha = 0.4f)) }
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)).background(Wc.info.copy(alpha = 0.15f)).clickable(remember { MutableInteractionSource() }, null) { viewModel.runSpeedTest(alive.ip, alive.port) }, contentAlignment = Alignment.Center) { Text("⚡", fontSize = 7.sp) }
                                Box(Modifier.size(18.dp).clip(RoundedCornerShape(3.dp)).background(Wc.success.copy(alpha = 0.15f)).clickable(remember { MutableInteractionSource() }, null) { viewModel.copyIndividualToClipboard(context, alive) }, contentAlignment = Alignment.Center) { Text("cpy", fontSize = 6.sp, color = Wc.success) }
                            }
                        }
                    }
                }
            }
        }
    }
}
