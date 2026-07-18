package com.br1ansouza.chromix.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LevelGeneratorTest {

    @Test
    fun `generation is deterministic per level number`() {
        for (level in listOf(1, 7, 15, 30, 50)) {
            assertEquals(LevelGenerator.generate(level), LevelGenerator.generate(level))
        }
    }

    @Test
    fun `difficulty curve follows spec`() {
        val rng = Random(0)
        with(LevelGenerator.paramsFor(1, rng)) {
            assertEquals(4, colorCount)
            assertEquals(2, emptyTubes)
            assertEquals(4, tubeCapacity)
        }
        with(LevelGenerator.paramsFor(16, rng)) {
            assertEquals(6, colorCount)
            assertEquals(2, emptyTubes)
        }
        with(LevelGenerator.paramsFor(30, rng)) {
            assertEquals(9, colorCount)
            assertEquals(2, emptyTubes)
            assertTrue(tubeCapacity in 4..5)
        }
        with(LevelGenerator.paramsFor(60, rng)) {
            assertEquals(12, colorCount)
            assertEquals(2, emptyTubes)
            assertTrue(tubeCapacity in 5..6)
        }
        // Viés de agrupamento cai com o nível até a mistura virar total.
        val bias1 = LevelGenerator.paramsFor(1, rng).runBias
        val bias20 = LevelGenerator.paramsFor(20, rng).runBias
        val bias40 = LevelGenerator.paramsFor(40, rng).runBias
        assertTrue(bias1 > bias20)
        assertTrue(bias20 > bias40)
        assertEquals(0.0, bias40, 1e-9)
    }

    @Test
    fun `generated level has expected structure`() {
        for (levelNumber in 1..40) {
            val level = LevelGenerator.generate(levelNumber)
            assertEquals(level.colorCount + level.emptyTubes, level.tubes.size)

            val ballsByColor = level.tubes.flatMap { it.balls }.groupBy { it.colorId }
            assertEquals(level.colorCount, ballsByColor.size)
            ballsByColor.values.forEach { balls ->
                assertEquals(level.tubeCapacity, balls.size)
            }
            level.tubes.forEach { assertTrue(it.balls.size <= it.capacity) }
        }
    }

    @Test
    fun `generated level is not already solved or trivial`() {
        for (levelNumber in 1..40) {
            val level = LevelGenerator.generate(levelNumber)
            assertFalse(GameRules.isWon(level.tubes))
            assertFalse(level.tubes.any { !it.isEmpty && it.isComplete })
        }
    }

    @Test
    fun `no tube starts with a monochrome bottom of 3 or more`() {
        for (levelNumber in 1..60) {
            val level = LevelGenerator.generate(levelNumber)
            level.tubes.forEach { tube ->
                if (tube.balls.size >= 3) {
                    assertFalse(
                        "level $levelNumber: tube ${tube.id} starts half-solved",
                        tube.balls.take(3).all { it.colorId == tube.balls[0].colorId }
                    )
                }
            }
        }
    }

    @Test
    fun `boards respect the per-level mixing floor`() {
        for (levelNumber in 1..60) {
            val level = LevelGenerator.generate(levelNumber)
            val floor = LevelGenerator.paramsFor(levelNumber, Random(0)).minTransitionRatio
            val transitions = level.tubes.sumOf { tube ->
                tube.balls.zipWithNext().count { (a, b) -> a.colorId != b.colorId }
            }
            val maxTransitions = level.tubes.sumOf { (it.balls.size - 1).coerceAtLeast(0) }
            assertTrue(
                "level $levelNumber: mix ratio ${transitions.toDouble() / maxTransitions}",
                transitions >= maxTransitions * floor
            )
        }
    }

    @Test
    fun `recorded solution solves every generated level with player move semantics`() {
        for (levelNumber in 1..60) {
            val (level, solution) = LevelGenerator.generateWithSolution(levelNumber)
            var tubes = level.tubes
            solution.forEachIndexed { index, move ->
                val next = GameRules.applyGroupMove(tubes, move)
                assertNotNull(
                    "level $levelNumber: invalid solution move #$index ($move)",
                    next
                )
                tubes = next!!.first
            }
            assertTrue("level $levelNumber: solution did not win", GameRules.isWon(tubes))
        }
    }
}
