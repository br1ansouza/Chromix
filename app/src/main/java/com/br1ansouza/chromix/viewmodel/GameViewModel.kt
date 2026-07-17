package com.br1ansouza.chromix.viewmodel

import androidx.lifecycle.ViewModel
import com.br1ansouza.chromix.domain.GameRules
import com.br1ansouza.chromix.domain.GameState
import com.br1ansouza.chromix.domain.LevelGenerator
import com.br1ansouza.chromix.domain.Move
import com.br1ansouza.chromix.domain.Tube
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel : ViewModel() {

    /** Último movimento aplicado, com sequência para disparar a animação de voo. */
    data class MoveRecord(val move: Move, val colorId: Int, val seq: Long)

    data class GameUiState(
        val levelNumber: Int,
        val tubes: List<Tube>,
        val tubeCapacity: Int,
        val selectedTubeId: Int? = null,
        val gameState: GameState = GameState.PLAYING,
        val moveCount: Int = 0,
        val canUndo: Boolean = false,
        val lastMove: MoveRecord? = null,
    )

    sealed interface GameEvent {
        data object ValidMove : GameEvent
        data class InvalidMove(val tubeId: Int) : GameEvent
        data class TubeCompleted(val tubeId: Int) : GameEvent
        data object LevelWon : GameEvent
    }

    private val undoStack = ArrayDeque<List<Tube>>()
    private var moveSeq = 0L

    private val _uiState = MutableStateFlow(newLevelState(1))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    fun onTubeTap(tubeId: Int) {
        val state = _uiState.value
        if (state.gameState == GameState.WON) return
        val selected = state.selectedTubeId

        when {
            selected == null -> {
                val tube = state.tubes.first { it.id == tubeId }
                if (!tube.isEmpty) {
                    _uiState.value = state.copy(selectedTubeId = tubeId)
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
        val movedColor = newTubes.first { it.id == move.toTubeId }.topBall!!.colorId
        val won = GameRules.isWon(newTubes)
        val destTube = newTubes.first { it.id == move.toTubeId }

        _uiState.value = state.copy(
            tubes = newTubes,
            selectedTubeId = null,
            moveCount = state.moveCount + 1,
            canUndo = true,
            gameState = if (won) GameState.WON else GameState.PLAYING,
            lastMove = MoveRecord(move, movedColor, ++moveSeq),
        )

        _events.tryEmit(GameEvent.ValidMove)
        if (destTube.isComplete) _events.tryEmit(GameEvent.TubeCompleted(destTube.id))
        if (won) _events.tryEmit(GameEvent.LevelWon)
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        _uiState.value = _uiState.value.copy(
            tubes = previous,
            selectedTubeId = null,
            gameState = GameState.PLAYING,
            moveCount = _uiState.value.moveCount - 1,
            canUndo = undoStack.isNotEmpty(),
            lastMove = null,
        )
    }

    fun resetLevel() {
        _uiState.value = newLevelState(_uiState.value.levelNumber)
    }

    fun nextLevel() {
        _uiState.value = newLevelState(_uiState.value.levelNumber + 1)
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
