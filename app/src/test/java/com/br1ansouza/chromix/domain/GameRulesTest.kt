package com.br1ansouza.chromix.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRulesTest {

    private fun tube(id: Int, vararg colors: Int, capacity: Int = 4) =
        Tube(id = id, capacity = capacity, balls = colors.map { Ball(it) })

    @Test
    fun `move to empty tube is valid`() {
        assertTrue(GameRules.canMove(tube(0, 1, 2), tube(1)))
    }

    @Test
    fun `move onto same color is valid`() {
        assertTrue(GameRules.canMove(tube(0, 1, 2), tube(1, 3, 2)))
    }

    @Test
    fun `move onto different color is invalid`() {
        assertFalse(GameRules.canMove(tube(0, 1, 2), tube(1, 3, 3)))
    }

    @Test
    fun `move to full tube is invalid`() {
        assertFalse(GameRules.canMove(tube(0, 2), tube(1, 2, 2, 2, 2)))
    }

    @Test
    fun `move from empty tube is invalid`() {
        assertFalse(GameRules.canMove(tube(0), tube(1, 2)))
    }

    @Test
    fun `move to same tube is invalid`() {
        assertFalse(GameRules.canMove(tube(0, 1), tube(0, 1)))
    }

    @Test
    fun `applyMove moves top ball between tubes`() {
        val tubes = listOf(tube(0, 1, 2), tube(1, 2))
        val result = GameRules.applyMove(tubes, Move(0, 1))
        assertNotNull(result)
        assertEquals(listOf(Ball(1)), result!!.first { it.id == 0 }.balls)
        assertEquals(listOf(Ball(2), Ball(2)), result.first { it.id == 1 }.balls)
    }

    @Test
    fun `applyMove returns null for invalid move`() {
        val tubes = listOf(tube(0, 1), tube(1, 2))
        assertNull(GameRules.applyMove(tubes, Move(0, 1)))
    }

    @Test
    fun `group move carries the whole same-color run`() {
        val tubes = listOf(tube(0, 1, 2, 2), tube(1, 2))
        val result = GameRules.applyGroupMove(tubes, Move(0, 1))
        assertNotNull(result)
        val (newTubes, count) = result!!
        assertEquals(2, count)
        assertEquals(listOf(Ball(1)), newTubes.first { it.id == 0 }.balls)
        assertEquals(
            listOf(Ball(2), Ball(2), Ball(2)),
            newTubes.first { it.id == 1 }.balls,
        )
    }

    @Test
    fun `group move is capped by destination space`() {
        val tubes = listOf(tube(0, 2, 2, 2), tube(1, 2, 2, 2))
        val result = GameRules.applyGroupMove(tubes, Move(0, 1))
        assertNotNull(result)
        val (newTubes, count) = result!!
        assertEquals(1, count)
        assertEquals(2, newTubes.first { it.id == 0 }.balls.size)
        assertEquals(4, newTubes.first { it.id == 1 }.balls.size)
    }

    @Test
    fun `group move to empty tube carries the run`() {
        val tubes = listOf(tube(0, 1, 3, 3, 3), tube(1))
        val (newTubes, count) = GameRules.applyGroupMove(tubes, Move(0, 1))!!
        assertEquals(3, count)
        assertEquals(listOf(Ball(1)), newTubes.first { it.id == 0 }.balls)
    }

    @Test
    fun `group move returns null when invalid`() {
        val tubes = listOf(tube(0, 1), tube(1, 2))
        assertNull(GameRules.applyGroupMove(tubes, Move(0, 1)))
    }

    @Test
    fun `won when every tube is empty or complete`() {
        val tubes = listOf(tube(0, 1, 1, 1, 1), tube(1, 2, 2, 2, 2), tube(2))
        assertTrue(GameRules.isWon(tubes))
    }

    @Test
    fun `not won with mixed tube`() {
        val tubes = listOf(tube(0, 1, 1, 1, 2), tube(1, 2, 2, 2, 1), tube(2))
        assertFalse(GameRules.isWon(tubes))
    }

    @Test
    fun `not won with incomplete uniform tube`() {
        val tubes = listOf(tube(0, 1, 1, 1), tube(1, 1), tube(2))
        assertFalse(GameRules.isWon(tubes))
    }
}
