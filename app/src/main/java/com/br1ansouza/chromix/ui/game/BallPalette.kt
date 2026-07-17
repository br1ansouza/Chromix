package com.br1ansouza.chromix.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Paleta de bolinhas: cores vibrantes sobre fundo preto. Cada cor tem um
 * marcador geométrico próprio desenhado sobre a bolinha, para não depender
 * apenas do matiz (acessibilidade para daltonismo).
 */
object BallPalette {

    val colors = listOf(
        Color(0xFFFF5252), // 0 vermelho
        Color(0xFFFF9100), // 1 laranja
        Color(0xFFFFD740), // 2 amarelo
        Color(0xFFC6FF00), // 3 lima
        Color(0xFF69F0AE), // 4 verde
        Color(0xFF1DE9B6), // 5 teal
        Color(0xFF40C4FF), // 6 azul claro
        Color(0xFF448AFF), // 7 azul
        Color(0xFF7C4DFF), // 8 roxo
        Color(0xFFE040FB), // 9 magenta
        Color(0xFFFF4081), // 10 rosa
        Color(0xFFA1887F), // 11 taupe
    )

    fun colorOf(colorId: Int): Color = colors[colorId % colors.size]

    fun DrawScope.drawBall(colorId: Int, center: Offset, radius: Float) {
        val color = colorOf(colorId)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    lerpToWhite(color, 0.35f),
                    color,
                    lerpToBlack(color, 0.30f),
                ),
                center = center + Offset(-radius * 0.3f, -radius * 0.35f),
                radius = radius * 1.8f,
            ),
            radius = radius,
            center = center,
        )
        drawMarker(colorId, center, radius)
    }

    /** Marcador geométrico distinto por cor, em branco translúcido. */
    private fun DrawScope.drawMarker(colorId: Int, center: Offset, radius: Float) {
        val marker = Color.White.copy(alpha = 0.85f)
        val r = radius * 0.38f
        val stroke = Stroke(width = radius * 0.16f)

        when (colorId % 12) {
            0 -> drawCircle(marker, r * 0.7f, center)                                  // ponto
            1 -> drawCircle(marker, r, center, style = stroke)                         // anel
            2 -> drawLine(marker, center - Offset(r, 0f), center + Offset(r, 0f), stroke.width) // barra —
            3 -> drawLine(marker, center - Offset(0f, r), center + Offset(0f, r), stroke.width) // barra |
            4 -> {                                                                     // cruz +
                drawLine(marker, center - Offset(r, 0f), center + Offset(r, 0f), stroke.width)
                drawLine(marker, center - Offset(0f, r), center + Offset(0f, r), stroke.width)
            }
            5 -> {                                                                     // xis
                val d = r * 0.75f
                drawLine(marker, center + Offset(-d, -d), center + Offset(d, d), stroke.width)
                drawLine(marker, center + Offset(-d, d), center + Offset(d, -d), stroke.width)
            }
            6 -> drawPath(trianglePath(center, r), marker)                             // triângulo
            7 -> drawRect(                                                             // quadrado
                marker,
                topLeft = center - Offset(r * 0.8f, r * 0.8f),
                size = androidx.compose.ui.geometry.Size(r * 1.6f, r * 1.6f),
            )
            8 -> drawPath(diamondPath(center, r), marker)                              // losango
            9 -> drawPath(trianglePath(center, r, flip = true), marker)                // triângulo invertido
            10 -> drawPath(diamondPath(center, r), marker, style = stroke)             // losango vazado
            11 -> drawRect(                                                            // quadrado vazado
                marker,
                topLeft = center - Offset(r * 0.8f, r * 0.8f),
                size = androidx.compose.ui.geometry.Size(r * 1.6f, r * 1.6f),
                style = stroke,
            )
        }
    }

    private fun trianglePath(center: Offset, r: Float, flip: Boolean = false): Path {
        val sign = if (flip) -1f else 1f
        return Path().apply {
            moveTo(center.x, center.y - r * sign)
            lineTo(center.x - r, center.y + r * 0.75f * sign)
            lineTo(center.x + r, center.y + r * 0.75f * sign)
            close()
        }
    }

    private fun diamondPath(center: Offset, r: Float): Path = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r, center.y)
        close()
    }

    private fun lerpToWhite(color: Color, fraction: Float): Color = Color(
        red = color.red + (1f - color.red) * fraction,
        green = color.green + (1f - color.green) * fraction,
        blue = color.blue + (1f - color.blue) * fraction,
    )

    private fun lerpToBlack(color: Color, fraction: Float): Color = Color(
        red = color.red * (1f - fraction),
        green = color.green * (1f - fraction),
        blue = color.blue * (1f - fraction),
    )
}
