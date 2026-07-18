package com.br1ansouza.chromix.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.br1ansouza.chromix.data.GamePreferences
import com.br1ansouza.chromix.domain.GameRules
import com.br1ansouza.chromix.domain.GameState
import com.br1ansouza.chromix.domain.LevelGenerator
import com.br1ansouza.chromix.domain.Move
import com.br1ansouza.chromix.domain.Tube
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameViewModel(application: Application) : AndroidViewModel(application) {

    /** Último movimento aplicado, com sequência para disparar a animação de voo. */
    data class MoveRecord(val move: Move, val colorId: Int, val count: Int, val seq: Long)

    data class GameUiState(
        val levelNumber: Int,
        val tubes: List<Tube>,
        val tubeCapacity: Int,
        val bestLevelReached: Int = 1,
        val selectedTubeId: Int? = null,
        val gameState: GameState = GameState.PLAYING,
        val moveCount: Int = 0,
        val canUndo: Boolean = false,
        val lastMove: MoveRecord? = null,
        val vibrationEnabled: Boolean = true,
        val soundEnabled: Boolean = true,
        /** Beco sem saída: nenhum movimento útil restante — só undo/reset salvam. */
        val noMovesLeft: Boolean = false,
    )

    sealed interface GameEvent {
        data object TubeSelected : GameEvent
        data object TubeDeselected : GameEvent
        data object ValidMove : GameEvent
        data class InvalidMove(val tubeId: Int) : GameEvent
        data class TubeCompleted(val tubeId: Int) : GameEvent
        data object LevelWon : GameEvent
    }

    private val prefs = GamePreferences(application)
    private val undoStack = ArrayDeque<List<Tube>>()
    private var moveSeq = 0L

    // Nulo até o progresso salvo ser carregado (leitura rápida, sem splash).
    private val _uiState = MutableStateFlow<GameUiState?>(null)
    val uiState: StateFlow<GameUiState?> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val saved = prefs.progress.first()
            _uiState.value = newLevelState(saved.currentLevel).copy(
                bestLevelReached = saved.bestLevelReached,
                vibrationEnabled = saved.vibrationEnabled,
                soundEnabled = saved.soundEnabled,
            )
        }
    }

    fun onTubeTap(tubeId: Int) {
        val state = _uiState.value ?: return
        if (state.gameState == GameState.WON) return
        val selected = state.selectedTubeId

        when {
            selected == null -> {
                val tube = state.tubes.first { it.id == tubeId }
                if (tube.isComplete) {
                    // Tubo resolvido é travado: shake avisa que não mexe mais.
                    _events.tryEmit(GameEvent.InvalidMove(tubeId))
                } else if (!tube.isEmpty) {
                    _uiState.value = state.copy(selectedTubeId = tubeId)
                    _events.tryEmit(GameEvent.TubeSelected)
                }
            }

            selected == tubeId -> {
                _uiState.value = state.copy(selectedTubeId = null)
                _events.tryEmit(GameEvent.TubeDeselected)
            }

            else -> attemptMove(state, Move(selected, tubeId))
        }
    }

    private fun attemptMove(state: GameUiState, move: Move) {
        val result = GameRules.applyGroupMove(state.tubes, move)
        if (result == null) {
            _events.tryEmit(GameEvent.InvalidMove(move.toTubeId))
            return
        }
        val (newTubes, movedCount) = result

        undoStack.addLast(state.tubes)
        val destTube = newTubes.first { it.id == move.toTubeId }
        val movedColor = destTube.topBall!!.colorId
        val won = GameRules.isWon(newTubes)
        val newBest = if (won) max(state.bestLevelReached, state.levelNumber + 1)
        else state.bestLevelReached

        _uiState.value = state.copy(
            tubes = newTubes,
            selectedTubeId = null,
            moveCount = state.moveCount + 1,
            canUndo = true,
            gameState = if (won) GameState.WON else GameState.PLAYING,
            lastMove = MoveRecord(move, movedColor, movedCount, ++moveSeq),
            bestLevelReached = newBest,
            noMovesLeft = !won && !hasUsefulMove(newTubes),
        )

        _events.tryEmit(GameEvent.ValidMove)
        if (destTube.isComplete) _events.tryEmit(GameEvent.TubeCompleted(destTube.id))
        if (won) {
            _events.tryEmit(GameEvent.LevelWon)
            viewModelScope.launch { prefs.setBestLevelReached(newBest) }
        }
    }

    fun undo() {
        val state = _uiState.value ?: return
        val previous = undoStack.removeLastOrNull() ?: return
        _uiState.value = state.copy(
            tubes = previous,
            selectedTubeId = null,
            gameState = GameState.PLAYING,
            moveCount = state.moveCount - 1,
            canUndo = undoStack.isNotEmpty(),
            lastMove = null,
            noMovesLeft = !hasUsefulMove(previous),
        )
    }

    /**
     * Existe movimento que muda o jogo? Tubos completos são ignorados como
     * origem: mexer neles nunca ajuda, então só eles terem movimento é beco
     * sem saída do mesmo jeito.
     */
    private fun hasUsefulMove(tubes: List<Tube>): Boolean =
        tubes.any { from ->
            !from.isEmpty && !from.isComplete &&
                tubes.any { to -> GameRules.canMove(from, to) }
        }

    fun toggleVibration() {
        val state = _uiState.value ?: return
        val enabled = !state.vibrationEnabled
        _uiState.value = state.copy(vibrationEnabled = enabled)
        viewModelScope.launch { prefs.setVibrationEnabled(enabled) }
    }

    fun toggleSound() {
        val state = _uiState.value ?: return
        val enabled = !state.soundEnabled
        _uiState.value = state.copy(soundEnabled = enabled)
        viewModelScope.launch { prefs.setSoundEnabled(enabled) }
    }

    fun resetLevel(): Job? = _uiState.value?.let { loadLevel(it.levelNumber) }

    fun nextLevel(): Job? = _uiState.value?.let { loadLevel(it.levelNumber + 1) }

    /** Troca de fase preservando preferências e recorde, persistindo o nível atual. */
    fun loadLevel(levelNumber: Int): Job = viewModelScope.launch {
        val fresh = newLevelState(levelNumber)
        val state = _uiState.value ?: return@launch
        _uiState.value = fresh.copy(
            bestLevelReached = state.bestLevelReached,
            vibrationEnabled = state.vibrationEnabled,
            soundEnabled = state.soundEnabled,
        )
        prefs.setCurrentLevel(levelNumber)
    }

    /** Geração fora da main thread: o solver pode passar de 100ms em fases raras. */
    private suspend fun newLevelState(levelNumber: Int): GameUiState {
        val level = withContext(Dispatchers.Default) { LevelGenerator.generate(levelNumber) }
        undoStack.clear()
        return GameUiState(
            levelNumber = levelNumber,
            tubes = level.tubes,
            tubeCapacity = level.tubeCapacity,
        )
    }
}
