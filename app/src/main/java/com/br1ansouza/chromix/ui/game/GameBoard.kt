package com.br1ansouza.chromix.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.br1ansouza.chromix.domain.Tube
import com.br1ansouza.chromix.ui.game.BallPalette.drawBall
import com.br1ansouza.chromix.viewmodel.GameViewModel
import kotlin.math.ceil
import kotlin.math.min

/** Medidas do tabuleiro em px, compartilhadas entre tubos e o overlay de voo. */
private data class BoardMetrics(
    val ballSize: Float,
    val tubeWidth: Float,
    val tubeHeight: Float,
    val headroom: Float,
    val bottomPad: Float,
) {
    val tubeTotalHeight: Float get() = headroom + tubeHeight

    /** Centro da bolinha [index] (0 = fundo) relativo à origem do composable do tubo. */
    fun ballCenter(index: Int): Offset = Offset(
        x = tubeWidth / 2f,
        y = tubeTotalHeight - bottomPad - ballSize / 2f - index * ballSize,
    )

    /** Posição da bolinha "levantada" acima da boca do tubo. */
    val liftedCenter: Offset get() = Offset(tubeWidth / 2f, headroom * 0.5f)
}

private data class Flight(
    val seq: Long,
    val colorId: Int,
    val toTubeId: Int,
    val hiddenIndex: Int,
    val start: Offset,
    val end: Offset,
)

private const val MAX_TUBES_PER_ROW = 6

@Composable
fun GameBoard(
    state: GameViewModel.GameUiState,
    shakeTrigger: Pair<Int, Long>?, // (tubeId, seq) do movimento inválido
    onTubeTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tubeCount = state.tubes.size
        val rows = ceil(tubeCount / MAX_TUBES_PER_ROW.toFloat()).toInt()
        val perRow = ceil(tubeCount / rows.toFloat()).toInt()
        val capacity = state.tubeCapacity

        val widthBased = (maxWidth / perRow) - 22.dp
        val heightBased = (maxHeight / rows - 40.dp) / (capacity + 1)
        val ballSizeDp: Dp = minOf(52.dp, widthBased, heightBased)

        val density = LocalDensity.current
        val metrics = with(density) {
            val ball = ballSizeDp.toPx()
            BoardMetrics(
                ballSize = ball,
                tubeWidth = ball + 10.dp.toPx(),
                tubeHeight = ball * capacity + 12.dp.toPx(),
                headroom = ball + 10.dp.toPx(),
                bottomPad = 6.dp.toPx(),
            )
        }
        val tubeWidthDp = with(density) { metrics.tubeWidth.toDp() }
        val tubeTotalHeightDp = with(density) { metrics.tubeTotalHeight.toDp() }

        val tubePositions = remember { mutableStateMapOf<Int, Offset>() }
        var boardOrigin by remember { mutableStateOf(Offset.Zero) }
        var flight by remember { mutableStateOf<Flight?>(null) }
        var flightProgress by remember { mutableFloatStateOf(0f) }

        // Dispara a animação de voo quando um movimento é aplicado.
        LaunchedEffect(state.lastMove) {
            val record = state.lastMove ?: return@LaunchedEffect
            val fromPos = tubePositions[record.move.fromTubeId] ?: return@LaunchedEffect
            val toPos = tubePositions[record.move.toTubeId] ?: return@LaunchedEffect
            val destTube = state.tubes.first { it.id == record.move.toTubeId }
            val landedIndex = destTube.balls.size - 1

            flight = Flight(
                seq = record.seq,
                colorId = record.colorId,
                toTubeId = record.move.toTubeId,
                hiddenIndex = landedIndex,
                start = fromPos + metrics.liftedCenter,
                end = toPos + metrics.ballCenter(landedIndex),
            )
            try {
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(durationMillis = 260, easing = FastOutSlowInEasing)) {
                    flightProgress = value
                }
            } finally {
                flight = null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { boardOrigin = it.positionInRoot() },
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.tubes.chunked(perRow).forEach { rowTubes ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowTubes.forEach { tube ->
                        val activeFlight = flight
                        TubeView(
                            tube = tube,
                            metrics = metrics,
                            widthDp = tubeWidthDp,
                            heightDp = tubeTotalHeightDp,
                            isSelected = state.selectedTubeId == tube.id,
                            hiddenBallIndex = activeFlight
                                ?.takeIf { it.toTubeId == tube.id }
                                ?.hiddenIndex,
                            shakeSeq = shakeTrigger?.takeIf { it.first == tube.id }?.second,
                            onTap = { onTubeTap(tube.id) },
                            onPositioned = { pos ->
                                tubePositions[tube.id] = pos - boardOrigin
                            },
                        )
                    }
                }
            }
        }

        // Overlay da bolinha em voo, em arco Bézier quadrático.
        flight?.let { f ->
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            ) {
                val t = flightProgress
                val control = Offset(
                    (f.start.x + f.end.x) / 2f,
                    min(f.start.y, f.end.y) - metrics.ballSize * 1.4f,
                )
                val oneMinusT = 1f - t
                val pos = Offset(
                    oneMinusT * oneMinusT * f.start.x + 2 * oneMinusT * t * control.x + t * t * f.end.x,
                    oneMinusT * oneMinusT * f.start.y + 2 * oneMinusT * t * control.y + t * t * f.end.y,
                )
                drawBall(f.colorId, pos, metrics.ballSize / 2f * 0.92f)
            }
        }
    }
}

