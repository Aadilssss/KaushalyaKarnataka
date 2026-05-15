package com.kaushalyakarnataka.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GalaxyBg = Color(0xFF0B0F1A)
private val PrimaryPurple = Color(0xFFA855F7)
private val SecondaryPink = Color(0xFFE879F9)
private val MutedSlate = Color(0xFF94A3B8)

private val KaushalyaDark = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    secondary = SecondaryPink,
    onSecondary = Color.Black,
    tertiary = Color(0xFF6366F1),
    background = GalaxyBg,
    surface = Color(0xFF161B2C),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E2638),
    outline = Color(0x33A855F7),
)

@Composable
fun KaushalyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KaushalyaDark,
        typography = KaushalyaTypography,
        content = content,
    )
}

object KaushalyaColors {
    val Background = GalaxyBg
    val GlassOutline = Color(0x4DFFFFFF)
    val GlassSurface = Color(0x1AFFFFFF)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Amber = Color(0xFFFBBF24)
    val Muted = MutedSlate
}
