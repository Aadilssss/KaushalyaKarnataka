package com.kaushalyakarnataka.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Fixed seed for stable, non-flickering stars across screens
    val starPositions = remember {
        val random = Random(42)
        List(100) {
            Offset(random.nextFloat(), random.nextFloat())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B0F1A),
                        Color(0xFF10182A),
                        Color(0xFF0B0F1A),
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            starPositions.forEach { pos ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = 1.5f,
                    center = Offset(pos.x * size.width, pos.y * size.height)
                )
            }
        }
        content()
    }
}
