package com.br1ansouza.chromix.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.br1ansouza.chromix.domain.GameState
import com.br1ansouza.chromix.ui.haptics.GameHaptics
import com.br1ansouza.chromix.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val loadedState by viewModel.uiState.collectAsState()
    val state = loadedState ?: run {
        // Progresso ainda carregando (milissegundos): mantém a tela preta.
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val context = LocalContext.current
    val haptics = remember { GameHaptics(context) }

    // (tubeId, seq) para reiniciar a animação de shake a cada movimento inválido.
    var shakeTrigger by remember { mutableStateOf<Pair<Int, Long>?>(null) }
    LaunchedEffect(Unit) {
        var seq = 0L
        viewModel.events.collect { event ->
            val vibrate = viewModel.uiState.value?.vibrationEnabled == true
            when (event) {
                is GameViewModel.GameEvent.InvalidMove -> {
                    shakeTrigger = event.tubeId to ++seq
                    if (vibrate) haptics.invalidMove()
                }
                is GameViewModel.GameEvent.ValidMove -> if (vibrate) haptics.validMove()
                is GameViewModel.GameEvent.TubeCompleted -> if (vibrate) haptics.tubeCompleted()
                is GameViewModel.GameEvent.LevelWon -> if (vibrate) haptics.levelWon()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameHud(
                levelNumber = state.levelNumber,
                canUndo = state.canUndo,
                vibrationEnabled = state.vibrationEnabled,
                onUndo = viewModel::undo,
                onReset = viewModel::resetLevel,
                onToggleVibration = viewModel::toggleVibration,
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
            }
        }

        WinOverlay(
            visible = state.gameState == GameState.WON,
            levelNumber = state.levelNumber,
            moveCount = state.moveCount,
            onNextLevel = viewModel::nextLevel,
        )
    }
}

@Composable
private fun GameHud(
    levelNumber: Int,
    canUndo: Boolean,
    vibrationEnabled: Boolean,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onToggleVibration: () -> Unit,
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
    }
}

@Composable
private fun WinOverlay(
    visible: Boolean,
    levelNumber: Int,
    moveCount: Int,
    onNextLevel: () -> Unit,
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nível $levelNumber concluído",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "$moveCount movimentos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = onNextLevel,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text("Próximo nível")
                }
            }
        }
    }
}
