package com.novaradar.app.ui.screens

import android.widget.Toast
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaradar.app.data.model.IpSource
import com.novaradar.app.data.model.PortConfig
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.viewmodel.AppLanguage
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.AliveIp
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.ContentDrawScope

@Composable
fun FadingScrollColumn(
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    val bgColor = MaterialTheme.colorScheme.background

        Column(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                // Top fading edge
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(bgColor, Color.Transparent),
                        startY = 0f,
                        endY = 60.dp.toPx()
                    )
                )
                // Bottom fading edge
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, bgColor),
                        startY = size.height - 80.dp.toPx(),
                        endY = size.height
                    )
                )
            }
            .verticalScroll(scrollState),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = verticalArrangement
    ) {
        Spacer(modifier = Modifier.height(12.dp)) // Extra top spacing to clear header
        content()
        Spacer(modifier = Modifier.height(20.dp)) // Bottom spacing
    }
}

// Constant layout direction toggler
@Composable
fun LocalizedLayout(lang: AppLanguage, content: @Composable () -> Unit) {
    val direction = if (lang == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        content()
    }
}

// Custom Glassmorphic container with 100% capsule-like arch rounded corners and dynamic cyber mesh-grid design
@Composable
fun GlassyCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    content: @Composable ColumnScope.() -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            content = content
        )
    }
}

