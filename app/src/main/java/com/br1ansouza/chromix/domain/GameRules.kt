package com.br1ansouza.chromix.domain

/**
 * Regras de movimento e condição de vitória.
 *
 * Movimento válido: bolinha do topo da origem vai para o topo do destino se o
 * destino estiver vazio ou com bolinha do topo da mesma cor, e não estiver cheio.
 */
object GameRules {

    fun canMove(from: Tube, to: Tube): Boolean {
        if (from.id == to.id) return false
        val ball = from.topBall ?: return false
        if (to.isFull) return false
        val target = to.topBall ?: return true
        return target.colorId == ball.colorId
    }

    /**
     * Aplica o movimento e retorna a nova lista de tubos, ou null se inválido.
     */
    fun applyMove(tubes: List<Tube>, move: Move): List<Tube>? {
        val from = tubes.firstOrNull { it.id == move.fromTubeId } ?: return null
        val to = tubes.firstOrNull { it.id == move.toTubeId } ?: return null
        if (!canMove(from, to)) return null

        val (newFrom, ball) = from.pop()
        val newTo = to.push(ball)
        return tubes.map {
            when (it.id) {
                newFrom.id -> newFrom
                newTo.id -> newTo
                else -> it
            }
        }
    }

    /** Vitória: todo tubo vazio ou cheio com uma única cor. */
    fun isWon(tubes: List<Tube>): Boolean =
        tubes.all { it.isEmpty || it.isComplete }
}
