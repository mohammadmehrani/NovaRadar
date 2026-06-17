package com.irnova.novaradar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.irnova.novaradar.Constants
import com.irnova.novaradar.R
import com.irnova.novaradar.data.model.DefaultSources
import com.irnova.novaradar.ui.theme.*
import com.irnova.novaradar.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val enabledIds by viewModel.enabledSourceIds.collectAsState()
    val selectedPorts by viewModel.selectedPorts.collectAsState()

    val ports = listOf("443", "2053", "2083", "2087", "2096", "8443", "80", "2052", "2082", "2086", "2095", "8080")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = if(isDarkMode) NovaPrimary else LightPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader(stringResource(R.string.theme).uppercase())
            
            Surface(
                color = if(isDarkMode) NovaSurface else Color.White, 
                shape = RoundedCornerShape(24.dp), 
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.dark_mode), color = if(isDarkMode) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it) }, colors = SwitchDefaults.colors(checkedTrackColor = NovaPrimary))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(stringResource(R.string.language).uppercase())
            
            Surface(
                color = if(isDarkMode) NovaSurface else Color.White, 
                shape = RoundedCornerShape(24.dp), 
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (language == "en") "English" else "فارسی", color = if(isDarkMode) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                    Row {
                        FilterChip(
                            selected = language == "en",
                            onClick = { viewModel.setLanguage("en") },
                            label = { Text("EN") },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NovaPrimary, selectedLabelColor = Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = language == "fa",
                            onClick = { viewModel.setLanguage("fa") },
                            label = { Text("FA") },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NovaPrimary, selectedLabelColor = Color.White)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(stringResource(R.string.ports).uppercase())
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Button(
                    onClick = { viewModel.selectAllPorts(ports) }, 
                    colors = ButtonDefaults.buttonColors(containerColor = if(isDarkMode) NovaSurface else Color.White, contentColor = NovaPrimary),
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(44.dp).border(1.dp, NovaPrimary.copy(0.3f), CircleShape)
                ) {
                    Text(stringResource(R.string.select_all).uppercase(), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { viewModel.clearAllPorts() }, 
                    colors = ButtonDefaults.buttonColors(containerColor = if(isDarkMode) NovaSurface else Color.White, contentColor = NovaRed),
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(44.dp).border(1.dp, NovaRed.copy(0.3f), CircleShape)
                ) {
                    Text(stringResource(R.string.clear_all).uppercase(), fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
            }
            
            // Capsule / Dome shape ports as requested
            Column {
                ports.chunked(3).forEach { rowPorts ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowPorts.forEach { port ->
                            PortItem(
                                port = port,
                                isSelected = selectedPorts.contains(port),
                                onToggle = { viewModel.togglePort(port, !selectedPorts.contains(port)) },
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                            )
                        }
                        repeat(3 - rowPorts.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader(stringResource(R.string.ip_sources).uppercase())
        }

        items(DefaultSources) { source ->
            SourceItem(
                name = source.name,
                isEnabled = enabledIds.contains(source.id),
                isDarkMode = isDarkMode,
                onToggle = { viewModel.toggleSource(source.id, it) }
            )
        }
        
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                Text(text = Constants.APP_VERSION, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.Gray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun PortItem(port: String, isSelected: Boolean, onToggle: () -> Unit, isDarkMode: Boolean, modifier: Modifier) {
    val activeColor = if(isDarkMode) NovaPrimary else LightPrimary
    Surface(
        onClick = onToggle,
        modifier = modifier.height(48.dp),
        shape = CircleShape, // Capsule / Dome shape
        color = if (isSelected) activeColor.copy(0.15f) else (if(isDarkMode) NovaSurface else Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) activeColor else (if(isDarkMode) Color.White.copy(0.05f) else Color.Black.copy(0.05f)))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = port,
                color = if (isSelected) activeColor else (if(isDarkMode) Color.White else Color.Black),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun SourceItem(name: String, isEnabled: Boolean, isDarkMode: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        color = if(isDarkMode) NovaSurface else Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp).background(if(isDarkMode) NovaPrimary.copy(0.1f) else LightPrimary.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = if(isDarkMode) NovaPrimary else LightPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, color = if(isDarkMode) Color.White else Color.Black, fontSize = 14.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = if(isDarkMode) NovaPrimary else LightPrimary)
            )
        }
    }
}
