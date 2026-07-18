package com.br1ansouza.chromix.domain

/**
 * Solver de Ball Sort com a MESMA semântica de movimento do jogador: cada
 * movimento carrega a sequência inteira de mesma cor do topo que couber no
 * destino ([GameRules.applyGroupMove]). Provar solvabilidade com movimentos
 * unitários não basta — o jogador não consegue dividir um grupo, então uma
 * fase "solvável" em teoria poderia ser inalcançável na prática.
 *
 * Busca em profundidade iterativa com memoização por estado canônico (tubos
 * ordenados — permutar tubos não muda o puzzle) e orçamento de expansões para
 * limitar o pior caso. DFS não devolve a solução mínima, apenas uma válida.
 */
object Solver {

    /** Resultado nulo = sem solução dentro do orçamento (ou de fato impossível). */
    fun solve(tubes: List<Tube>, maxExpansions: Int = 200_000): List<Move>? {
        if (GameRules.isWon(tubes)) return emptyList()

        val visited = HashSet<String>()
        // Pilha de (estado, movimentos candidatos ainda não tentados, movimento que gerou o estado).
        class Frame(val state: List<Tube>, val moves: Iterator<Move>, val via: Move?)

        val stack = ArrayDeque<Frame>()
        stack.addLast(Frame(tubes, candidateMoves(tubes).iterator(), null))
        visited.add(canonical(tubes))
        var expansions = 0

        while (stack.isNotEmpty()) {
            val frame = stack.last()
            if (!frame.moves.hasNext()) {
                stack.removeLast()
                continue
            }
            val move = frame.moves.next()
            val next = GameRules.applyGroupMove(frame.state, move)?.first ?: continue
            if (GameRules.isWon(next)) {
                return stack.mapNotNull { it.via } + move
            }
            if (!visited.add(canonical(next))) continue
            if (++expansions > maxExpansions) return null
            stack.addLast(Frame(next, candidateMoves(next).iterator(), move))
        }
        return null
    }

    /**
     * Movimentos que valem a pena explorar. Poda:
     * - origem vazia ou já completa;
     * - tubo monocromático despejado num vazio (estado equivalente por simetria);
     * - destinos vazios por último (empilhar sobre a mesma cor tende a resolver antes).
     */
    private fun candidateMoves(tubes: List<Tube>): List<Move> {
        val toSameColor = mutableListOf<Move>()
        val toEmpty = mutableListOf<Move>()
        for (from in tubes) {
            if (from.isEmpty || from.isComplete) continue
            val mono = from.balls.all { it.colorId == from.balls[0].colorId }
            var emptyUsed = false
            for (to in tubes) {
                if (to.id == from.id || !GameRules.canMove(from, to)) continue
                if (to.isEmpty) {
                    // Todos os tubos vazios são intercambiáveis: basta tentar um.
                    if (mono || emptyUsed) continue
                    emptyUsed = true
                    toEmpty += Move(from.id, to.id)
                } else {
                    toSameColor += Move(from.id, to.id)
                }
            }
        }
        return toSameColor + toEmpty
    }

    /** Chave canônica: conteúdo dos tubos, ordenado — identifica estados equivalentes. */
    private fun canonical(tubes: List<Tube>): String =
        tubes.map { tube -> tube.balls.joinToString(",") { it.colorId.toString() } }
            .sorted()
            .joinToString("|")
}
