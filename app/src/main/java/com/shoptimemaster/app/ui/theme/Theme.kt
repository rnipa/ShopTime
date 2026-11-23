package com.shoptimemaster.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CharcoalBg = Color(0xFF090D0B)
val SurfaceDark = Color(0xFF111714)
val EmeraldPrimary = Color(0xFF10B981)
val SoftGreenHighlight = Color(0xFF34D399)

private val DarkColors = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = SoftGreenHighlight,
    background = CharcoalBg,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = Color(0xFFF4F4F5),
    onSurface = Color(0xFFF4F4F5)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF059669),
    secondary = EmeraldPrimary,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF090D0B),
    onSurface = Color(0xFF090D0B)
)

@Composable
fun ShopTimeMasterTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
