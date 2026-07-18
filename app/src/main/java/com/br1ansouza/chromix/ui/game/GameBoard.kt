package com.br1ansouza.chromix.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.mutableLongStateOf
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

/**
 * Medidas do tabuleiro em px, compartilhadas entre tubos e o overlay de voo.
 * O composable do tubo tem exatamente a altura do vidro: a bolinha levantada é
 * desenhada ACIMA dos limites (nada na hierarquia clipa), então não se reserva
 * headroom no layout — era isso que deixava faixas vazias sobre cada fileira.
 */
private data class BoardMetrics(
    val ballSize: Float,
    val tubeWidth: Float,
    val tubeHeight: Float,
    val bottomPad: Float,
) {
    /** Centro da bolinha [index] (0 = fundo) relativo à origem do composable do tubo. */
    fun ballCenter(index: Int): Offset = Offset(
        x = tubeWidth / 2f,
        y = tubeHeight - bottomPad - ballSize / 2f - index * ballSize,
    )

    /** Posição da bolinha "levantada", acima da boca do tubo (y negativo). */
    val liftedCenter: Offset get() = Offset(tubeWidth / 2f, -ballSize * 0.55f)
}

private const val MAX_TUBES_PER_ROW = 6
private const val FLIGHT_SEGMENT_MS = 220
private const val FLIGHT_STAGGER_MS = 90

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

        // Ocupa o máximo da tela: teto alto e margens mínimas (sobrava muita
        // borda em telas grandes). A folga de altura cobre lift + espaçamento.
        val widthBased = (maxWidth / perRow) - 18.dp
        val heightBased = (maxHeight / rows - 48.dp) / capacity
        val ballSizeDp: Dp = minOf(72.dp, widthBased, heightBased)

        val density = LocalDensity.current
        val metrics = with(density) {
            val ball = ballSizeDp.toPx()
            BoardMetrics(
                ballSize = ball,
                tubeWidth = ball + 10.dp.toPx(),
                tubeHeight = ball * capacity + 12.dp.toPx(),
                bottomPad = 6.dp.toPx(),
            )
        }
        val tubeWidthDp = with(density) { metrics.tubeWidth.toDp() }
        val tubeHeightDp = with(density) { metrics.tubeHeight.toDp() }

        val tubePositions = remember { mutableStateMapOf<Int, Offset>() }
        var boardOrigin by remember { mutableStateOf(Offset.Zero) }

        // O voo é derivado do estado na própria composição: no mesmo frame em que
        // o movimento é aplicado, a bolinha destino já nasce escondida e o overlay
        // já desenha na posição inicial — sem "teleporte" antes da animação.
        var completedFlightSeq by remember { mutableLongStateOf(0L) }
        var flightProgress by remember { mutableFloatStateOf(0f) }
        val pendingMove = state.lastMove?.takeIf { it.seq > completedFlightSeq }
        remember(pendingMove?.seq) { flightProgress = 0f }

        LaunchedEffect(state.lastMove?.seq) {
            val record = state.lastMove ?: return@LaunchedEffect
            if (record.seq <= completedFlightSeq) return@LaunchedEffect
            // Grupo: bolinhas decolam em fila (stagger), cada uma com seu
            // segmento de voo; o progresso mestre é linear e o easing é
            // aplicado por segmento no desenho.
            val totalMs = FLIGHT_SEGMENT_MS + (record.count - 1) * FLIGHT_STAGGER_MS
            try {
                Animatable(0f).animateTo(
                    1f,
                    tween(durationMillis = totalMs, easing = LinearEasing),
                ) {
                    flightProgress = value
                }
            } finally {
                completedFlightSeq = record.seq
            }
        }

        val hiddenTubeId = pendingMove?.move?.toTubeId
        // No destino, as últimas [count] bolinhas ficam escondidas enquanto o
        // overlay as desenha voando (ou já pousadas, no fim do segmento).
        val hiddenFromIndex = pendingMove?.let { pm ->
            state.tubes.first { it.id == pm.move.toTubeId }.balls.size - pm.count
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { boardOrigin = it.positionInRoot() },
            // Fileiras ancoradas no topo, gap enxuto (a bolinha levantada pode
            // sobrepor de leve a fileira de cima — passageiro e sem clip).
            verticalArrangement = Arrangement.spacedBy(
                ballSizeDp * 0.5f,
                Alignment.Top,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.tubes.chunked(perRow).forEach { rowTubes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTubes.forEach { tube ->
                        TubeView(
                            tube = tube,
                            metrics = metrics,
                            widthDp = tubeWidthDp,
                            heightDp = tubeHeightDp,
                            isSelected = state.selectedTubeId == tube.id,
                            isFlightSource = pendingMove?.move?.fromTubeId == tube.id,
                            hiddenFromIndex = if (tube.id == hiddenTubeId) hiddenFromIndex else null,
                            shakeSeq = shakeTrigger?.takeIf { it.first == tube.id }?.second,
                            onTap = { onTubeTap(tube.id) },
                            onPositioned = { pos -> tubePositions[tube.id] = pos },
                        )
                    }
                }
            }
        }

        // Overlay das bolinhas em voo, em arco Bézier quadrático, em fila.
        pendingMove?.let { record ->
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
            ) {
                val fromPos = tubePositions[record.move.fromTubeId]?.minus(boardOrigin)
                    ?: return@Canvas
                val toPos = tubePositions[record.move.toTubeId]?.minus(boardOrigin)
                    ?: return@Canvas
                val baseIndex = hiddenFromIndex ?: return@Canvas
                val start = fromPos + metrics.liftedCenter
                val totalMs = FLIGHT_SEGMENT_MS + (record.count - 1) * FLIGHT_STAGGER_MS

                repeat(record.count) { i ->
                    val end = toPos + metrics.ballCenter(baseIndex + i)
                    val raw = ((flightProgress * totalMs - i * FLIGHT_STAGGER_MS) /
                        FLIGHT_SEGMENT_MS).coerceIn(0f, 1f)
                    val t = FastOutSlowInEasing.transform(raw)

                    val control = Offset(
                        (start.x + end.x) / 2f,
                        min(start.y, end.y) - metrics.ballSize * 1.4f,
                    )
                    val oneMinusT = 1f - t
                    val pos = Offset(
                        oneMinusT * oneMinusT * start.x + 2 * oneMinusT * t * control.x +
                            t * t * end.x,
                        oneMinusT * oneMinusT * start.y + 2 * oneMinusT * t * control.y +
                            t * t * end.y,
                    )
                    drawBall(record.colorId, pos, metrics.ballSize / 2f * 0.92f)
                }
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
    isFlightSource: Boolean,
    hiddenFromIndex: Int?,
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

    // Levantada suave da bolinha do topo ao selecionar. Quando a deseleção vem
    // de um movimento (o voo assume a bolinha), o recuo é instantâneo para a
    // nova bolinha do topo não aparecer descendo do nada.
    val lift = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        when {
            isSelected -> lift.animateTo(1f, tween(140, easing = FastOutSlowInEasing))
            isFlightSource -> lift.snapTo(0f)
            lift.value > 0f -> lift.animateTo(0f, tween(120))
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
            val tubeTop = 0f
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
                if (hiddenFromIndex != null && index >= hiddenFromIndex) return@forEachIndexed
                val isTop = index == tube.balls.lastIndex
                val center = if (isTop && lift.value > 0f) {
                    val slot = metrics.ballCenter(index)
                    Offset(
                        slot.x,
                        slot.y + (metrics.liftedCenter.y - slot.y) * lift.value,
                    )
                } else {
                    metrics.ballCenter(index)
                }
                drawBall(ball.colorId, center, ballRadius)
            }
        }
    }
}
