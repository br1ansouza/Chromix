package com.br1ansouza.chromix.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.br1ansouza.chromix.domain.GameState
import com.br1ansouza.chromix.ui.haptics.GameHaptics
import com.br1ansouza.chromix.ui.sound.GameSounds
import com.br1ansouza.chromix.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    onOpenLevels: () -> Unit = {},
) {
    val loadedState by viewModel.uiState.collectAsState()
    val background = MaterialTheme.colorScheme.background
    val state = loadedState ?: run {
        // Progresso ainda carregando (milissegundos): mantém a tela vazia.
        Box(modifier = Modifier.fillMaxSize().background(background))
        return
    }
    val context = LocalContext.current
    val haptics = remember { GameHaptics(context) }
    val sounds = remember { GameSounds(context) }
    DisposableEffect(Unit) {
        onDispose { sounds.release() }
    }

    // (tubeId, seq) para reiniciar a animação de shake a cada movimento inválido.
    var shakeTrigger by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    LaunchedEffect(Unit) {
        var seq = 0L
        viewModel.events.collect { event ->
            val vibrate = viewModel.uiState.value?.vibrationEnabled == true
            val sound = viewModel.uiState.value?.soundEnabled == true
            when (event) {
                is GameViewModel.GameEvent.TubeSelected -> {
                    if (sound) sounds.ballSelected()
                    if (vibrate) haptics.tubeSelected()
                }
                is GameViewModel.GameEvent.TubeDeselected -> if (sound) sounds.ballSelected()
                is GameViewModel.GameEvent.InvalidMove -> {
                    shakeTrigger = event.tubeId to ++seq
                    if (vibrate) haptics.invalidMove()
                }
                is GameViewModel.GameEvent.ValidMove -> if (vibrate) haptics.validMove()
                is GameViewModel.GameEvent.TubeCompleted -> if (vibrate) haptics.tubeCompleted()
                is GameViewModel.GameEvent.LevelWon -> {
                    if (vibrate) haptics.levelWon()
                    if (sound) sounds.levelWon()
                }
            }
        }
    }

    // Reset com respiro: tabuleiro encolhe/some (120ms), regenera, volta (180ms).
    val scope = rememberCoroutineScope()
    val boardVisibility = remember { Animatable(1f) }
    val animatedReset: () -> Unit = {
        scope.launch {
            boardVisibility.animateTo(0f, tween(120, easing = FastOutSlowInEasing))
            viewModel.resetLevel()
            boardVisibility.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameHud(
                levelNumber = state.levelNumber,
                canUndo = state.canUndo,
                vibrationEnabled = state.vibrationEnabled,
                soundEnabled = state.soundEnabled,
                onUndo = viewModel::undo,
                onReset = animatedReset,
                onToggleVibration = viewModel::toggleVibration,
                onToggleSound = viewModel::toggleSound,
                onOpenLevels = onOpenLevels,
            )

            Crossfade(
                targetState = state.levelNumber,
                animationSpec = tween(220),
                label = "level-transition",
                modifier = Modifier.weight(1f),
            ) { levelNumber ->
                // O Crossfade mantém o tabuleiro antigo durante a transição;
                // só o nível atual usa o estado vivo do ViewModel.
                if (levelNumber == state.levelNumber) {
                    GameBoard(
                        state = state,
                        shakeTrigger = shakeTrigger,
                        onTubeTap = viewModel::onTubeTap,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .graphicsLayer {
                                alpha = boardVisibility.value
                                val scale = 0.94f + 0.06f * boardVisibility.value
                                scaleX = scale
                                scaleY = scale
                            },
                    )
                }
            }
        }

        WinOverlay(
            visible = state.gameState == GameState.WON,
            levelNumber = state.levelNumber,
            moveCount = state.moveCount,
            onNextLevel = viewModel::nextLevel,
            onBackToLevels = onOpenLevels,
        )
    }
}

@Composable
private fun GameHud(
    levelNumber: Int,
    canUndo: Boolean,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onToggleVibration: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenLevels: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Nível $levelNumber",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleSound) {
            Icon(
                imageVector = if (soundEnabled) {
                    Icons.AutoMirrored.Filled.VolumeUp
                } else {
                    Icons.AutoMirrored.Filled.VolumeOff
                },
                contentDescription = if (soundEnabled) "Desativar som" else "Ativar som",
                tint = if (soundEnabled) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onToggleVibration) {
            Icon(
                imageVector = Icons.Filled.Vibration,
                contentDescription = if (vibrationEnabled) {
                    "Desativar vibração"
                } else {
                    "Ativar vibração"
                },
                tint = if (vibrationEnabled) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Desfazer",
                tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Reiniciar fase",
                tint = Color.White,
            )
        }
        IconButton(onClick = onOpenLevels) {
            Icon(
                imageVector = Icons.Filled.GridView,
                contentDescription = "Escolher nível",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun WinOverlay(
    visible: Boolean,
    levelNumber: Int,
    moveCount: Int,
    onNextLevel: () -> Unit,
    onBackToLevels: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.85f, animationSpec = tween(220)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            // Comemoração na moral gaúcha, estável por nível.
            val exclamations = remember {
                listOf(
                    "Bah, tri massa!",
                    "Tchê, mandou bem!",
                    "Mas que capaz!",
                    "Baita capricho!",
                    "Tri bem!",
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.84f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                // Listras da bandeira do RS coroando o card.
                Row {
                    listOf(
                        Color(0xFF00963F),
                        Color(0xFFDF2A33),
                        Color(0xFFECBE13),
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(width = 30.dp, height = 4.dp)
                                .background(color, RoundedCornerShape(2.dp)),
                        )
                    }
                }
                Text(
                    text = exclamations[levelNumber % exclamations.size],
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    text = "Nível $levelNumber concluído",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "$moveCount movimentos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Button(
                    onClick = onNextLevel,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                ) {
                    Text(
                        text = "Próximo nível",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                TextButton(
                    onClick = onBackToLevels,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("Voltar aos níveis", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
