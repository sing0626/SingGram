package com.sing.tgthird.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val tgThirdColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F6F68),
    onPrimary = Color.White,
    secondary = Color(0xFFC7523A),
    onSecondary = Color.White,
    tertiary = Color(0xFFA05D22),
    background = Color(0xFFFBFAF7),
    onBackground = Color(0xFF24211D),
    surface = Color(0xFFF0EEE9),
    onSurface = Color(0xFF24211D),
    surfaceVariant = Color(0xFFE4DFD6),
    onSurfaceVariant = Color(0xFF5D564D),
    outline = Color(0xFFCCC5BA),
    error = Color(0xFF9B2F21)
)

@Composable
fun TgThirdTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = tgThirdColors,
        content = content
    )
}
