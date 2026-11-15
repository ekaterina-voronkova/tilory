package com.voronkova.tilory.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardTest {

    @Test
    fun solvedBoardIsOneGroup() {
        val b = Board.solved(3, 4)
        assertTrue(b.isSolved)
        assertEquals(12, b.largestGroupSize())
    }

    @Test
    fun noGlueAcrossRowEnds() {
        // Tiles 3 and 4 are consecutive ids but 3 ends row 0 and 4 starts row 1.
        val b = Board(2, 4, listOf(0, 1, 3, 4, 2, 5, 6, 7))
        assertFalse(b.gluedRight(2))
    }

    @Test
    fun shuffledHasNoGluedPairs() {
        repeat(50) { seed ->
            val b = Board.shuffled(4, 5, Random(seed))
            assertEquals(0, b.gluedPairs())
        }
    }

    @Test
    fun singleTileDropIsSwap() {
        val b = Board(2, 2, listOf(3, 2, 1, 0))
        val next = b.move(0, 1, 1)!!
        assertEquals(listOf(0, 2, 1, 3), next.tiles)
    }

    @Test
    fun noMoveReturnsNull() {
        val b = Board(2, 2, listOf(3, 2, 1, 0))
        assertNull(b.move(0, 0, 0))
    }

    @Test
    fun groupDroppedOffEdgeIsShiftedInwards() {
        // Tiles 0,1 glued on cells 0,1 of a 3x3 board; drag two columns right: only one fits.
        val b = Board(3, 3, listOf(0, 1, 5, 8, 3, 6, 4, 2, 7))
        assertEquals(listOf(0, 1), b.groupOf(0))
        val next = b.move(0, 0, 2)!!
        assertEquals(0, next.tiles[1])
        assertEquals(1, next.tiles[2])
        assertEquals(5, next.tiles[0]) // displaced tile walks back into the freed cell
    }

    @Test
    fun dropCanBreakAnotherGroup() {
        // Row 0 of a 3x3: tiles 4,5 glued on cells 1,2. Tile 0 dropped on cell 1 breaks them apart.
        val b = Board(3, 3, listOf(0, 4, 5, 6, 8, 7, 3, 2, 1))
        assertEquals(listOf(1, 2), b.groupOf(1))
        val next = b.move(0, 0, 1)!!
        assertEquals(listOf(4, 0, 5, 6, 8, 7, 3, 2, 1), next.tiles)
        assertEquals(listOf(1), next.groupOf(1))
    }

    @Test
    fun movesAlwaysKeepPermutation() {
        val random = Random(7)
        var b = Board.shuffled(5, 6, random)
        repeat(2_000) {
            val cell = random.nextInt(b.size)
            val moved = b.move(cell, random.nextInt(-5, 6), random.nextInt(-6, 7))
            if (moved != null) b = moved // Board's init checks the permutation
        }
    }

    @Test
    fun draggedGroupStaysWhole() {
        val random = Random(11)
        var b = Board.shuffled(4, 4, random)
        repeat(1_000) {
            val cell = random.nextInt(b.size)
            val group = b.groupOf(cell)
            val (dr, dc) = b.clampShift(group, random.nextInt(-3, 4), random.nextInt(-3, 4))
            val moved = b.move(cell, dr, dc) ?: return@repeat
            val landed = b.cell(b.row(cell) + dr, b.col(cell) + dc)
            assertTrue(moved.groupOf(landed).size >= group.size)
            b = moved
        }
    }

    @Test
    fun gridChooserKeepsTilesNearSquare() {
        assertEquals(Grid(6, 8), GridChooser.choose(48, 4f / 3f, 20, 20))
        assertEquals(Grid(8, 6), GridChooser.choose(48, 3f / 4f, 20, 20))
        assertEquals(Grid(3, 3), GridChooser.choose(4, 1f, 20, 20))
    }

    @Test
    fun gridChooserRespectsLimits() {
        val g = GridChooser.choose(200, 1f, 9, 9)
        assertTrue(g.rows <= 9 && g.cols <= 9)
    }
}
