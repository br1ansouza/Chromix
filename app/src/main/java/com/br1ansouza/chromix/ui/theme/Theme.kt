package com.br1ansouza.chromix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema escuro fixo: fundo preto, destaques vibrantes sobre o preto.
private val ChromixColorScheme = darkColorScheme(
    primary = Color(0xFF40C4FF),
    onPrimary = Color.Black,
    secondary = Color(0xFF69F0AE),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    error = Color(0xFFFF5252),
)

@Composable
fun ChromixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChromixColorScheme,
        content = content
    )
}
