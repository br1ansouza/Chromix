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
        // Tubo resolvido fica travado: desmontá-lo nunca ajuda.
        if (from.isComplete) return false
        val ball = from.topBall ?: return false
        if (to.isFull) return false
        val target = to.topBall ?: return true
        return target.colorId == ball.colorId
    }

    /**
     * Aplica o movimento de UMA bolinha e retorna a nova lista de tubos, ou
     * null se inválido. Base das provas de solvabilidade (a solução gravada
     * pelo gerador é em movimentos unitários).
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

    /**
     * Quantas bolinhas o movimento em grupo carrega: a sequência de mesma cor
     * no topo da origem, limitada ao espaço livre do destino. Zero se inválido.
     */
    fun groupMoveSize(from: Tube, to: Tube): Int {
        if (!canMove(from, to)) return 0
        val topColor = from.topBall!!.colorId
        val run = from.balls.reversed().takeWhile { it.colorId == topColor }.count()
        return minOf(run, to.capacity - to.balls.size)
    }

    /**
     * Movimento em grupo: move toda a sequência de mesma cor do topo que
     * couber no destino (um movimento unitário repetido). Retorna a nova
     * lista de tubos e a quantidade movida, ou null se inválido.
     */
    fun applyGroupMove(tubes: List<Tube>, move: Move): Pair<List<Tube>, Int>? {
        val from = tubes.firstOrNull { it.id == move.fromTubeId } ?: return null
        val to = tubes.firstOrNull { it.id == move.toTubeId } ?: return null
        val count = groupMoveSize(from, to)
        if (count == 0) return null

        var result = tubes
        repeat(count) { result = applyMove(result, move)!! }
        return result to count
    }

    /** Vitória: todo tubo vazio ou cheio com uma única cor. */
    fun isWon(tubes: List<Tube>): Boolean =
        tubes.all { it.isEmpty || it.isComplete }
}
