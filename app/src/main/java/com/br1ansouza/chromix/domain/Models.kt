package com.br1ansouza.chromix.domain

/**
 * Modelos do domínio do jogo. Puro Kotlin, sem dependência do framework Android,
 * para permitir testes unitários na JVM.
 *
 * Estruturas imutáveis: cada movimento produz novas instâncias, o que simplifica
 * undo (pilha de estados) e integra bem com o state do Compose.
 */

data class Ball(val colorId: Int)

data class Tube(
    val id: Int,
    val capacity: Int = 4,
    val balls: List<Ball> = emptyList(), // index 0 = fundo, last = topo
) {
    val topBall: Ball? get() = balls.lastOrNull()
    val isEmpty: Boolean get() = balls.isEmpty()
    val isFull: Boolean get() = balls.size >= capacity

    /** Cheio e com uma cor só — tubo "resolvido". */
    val isComplete: Boolean
        get() = isFull && balls.all { it.colorId == balls.first().colorId }

    fun push(ball: Ball): Tube = copy(balls = balls + ball)

    fun pop(): Pair<Tube, Ball> {
        val ball = balls.last()
        return copy(balls = balls.dropLast(1)) to ball
    }
}

data class Level(
    val levelNumber: Int,
    val tubes: List<Tube>,
    val colorCount: Int,
    val emptyTubes: Int,
    val tubeCapacity: Int,
)

data class Move(val fromTubeId: Int, val toTubeId: Int)

enum class GameState { PLAYING, WON, ANIMATING }
