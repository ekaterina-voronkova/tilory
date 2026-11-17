package com.voronkova.tilory.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voronkova.tilory.data.AppData
import com.voronkova.tilory.data.FolderImage
import com.voronkova.tilory.data.Folders
import com.voronkova.tilory.data.HiddenPhoto
import com.voronkova.tilory.data.SurpriseMemory
import com.voronkova.tilory.data.SurprisePicker
import com.voronkova.tilory.data.SurpriseSource
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
import com.voronkova.tilory.game.GridChooser
import com.voronkova.tilory.update.UpdateController
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
    data class Completion(val entry: HistoryEntry, val mode: Mode = Mode.OWN) : Screen
    data object History : Screen
    data class Viewer(val entry: HistoryEntry) : Screen
    data object Settings : Screen
    data object Sources : Screen
    data object Hidden : Screen
}

/** Why Surprise could not start. */
enum class SurpriseProblem { NO_SOURCES, ALL_USED, UNREADABLE }

@OptIn(ExperimentalCoroutinesApi::class)
class TiloryViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val photos = PhotoImporter(app)
    val update = UpdateController(app, viewModelScope)
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

    /** A Surprise puzzle just skipped: the follow-up question with Undo is shown (design §5). */
    private val _skipQuestion = MutableStateFlow<PuzzleState?>(null)
    val skipQuestion: StateFlow<PuzzleState?> = _skipQuestion.asStateFlow()

    private val _surpriseProblem = MutableStateFlow<SurpriseProblem?>(null)
    val surpriseProblem: StateFlow<SurpriseProblem?> = _surpriseProblem.asStateFlow()

    val folders = Folders(app)

    /** The document skipped last with "just another one", so it does not come straight back. */
    private var justSkipped: String? = null

    private var resumedAt: Long? = null

    init {
        cleanUpPhotos()
    }

    /** Deletes stored photo copies that neither History, the current puzzle nor a skipped one refers to. */
    private fun cleanUpPhotos() {
        val d = _data.value
        val keep = (
            d.history.map { it.photo.file } +
                d.surprise.hidden.map { it.photo.file } +
                listOfNotNull(d.current?.photo?.file, _skipped.value?.photo?.file, _skipQuestion.value?.photo?.file)
            ).toSet()
        viewModelScope.launch(io) { photos.cleanUp(keep) }
    }

    /** Empties History and deletes the app's copies of those photos; gallery originals are untouched (#38). */
    fun clearHistory() {
        update { it.copy(history = emptyList()) }
        cleanUpPhotos()
    }

    /** Disk space taken by the History photos, in bytes. Blocking: call off the main thread. */
    fun historyBytes(): Long =
        _data.value.history.map { it.photo.file }.distinct().sumOf { photos.file(Photo(it, 0, 0)).length() }

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

    // --- Surprise (design §2, #4) ---

    /**
     * Starts a puzzle from a random photo in the Surprise folders, with the Surprise difficulty and reference image.
     * The photo is never shown before it is solved.
     */
    fun startSurprise() {
        val memory = _data.value.surprise
        if (memory.sources.isEmpty()) {
            _surpriseProblem.value = SurpriseProblem.NO_SOURCES
            return
        }
        _busy.value = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { pickSurprise(memory) }
            _busy.value = false
            when (result) {
                is SurprisePick.Found -> {
                    val (photo, image) = result
                    val settings = _data.value.settings
                    val config = getApplication<Application>().resources.configuration
                    val landscape = settings.rotateLandscape && photo.width > photo.height
                    val max = maxGridFor(photo, landscape, config.screenWidthDp, config.screenHeightDp)
                    val target = settings.surpriseTiles.coerceAtMost(max.count.coerceAtLeast(9))
                    val grid = GridChooser.choose(target, photo.aspect, max.rows, max.cols)
                    val board = Board.shuffled(grid.rows, grid.cols)
                    val state = PuzzleState(
                        photo, Mode.SURPRISE, grid.rows, grid.cols, board.tiles, 0L, settings.surpriseReference,
                        document = image.document, tree = image.tree,
                    )
                    _skipped.value = null
                    update { it.copy(current = state) }
                    openPuzzle(state)
                }
                is SurprisePick.Problem -> _surpriseProblem.value = result.problem
            }
        }
    }

    private sealed interface SurprisePick {
        data class Found(val photo: Photo, val image: FolderImage) : SurprisePick
        data class Problem(val problem: SurpriseProblem) : SurprisePick
    }

    private fun pickSurprise(memory: SurpriseMemory): SurprisePick {
        val images = memory.sources.flatMap { folders.images(it) }
        val candidates = SurprisePicker.candidates(images, memory, justSkipped)
        if (candidates.isEmpty()) return SurprisePick.Problem(SurpriseProblem.ALL_USED)
        val hiddenPhotos = memory.hidden.map { it.photo.file }.toSet()
        // Try a few: a file may be unreadable, or be a copy of a photo already solved from another folder.
        for (image in candidates.take(12)) {
            val photo = runCatching { photos.import(Uri.parse(image.document)) }.getOrNull() ?: continue
            if (photo.file in memory.solvedPhotos || photo.file in hiddenPhotos) {
                update { it.copy(surprise = it.surprise.copy(solvedDocuments = it.surprise.solvedDocuments + image.document)) }
                continue
            }
            return SurprisePick.Found(photo, image)
        }
        return SurprisePick.Problem(SurpriseProblem.UNREADABLE)
    }

    fun clearSurpriseProblem() {
        _surpriseProblem.value = null
    }

    fun addSource(tree: Uri) {
        viewModelScope.launch {
            val source = runCatching { withContext(Dispatchers.IO) { folders.add(tree) } }.getOrNull() ?: return@launch
            update {
                val sources = it.surprise.sources.filter { s -> s.tree != source.tree } + source
                it.copy(surprise = it.surprise.copy(sources = sources))
            }
        }
    }

    fun removeSource(tree: String) {
        val source = _data.value.surprise.sources.firstOrNull { it.tree == tree } ?: return
        update { it.copy(surprise = it.surprise.copy(sources = it.surprise.sources - source)) }
        viewModelScope.launch(Dispatchers.IO) { folders.release(source) }
    }

    /** Photos in [source], and how many of them have not come yet. Blocking: call off the main thread. */
    fun sourceCounts(source: SurpriseSource): Pair<Int, Int> {
        val images = folders.images(source)
        val left = SurprisePicker.candidates(images, _data.value.surprise, justSkipped = null).size
        return images.size to left
    }

    fun restoreHidden(hidden: HiddenPhoto) {
        update { it.copy(surprise = it.surprise.copy(hidden = it.surprise.hidden - hidden)) }
        cleanUpPhotos()
    }

    /** The answer to the question after a Surprise skip (design §5). */
    enum class SkipAnswer { UNDO, ANOTHER, NEVER_SHOW, NOT_THIS_FOLDER }

    fun answerSkip(answer: SkipAnswer) {
        val state = _skipQuestion.value ?: return
        _skipQuestion.value = null
        when (answer) {
            SkipAnswer.UNDO -> {
                update { it.copy(current = state) }
                openPuzzle(state)
                return
            }
            SkipAnswer.ANOTHER -> justSkipped = state.document
            SkipAnswer.NEVER_SHOW -> state.document?.let { doc ->
                update { it.copy(surprise = it.surprise.copy(hidden = it.surprise.hidden + HiddenPhoto(state.photo, doc))) }
            }
            SkipAnswer.NOT_THIS_FOLDER -> state.tree?.let(::removeSource)
        }
        cleanUpPhotos()
        startSurprise()
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
        update {
            val history = if (it.settings.keepHistory) listOf(entry) + it.history else it.history
            // A photo solved in Surprise never comes again there (design §2).
            val surprise = if (current.mode == Mode.SURPRISE) {
                it.surprise.copy(
                    solvedDocuments = it.surprise.solvedDocuments + listOfNotNull(current.document),
                    solvedPhotos = it.surprise.solvedPhotos + current.photo.file,
                )
            } else {
                it.surprise
            }
            it.copy(current = null, history = history, surprise = surprise)
        }
        viewModelScope.launch {
            delay(1200)
            _screen.value = Screen.Completion(entry, current.mode)
        }
    }

    // --- Skip and undo (design §5) ---

    fun skip() {
        val current = _data.value.current ?: return
        pauseTimer()
        val state = _data.value.current ?: current
        update { it.copy(current = null) }
        _board.value = null
        _screen.value = Screen.Home
        // Surprise asks why, with Undo, and then goes on to the next photo; Own pick offers Undo only (design §5).
        if (state.mode == Mode.SURPRISE) _skipQuestion.value = state else _skipped.value = state
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
            Screen.Sources, Screen.Hidden -> Screen.Settings
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
