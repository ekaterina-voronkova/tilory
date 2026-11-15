package com.voronkova.tilory.game

import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

/**
 * The puzzle board: a rows × cols grid with exactly one tile per cell.
 *
 * Tile ids are their correct cell index (row * cols + col) in the original picture. [tiles] holds the tile id that
 * currently lies on each cell. Boards are immutable; [move] returns a new board.
 *
 * Gluing is not stored: two neighbouring cells are glued when their tiles are neighbours the same way in the
 * picture. A group is a connected set of glued cells, so a group can fall apart when something is dropped on it
 * (design §3).
 */
class Board(val rows: Int, val cols: Int, tiles: List<Int>) {
    val size = rows * cols
    val tiles: List<Int> = tiles.toList()

    init {
        require(rows >= 1 && cols >= 1) { "Empty board" }
        require(this.tiles.size == size) { "Expected $size tiles, got ${this.tiles.size}" }
        require(this.tiles.sorted() == (0 until size).toList()) { "Tiles must be a permutation" }
    }

    fun row(cell: Int) = cell / cols
    fun col(cell: Int) = cell % cols
    fun cell(row: Int, col: Int) = row * cols + col

    /** True when the tile on [cell] is glued to the tile on its right. */
    fun gluedRight(cell: Int): Boolean {
        if (col(cell) == cols - 1) return false
        val a = tiles[cell]
        return tiles[cell + 1] == a + 1 && a % cols != cols - 1
    }

    /** True when the tile on [cell] is glued to the tile below it. */
    fun gluedDown(cell: Int): Boolean {
        if (row(cell) == rows - 1) return false
        return tiles[cell + cols] == tiles[cell] + cols
    }

    /** Number of glued neighbour pairs; grows when a drop glues new tiles. */
    fun gluedPairs(): Int {
        var n = 0
        for (cell in 0 until size) {
            if (gluedRight(cell)) n++
            if (gluedDown(cell)) n++
        }
        return n
    }

    /** Cells of the group that contains [cell], sorted. */
    fun groupOf(cell: Int): List<Int> {
        val seen = BooleanArray(size)
        val stack = ArrayDeque<Int>()
        val result = ArrayList<Int>()
        stack.add(cell)
        seen[cell] = true
        while (stack.isNotEmpty()) {
            val c = stack.removeLast()
            result.add(c)
            for (n in gluedNeighbours(c)) {
                if (!seen[n]) {
                    seen[n] = true
                    stack.add(n)
                }
            }
        }
        result.sort()
        return result
    }

    private fun gluedNeighbours(c: Int): List<Int> {
        val out = ArrayList<Int>(4)
        if (gluedRight(c)) out.add(c + 1)
        if (gluedDown(c)) out.add(c + cols)
        if (col(c) > 0 && gluedRight(c - 1)) out.add(c - 1)
        if (row(c) > 0 && gluedDown(c - cols)) out.add(c - cols)
        return out
    }

    /** Size of the largest group, for the progress shown on the main screen. */
    fun largestGroupSize(): Int {
        val seen = BooleanArray(size)
        var best = 0
        for (cell in 0 until size) {
            if (seen[cell]) continue
            val group = groupOf(cell)
            group.forEach { seen[it] = true }
            if (group.size > best) best = group.size
        }
        return best
    }

    val isSolved: Boolean get() = groupOf(0).size == size

    /**
     * Limits a requested shift of [group] so that the whole group stays on the board: a group dropped partly off
     * the board is shifted inwards (design §3).
     */
    fun clampShift(group: List<Int>, dRow: Int, dCol: Int): Pair<Int, Int> {
        val minR = group.minOf { row(it) }
        val maxR = group.maxOf { row(it) }
        val minC = group.minOf { col(it) }
        val maxC = group.maxOf { col(it) }
        return dRow.coerceIn(-minR, rows - 1 - maxR) to dCol.coerceIn(-minC, cols - 1 - maxC)
    }

    /**
     * Drags the group containing [cell] by [dRow] rows and [dCol] columns (clamped to the board) and returns the new
     * board, or null when nothing moves.
     *
     * Tiles on the target cells are displaced into the cells the group left. Each displaced tile walks back against
     * the drag direction until it reaches a freed cell, so displaced tiles keep their arrangement where possible;
     * dropping a single tile is a plain swap.
     */
    fun move(cell: Int, dRow: Int, dCol: Int): Board? {
        val group = groupOf(cell)
        val (dr, dc) = clampShift(group, dRow, dCol)
        if (dr == 0 && dc == 0) return null

        val source = group.toHashSet()
        val target = group.map { cell(row(it) + dr, col(it) + dc) }.toHashSet()
        val next = tiles.toIntArray()

        for (s in group) next[cell(row(s) + dr, col(s) + dc)] = tiles[s]
        for (d in target) {
            if (d in source) continue
            var r = row(d) - dr
            var c = col(d) - dc
            while (cell(r, c) in target) {
                r -= dr
                c -= dc
            }
            next[cell(r, c)] = tiles[d]
        }
        return Board(rows, cols, next.toList())
    }

    companion object {
        /** A shuffled board where no two tiles start glued (design §3). */
        fun shuffled(rows: Int, cols: Int, random: Random = Random.Default): Board {
            val ids = (0 until rows * cols).toList()
            var board = Board(rows, cols, ids.shuffled(random))
            repeat(10_000) {
                if (board.gluedPairs() == 0) return board
                board = Board(rows, cols, ids.shuffled(random))
            }
            return board
        }

        fun solved(rows: Int, cols: Int) = Board(rows, cols, (0 until rows * cols).toList())
    }
}

/** A grid size: rows × columns. */
data class Grid(val rows: Int, val cols: Int) {
    val count get() = rows * cols
}

object GridChooser {
    const val MIN_SIDE = 3

    /**
     * Picks rows × columns close to [targetCount] tiles for a picture of [aspect] (width / height), keeping tiles
     * close to square and within [maxRows] × [maxCols]. The picture is not cropped, so tiles may be slightly
     * rectangular.
     */
    fun choose(targetCount: Int, aspect: Float, maxRows: Int, maxCols: Int): Grid {
        val rMax = maxRows.coerceAtLeast(MIN_SIDE)
        val cMax = maxCols.coerceAtLeast(MIN_SIDE)
        var best = Grid(MIN_SIDE, MIN_SIDE)
        var bestScore = Double.MAX_VALUE
        for (r in MIN_SIDE..rMax) {
            for (c in MIN_SIDE..cMax) {
                val tileAspect = aspect.toDouble() * r / c
                val countMiss = abs(r * c - targetCount).toDouble() / targetCount
                val score = countMiss + 0.6 * abs(ln(tileAspect))
                if (score < bestScore) {
                    bestScore = score
                    best = Grid(r, c)
                }
            }
        }
        return best
    }

    /** The largest grid allowed when the board is [boardWidth] × [boardHeight] and a tile is at least [minTile]. */
    fun maxGrid(boardWidth: Float, boardHeight: Float, minTile: Float): Grid =
        Grid(
            (boardHeight / minTile).toInt().coerceAtLeast(MIN_SIDE),
            (boardWidth / minTile).toInt().coerceAtLeast(MIN_SIDE),
        )
}
