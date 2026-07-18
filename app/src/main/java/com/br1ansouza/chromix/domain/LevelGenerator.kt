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

    data class LevelParams(
        val colorCount: Int,
        val emptyTubes: Int,
        val tubeCapacity: Int,
        /**
         * Probabilidade de estender uma sequência de mesma cor ao montar o
         * embaralhamento (teto de 3 seguidas). Alto nos níveis baixos = grupos
         * prontos = fase amigável; cai a zero por volta do nível 40, quando a
         * mistura vira total. É o knob contínuo da dificuldade entre os saltos
         * de cor/capacidade.
         */
        val runBias: Double,
        /**
         * Piso de mistura (transições de cor adjacentes / máximo possível):
         * descarta embaralhamentos degenerados. Cresce com o nível junto com a
         * queda do [runBias]; fica sempre folgado abaixo da mistura esperada
         * para o viés do nível, senão a regeneração entraria em loop.
         */
        val minTransitionRatio: Double,
    )

    data class GeneratedLevel(
        val level: Level,
        /** Sequência de movimentos EM GRUPO que resolve a fase, na ordem de execução. */
        val solution: List<Move>,
    )

    fun paramsFor(levelNumber: Int, rng: Random): LevelParams {
        val colorCount = min(4 + levelNumber / 6, MAX_COLORS)
        val tubeCapacity = when {
            levelNumber < 21 -> 4
            levelNumber < 41 -> 4 + rng.nextInt(2)  // 4–5
            else -> 5 + rng.nextInt(2)              // 5–6
        }
        val runBias = (0.6 - levelNumber * 0.015).coerceAtLeast(0.0)
        val minTransitionRatio = min(0.35 + levelNumber * 0.01, 0.6)
        return LevelParams(colorCount, EMPTY_TUBES, tubeCapacity, runBias, minTransitionRatio)
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

        val balls = shuffleWithRunBias(params, rng)

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
     * Sequência embaralhada com viés de sequência: sorteia uma cor entre as
     * restantes e, com probabilidade [LevelParams.runBias], estende a mesma cor
     * (até 3 seguidas) antes de sortear de novo. Com viés zero equivale a um
     * embaralhamento uniforme.
     */
    private fun shuffleWithRunBias(params: LevelParams, rng: Random): List<Ball> {
        val remaining = IntArray(params.colorCount) { params.tubeCapacity }
        var left = params.colorCount * params.tubeCapacity
        val sequence = ArrayList<Ball>(left)
        while (left > 0) {
            // Sorteio ponderado pelas bolinhas restantes de cada cor.
            var pick = rng.nextInt(left)
            var color = 0
            while (pick >= remaining[color]) {
                pick -= remaining[color]
                color++
            }
            var run = 1
            while (run < 3 && remaining[color] > run && rng.nextDouble() < params.runBias) run++
            repeat(run) { sequence.add(Ball(color)) }
            remaining[color] -= run
            left -= run
        }
        return sequence
    }

    /**
     * Rejeita embaralhamentos que por acaso ficaram fáceis demais: fase já
     * vencida, tubo já completo, fundo monocromático de 3+ bolinhas (tubo
     * "meio pronto") ou mistura abaixo do piso do nível.
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
        return transitions < maxTransitions * params.minTransitionRatio
    }
}
