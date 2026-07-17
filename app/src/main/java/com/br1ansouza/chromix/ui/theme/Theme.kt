package com.br1ansouza.chromix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema escuro fixo com acentos da paleta RS (pôr-do-sol no pampa).
private val ChromixColorScheme = darkColorScheme(
    primary = Color(0xFF2FB8AC),
    onPrimary = Color.Black,
    secondary = Color(0xFFF6B149),
    onSecondary = Color.Black,
    background = Color(0xFF121216),
    onBackground = Color.White,
    surface = Color(0xFF1B1B21),
    onSurface = Color.White,
    error = Color(0xFFDF2A33),
)

@Composable
fun ChromixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChromixColorScheme,
        content = content
    )
}
