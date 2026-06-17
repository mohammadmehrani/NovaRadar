package com.irnova.novaradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.irnova.novaradar.ui.screens.HomeScreen
import com.irnova.novaradar.ui.theme.NovaRadarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovaRadarTheme {
                HomeScreen()
            }
        }
    }
}
