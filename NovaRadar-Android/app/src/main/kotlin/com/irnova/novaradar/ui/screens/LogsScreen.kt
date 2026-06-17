package com.irnova.novaradar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irnova.novaradar.ui.theme.NovaSurface
import com.irnova.novaradar.ui.theme.RadarGreen
import com.irnova.novaradar.ui.viewmodel.HomeViewModel

@Composable
fun LogsScreen(viewModel: HomeViewModel) {
    val logs by viewModel.logs.collectAsState()
    val scrollState = rememberLazyListState()
    val isDark = !MaterialTheme.colorScheme.surface.let { it == Color.White || it == com.irnova.novaradar.ui.theme.LightSurface }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) scrollState.animateScrollToItem(logs.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            "SYSTEM TERMINAL",
            color = if(isDark) com.irnova.novaradar.ui.theme.NovaPrimary else com.irnova.novaradar.ui.theme.LightPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Terminal Window Style
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp) // Leave space for the floating bottom bar
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp)),
            color = Color.Black,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.padding(20.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = "> $log",
                        color = RadarGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
