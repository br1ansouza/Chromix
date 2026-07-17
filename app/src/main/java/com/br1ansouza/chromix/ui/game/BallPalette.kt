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

    // Paleta "pôr-do-sol no pampa": tons quentes + campos e céu, na pegada RS.
    // Ordenada intercalando quente/frio para os primeiros níveis (que usam as
    // primeiras N cores) terem contraste máximo entre si.
    val colors = listOf(
        Color(0xFFDF2A33), // 0 vermelho
        Color(0xFF2FB8AC), // 1 turquesa
        Color(0xFFECBE13), // 2 dourado
        Color(0xFF046D8B), // 3 azul profundo
        Color(0xFFF8572D), // 4 laranja
        Color(0xFF00963F), // 5 verde bandeira RS
        Color(0xFFA22543), // 6 vinho
        Color(0xFF93A42A), // 7 oliva
        Color(0xFFF6B149), // 8 areia
        Color(0xFF8C4A9E), // 9 violeta
        Color(0xFF309292), // 10 petróleo
        Color(0xFF6B312D), // 11 marrom
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