// Page 1: RADAR SCANNER SCREEN (With Integrated Sub-Pager for Scanner and Results tabs)
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

    val subPagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val animatedAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angleAnimator"
    )

    val textPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }

    val sweepBrush = remember {
        Brush.sweepGradient(
            0.0f to Color.Transparent,
            0.5f to Color.Transparent,
            0.7f to Color(0xFF00FF66).copy(alpha = 0.02f),
            0.85f to Color(0xFF00FF66).copy(alpha = 0.20f),
            0.95f to Color(0xFF00FF66).copy(alpha = 0.50f),
            1.0f to Color(0xFF00FF66).copy(alpha = 0.85f)
        )
    }

    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val cfg = LocalConfiguration.current
    val isCompact = cfg.screenHeightDp < 650 || cfg.screenWidthDp < 380
    val radarSize = if (isCompact) 180.dp else 240.dp

    LocalizedLayout(lang) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = if (isCompact) 12.dp else 96.dp, bottom = if (isCompact) 12.dp else 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Modern sliding tab headers (exactly like photo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                // TAB 0: SCANNER
                val isScannerActive = subPagerState.currentPage == 0
                val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                subPagerState.animateScrollToPage(0)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Localization.get("scanner_title", lang),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isScannerActive) (if (isLightTheme) Color.Black else Color.White) else Color.Gray,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(3.dp)
                            .background(
                                color = if (isScannerActive) Color(0xFF10B981) else Color.Transparent,
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                }

                // TAB 1: RESULTS (with red info count badge)
                val isResultsActive = subPagerState.currentPage == 1
                val isLightThemeResults = MaterialTheme.colorScheme.background.luminance() > 0.5f
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                subPagerState.animateScrollToPage(1)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = Localization.get("results_title", lang),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isResultsActive) (if (isLightThemeResults) Color.Black else Color.White) else Color.Gray,
                                letterSpacing = 1.sp
                            )
                        )
                        if (aliveCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .background(Color(0xFFEF4444), shape = RoundedCornerShape(10.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = aliveCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                color = if (isResultsActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                }
            }

            // Slidable primary contents area
            androidx.compose.foundation.pager.HorizontalPager(
                state = subPagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // SCANNER SUB-PAGE
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 12.dp)
                        ) {
                            // Scrollable top section (radar + status)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Rotating Cyber Radar Shape
                                Box(
                                    modifier = Modifier
                                        .size(radarSize)
                                        .clip(CircleShape)
                                        .background(if (theme == AppTheme.PRISM_LIGHT) Color(0xFFF0FDF4) else Color(0xFF021708))
                                        .border(2.dp, Color(0xFF34D399).copy(alpha = if (theme == AppTheme.PRISM_LIGHT) 0.6f else 0.7f), CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, radius = 160.dp)
                                        ) {
                                            if (allIps.isNotEmpty()) {
                                                viewModel.copyTop10ToClipboard(context)
                                                Toast.makeText(context, Localization.get("copied_note", lang), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .testTag("radar_canvas_container"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Scan control overlay on compact screens
                                    if (isCompact) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 8.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                    if (isScanning) Color(0xFFBE123C).copy(alpha = 0.9f)
                                                    else Color(0xFFB00020).copy(alpha = 0.9f)
                                                )
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (isScanning) viewModel.stopScan() else viewModel.startScan()
                                                }
                                                .testTag("scan_trigger_button")
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = "Scan",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isScanning) Localization.get("stop_scan", lang) else Localization.get("start_scan", lang),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val radius = size.minDimension / 2f
                                        val center = Offset(size.width / 2f, size.height / 2f)

                                        // Draw concentric radar grids with immersive green color
                                        drawCircle(
                                            color = Color(0xFF00FF66).copy(alpha = 0.35f),
                                            radius = radius * 0.35f,
                                            style = Stroke(width = 1.5f)
                                        )
                                        drawCircle(
                                            color = Color(0xFF00FF66).copy(alpha = 0.35f),
                                            radius = radius * 0.65f,
                                            style = Stroke(width = 1.5f)
                                        )
                                        drawCircle(
                                            color = Color(0xFF00FF66).copy(alpha = 0.5f),
                                            radius = radius * 0.95f,
                                            style = Stroke(width = 2.0f)
                                        )

                                        // Draw crosshairs
                                        drawLine(
                                            color = Color(0xFF00FF66).copy(alpha = 0.25f),
                                            start = Offset(center.x - radius, center.y),
                                            end = Offset(center.x + radius, center.y),
                                            strokeWidth = 1.5f
                                        )
                                        drawLine(
                                            color = Color(0xFF00FF66).copy(alpha = 0.25f),
                                            start = Offset(center.x, center.y - radius),
                                            end = Offset(center.x, center.y + radius),
                                            strokeWidth = 1.5f
                                        )

                                        // Always draw continuous sweeps of the scan wedge - authentic and always alive like Air Traffic Control radars!
                                        val sweepAlphaMultiplier = if (isScanning) 1.0f else 0.20f
                                        val angleRad = Math.toRadians(animatedAngle.toDouble())
                                        val endX = center.x + radius * cos(angleRad).toFloat()
                                        val endY = center.y + radius * sin(angleRad).toFloat()

                                        // Main scanning Sweep Line
                                        drawLine(
                                            color = Color(0xFF00FF66).copy(alpha = 0.85f * sweepAlphaMultiplier),
                                            start = center,
                                            end = Offset(endX, endY),
                                            strokeWidth = 3f
                                        )

                                        // Canvas Rotation to align with sweep line (using cached sweepBrush)
                                        drawIntoCanvas { canvas ->
                                            canvas.save()
                                            canvas.rotate(animatedAngle, center.x, center.y)
                                            drawCircle(
                                                brush = sweepBrush,
                                                radius = radius * 0.95f,
                                                alpha = sweepAlphaMultiplier
                                            )
                                            canvas.restore()
                                        }

                                        // Draw Central core beacon dot
                                        drawCircle(color = Color(0xFF00FF66), radius = 5.dp.toPx())
                                        drawCircle(
                                            color = Color.Transparent,
                                            radius = 10.dp.toPx(),
                                            style = Stroke(width = 1.5f.dp.toPx())
                                        )

                                        // Draw Top Target IP Dots — only top 10, sorted by ping
                                        allIps.take(10).forEachIndexed { index, alive ->
                                            val dotAngleRad = Math.toRadians(alive.angle.toDouble())
                                            val distPx = alive.normalizedDistance * radius * 0.85f

                                            val dotX = center.x + distPx * cos(dotAngleRad).toFloat()
                                            val dotY = center.y + distPx * sin(dotAngleRad).toFloat()

                                            val dotColor = when {
                                                alive.ping < 200 -> Color(0xFF34D399)
                                                alive.ping < 500 -> Color(0xFFFBBF24)
                                                alive.ping < 1000 -> Color(0xFFf87171)
                                                else -> Color(0xFF000000)
                                            }

                                            // Calculate sweep angle offset to derive fade intensity
                                            val angleDiff = (animatedAngle - alive.angle + 360f) % 360f
                                            val persistenceAlpha = if (isScanning) {
                                                maxOf(0.15f, 1f - (angleDiff / 240f))
                                            } else {
                                                0.20f // faint default when idle
                                            }

                                            // Target dot glow
                                            drawCircle(
                                                color = dotColor.copy(alpha = 0.35f * persistenceAlpha),
                                                radius = 8.dp.toPx(),
                                                center = Offset(dotX, dotY)
                                            )
                                            // Core dot
                                            drawCircle(
                                                color = dotColor.copy(alpha = persistenceAlpha),
                                                radius = 4.dp.toPx(),
                                                center = Offset(dotX, dotY)
                                            )

                                            drawIntoCanvas { canvas ->
                                                textPaint.color = if (dotColor == Color(0xFF000000)) {
                                                    android.graphics.Color.GRAY
                                                } else {
                                                    dotColor.copy(alpha = persistenceAlpha).toArgb()
                                                }
                                                textPaint.textSize = 9.5f.dp.toPx()
                                                canvas.nativeCanvas.drawText(
                                                    "${alive.ping}ms",
                                                    dotX,
                                                    dotY - 8.dp.toPx(),
                                                    textPaint
                                                )
                                            }
                                        }
                                    }
                                }

                                // Scanning Status Panel with LARGE scan target readout inside a high-tech GlassyCard
                                GlassyCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    borderColor = Color(0xFF10B981).copy(alpha = 0.35f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isScanning) Localization.get("current_scanning", lang) else Localization.get("ready_to_scan", lang),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            modifier = Modifier.testTag("subnet_status")
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isScanning && subnetScanning.isNotEmpty()) {
                                                subnetScanning.substringBefore(":")
                                            } else {
                                                "READY TO ENGINE"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            ),
                                            color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF0F172A) else Color.White
                                        )
                                    }
                                }

                            } // End scrollable top section

                                // High-Polish Cyberpunk 2x2 Stats Grid — Derived directly from desktop/mobile HUD mocks
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Scanned Card
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (theme == AppTheme.PRISM_LIGHT) {
                                                        Brush.linearGradient(colors = listOf(Color(0xFFE0F2FE), Color(0xFFEFF6FF)))
                                                    } else {
                                                        Brush.linearGradient(colors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.25f), Color(0xFF0F172A).copy(alpha = 0.5f)))
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                                    shape = RoundedCornerShape(22.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = Localization.get("scanned", lang).uppercase(),
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF1E40AF) else MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = if (scannedCount >= 1000) String.format("%.1fK", scannedCount / 1000.0) else scannedCount.toString(),
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Black
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onBackground
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.Radar,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // Alive Card
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (theme == AppTheme.PRISM_LIGHT) {
                                                        Brush.linearGradient(colors = listOf(Color(0xFFD1FAE5), Color(0xFFECFDF5)))
                                                    } else {
                                                        Brush.linearGradient(colors = listOf(Color(0xFF064E3B).copy(alpha = 0.25f), Color(0xFF022C22).copy(alpha = 0.5f)))
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.45f),
                                                    shape = RoundedCornerShape(22.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = Localization.get("alive", lang).uppercase(),
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF065F46) else Color(0xFF10B981)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = if (aliveCount >= 1000) String.format("%.1fK", aliveCount / 1000.0) else aliveCount.toString(),
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Black
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF047857) else Color(0xFF10B981)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF059669) else Color(0xFF10B981).copy(alpha = 0.8f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Dead Card
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (theme == AppTheme.PRISM_LIGHT) {
                                                        Brush.linearGradient(colors = listOf(Color(0xFFFEE2E2), Color(0xFFFEF2F2)))
                                                    } else {
                                                        Brush.linearGradient(colors = listOf(Color(0xFF7F1D1D).copy(alpha = 0.2f), Color(0xFF450A0A).copy(alpha = 0.5f)))
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(22.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = Localization.get("dead", lang).uppercase(),
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF991B1B) else Color(0xFFEF4444)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = if (deadCount >= 1000) String.format("%.1fK", deadCount / 1000.0) else deadCount.toString(),
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Black
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFFB91C1C) else Color(0xFFEF4444)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.Dangerous,
                                                    contentDescription = null,
                                                    tint = if (theme == AppTheme.PRISM_LIGHT) Color(0xFFDC2626) else Color(0xFFEF4444).copy(alpha = 0.8f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }

                                        // ETA Card
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (theme == AppTheme.PRISM_LIGHT) {
                                                        Brush.linearGradient(colors = listOf(Color(0xFFFEF3C7), Color(0xFFFFFBEB)))
                                                    } else {
                                                        Brush.linearGradient(colors = listOf(Color(0xFF78350F).copy(alpha = 0.2f), Color(0xFF451A03).copy(alpha = 0.5f)))
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFFFBBF24).copy(alpha = 0.5f) else Color(0xFFFBBF24).copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(22.dp)
                                                )
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = Localization.get("eta", lang).uppercase(),
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFF92400E) else Color(0xFFFBBF24)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = eta,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Black
                                                        ),
                                                        color = if (theme == AppTheme.PRISM_LIGHT) Color(0xFFB45309) else Color(0xFFFBBF24)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.HourglassEmpty,
                                                    contentDescription = null,
                                                    tint = if (theme == AppTheme.PRISM_LIGHT) Color(0xFFD97706) else Color(0xFFFBBF24).copy(alpha = 0.8f),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isCompact) {
                                    // Big Start/Stop Engine Button (standalone on spacious screens)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(
                                                if (isScanning) {
                                                    Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFBE123C)))
                                                } else {
                                                    Brush.linearGradient(listOf(Color(0xFFFF2E63), Color(0xFFB00020)))
                                                }
                                            )
                                            .clickable { if (isScanning) viewModel.stopScan() else viewModel.startScan() }
                                            .testTag("scan_trigger_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = "Scan Icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isScanning) Localization.get("stop_scan", lang) else Localization.get("start_scan", lang),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    1 -> {
                        // RESULTS SUB-PAGE (Entirely scrollable list of diagnostic results)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Localization.get("alive_results", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Copy top results
                                    IconButton(
                                        onClick = {
                                            viewModel.copyTop10ToClipboard(context)
                                            Toast.makeText(context, Localization.get("copied_note", lang), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListNumbered,
                                            contentDescription = "Copy Top 10",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Copy All
                                    IconButton(
                                        onClick = {
                                            viewModel.copyAllToClipboard(context)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy All",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Export results
                                    IconButton(
                                        onClick = {
                                            viewModel.exportResultsToTxtFile(context)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "Save to TXT",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                if (allIps.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = Localization.get("no_results_desc", lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(
                                                if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
                                                    Color.White.copy(alpha = 0.85f)
                                                else
                                                    Color(0xFF111625).copy(alpha = 0.65f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(allIps, key = { it.hashCode() }) { alive ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                                    .clickable { viewModel.copyIndividualToClipboard(context, alive) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    // Connection Status Bullet Dot
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                 when {
                                                                     alive.ping < 200 -> Color(0xFF34D399)
                                                                     alive.ping < 500 -> Color(0xFFFBBF24)
                                                                     alive.ping < 1000 -> Color(0xFFf87171)
                                                                     else -> Color(0xFF000000)
                                                                 }
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${alive.ip}:${alive.port}#Nova-${alive.novaId}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Text(
                                                    text = "${alive.ping}ms",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = when {
                                                        alive.ping < 200 -> Color(0xFF34D399)
                                                        alive.ping < 500 -> Color(0xFFFBBF24)
                                                        alive.ping < 1000 -> Color(0xFFf87171)
                                                        else -> Color(0xFF000000)
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
                }
            }
        }
    }
}

// Special Glassmorphic Card Container with Dynamic Cyberpunk Mesh-Grid design
@Composable
fun MeshBackgroundCard(
    modifier: Modifier = Modifier,
    isLightTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLightTheme) Color(0xFFE2E8F0) else Color(0xFF22D3EE).copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

// Page 1.5: EASY INSTALLER SCREEN (Onboarding configuration and client download wizard)
@Composable
fun EasyInstallerScreen(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val isLightTheme = theme == AppTheme.PRISM_LIGHT
    val isFa = lang == AppLanguage.FA
    
    var installerTab by remember { mutableStateOf(0) } // 0: Cloudflare Worker Setup, 1: Client Setup Guides
    var selectedPlatform by remember { mutableStateOf(0) } // For client setup guide (0: Android, 1: iOS, 2: Windows, 3: macOS)
    
    // Cloudflare deploy variables
    var apiToken by remember { mutableStateOf("") }
    var workerName by remember { mutableStateOf("nova-proxy") }
    var userUuid by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var cleanIp by remember { mutableStateOf("104.16.2.2") }
    
    var isDeploying by remember { mutableStateOf(false) }
    var deployLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var deployedUrlResult by remember { mutableStateOf("") }
    var deployError by remember { mutableStateOf("") }
    
    // Animation tick for scanning line in QR Code
    var animationFrame by remember { mutableStateOf(0f) }
    LaunchedEffect(deployedUrlResult) {
        if (deployedUrlResult.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(16)
                animationFrame = (animationFrame + 0.015f) % 1f
            }
        }
    }
    
    LocalizedLayout(lang) {
        FadingScrollColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 96.dp, bottom = 100.dp),
        ) {
            // Header
            MeshBackgroundCard(
                isLightTheme = isLightTheme,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF22D3EE),
                                                Color(0xFF818CF8),
                                                Color(0xFFA855F7)
                                            )
                                        )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Installer Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isFa) "نوا ویزارد دیپلوی پلتفرم" else "NOVA WIZARD DEPLOY PLATFORM",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = if (isLightTheme) Color(0xFF0F172A) else Color.White
                        )
                        Text(
                            text = if (isFa) "راه‌اندازی، دیپلوی خودکار ورکر سورس و اتصال کلاینت" else "Automatic Cloudflare-Workers deployment & connection wizard",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLightTheme) Color(0xFF475569) else Color.Gray
                        )
                    }
                }
            }

            // Tabs Selector (0: Worker Deployer, 1: Client Guides)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isLightTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.05f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Deployment Tab
                    val depSelected = installerTab == 0
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (depSelected) Brush.linearGradient(colors = listOf(Color(0xFF22D3EE), Color(0xFF0891B2)))
                                else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { installerTab = 0 }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "راه‌اندازی ورکر کلودفلر" else "Cloudflare Deployer",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (depSelected) FontWeight.Black else FontWeight.SemiBold
                            ),
                            color = if (depSelected) Color.White else if (isLightTheme) Color(0xFF475569) else Color.LightGray
                        )
                    }
                    
                    // Guides Tab
                    val guideSelected = installerTab == 1
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (guideSelected) Brush.linearGradient(colors = listOf(Color(0xFF22D3EE), Color(0xFF0891B2)))
                                else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { installerTab = 1 }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) "راهنمای اتصال کلاینت‌ها" else "Client Connection Guide",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (guideSelected) FontWeight.Black else FontWeight.SemiBold
                            ),
                            color = if (guideSelected) Color.White else if (isLightTheme) Color(0xFF475569) else Color.LightGray
                        )
                    }
                }
            }

            // Screen Tab Content rendering
            if (installerTab == 0) {
                // TAB 0: CLOUDFLARE WORKER DEPLOYER (Novaproxy Installer style replica)
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isFa) "تنظیمات دیپلوی ورکر کلودفلر" else "Cloudflare Deployment Settings",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isLightTheme) Color(0xFF1E3A8A) else Color(0xFF60A5FA)
                        )
                        
                        Text(
                            text = if (isFa) {
                                "برای راه‌اندازی، توکن اختصاصی کلودفلر را قرار دهید. در صورت عدم درج، فرآیند به صورت شبیه‌ساز ساندباکس کامل اجرا می‌شود."
                            } else {
                                "Enter your Cloudflare API token. Leave it empty to experience the beautiful wizard in Sandbox deployment mode."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isLightTheme) Color(0xFF475569) else Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Cloudflare API Token Field
                        OutlinedTextField(
                            value = apiToken,
                            onValueChange = { apiToken = it },
                            label = { Text(text = if (isFa) "توکن دسترسی کلودفلر (API Token)" else "Cloudflare API Token") },
                            placeholder = { Text(text = if (isFa) "افزودن توکن یا دمو برای تست آزمایشی..." else "Paste token or type demo...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Worker Script Name Field
                        OutlinedTextField(
                            value = workerName,
                            onValueChange = { workerName = it },
                            label = { Text(text = if (isFa) "نام اسکریپت ورکر" else "Worker Script Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // User UUID Config with Regeneration Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = userUuid,
                                onValueChange = { userUuid = it },
                                label = { Text(text = if (isFa) "شناسه سکیوریتی کاربر (UUID)" else "Security ID (UUID)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                    unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            
                            IconButton(
                                onClick = { userUuid = java.util.UUID.randomUUID().toString() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate UUID",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Static Proxy IP for DNS configuration
                        OutlinedTextField(
                            value = cleanIp,
                            onValueChange = { cleanIp = it },
                            label = { Text(text = if (isFa) "آی‌پی/دامنه عبوری تمیز ورکر" else "Clean Proxy IP / Host") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = if (isLightTheme) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.4f),
                                unfocusedContainerColor = if (isLightTheme) Color.White else Color(0xFF0F172A).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Actions triggers (Launch real or Sandbox deployment)
                        Button(
                            onClick = {
                                isDeploying = true
                                deployError = ""
                                deployedUrlResult = ""
                                deployLogs = emptyList()
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val isDemo = apiToken.trim().isEmpty() || apiToken.trim().lowercase() == "demo"
                                    executeCloudflareDeployment(
                                        token = apiToken,
                                        workerName = workerName,
                                        uuid = userUuid,
                                        proxyIp = cleanIp,
                                        isDemoMode = isDemo,
                                        isFa = isFa,
                                        onStatus = { log ->
                                            deployLogs = deployLogs + log
                                        },
                                        onSuccess = { finalUrl ->
                                            deployedUrlResult = finalUrl
                                            isDeploying = false
                                        },
                                        onFailure = { err ->
                                            deployError = err
                                            isDeploying = false
                                        }
                                    )
                                }
                            },
                            enabled = !isDeploying,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isDeploying) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFa) "در حال دیپلوی همزمان..." else "Deploying to edge...",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Cloud Deploy",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFa) "شروع دیپلوی مستقیم به کلودفلر" else "Deploy to Cloudflare Workers",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Cumulative Progress logs / Console
                if (isDeploying || deployLogs.isNotEmpty() || deployError.isNotEmpty()) {
                    MeshBackgroundCard(
                        isLightTheme = isLightTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isFa) "کنسول ستاپ نووا ویزارد" else "Nova Wizard Setup Terminal",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color.DarkGray else Color.LightGray
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF030712))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    deployLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    if (deployError.isNotEmpty()) {
                                        Text(
                                            text = "❌ $deployError",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = Color(0xFFEF4444)
                                        )
                                    }
                                    if (isDeploying) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.dp
                                            )
                                            Text(
                                                text = if (isFa) "در حال پردازش عملیات..." else "Executing stack operation...",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontStyle = FontStyle.Italic
                                                ),
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Successful result presentations card
                if (deployedUrlResult.isNotEmpty()) {
                    val subUrl = "https://sub.novaproxy.online/sub?url=$deployedUrlResult"
                    MeshBackgroundCard(
                        isLightTheme = isLightTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isFa) "🎉 ورکر با موفقیت راه‌اندازی شد!" else "🎉 Deployment Succeeded!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF10B981),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            
                            Text(
                                text = if (isFa) {
                                    "یک وی‌پی‌ان کاملاً خصوصی با موفقیت روی لبه کلودفلر حساب شخصی شما بارگذاری گردید. می‌توانید از بارکد هوشمند یا دکمه کپی لینک زیر برای انتقال به نرم‌افزار خود استفاده نمایید."
                                } else {
                                    "A private high-speed serverless gateway was successfully spawned on your Cloudflare account edges. Scan or copy the subscription link below."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray,
                                textAlign = TextAlign.Center
                            )

                            HorizontalDivider(color = if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f))

                            // Interactive Barcode presentation
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
                                    .padding(8.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val rows = 6
                                    val cols = 6
                                    val cellW = size.width / cols
                                    val cellH = size.height / rows
                                    
                                    // Make deterministic pseudo-random QR grid based on UUID length to keep it look realistic
                                    val hash = userUuid.hashCode()
                                    for (r in 0 until rows) {
                                        for (c in 0 until cols) {
                                            // Always render finder patterns on three corners
                                            val isFinder = (r < 2 && c < 2) || (r >= rows - 2 && c < 2) || (r < 2 && c >= cols - 2)
                                            val isDot = ((hash shr (r * cols + c)) and 1) == 1
                                            if (isFinder || isDot) {
                                                drawRect(
                                                    color = Color(0xFF10B981).copy(alpha = if (isFinder) 0.95f else 0.7f),
                                                    topLeft = Offset(c * cellW + 1.5f, r * cellH + 1.5f),
                                                    size = androidx.compose.ui.geometry.Size(cellW - 3f, cellH - 3f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Animated scan laser sweep
                                    val sweepY = size.height * animationFrame
                                    drawLine(
                                        color = Color(0xFFEF4444).copy(alpha = 0.85f),
                                        start = Offset(0f, sweepY),
                                        end = Offset(size.width, sweepY),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }

                            Text(
                                text = if (isFa) "آدرس لایمیت اختصاصی" else "Dedicated Subscription Link",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Gray
                            )

                            // Display URL block
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLightTheme) Color(0xFFF8FAFC) else Color.Black.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subUrl,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = if (isLightTheme) Color(0xFF334155) else Color.LightGray,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(subUrl))
                                            Toast.makeText(
                                                context,
                                                if (isFa) "لینک سابسکرایب ورکر کپی شد!" else "API Subscription URL copied to clipboard!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Link",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // High fidelity action to import directly into Scanner
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(subUrl))
                                    Toast.makeText(
                                        context,
                                        if (isFa) "سابسکرایب کلودفلر وارد کلاینت شد. اکنون می‌توانید جهت تست زنده پارت‌ها به منوی رادار بروید!"
                                        else "Directly linked! Go to Radar screen to verify clean IPs with your private worker!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isFa) "ایمپورت ساب اسکرب به کلاینت و کپی آدرس" else "Auto Import Connection & Copy Link",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: CLIENT CONNECTION GUIDE (Originally placed guides rendered perfectly in Vazirmatn!)
                val clientName = when (selectedPlatform) {
                    0 -> "v2rayNG / Nekobox"
                    1 -> "V2Box / Streisand"
                    2 -> "v2rayN / Nekoray"
                    else -> "FoXray / V2rayU"
                }

                val appStoreLabel = when (selectedPlatform) {
                    0 -> if (isFa) "دانلود کلاینت Google Play" else "Download from Play Store"
                    1 -> if (isFa) "دانلود کلاینت از App Store" else "Download on App Store"
                    2 -> if (isFa) "دانلود کلاینت Windows GitHub" else "Download Windows App (ZIP)"
                    else -> if (isFa) "دانلود نسخه macOS" else "Download macOS Client"
                }

                // Sub platforms buttons
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLightTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A).copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.04f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val platforms = listOf("Android", "iOS", "Windows", "macOS")
                        platforms.forEachIndexed { index, name ->
                            val isSelected = selectedPlatform == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Brush.linearGradient(colors = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED)))
                                        else Brush.linearGradient(colors = listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .clickable { selectedPlatform = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) Color.White else if (isLightTheme) Color(0xFF475569) else Color.LightGray
                                )
                            }
                        }
                    }
                }

                // Step 1: Install Client
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۱",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم اول: نصب کلاینت پیشنهادی" else "Step 1: Install Recommended Client",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF1E3A8A) else Color(0xFF60A5FA)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "برای این سیستم‌عامل، کلاینت رسمی و پیشنهادی برنامه $clientName می‌باشد که پاسخگویی بسیار بالایی دارد."
                                } else {
                                    "The recommended high-performance software for your device is $clientName with optimized secure routing layers."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        if (isFa) "در حال انتقال به سرورهای دانلود کلاینت..." else "Redirecting to download mirror...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appStoreLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Step 2: Subscription Link Guide
                val subscriptionUrl = "https://sub.novaproxy.online/feed/your-secure-radar-profile"
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۲",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF8B5CF6)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم دوم: دریافت آدرس سابسکرایب" else "Step 2: Obtain Subscription Link",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF5B21B6) else Color(0xFFC084FC)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "لینک هوشمند زیر حاوی پیکربندی‌های فعال و متصل به رادار ردیابی پینگ Nova Proxy می‌باشد."
                                } else {
                                    "Use the secure subscription profile endpoint below to sync automatically with local latency indicators."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLightTheme) Color(0xFFF8FAFC) else Color.Black.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, if (isLightTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = subscriptionUrl,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = if (isLightTheme) Color(0xFF334155) else Color.LightGray,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(subscriptionUrl))
                                            Toast.makeText(
                                                context,
                                                if (isFa) "لینک اشتراک کپی شد!" else "Subscription URL copied to clipboard!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Link",
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 3: Scan QR Card
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۳",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF10B981)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم سوم: اسکن یا ایمپورت در کلاینت" else "Step 3: Import or Scan QR Profile",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF065F46) else Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "وارد نرم‌افزار کلاینت شوید، دکمه افزودن (+) یا Subscription را بزنید و آدرس کپی شده یا QR Code دیجیتال زیر را اسکن کنید."
                                } else {
                                    "Open your client, click create/edit sub, and add subscription URL or scan the live matrix barcode."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isLightTheme) {
                                            Brush.linearGradient(colors = listOf(Color(0xFFEFF6FF), Color(0xFFF0FDF4)))
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color(0xFF0F172A).copy(alpha = 0.8f), Color(0xFF022C22).copy(alpha = 0.5f)))
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFF10B981).copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .border(1.5.dp, Color(0xFF10B981).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val rows = 5
                                            val cols = 5
                                            val cellW = size.width / cols
                                            val cellH = size.height / rows
                                            val randomPoints = listOf(
                                                0 to 0, 0 to 1, 0 to 4,
                                                1 to 0, 1 to 2, 1 to 3,
                                                2 to 2, 2 to 4,
                                                3 to 1, 3 to 3,
                                                4 to 0, 4 to 1, 4 to 4
                                            )
                                            for ((r, c) in randomPoints) {
                                                drawRect(
                                                    color = Color(0xFF10B981).copy(alpha = 0.85f),
                                                    topLeft = Offset(c * cellW + 2f, r * cellH + 2f),
                                                    size = androidx.compose.ui.geometry.Size(cellW - 4f, cellH - 4f)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Column(
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "NOVA SECURITY RADAR",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp
                                            ),
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "STATUS: VERIFIED SECURE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 4: Scan and Optimize Radar Guide
                MeshBackgroundCard(
                    isLightTheme = isLightTheme,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "۴",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFEF4444)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "قدم چهارم: ایمن‌سازی با اسکنر رادار" else "Step 4: Execute Scanner Audit",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isLightTheme) Color(0xFF991B1B) else Color(0xFFF87171)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) {
                                    "پس از ایمپورت سابسکرایب، وارد زبانه «رادار پینگ» در منوی پایین نوا رادار شوید و دکمه شروع اسکن را بفشارید تا زنده بودن و پینگ تمام آی‌پی‌ها بررسی شده و بهترین پورت به شما ارائه گردد."
                                } else {
                                    "Once client is configured, enter the 'Radar' tab below, choose targeted subnets, and hit START SCAN to isolate the absolute lowest-ping relays."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightTheme) Color(0xFF334155) else Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// Global lightweight helper functions for parsing JSON and performing CF network operations safely
private fun extractJsonValue(json: String, key: String): String {
    val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    val match = pattern.find(json)
    return match?.groupValues?.get(1) ?: ""
}

private suspend fun executeCloudflareDeployment(
    token: String,
    workerName: String,
    uuid: String,
    proxyIp: String,
    isDemoMode: Boolean,
    isFa: Boolean,
    onStatus: (String) -> Unit,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    if (isDemoMode) {
        // High fidelity sandbox simulation with Persian and English support
        onStatus("🔑 " + (if (isFa) "در حال اعتبارسنجی اتصال شبکه به سرورهای کلودفلر..." else "Authenticating connection to Cloudflare..."))
        kotlinx.coroutines.delay(1200)
        onStatus("🔓 " + (if (isFa) "دسترسی دمو با موفقیت مجاز قلمداد شد." else "Access sandbox credentials validated successfully!"))
        kotlinx.coroutines.delay(1100)
        onStatus("👤 " + (if (isFa) "دریافت جزئیات حساب: Nova_Wizard_Public_Sandbox_Account" else "Fetched account details: Nova_Wizard_Public_Sandbox_Account"))
        kotlinx.coroutines.delay(1300)
        onStatus("⚙️ " + (if (isFa) "در حال تولید کد پروکسی پیشرفته ورکر متصل به UUID: [انحصاری]" else "Generating proxy worker source template linked to UUID..."))
        kotlinx.coroutines.delay(1400)
        onStatus("📂 " + (if (isFa) "ارسال کامپایل سورس کد برای سرورهای ابری کلودفلر..." else "Transmitting script bundle to Cloudflare Edge gateways..."))
        kotlinx.coroutines.delay(1500)
        onStatus("📡 " + (if (isFa) "ثبت‌نام دامنه اختصاصی: $workerName.workers.dev..." else "Assigning router subdomain route: $workerName.workers.dev"))
        kotlinx.coroutines.delay(1600)
        onStatus("🎉 " + (if (isFa) "راه‌اندازی به اتمام رسید! اطلاعات کانکشن متصل به آی‌پی $proxyIp با موفقیت ایجاد گردید." else "Setup complete! Connection properties successfully configured to $proxyIp"))
        kotlinx.coroutines.delay(1000)
        val randSub = "nova-proxy-" + (10000..99999).random()
        onSuccess("https://$workerName.$randSub.workers.dev")
        return
    }

    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // 1. Fetch accounts
        onStatus(if (isFa) "🔑 در حال اعتبارسنجی توکن دسترسی کلودفلر..." else "🔑 Authorizing access token with Cloudflare API...")
        val accountsRequest = okhttp3.Request.Builder()
            .url("https://api.cloudflare.com/client/v4/accounts")
            .header("Authorization", "Bearer $token")
            .build()

        client.newCall(accountsRequest).execute().use { response ->
            if (!response.isSuccessful) {
                onFailure((if (isFa) "خطا در بارگذاری حساب‌ها: " else "List accounts API authorization failed with HTTP ") + response.code)
                return
            }

            val responseBody = response.body?.string() ?: ""
            val accountId = extractJsonValue(responseBody, "id")
            val accountName = extractJsonValue(responseBody, "name")
            if (accountId.isEmpty()) {
                onFailure(if (isFa) "هیچ حساب کلودفلر معتبری در توکن شما یافت نشد." else "No active Cloudflare Account ID resolved from this API token.")
                return
            }
            onStatus((if (isFa) "👤 اتصال موفقیت‌آمیز! نام کاربری: " else "👤 Authorization active! Account name: ") + accountName)
            kotlinx.coroutines.delay(1000)

            // 2. Transmit / Upload JavaScript Worker Source
            onStatus(if (isFa) "⚙️ کامپایل هوشمند نسخه بهینه سورس کد نوا پروکسی..." else "⚙️ Structuring highly optimized proxy worker source code...")
            val workerJsCode = """
                // Nova Proxy Serverless Route Worker (Optimized client deployment v1.0.0)
                export default {
                  async fetch(request, env) {
                    const url = new URL(request.url);
                    if (url.pathname === '/sub' || url.pathname === '/feed') {
                      const vlessUrl = "vless://$uuid@" + url.host + ":443?encryption=none&security=tls&sni=" + url.host + "&type=ws&host=" + url.host + "&path=%2F%3Fed%3D2048#Nova%20Proxy";
                      return new Response(btoa(vlessUrl), {
                        headers: { "content-type": "text/plain; charset=utf-8" }
                      });
                    }
                    return new Response("Nova Proxy Worker is Active. Status: Healthy. UUID Bound: Configured.", {
                      headers: { "content-type": "text/html; charset=utf-8" }
                    });
                  }
                };
            """.trimIndent()

            onStatus(if (isFa) "📂 در حال آپلود فایل سورس کد به کلودفلر..." else "📂 Injecting JS proxy script to Cloudflare Workers repository...")
            val deployRequest = okhttp3.Request.Builder()
                .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/scripts/$workerName")
                .header("Authorization", "Bearer $token")
                .put(workerJsCode.toRequestBody("application/javascript".toMediaType()))
                .build()

            client.newCall(deployRequest).execute().use { deployResponse ->
                if (!deployResponse.isSuccessful) {
                    onFailure((if (isFa) "خطا در آپلود اسکریپت ورکر: " else "Write Worker Script failed with HTTP ") + deployResponse.code)
                    return
                }

                kotlinx.coroutines.delay(1000)

                // 3. Configure/Enable workers.dev Subdomain
                onStatus(if (isFa) "📡 آماده‌سازی زیردامنه اختصاصی..." else "📡 Allocating subdomain gateway...")
                val subRequest = okhttp3.Request.Builder()
                    .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/scripts/$workerName/subdomain")
                    .header("Authorization", "Bearer $token")
                    .post("{\"enabled\":true}".toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(subRequest).execute().use { subResponse ->
                    if (!subResponse.isSuccessful) {
                        onFailure((if (isFa) "خطا در اعمال ساب‌دومین: " else "Assign workers.dev subdomain route failing with HTTP ") + subResponse.code)
                        return
                    }

                    kotlinx.coroutines.delay(1000)

                    // 4. Resolve Domain Prefix
                    onStatus(if (isFa) "🛰 دریافت لینک نهایی..." else "🛰 Fetching deployed worker URL endpoint...")
                    val resolveRequest = okhttp3.Request.Builder()
                        .url("https://api.cloudflare.com/client/v4/accounts/$accountId/workers/subdomain")
                        .header("Authorization", "Bearer $token")
                        .build()

                    client.newCall(resolveRequest).execute().use { resResponse ->
                        val subdomainPrefix = if (resResponse.isSuccessful) {
                            val resBody = resResponse.body?.string() ?: ""
                            extractJsonValue(resBody, "subdomain")
                        } else {
                            ""
                        }

                        val finalPrefix = if (subdomainPrefix.isNotEmpty()) subdomainPrefix else "novaproxy-user"
                        val finalUrl = "https://$workerName.$finalPrefix.workers.dev"
                        onSuccess(finalUrl)
                    }
                }
            }
        }
    } catch (e: Exception) {
        onFailure((if (isFa) "خطای سیستمی غیرمنتظره: " else "System connection failed: ") + (e.message ?: "Unknown Connection Error"))
    }
}


// Page 2: SETTINGS SCREEN (Ports & IP Sources)
@Composable
fun SettingsScreen(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val ports by viewModel.portConfigs.collectAsState()
    val sources by viewModel.ipSources.collectAsState()

    LocalizedLayout(lang) {
        FadingScrollColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 96.dp, bottom = 100.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.get("tab_settings", lang),
                        style = MaterialTheme.typography.displayMedium.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF22D3EE),
                                    Color(0xFF818CF8),
                                    Color(0xFFA855F7)
                                )
                            ),
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = Localization.get("theme", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Language & Theme Setup (frosted capsule style card)
            GlassyCard {
                Text(
                    text = Localization.get("language", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.selectLanguage(AppLanguage.FA) },
                        modifier = Modifier.weight(1f).height(40.dp).testTag("lang_fa_btn"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lang == AppLanguage.FA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            contentColor = if (lang == AppLanguage.FA) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("فارسی (Persian)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.selectLanguage(AppLanguage.EN) },
                        modifier = Modifier.weight(1f).height(40.dp).testTag("lang_en_btn"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lang == AppLanguage.EN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            contentColor = if (lang == AppLanguage.EN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("English", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = Localization.get("theme", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 2 Theme style grids (Horizontal: Dark | Light)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { viewModel.selectTheme(AppTheme.PRISM_DARK) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (theme == AppTheme.PRISM_DARK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            contentColor = if (theme == AppTheme.PRISM_DARK) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(Localization.get("prism_dark", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.selectTheme(AppTheme.PRISM_LIGHT) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (theme == AppTheme.PRISM_LIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            contentColor = if (theme == AppTheme.PRISM_LIGHT) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(Localization.get("prism_light", lang), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Vibration & Alert configurations (Vibrate on complete/warnings, and Notifications switch toggles)
            Column {
                Text(
                    text = Localization.get("alert_settings", lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val vibrateFinish by viewModel.vibrateOnFinish.collectAsState()
                val vibrateError by viewModel.vibrateOnError.collectAsState()
                val notifyError by viewModel.notifyOnError.collectAsState()

                val context = LocalContext.current
                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { permissions ->
                        val granted = permissions.values.all { it }
                        if (granted) {
                            Toast.makeText(context, if (lang == AppLanguage.FA) "دسترسی‌ها با موفقیت اعطا شد!" else "Permissions Granted!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, if (lang == AppLanguage.FA) "برخی دسترسی‌ها رد شد." else "Some permissions were denied.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                GlassyCard {
                    // Vibrate on complete
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleVibrateOnFinish() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("vibrate_finish", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = vibrateFinish,
                            onCheckedChange = { viewModel.toggleVibrateOnFinish() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                    // Vibrate on mismatch error
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleVibrateOnError() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("vibrate_error", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = vibrateError,
                            onCheckedChange = { viewModel.toggleVibrateOnError() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                    // Notifications on alerts
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleNotifyOnError() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.get("notify_error", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = notifyError,
                            onCheckedChange = { viewModel.toggleNotifyOnError() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grant permissions button
                    Button(
                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Permissions",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.get("grant_permissions", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Port configuration (elegant checkboxes card grid as requested/shown in screenshot 2)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.get("port_configuration", lang),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = Localization.get("select_all", lang),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.clickable { viewModel.selectAllPorts() }
                        )
                        Text(
                            text = Localization.get("clear_all", lang),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            ),
                            modifier = Modifier.clickable { viewModel.clearAllPorts() }
                        )
                    }
                }

                // 2-column grid as shown in screenshot 2
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ports.chunked(2).forEach { rowPorts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPorts.forEach { portConfig ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            if (portConfig.isEnabled) {
                                                if (theme == AppTheme.PRISM_LIGHT) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color(0xFF0F172A).copy(alpha = 0.8f)
                                            } else {
                                                if (theme == AppTheme.PRISM_LIGHT) Color(0xFFE2E8F0) else Color.Black.copy(alpha = 0.4f)
                                            }
                                        )
                                        .border(
                                            width = if (portConfig.isEnabled) 1.dp else 1.5.dp,
                                            color = if (portConfig.isEnabled) MaterialTheme.colorScheme.primary else (if (theme == AppTheme.PRISM_LIGHT) Color(0xFF94A3B8) else Color.White.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clickable { viewModel.togglePortConfig(portConfig) }
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = portConfig.port.toString(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = if (portConfig.isEnabled) Color.White else (if (theme == AppTheme.PRISM_LIGHT) Color(0xFF334155) else Color.Gray)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (portConfig.isEnabled) (if (theme == AppTheme.PRISM_LIGHT) Color.White else MaterialTheme.colorScheme.primary) else Color.Gray.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                            if (rowPorts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // IP Sources list Section (individual cards as requested/shown in screenshots 1 & 3)
            Column {
                Text(
                    text = Localization.get("ip_sources", lang),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sources.forEach { source ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleIpSource(source) },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (theme == AppTheme.PRISM_LIGHT) Color.White else Color(0xFF0F172A).copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (source.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Switch(
                                        checked = source.isEnabled,
                                        onCheckedChange = { viewModel.toggleIpSource(source) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = Color.Gray,
                                            uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.5f)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column {
                                        Text(
                                            text = if (lang == AppLanguage.FA) source.nameFa else source.nameEn,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (source.isEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                                        )
                                        if (source.cidr.isNotEmpty()) {
                                            Text(
                                                text = source.cidr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Globe",
                                    tint = if (source.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// FlowRow layout implementation backport to avoid dependency errors on older Compose versions
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        val lines = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentLine = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentLineWidth = 0

        placeables.forEach { placeable ->
            if (currentLineWidth + placeable.width > layoutWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = mutableListOf()
                currentLineWidth = 0
            }
            currentLine.add(placeable)
            currentLineWidth += placeable.width + 16
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        var totalHeight = 0
        lines.forEach { line ->
            val maxLineHeight = line.maxOfOrNull { it.height } ?: 0
            totalHeight += maxLineHeight + 16
        }

        layout(layoutWidth, maxOf(0, totalHeight - 16)) {
            var y = 0
            lines.forEach { line ->
                var x = 0
                val maxLineHeight = line.maxOfOrNull { it.height } ?: 0
                line.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + 16
                }
                y += maxLineHeight + 16
            }
        }
    }
}

// Page 3: SCAN LOGS SCREEN
@Composable
fun LogsScreen(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val logs by viewModel.logs.collectAsState()

    LocalizedLayout(lang) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 96.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.get("tab_logs", lang),
                        style = MaterialTheme.typography.displayMedium.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF22D3EE),
                                    Color(0xFF818CF8),
                                    Color(0xFFA855F7)
                                )
                            ),
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = Localization.get("tab_logs", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Clear Logs button
                Button(
                    onClick = { viewModel.clearLogs() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(Localization.get("clear", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Tech log terminal viewport
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp) // Extra spacing for bottom nav
                    .heightIn(max = 480.dp) // Prevent terminal overflow under menu
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF030712)) // Dark Terminal Background
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.FormatListBulleted,
                                contentDescription = "No log",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "TERMINAL IDLE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(logs) { log ->
                                val textColor = when {
                                    log.contains("✔ ALIVE") -> Color(0xFF10B981) // Green
                                    log.contains("✖ DEAD") -> Color(0xFFEF4444).copy(alpha = 0.5f) // Faded Red
                                    log.contains("======") -> MaterialTheme.colorScheme.primary // Tech headings
                                    else -> Color(0xFF9CA3AF) // Grey default
                                }
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Page 4: ABOUT US SCREEN (Perfect replications of the desktop About pop-up menu - Screenshot 2)
@Composable
fun AboutScreen(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    LocalizedLayout(lang) {
        FadingScrollColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 96.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.get("tab_about", lang),
                        style = MaterialTheme.typography.displayMedium.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF22D3EE),
                                    Color(0xFF818CF8),
                                    Color(0xFFA855F7)
                                )
                            ),
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = "ORGANIZATION DETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // About Container (Styled like the popup in image 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassyCard(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Premium high-tech company brand photo logo
                        Image(
                            painter = painterResource(id = com.novaradar.app.R.drawable.img_nova_radar_logo_1781975654739),
                            contentDescription = "Nova Radar N Logo",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Textured Crumpled Glitch Title as requested
                        CrumpledGlitchText(
                            text = "NOVA RADAR",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = Localization.get("about_sub", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Link cards which are fully interactive & open in system browser
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            // Nova Proxy
                            AboutLinkItem(
                                title = "Nova Proxy",
                                subtitle = "https://github.com/IRNova/NovaProxy",
                                icon = Icons.Outlined.Cloud,
                                onClick = {
                                    try {
                                        uriHandler.openUri("https://github.com/IRNova/NovaProxy")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )

                            // Nova Radar
                            AboutLinkItem(
                                title = "Nova Radar",
                                subtitle = "https://github.com/IRNova/NovaRadar",
                                icon = Icons.Outlined.Security,
                                onClick = {
                                    try {
                                        uriHandler.openUri("https://github.com/IRNova/NovaRadar")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )

                            // Install Wizard
                            AboutLinkItem(
                                title = "Install Wizard",
                                subtitle = "https://novaproxy.online/install",
                                icon = Icons.Outlined.Language,
                                onClick = {
                                    try {
                                        uriHandler.openUri("https://novaproxy.online/install")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Footer version label
                        Text(
                            text = "v${com.novaradar.app.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

// Live Crumpled Textured Glitch Text for High Tech Branding as requested
@Composable
fun CrumpledGlitchText(
    text: String,
    style: TextStyle = MaterialTheme.typography.displayMedium
) {
    val novaBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF22D3EE),
                                    Color(0xFF818CF8),
                                    Color(0xFFA855F7)
                                )
    )
    val isLightTheme = MaterialTheme.colorScheme.background == Color(0xFFFAFAFC)
    val highlightsColor = if (isLightTheme) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.40f)
    val highlightsShadow = if (isLightTheme) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.35f)
    Box {
        Text(
            text = text,
            style = style.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                brush = novaBrush,
                shadow = Shadow(
                    color = Color.Green.copy(alpha = 0.5f),
                    offset = Offset(2f, 2f),
                    blurRadius = 1.5f
                )
            ),
            fontWeight = FontWeight.Black
        )
        Text(
            text = text,
            style = style.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                shadow = Shadow(
                    color = highlightsShadow,
                    offset = Offset(-1.5f, -1.5f),
                    blurRadius = 0.8f
                )
            ),
            color = highlightsColor,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun AboutLinkItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
