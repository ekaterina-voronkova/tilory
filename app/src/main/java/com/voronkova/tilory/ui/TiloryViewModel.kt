package com.voronkova.tilory.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voronkova.tilory.data.AppData
import com.voronkova.tilory.data.HistoryEntry
import com.voronkova.tilory.data.Mode
import com.voronkova.tilory.data.Photo
import com.voronkova.tilory.data.PhotoImporter
import com.voronkova.tilory.data.PuzzleState
import com.voronkova.tilory.data.ReferenceMode
import com.voronkova.tilory.data.Settings
import com.voronkova.tilory.data.Store
import com.voronkova.tilory.game.Board
import com.voronkova.tilory.game.Grid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data class Options(val photo: Photo) : Screen
    data object Puzzle : Screen
    data class Completion(val entry: HistoryEntry) : Screen
    data object History : Screen
    data class Viewer(val entry: HistoryEntry) : Screen
    data object Settings : Screen
}

@OptIn(ExperimentalCoroutinesApi::class)
class TiloryViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val photos = PhotoImporter(app)
    private val io = Dispatchers.IO.limitedParallelism(1)

    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _board = MutableStateFlow<Board?>(null)
    val board: StateFlow<Board?> = _board.asStateFlow()

    private val _image = MutableStateFlow<ImageBitmap?>(null)
    val image: StateFlow<ImageBitmap?> = _image.asStateFlow()

    /** True for the short moment between the last drop and the completion screen. */
    private val _solved = MutableStateFlow(false)
    val solved: StateFlow<Boolean> = _solved.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error: StateFlow<Boolean> = _error.asStateFlow()

    /** The puzzle just skipped, while Undo is still offered (design §5). */
    private val _skipped = MutableStateFlow<PuzzleState?>(null)
    val skipped: StateFlow<PuzzleState?> = _skipped.asStateFlow()

    private var resumedAt: Long? = null

    init {
        val d = _data.value
        val keep = (d.history.map { it.photo.file } + listOfNotNull(d.current?.photo?.file)).toSet()
        viewModelScope.launch(io) { photos.cleanUp(keep) }
    }

    // --- Starting a puzzle ---

    fun importPhoto(uri: Uri) {
        _busy.value = true
        viewModelScope.launch {
            val photo = runCatching { withContext(Dispatchers.IO) { photos.import(uri) } }.getOrNull()
            _busy.value = false
            if (photo == null) _error.value = true else _screen.value = Screen.Options(photo)
        }
    }

    fun clearError() {
        _error.value = false
    }

    fun startPuzzle(photo: Photo, grid: Grid, targetTiles: Int, reference: ReferenceMode) {
        val board = Board.shuffled(grid.rows, grid.cols)
        val state = PuzzleState(photo, Mode.OWN, grid.rows, grid.cols, board.tiles, 0L, reference)
        _skipped.value = null
        update {
            it.copy(
                current = state,
                settings = it.settings.copy(ownTiles = targetTiles, ownReference = reference),
            )
        }
        openPuzzle(state)
    }

    fun continuePuzzle() {
        _data.value.current?.let { openPuzzle(it) }
    }

    private fun openPuzzle(state: PuzzleState) {
        _solved.value = false
        _board.value = Board(state.rows, state.cols, state.tiles)
        _image.value = null
        _screen.value = Screen.Puzzle
        viewModelScope.launch {
            _image.value = withContext(Dispatchers.IO) { photos.load(state.photo)?.asImageBitmap() }
        }
    }

    // --- Playing ---

    /** Applies a drop. Returns true when new tiles glued, so the screen can vibrate. */
    fun move(cell: Int, dRow: Int, dCol: Int): Boolean {
        if (_solved.value) return false
        val board = _board.value ?: return false
        val next = board.move(cell, dRow, dCol) ?: return false
        _board.value = next
        val glued = next.gluedPairs() > board.gluedPairs()
        if (next.isSolved) {
            finish()
        } else {
            update { d -> d.copy(current = d.current?.copy(tiles = next.tiles, elapsedMs = elapsedNow())) }
        }
        return glued
    }

    fun resumeTimer() {
        if (resumedAt == null && _data.value.current != null) resumedAt = SystemClock.elapsedRealtime()
    }

    fun pauseTimer() {
        val current = _data.value.current ?: return
        val elapsed = elapsedNow()
        resumedAt = null
        update { it.copy(current = current.copy(elapsedMs = elapsed)) }
    }

    private fun elapsedNow(): Long {
        val base = _data.value.current?.elapsedMs ?: 0L
        val start = resumedAt ?: return base
        val now = SystemClock.elapsedRealtime()
        resumedAt = now
        return base + (now - start)
    }

    private fun finish() {
        val current = _data.value.current ?: return
        val elapsed = elapsedNow()
        resumedAt = null
        val entry = HistoryEntry(current.photo, System.currentTimeMillis(), elapsed, current.rows, current.cols)
        _solved.value = true
        update { it.copy(current = null, history = listOf(entry) + it.history) }
        viewModelScope.launch {
            delay(1200)
            _screen.value = Screen.Completion(entry)
        }
    }

    // --- Skip and undo (design §5) ---

    fun skip() {
        val current = _data.value.current ?: return
        pauseTimer()
        _skipped.value = _data.value.current ?: current
        update { it.copy(current = null) }
        _board.value = null
        _screen.value = Screen.Home
    }

    fun undoSkip() {
        val state = _skipped.value ?: return
        _skipped.value = null
        update { it.copy(current = state) }
        openPuzzle(state)
    }

    fun skipExpired() {
        _skipped.value = null
    }

    // --- Navigation and settings ---

    fun show(screen: Screen) {
        if (_screen.value == Screen.Puzzle) pauseTimer()
        _screen.value = screen
    }

    fun back() {
        _screen.value = when (_screen.value) {
            is Screen.Viewer -> Screen.History
            else -> Screen.Home
        }
    }

    fun updateSettings(change: (Settings) -> Settings) {
        update { it.copy(settings = change(it.settings)) }
    }

    private fun update(change: (AppData) -> AppData) {
        _data.update(change)
        val snapshot = _data.value
        viewModelScope.launch(io) { store.save(snapshot) }
    }
}
