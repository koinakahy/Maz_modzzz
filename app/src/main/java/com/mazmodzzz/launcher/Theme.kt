package com.mazmodzzz.launcher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Cyan = Color(0xFF00E5FF)
val Violet = Color(0xFFB388FF)
val Ink = Color(0xFF0A0B14)
val Panel = Color(0xFF151728)
val Panel2 = Color(0xFF1D2036)
val Muted = Color(0xFF9EA3C4)

val Accents: List<Int> = listOf(
    0xFF00E5FF, 0xFFB388FF, 0xFFFF4081, 0xFFFFC400,
    0xFF69F0AE, 0xFFFF6E40, 0xFF448AFF, 0xFFEA80FC
).map { it.toInt() }

val BrandBrush = Brush.horizontalGradient(listOf(Cyan, Violet))

@Composable
fun MazTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            secondary = Violet,
            background = Ink,
            surface = Panel,
            surfaceVariant = Panel2,
            onPrimary = Ink,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Muted
        ),
        content = content
    )
}
