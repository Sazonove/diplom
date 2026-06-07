package com.example.diplom.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF10B981),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
)

@Composable
fun DiplomTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