@Composable
private fun TubeView(
    tube: Tube,
    metrics: BoardMetrics,
    widthDp: Dp,
    heightDp: Dp,
    isSelected: Boolean,
    hiddenBallIndex: Int?,
    shakeSeq: Long?,
    onTap: () -> Unit,
    onPositioned: (Offset) -> Unit,
) {
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shakeSeq) {
        if (shakeSeq == null) return@LaunchedEffect
        shakeOffset.animateTo(
            0f,
            keyframes {
                durationMillis = 150
                -8f at 25
                8f at 60
                -5f at 95
                5f at 125
                0f at 150
            },
        )
    }

    val pulse = remember { Animatable(1f) }
    LaunchedEffect(tube.isComplete) {
        if (tube.isComplete) {
            pulse.animateTo(1.08f, tween(120))
            pulse.animateTo(1f, tween(160))
        }
    }

    Box(
        modifier = Modifier
            .size(widthDp, heightDp)
            .graphicsLayer {
                translationX = shakeOffset.value
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .onGloballyPositioned { onPositioned(it.positionInRoot()) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ballRadius = metrics.ballSize / 2f * 0.92f

            // Corpo do tubo: fundo levemente visível + contorno com boca aberta.
            val tubeTop = metrics.headroom
            val outline = if (isSelected || tube.isComplete) {
                Color.White.copy(alpha = 0.75f)
            } else {
                Color.White.copy(alpha = 0.30f)
            }
            drawRoundRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = Offset(0f, tubeTop),
                size = Size(metrics.tubeWidth, metrics.tubeHeight),
                cornerRadius = CornerRadius(metrics.tubeWidth * 0.28f),
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(0f, tubeTop),
                size = Size(metrics.tubeWidth, metrics.tubeHeight),
                cornerRadius = CornerRadius(metrics.tubeWidth * 0.28f),
                style = Stroke(width = 2.dp.toPx()),
            )

            tube.balls.forEachIndexed { index, ball ->
                if (index == hiddenBallIndex) return@forEachIndexed
                val isTop = index == tube.balls.lastIndex
                val center = if (isTop && isSelected) {
                    metrics.liftedCenter
                } else {
                    metrics.ballCenter(index)
                }
                drawBall(ball.colorId, center, ballRadius)
            }
        }
    }
}
