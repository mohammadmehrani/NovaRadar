package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.viewmodel.AppTheme

// --- Nova Proxy Dark Colors (Deep space-grade indigo gradient + Glowing cyber neon) ---
val GeminiDarkBg = Color(0xFF080B11) // Luxury cyber dark indigo background
val GeminiDarkSurface = Color(0xFF111625) // Superb high-contrast translucent mesh-ready card
val GeminiDarkPrimary = Color(0xFF3B82F6) // Neon Royal Blue
val GeminiDarkSecondary = Color(0xFF8B5CF6) // Majestic Electric Purple
val GeminiDarkTertiary = Color(0xFF10B981) // Hyper active cyber green
val GeminiDarkOnBg = Color(0xFFF8FAFC) // Crisp white

// --- Nova Proxy Light Colors (Premium clean grey-white) ---
val GeminiLightBg = Color(0xFFF1F5F9) // Elegant soft blue-grey slate
val GeminiLightSurface = Color(0xFFFFFFFF) // Pure glassmorphic white card
val GeminiLightPrimary = Color(0xFF2563EB) // High-contrast blue
val GeminiLightSecondary = Color(0xFF7C3AED) // Deep violet
val GeminiLightTertiary = Color(0xFF0D9488) // Soft Teal
val GeminiLightOnBg = Color(0xFF0F172A) // Elite dark slate text contrast

// --- Other Cybernetic Proxy Themes ---
val SolarizedDarkBg = Color(0xFF05111B) // Holographic deep sea navy
val SolarizedDarkSurface = Color(0xFF0B1F30) // Translucent oceanic slate
val SolarizedDarkPrimary = Color(0xFF06B6D4) // Vibrant cyan beam
val SolarizedDarkSecondary = Color(0xFF22D3EE) // Luminous cyan
val SolarizedDarkOnBg = Color(0xFFE2E8F0) // Off-white

val StandardDarkBg = Color(0xFF090A0F) // Obsidian carbon midnight
val StandardDarkSurface = Color(0xFF141722) // Translucent charcoal capsule
val StandardDarkPrimary = Color(0xFF10B981) // Active matrix green
val StandardDarkSecondary = Color(0xFF22C55E) // Radiant leaf green
val StandardDarkOnBg = Color(0xFFF1F5F9) // Light grey

private val GeminiDarkColorScheme = darkColorScheme(
    primary = GeminiDarkPrimary,
    onPrimary = Color.White,
    secondary = GeminiDarkSecondary,
    onSecondary = Color.White,
    tertiary = GeminiDarkTertiary,
    background = GeminiDarkBg,
    onBackground = GeminiDarkOnBg,
    surface = GeminiDarkSurface,
    onSurface = GeminiDarkOnBg
)

private val GeminiLightColorScheme = lightColorScheme(
    primary = GeminiLightPrimary,
    onPrimary = Color.White,
    secondary = GeminiLightSecondary,
    onSecondary = Color.White,
    tertiary = GeminiLightTertiary,
    background = GeminiLightBg,
    onBackground = GeminiLightOnBg,
    surface = GeminiLightSurface,
    onSurface = GeminiLightOnBg
)

private val SolarizedDarkColorScheme = darkColorScheme(
    primary = SolarizedDarkPrimary,
    onPrimary = Color.Black,
    secondary = SolarizedDarkSecondary,
    onSecondary = Color.White,
    tertiary = SolarizedDarkPrimary,
    background = SolarizedDarkBg,
    onBackground = SolarizedDarkOnBg,
    surface = SolarizedDarkSurface,
    onSurface = SolarizedDarkOnBg
)

private val StandardDarkColorScheme = darkColorScheme(
    primary = StandardDarkPrimary,
    onPrimary = Color.Black,
    secondary = StandardDarkSecondary,
    onSecondary = Color.White,
    tertiary = StandardDarkSecondary,
    background = StandardDarkBg,
    onBackground = StandardDarkOnBg,
    surface = StandardDarkSurface,
    onSurface = StandardDarkOnBg
)

@Composable
fun NovaRadarTheme(
    theme: AppTheme = AppTheme.GEMINI_DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.GEMINI_DARK -> GeminiDarkColorScheme
        AppTheme.GEMINI_LIGHT -> GeminiLightColorScheme
        AppTheme.SOLARIZED_DARK -> SolarizedDarkColorScheme
        AppTheme.STANDARD_DARK -> StandardDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
