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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    /** Último movimento aplicado, com sequência para disparar a animação de voo. */
    data class MoveRecord(val move: Move, val colorId: Int, val seq: Long)

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
    )

    sealed interface GameEvent {
        data object TubeSelected : GameEvent
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
                if (!tube.isEmpty) {
                    _uiState.value = state.copy(selectedTubeId = tubeId)
                    _events.tryEmit(GameEvent.TubeSelected)
                }
            }

            selected == tubeId -> _uiState.value = state.copy(selectedTubeId = null)

            else -> attemptMove(state, Move(selected, tubeId))
        }
    }

    private fun attemptMove(state: GameUiState, move: Move) {
        val newTubes = GameRules.applyMove(state.tubes, move)
        if (newTubes == null) {
            _events.tryEmit(GameEvent.InvalidMove(move.toTubeId))
            return
        }

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
            lastMove = MoveRecord(move, movedColor, ++moveSeq),
            bestLevelReached = newBest,
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
        )
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

    fun resetLevel() {
        val state = _uiState.value ?: return
        loadLevel(state.levelNumber)
    }

    fun nextLevel() {
        val state = _uiState.value ?: return
        loadLevel(state.levelNumber + 1)
    }

    /** Troca de fase preservando preferências e recorde, persistindo o nível atual. */
    fun loadLevel(levelNumber: Int) {
        val state = _uiState.value ?: return
        _uiState.value = newLevelState(levelNumber).copy(
            bestLevelReached = state.bestLevelReached,
            vibrationEnabled = state.vibrationEnabled,
            soundEnabled = state.soundEnabled,
        )
        viewModelScope.launch { prefs.setCurrentLevel(levelNumber) }
    }

    private fun newLevelState(levelNumber: Int): GameUiState {
        val level = LevelGenerator.generate(levelNumber)
        undoStack.clear()
        return GameUiState(
            levelNumber = levelNumber,
            tubes = level.tubes,
            tubeCapacity = level.tubeCapacity,
        )
    }
}
