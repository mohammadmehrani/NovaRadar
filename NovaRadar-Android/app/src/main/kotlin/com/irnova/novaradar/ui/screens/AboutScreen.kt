package com.irnova.novaradar.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irnova.novaradar.Constants
import com.irnova.novaradar.R
import com.irnova.novaradar.ui.theme.*

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val version = Constants.APP_VERSION
    val isDark = !MaterialTheme.colorScheme.surface.let { it == Color.White || it == LightSurface }
    val brush = if (isDark) Brush.linearGradient(GeminiDarkGradient) else Brush.linearGradient(GeminiLightGradient)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Large Dynamic Logo with 'N' - Multi-color background
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(brush, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(stringResource(R.string.app_name).uppercase(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = if(isDark) Color.White else Color.Black)
            Text("NEURAL NETWORK INTELLIGENCE", color = if(isDark) NovaPrimary else LightPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Gemini Multi-color boxes for social links
            AboutLinkItem(Icons.Default.Code, "GitHub", "github.com/IRNova/NovaRadar", brush, isDark) {
                openUrl(context, Constants.GITHUB_URL)
            }
            AboutLinkItem(Icons.Default.Send, "Telegram Channel", "t.me/irnova_proxy", brush, isDark) {
                openUrl(context, Constants.TELEGRAM_CHANNEL)
            }
            AboutLinkItem(Icons.Default.SupportAgent, "Support", "@irnovaproxy", brush, isDark) {
                openUrl(context, Constants.SUPPORT_URL)
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Version Info Box
            Surface(
                color = if(isDark) NovaSurface else Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.version), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(version, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.created_by).uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            
            Spacer(modifier = Modifier.height(160.dp)) // Extra padding for bottom bar
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLinkItem(icon: ImageVector, title: String, subtitle: String, brush: Brush, isDark: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if(isDark) NovaSurface else Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).border(1.dp, if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), RoundedCornerShape(24.dp)),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).background(brush, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = if(isDark) Color.White else Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray.copy(0.5f))
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {}
}
