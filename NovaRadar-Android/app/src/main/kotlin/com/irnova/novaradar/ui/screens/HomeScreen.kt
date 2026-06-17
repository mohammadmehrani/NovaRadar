package com.irnova.novaradar.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.irnova.novaradar.R
import com.irnova.novaradar.data.model.ScanResult
import com.irnova.novaradar.data.model.ScanStats
import com.irnova.novaradar.ui.components.RadarAnimation
import androidx.compose.ui.tooling.preview.Preview
import com.irnova.novaradar.ui.theme.*
import com.irnova.novaradar.ui.viewmodel.HomeViewModel
import java.io.File

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    NovaRadarTheme {
        // We might need a mock ViewModel here or just a dummy UI
        Box(modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsState()
    val results by viewModel.results.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val isDark = !MaterialTheme.colorScheme.surface.let { it == Color.White || it == LightSurface }
    val primaryBrush = if (isDark) Brush.linearGradient(GeminiDarkGradient) else Brush.linearGradient(GeminiLightGradient)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderSection(primaryBrush, context)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = if(isDark) NovaPrimary else LightPrimary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = if(isDark) NovaPrimary else LightPrimary,
                        height = 3.dp
                    )
                },
                modifier = Modifier.padding(horizontal = 24.dp).height(48.dp)
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(stringResource(R.string.radar).uppercase(), modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.results).uppercase(), modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Black, fontSize = 11.sp)
                        if (results.isNotEmpty()) {
                            Badge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp), containerColor = NovaRed) {
                                Text(results.size.toString(), color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).padding(horizontal = 24.dp)) {
                if (selectedTab == 0) {
                    RadarTab(viewModel, stats, results, isDark, primaryBrush)
                } else {
                    ResultsTab(viewModel, results, context)
                }
            }
        }

        // FIXED START ENGINE BUTTON - Above Bottom Bar
        if (selectedTab == 0) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp).padding(horizontal = 24.dp)) {
                Button(
                    onClick = { if (stats.scanning) viewModel.stopScan() else viewModel.startScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            if (stats.scanning) Brush.linearGradient(listOf(NovaRed, Color(0xFFB71C1C))) else primaryBrush, 
                            CircleShape
                        ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                ) {
                    Icon(if(stats.scanning) Icons.Default.Stop else Icons.Default.PlayArrow, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (stats.scanning) stringResource(R.string.stop_scan).uppercase() else stringResource(R.string.start_scan).uppercase(),
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(brush: Brush, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, start = 24.dp, end = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.mipmap.novaradar),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(stringResource(R.string.app_name).uppercase(), color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                stringResource(R.string.nova_group).uppercase(), 
                color = if(MaterialTheme.colorScheme.surface == Color.White) LightPrimary else NovaPrimary, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.ExtraBold, 
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { 
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/irnova_proxy")))
                    } catch (e: Exception) {}
                }
            )
        }
    }
}

@Composable
fun RadarTab(viewModel: HomeViewModel, stats: ScanStats, results: List<ScanResult>, isDark: Boolean, brush: Brush) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize()) {
        // GREEN RADAR - TAP TO COPY TOP 10
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clickable { 
                    if (results.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(viewModel.getResultsForCopy(true)))
                        Toast.makeText(context, "Top 10 Verified Assets Copied", Toast.LENGTH_SHORT).show()
                    }
                }, 
            contentAlignment = Alignment.Center
        ) {
            RadarAnimation(isScanning = stats.scanning, topResults = results, modifier = Modifier.size(240.dp))
            if (stats.scanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
                   Text(stringResource(R.string.scanning_subnet).uppercase(), color = RadarGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                   Text(stats.currentIP, color = if(isDark) Color.White else Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5 Optimized Stat Containers
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBox(
                label = stringResource(R.string.quick_scan_tcp),
                value = if(stats.scanning) stats.currentIP else stringResource(R.string.ready_scan),
                icon = Icons.Default.Security,
                brush = brush,
                isDark = isDark
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBoxMini(stringResource(R.string.alive), stats.aliveCount.toString(), Icons.Default.CheckCircle, NovaLatencyGood, isDark, Modifier.weight(1f))
                StatBoxMini(stringResource(R.string.scanned), stats.totalScanned.toString(), Icons.Default.Search, if(isDark) Color.White else Color.Black, isDark, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBoxMini(stringResource(R.string.dead), stats.deadCount.toString(), Icons.Default.Warning, NovaRed, isDark, Modifier.weight(1f))
                StatBoxMini(stringResource(R.string.eta), if(stats.scanning) "SCANNING" else "--:--", Icons.Default.Timer, if(isDark) Color.White else Color.Black, isDark, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatusBox(label: String, value: String, icon: ImageVector, brush: Brush, isDark: Boolean) {
    Surface(
        color = if(isDark) NovaSurface else Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(20.dp)),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(brush, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label.uppercase(), color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(value, color = if(isDark) Color.White else Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun StatBoxMini(label: String, value: String, icon: ImageVector, valueColor: Color, isDark: Boolean, modifier: Modifier) {
    Surface(
        color = if(isDark) NovaSurface else Color.White,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(20.dp)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if(isDark) Color.Gray else Color.LightGray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label.uppercase(), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ResultsTab(viewModel: HomeViewModel, results: List<ScanResult>, context: Context) {
    val clipboard = LocalClipboardManager.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) saveToDownloads(context, viewModel.getResultsForCopy(false), false)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { clipboard.setText(AnnotatedString(viewModel.getResultsForCopy(true))) }, modifier = Modifier.weight(1f).height(48.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = NovaSurface)) {
                Text(stringResource(R.string.copy_top_10), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { clipboard.setText(AnnotatedString(viewModel.getResultsForCopy(false))) }, modifier = Modifier.weight(1f).height(48.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = NovaSurface)) {
                Text(stringResource(R.string.copy_all), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { 
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    saveToDownloads(context, viewModel.getResultsForCopy(false), false)
                }
            }, modifier = Modifier.size(48.dp).background(NovaSurface, CircleShape)) {
                Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Surface(color = NovaSurface.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("#", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(32.dp))
                Text("PING", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                Text("ASSET ADDRESS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            }
        }
        
        Surface(color = Color.Transparent, modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
            if (results.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(stringResource(R.string.no_results), color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn {
                    itemsIndexed(results) { index, result ->
                        val pingColor = when {
                            result.latencyMs < 200 -> NovaLatencyGood
                            result.latencyMs < 500 -> NovaYellow
                            result.latencyMs < 800 -> NovaOrange
                            else -> NovaDarkGray
                        }
                        Surface(
                            color = NovaSurface.copy(0.3f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.width(32.dp), fontWeight = FontWeight.Bold)
                                Text("${result.latencyMs}ms", color = pingColor, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                                Text(text = result.link, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(110.dp))
    }
}

private fun saveToDownloads(context: Context, content: String, isJson: Boolean) {
    try {
        val extension = if (isJson) "json" else "txt"
        val filename = "Nova_Results_${System.currentTimeMillis()}.$extension"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, if(isJson) "application/json" else "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { stream ->
                    stream.write(content.toByteArray())
                }
                Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_LONG).show()
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            file.writeText(content)
            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
