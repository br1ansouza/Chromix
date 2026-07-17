package com.br1ansouza.chromix.domain

import kotlin.math.min
import kotlin.random.Random

/**
 * Geração procedural de fases, determinística por [Level.levelNumber] e
 * solucionável por construção.
 *
 * Estratégia: parte do estado resolvido e aplica "movimentos inversos". Um
 * movimento inverso remove a bolinha do topo de um tubo — desde que ela esteja
 * sobre outra da mesma cor ou seja a bolinha do fundo — e a coloca em qualquer
 * outro tubo não cheio. Cada movimento inverso corresponde exatamente a um
 * movimento normal do jogo no sentido contrário, então reverter a sequência
 * leva de volta ao estado resolvido: a fase sempre tem solução.
 *
 * (Embaralhar com movimentos *normais* não funcionaria: eles só empilham
 * bolinha sobre a mesma cor ou tubo vazio, logo os tubos permaneceriam
 * monocromáticos e a fase nasceria trivial.)
 */
object LevelGenerator {

    const val MAX_COLORS = 12
    private const val MAX_SHUFFLE_MOVES = 300

    data class LevelParams(
        val colorCount: Int,
        val emptyTubes: Int,
        val tubeCapacity: Int,
        val shuffleMoves: Int,
    )

    data class GeneratedLevel(
        val level: Level,
        /** Sequência de movimentos que resolve a fase, na ordem de execução. */
        val solution: List<Move>,
    )

    fun paramsFor(levelNumber: Int, rng: Random): LevelParams {
        val colorCount = min(4 + levelNumber / 3, MAX_COLORS)
        val emptyTubes = if (levelNumber < 15) 2 else 1
        val tubeCapacity = when {
            levelNumber < 21 -> 4
            levelNumber < 41 -> 4 + rng.nextInt(2)  // 4–5
            else -> 5 + rng.nextInt(2)              // 5–6
        }
        val shuffleMoves = min(40 + levelNumber * 3, MAX_SHUFFLE_MOVES)
        return LevelParams(colorCount, emptyTubes, tubeCapacity, shuffleMoves)
    }

    fun generate(levelNumber: Int): Level = generateWithSolution(levelNumber).level

    fun generateWithSolution(levelNumber: Int): GeneratedLevel {
        var attempt = 0
        while (true) {
            val rng = Random(levelNumber.toLong() * 1_000 + attempt)
            val candidate = tryGenerate(levelNumber, rng)
            if (candidate != null && !isTrivial(candidate.level)) return candidate
            attempt++
        }
    }

    private fun tryGenerate(levelNumber: Int, rng: Random): GeneratedLevel? {
        val params = paramsFor(levelNumber, rng)

        var tubes = buildList {
            repeat(params.colorCount) { color ->
                add(Tube(id = size, capacity = params.tubeCapacity,
                    balls = List(params.tubeCapacity) { Ball(color) }))
            }
            repeat(params.emptyTubes) {
                add(Tube(id = size, capacity = params.tubeCapacity))
            }
        }

        val reverseTrace = mutableListOf<Move>()
        var lastMove: Move? = null

        repeat(params.shuffleMoves) {
            val candidates = buildList {
                for (source in tubes) {
                    if (!canRemoveInReverse(source)) continue
                    for (dest in tubes) {
                        if (dest.id == source.id || dest.isFull) continue
                        // Evita desfazer imediatamente o movimento anterior.
                        if (lastMove?.let { it.fromTubeId == dest.id && it.toTubeId == source.id } == true) continue
                        add(Move(source.id, dest.id))
                    }
                }
            }
            if (candidates.isEmpty()) return@repeat

            val move = candidates[rng.nextInt(candidates.size)]
            val (newSource, ball) = tubes.first { it.id == move.fromTubeId }.pop()
            val newDest = tubes.first { it.id == move.toTubeId }.push(ball)
            tubes = tubes.map {
                when (it.id) {
                    newSource.id -> newSource
                    newDest.id -> newDest
                    else -> it
                }
            }
            lastMove = move
            reverseTrace += move
        }

        if (reverseTrace.isEmpty()) return null

        // Solução: reverte a ordem e o sentido de cada movimento inverso.
        val solution = reverseTrace.reversed().map { Move(it.toTubeId, it.fromTubeId) }

        val level = Level(
            levelNumber = levelNumber,
            tubes = tubes,
            colorCount = params.colorCount,
            emptyTubes = params.emptyTubes,
            tubeCapacity = params.tubeCapacity,
        )
        return GeneratedLevel(level, solution)
    }

    /**
     * A bolinha do topo só pode ser removida no embaralhamento inverso se estiver
     * sobre outra da mesma cor ou for a bolinha do fundo — condição necessária
     * para que o movimento normal correspondente (recolocá-la) seja válido.
     */
    private fun canRemoveInReverse(tube: Tube): Boolean {
        val size = tube.balls.size
        if (size == 0) return false
        if (size == 1) return true
        return tube.balls[size - 1].colorId == tube.balls[size - 2].colorId
    }

    /**
     * Rejeita embaralhamentos que por acaso ficaram fáceis demais: fase já
     * vencida, algum tubo completo, ou pouca mistura de cores.
     */
    private fun isTrivial(level: Level): Boolean {
        if (GameRules.isWon(level.tubes)) return true
        if (level.tubes.any { !it.isEmpty && it.isComplete }) return true

        val colorTransitions = level.tubes.sumOf { tube ->
            tube.balls.zipWithNext().count { (a, b) -> a.colorId != b.colorId }
        }
        return colorTransitions < level.colorCount
    }
}
