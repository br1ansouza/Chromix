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
        with(LevelGenerator.paramsFor(14, rng)) {
            assertEquals(2, emptyTubes)
        }
        with(LevelGenerator.paramsFor(15, rng)) {
            assertEquals(9, colorCount)
            assertEquals(1, emptyTubes)
        }
        with(LevelGenerator.paramsFor(30, rng)) {
            assertTrue(tubeCapacity in 4..5)
        }
        with(LevelGenerator.paramsFor(60, rng)) {
            assertEquals(12, colorCount)
            assertTrue(tubeCapacity in 5..6)
        }
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
    fun `recorded solution solves every generated level`() {
        for (levelNumber in 1..60) {
            val (level, solution) = LevelGenerator.generateWithSolution(levelNumber)
            var tubes = level.tubes
            solution.forEachIndexed { index, move ->
                val next = GameRules.applyMove(tubes, move)
                assertNotNull(
                    "level $levelNumber: invalid solution move #$index ($move)",
                    next
                )
                tubes = next!!
            }
            assertTrue("level $levelNumber: solution did not win", GameRules.isWon(tubes))
        }
    }
}
