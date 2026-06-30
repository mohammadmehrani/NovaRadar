package com.novaradar.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novaradar.app.ui.localization.Localization
import com.novaradar.app.ui.screens.*
import com.novaradar.app.ui.theme.NovaRadarTheme
import com.novaradar.app.ui.viewmodel.AppTheme
import com.novaradar.app.ui.viewmodel.NovaRadarViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NovaRadarViewModel = viewModel()
            val theme by viewModel.selectedTheme.collectAsState()
            val lang by viewModel.selectedLanguage.collectAsState()

            val prefs = remember { this.getSharedPreferences("nova_radar_prefs", Context.MODE_PRIVATE) }
            var showWizard by remember { mutableStateOf(prefs.getBoolean("first_launch", true)) }

            NovaRadarTheme(theme = theme) {
                val view = androidx.compose.ui.platform.LocalView.current
                val isLightTheme = theme == AppTheme.PRISM_LIGHT
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as android.app.Activity).window
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                        controller.isAppearanceLightStatusBars = isLightTheme
                        controller.isAppearanceLightNavigationBars = isLightTheme
                    }
                }

                val isDark = theme == AppTheme.PRISM_DARK
                val meshColors = if (isDark) {
                    listOf(Color(0xFF0A0E1A), Color(0xFF0F1A3A), Color(0xFF060A15))
                } else {
                    listOf(Color(0xFFFFFFFF), Color(0xFFEFF6FF), Color(0xFFF8FAFC))
                }
                val meshGradient = Brush.linearGradient(colors = meshColors)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(meshGradient)
                ) {
                    MainAppLayout(viewModel)
                }

                if (showWizard) {
                    WelcomeWizard(onDismiss = {
                        showWizard = false
                        prefs.edit().putBoolean("first_launch", false).apply()
                    })
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val theme by viewModel.selectedTheme.collectAsState()
    val isDark = theme == AppTheme.PRISM_DARK
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_horizontal_pager")
                    ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .padding(top = 6.dp)
                    ) {
                        when (page) {
                            0 -> SettingsScreen(viewModel)
                            1 -> ImportScreen(viewModel)
                            2 -> RadarScreen(viewModel)
                            3 -> EasyInstallerScreen(viewModel)
                            4 -> AboutScreen(viewModel)
                        }
                    }
                }
            }

            val isScanning by viewModel.isScanning.collectAsState()
            val pulseAnim by rememberInfiniteTransition().animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "navPulse"
            )
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 4.dp)
                    .border(
                        width = 0.5.dp,
                        color = if (isDark) Color(0xFF2D3A5C).copy(alpha = 0.3f) else Color(0xFFCBD5E1).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(bottom = 8.dp),
                containerColor = if (isDark) Color(0xFF0A0E1A).copy(alpha = 0.97f) else Color(0xFFF8FAFC).copy(alpha = 0.97f),
                tonalElevation = 0.dp
            ) {
                val items = listOf(
                    NavigationItemData(key = "tab_settings", selectedIcon = Icons.Filled.Settings, unselectedIcon = Icons.Outlined.Settings),
                    NavigationItemData(key = "tab_import", selectedIcon = Icons.Filled.Add, unselectedIcon = Icons.Outlined.Add),
                    NavigationItemData(key = "tab_radar", selectedIcon = Icons.Filled.Radar, unselectedIcon = Icons.Outlined.Radar),
                    NavigationItemData(key = "tab_installer", selectedIcon = Icons.Filled.Download, unselectedIcon = Icons.Outlined.Download),
                    NavigationItemData(key = "tab_about", selectedIcon = Icons.Filled.Info, unselectedIcon = Icons.Outlined.Info)
                )

                items.forEachIndexed { index, item ->
                    val isSelected = pagerState.currentPage == index
                    val isRadar = index == 2

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                if (isRadar) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isScanning)
                                                    Brush.linearGradient(listOf(Color(0xFFBE123C), Color(0xFF9F1239)))
                                                else
                                                    Brush.linearGradient(listOf(Color(0xFF0D7DB3), Color(0xFF065A8C)))
                                            )
                                            .clickable {
                                                if (isScanning) viewModel.stopScan()
                                                else { viewModel.startScan(); coroutineScope.launch { pagerState.animateScrollToPage(2) } }
                                            }
                                            .testTag("nav_start_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Radar, "Start/Stop", tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) (if (isDark) Color(0xFF4DA8FF).copy(alpha = 0.12f) else Color(0xFF0D7DB3).copy(alpha = 0.1f))
                                                else Color.Transparent
                                            )
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = Localization.get(item.key, lang),
                                            tint = if (isSelected) {
                                                if (isDark) Color(0xFF60B5FF) else Color(0xFF0D7DB3)
                                            } else {
                                                if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.55f) else Color(0xFF1E293B).copy(alpha = 0.45f)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                        modifier = Modifier.testTag("nav_item_${item.key}")
                    )
                }
            }
        }
    }
}

data class NavigationItemData(
    val key: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)
