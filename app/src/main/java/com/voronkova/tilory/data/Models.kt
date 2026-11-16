package com.voronkova.tilory.data

import kotlinx.serialization.Serializable

@Serializable
enum class ReferenceMode { OFF, PEEK, ALWAYS }

@Serializable
enum class Mode { OWN, SURPRISE }

/** A photo copied into the app (design §9): a downscaled JPEG in filesDir/photos. */
@Serializable
data class Photo(val file: String, val width: Int, val height: Int) {
    val aspect: Float get() = width.toFloat() / height
}

/** The one unfinished puzzle (design §8). */
@Serializable
data class PuzzleState(
    val photo: Photo,
    val mode: Mode,
    val rows: Int,
    val cols: Int,
    val tiles: List<Int>,
    val elapsedMs: Long,
    val reference: ReferenceMode,
)

@Serializable
data class HistoryEntry(
    val photo: Photo,
    val solvedAt: Long,
    val elapsedMs: Long,
    val rows: Int,
    val cols: Int,
)

/** Difficulty presets as tile counts (design §4). */
enum class Preset(val tiles: Int) { EASY(12), MEDIUM(30), HARD(63), CHALLENGE(108), CUSTOM(0) }

@Serializable
data class Settings(
    val ownTiles: Int = Preset.MEDIUM.tiles,
    val ownReference: ReferenceMode = ReferenceMode.PEEK,
    val vibration: Boolean = true,
    /** When off, solved puzzles are not added to History (#38). */
    val keepHistory: Boolean = true,
)

/** The smallest tile allowed, in dp; limits the largest grid (design §4, #17). */
const val MIN_TILE_DP = 40

@Serializable
data class AppData(
    val current: PuzzleState? = null,
    val history: List<HistoryEntry> = emptyList(),
    val settings: Settings = Settings(),
)
