package com.irnova.novaradar.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.irnova.novaradar.R
import com.irnova.novaradar.ui.theme.*
import com.irnova.novaradar.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(homeViewModel: HomeViewModel = hiltViewModel()) {
    val icons = listOf(Icons.Default.TrackChanges, Icons.Default.Terminal, Icons.Default.Settings, Icons.Default.Info)
    
    // 4 Pages: Scanner, Logs, Settings, About
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val isDark = !MaterialTheme.colorScheme.surface.let { it == Color.White || it == LightSurface }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Horizontal Pager for Swipe Navigation - Enabled as requested
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true 
        ) { page ->
            when (page) {
                0 -> HomeScreen(homeViewModel)
                1 -> LogsScreen(homeViewModel)
                2 -> SettingsScreen()
                3 -> AboutScreen()
            }
        }

        // Modern Bottom Bar - Fixed at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .fillMaxWidth()
                .height(76.dp),
            shape = CircleShape,
            color = if (isDark) NovaSurface.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.95f),
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (index in 0 until 4) {
                    val isSelected = pagerState.currentPage == index
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) (if(isDark) NovaPrimary else LightPrimary) else Color.Gray.copy(alpha = 0.5f),
                        animationSpec = tween(300), label = "tint"
                    )
                    
                    val iconSize by animateDpAsState(
                        targetValue = if (isSelected) 26.dp else 22.dp,
                        animationSpec = tween(300), label = "size"
                    )

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                icons[index],
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(iconSize)
                            )
                            if (isSelected) {
                                Box(modifier = Modifier.padding(top = 4.dp).size(4.dp).background(iconTint, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
