package com.br1ansouza.chromix.domain

import kotlin.math.min
import kotlin.random.Random

/**
 * Geração procedural de fases, determinística por [Level.levelNumber] e
 * solucionável por construção.
 *
 * Estratégia: distribui TODAS as bolinhas aleatoriamente pelos tubos de cor
 * (mistura máxima) e valida com o [Solver] — que usa a mesma semântica de
 * movimento em grupo do jogador. Tabuleiros insolúveis ou fáceis demais são
 * rejeitados e regenerados com a próxima seed derivada (determinístico:
 * `Random(level * 1000 + attempt)`).
 *
 * (A estratégia anterior — embaralhar o estado resolvido com movimentos
 * inversos — tinha um vício estrutural: o movimento inverso só pode remover a
 * bolinha do topo se ela estiver sobre a mesma cor, então os fundos dos tubos
 * nunca eram desenterrados e as fases nasciam com a metade de baixo já
 * resolvida.)
 *
 * Dois tubos vazios sempre: com tabuleiro bem misturado, um único tubo vazio
 * torna a fase quase sempre impossível (medido: 0/30 solváveis). A dificuldade
 * vem da contagem de cores, da capacidade e da mistura total.
 */
object LevelGenerator {

    const val MAX_COLORS = 12
    private const val EMPTY_TUBES = 2

    /**
     * Piso de mistura: fração mínima de transições de cor adjacentes sobre o
     * máximo possível. Distribuição aleatória fica tipicamente acima de 85%;
     * o piso descarta os raros embaralhamentos degenerados.
     */
    private const val MIN_TRANSITION_RATIO = 0.6

    data class LevelParams(
        val colorCount: Int,
        val emptyTubes: Int,
        val tubeCapacity: Int,
    )

    data class GeneratedLevel(
        val level: Level,
        /** Sequência de movimentos EM GRUPO que resolve a fase, na ordem de execução. */
        val solution: List<Move>,
    )

    fun paramsFor(levelNumber: Int, rng: Random): LevelParams {
        val colorCount = min(4 + levelNumber / 3, MAX_COLORS)
        val tubeCapacity = when {
            levelNumber < 21 -> 4
            levelNumber < 41 -> 4 + rng.nextInt(2)  // 4–5
            else -> 5 + rng.nextInt(2)              // 5–6
        }
        return LevelParams(colorCount, EMPTY_TUBES, tubeCapacity)
    }

    fun generate(levelNumber: Int): Level = generateWithSolution(levelNumber).level

    fun generateWithSolution(levelNumber: Int): GeneratedLevel {
        var attempt = 0
        while (true) {
            val rng = Random(levelNumber.toLong() * 1_000 + attempt)
            val candidate = tryGenerate(levelNumber, rng)
            if (candidate != null) return candidate
            attempt++
        }
    }

    private fun tryGenerate(levelNumber: Int, rng: Random): GeneratedLevel? {
        val params = paramsFor(levelNumber, rng)

        val balls = buildList {
            repeat(params.colorCount) { color ->
                repeat(params.tubeCapacity) { add(Ball(color)) }
            }
        }.shuffled(rng)

        val tubes = buildList {
            var next = 0
            repeat(params.colorCount) {
                add(Tube(id = size, capacity = params.tubeCapacity,
                    balls = balls.subList(next, next + params.tubeCapacity)))
                next += params.tubeCapacity
            }
            repeat(params.emptyTubes) {
                add(Tube(id = size, capacity = params.tubeCapacity))
            }
        }

        if (isTrivial(tubes, params)) return null
        val solution = Solver.solve(tubes) ?: return null

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
     * Rejeita embaralhamentos que por acaso ficaram fáceis demais: fase já
     * vencida, tubo já completo, fundo monocromático de 3+ bolinhas (tubo
     * "meio pronto") ou pouca mistura no total.
     */
    private fun isTrivial(tubes: List<Tube>, params: LevelParams): Boolean {
        if (GameRules.isWon(tubes)) return true
        if (tubes.any { !it.isEmpty && it.isComplete }) return true
        if (tubes.any { tube ->
                tube.balls.size >= 3 && tube.balls.take(3).all { it.colorId == tube.balls[0].colorId }
            }) return true

        val transitions = tubes.sumOf { tube ->
            tube.balls.zipWithNext().count { (a, b) -> a.colorId != b.colorId }
        }
        val maxTransitions = tubes.sumOf { (it.balls.size - 1).coerceAtLeast(0) }
        return transitions < maxTransitions * MIN_TRANSITION_RATIO
    }
}
