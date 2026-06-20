package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.localization.Localization
import com.example.ui.screens.*
import com.example.ui.theme.NovaRadarTheme
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.AppTheme
import com.example.ui.viewmodel.NovaRadarViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NovaRadarViewModel = viewModel()
            val theme by viewModel.selectedTheme.collectAsState()
            val lang by viewModel.selectedLanguage.collectAsState()

            NovaRadarTheme(theme = theme) {
                // Dynamically update status bar and navigation bar characteristics according to active theme style
                val view = androidx.compose.ui.platform.LocalView.current
                val isLightTheme = theme == AppTheme.GEMINI_LIGHT
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as android.app.Activity).window
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        
                        val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                        // If light theme is active, we showcase dark symbols to keep contrast legible
                        controller.isAppearanceLightStatusBars = isLightTheme
                        controller.isAppearanceLightNavigationBars = isLightTheme
                    }
                }

                // Outer bleed box with background gradient suited to active theme style
                val startBgColor = MaterialTheme.colorScheme.background
                val endBgColor = when (theme) {
                    AppTheme.GEMINI_DARK -> Color(0xFF060913)
                    AppTheme.GEMINI_LIGHT -> Color(0xFFECEFF4)
                    AppTheme.SOLARIZED_DARK -> Color(0xFF00151A)
                    AppTheme.STANDARD_DARK -> Color(0xFF000000)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(startBgColor, endBgColor)
                            )
                        )
                ) {
                    MainAppLayout(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppLayout(viewModel: NovaRadarViewModel) {
    val lang by viewModel.selectedLanguage.collectAsState()
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Horizontal Pager allows effortless fingertip scrolling between tabs
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_horizontal_pager")
            ) { page ->
                when (page) {
                    0 -> EasyInstallerScreen(viewModel)
                    1 -> SettingsScreen(viewModel)
                    2 -> RadarScreen(viewModel)
                    3 -> LogsScreen(viewModel)
                    4 -> AboutScreen(viewModel)
                }
            }

            // Persistent Glassmorphic Top Header Box (Mirroring menu bounds / styling)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp, start = 14.dp, end = 14.dp)
                    .height(68.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Adaptive brand picture logo matching dimensions of menu item
                    Image(
                        painter = painterResource(id = R.drawable.img_nova_radar_logo_1781975654739),
                        contentDescription = "Nova Radar Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "NOVA RADAR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Text(
                            text = "NOVA PROXY GROUP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            // Floating Glassmorphic Navigation Capsule with enhanced visibility & glass translucency
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 14.dp, start = 14.dp, end = 14.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(32.dp)
                    ),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavigationItemData(
                        key = "tab_installer",
                        selectedIcon = Icons.Filled.Download,
                        unselectedIcon = Icons.Outlined.Download
                    ),
                    NavigationItemData(
                        key = "tab_settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings
                    ),
                    NavigationItemData(
                        key = "tab_radar",
                        selectedIcon = Icons.Filled.Radar,
                        unselectedIcon = Icons.Outlined.Radar
                    ),
                    NavigationItemData(
                        key = "tab_logs",
                        selectedIcon = Icons.Filled.List,
                        unselectedIcon = Icons.Outlined.List
                    ),
                    NavigationItemData(
                        key = "tab_about",
                        selectedIcon = Icons.Filled.Info,
                        unselectedIcon = Icons.Outlined.Info
                    )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                if (isRadar) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_nova_radar_logo_1781975654739),
                                        contentDescription = Localization.get(item.key, lang),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            ),
                                        contentScale = ContentScale.Crop,
                                        alpha = if (isSelected) 1f else 0.75f
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = Localization.get(item.key, lang),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (isRadar) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_item_${item.key}")
                        )
                    }
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
