package com.voronkova.tilory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.data.HistoryEntry
import com.voronkova.tilory.data.Preset
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

// --- Completion (design §6) ---

/**
 * Same layout as the puzzle screen ([GameLayout]), so the solved photo stays exactly where the board was (#22, #27,
 * #39). Bar: Home, Next puzzle (centred), History. The solve time is only in History.
 */
@Composable
fun CompletionScreen(vm: TiloryViewModel, entry: HistoryEntry, onNext: () -> Unit) {
    // The puzzle's bitmap is still loaded right after solving; use it so there is no blank frame.
    val puzzleImage by vm.image.collectAsStateWithLifecycle()
    val stored = rememberPhoto(vm, entry.photo, thumbnail = false)
    val image = puzzleImage ?: stored
    GameLayout(
        bar = { vertical ->
            val home: @Composable (Modifier) -> Unit = { m ->
                TextButton(onClick = { vm.show(Screen.Home) }, m) { Text(stringResource(R.string.home)) }
            }
            val next: @Composable (Modifier) -> Unit = { m ->
                Button(onClick = onNext, modifier = m) {
                    Text(stringResource(R.string.next_puzzle), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            val history: @Composable (Modifier) -> Unit = { m ->
                TextButton(onClick = { vm.show(Screen.History) }, m) { Text(stringResource(R.string.history)) }
            }
            Box(Modifier.fillMaxSize().padding(4.dp)) {
                if (vertical) {
                    home(Modifier.align(Alignment.TopCenter))
                    next(Modifier.align(Alignment.Center))
                    history(Modifier.align(Alignment.BottomCenter))
                } else {
                    home(Modifier.align(Alignment.CenterStart))
                    next(Modifier.align(Alignment.Center))
                    history(Modifier.align(Alignment.CenterEnd))
                }
            }
        },
    ) {
        image?.let { ZoomableImage(it, background = false) }
    }
}

/** A full-screen photo: pinch to zoom, drag to pan, double tap to reset. */
@Composable
fun ZoomableImage(image: ImageBitmap, background: Boolean = true) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Image(
        image,
        null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .then(if (background) Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest) else Modifier)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offset = Offset.Zero
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}

// --- History (design §7) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        },
        actions = actions,
    )
}

@Composable
fun HistoryScreen(vm: TiloryViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var askClear by remember { mutableStateOf(false) }
    if (askClear) ClearHistoryDialog(vm) { askClear = false }
    Scaffold(
        topBar = {
            BackTopBar(stringResource(R.string.history), vm::back) {
                if (data.history.isNotEmpty()) {
                    IconButton(onClick = { askClear = true }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.clear_history))
                    }
                }
            }
        },
    ) { padding ->
        if (data.history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(data.history, key = { "${it.photo.file}-${it.solvedAt}" }) { entry ->
                    val thumb = rememberPhoto(vm, entry.photo, thumbnail = true)
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { vm.show(Screen.Viewer(entry)) },
                    ) {
                        thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewerScreen(vm: TiloryViewModel, entry: HistoryEntry) {
    val image = rememberPhoto(vm, entry.photo, thumbnail = false)
    val date = remember(entry) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.solvedAt))
    }
    Scaffold(topBar = { BackTopBar(date, vm::back) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                image?.let { ZoomableImage(it) }
            }
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "${entry.rows} × ${entry.cols} · " +
                        stringResource(R.string.solved_in, formatDuration(entry.elapsedMs)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = { vm.show(Screen.Options(entry.photo)) }, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.play_again))
                }
            }
        }
    }
}

// --- Settings ---

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: TiloryViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val settings = data.settings
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.settings), vm::back) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.defaults_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.difficulty), style = MaterialTheme.typography.bodyMedium)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Preset.entries.filter { it != Preset.CUSTOM }.forEach { p ->
                    androidx.compose.material3.FilterChip(
                        selected = settings.ownTiles == p.tiles,
                        onClick = { vm.updateSettings { it.copy(ownTiles = p.tiles) } },
                        label = { Text(presetLabel(p)) },
                    )
                }
            }
            if (Preset.entries.none { it.tiles == settings.ownTiles }) {
                Text("${presetLabel(Preset.CUSTOM)}: ${stringResource(R.string.tiles_count, settings.ownTiles)}")
            }
            Text(stringResource(R.string.reference), style = MaterialTheme.typography.bodyMedium)
            ReferenceChips(settings.ownReference) { mode -> vm.updateSettings { it.copy(ownReference = mode) } }

            // Surprise starts without an options screen (it would show the photo), so its choices live here (#4).
            HorizontalDivider()
            Text(stringResource(R.string.surprise_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.difficulty), style = MaterialTheme.typography.bodyMedium)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Preset.entries.filter { it != Preset.CUSTOM }.forEach { p ->
                    androidx.compose.material3.FilterChip(
                        selected = settings.surpriseTiles == p.tiles,
                        onClick = { vm.updateSettings { it.copy(surpriseTiles = p.tiles) } },
                        label = { Text(presetLabel(p)) },
                    )
                }
            }
            Text(stringResource(R.string.reference), style = MaterialTheme.typography.bodyMedium)
            ReferenceChips(settings.surpriseReference) { mode ->
                vm.updateSettings { it.copy(surpriseReference = mode) }
            }
            OutlinedButton(onClick = { vm.show(Screen.Sources) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sources_title) + " · " + data.surprise.sources.size)
            }
            OutlinedButton(onClick = { vm.show(Screen.Hidden) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.hidden_title) + " · " + data.surprise.hidden.size)
            }

            HorizontalDivider()
            Text(stringResource(R.string.history), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.keep_history))
                    Text(
                        stringResource(R.string.keep_history_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.keepHistory,
                    onCheckedChange = { on -> vm.updateSettings { it.copy(keepHistory = on) } },
                )
            }
            var askClear by remember { mutableStateOf(false) }
            if (askClear) ClearHistoryDialog(vm) { askClear = false }
            OutlinedButton(
                onClick = { askClear = true },
                enabled = data.history.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val count = data.history.size
                Text(
                    if (count > 0) {
                        stringResource(R.string.clear_history) + " · " +
                            androidx.compose.ui.res.pluralStringResource(R.plurals.history_photos, count, count)
                    } else {
                        stringResource(R.string.clear_history)
                    },
                )
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rotate_landscape), Modifier.weight(1f))
                Switch(
                    checked = settings.rotateLandscape,
                    onCheckedChange = { on -> vm.updateSettings { it.copy(rotateLandscape = on) } },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.vibration), Modifier.weight(1f))
                Switch(
                    checked = settings.vibration,
                    onCheckedChange = { on -> vm.updateSettings { it.copy(vibration = on) } },
                )
            }

            Spacer(Modifier.size(16.dp))
            Text(
                stringResource(R.string.version, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
