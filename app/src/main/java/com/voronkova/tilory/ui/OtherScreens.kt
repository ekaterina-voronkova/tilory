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
import com.voronkova.tilory.data.ColorSource
import com.voronkova.tilory.data.HistoryEntry
import com.voronkova.tilory.data.Preset
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

// --- Completion (design §6) ---

/**
 * The photo area repeats the puzzle screen's layout (status bar, 56 dp top row, board area with 8 dp padding), so
 * the solved photo stays where the board was (#22). The buttons sit in one row at the bottom, laid over the photo
 * area on a translucent strip, so they do not shrink it. The solve time is only in History.
 */
@Composable
fun CompletionScreen(vm: TiloryViewModel, entry: HistoryEntry, onNext: () -> Unit) {
    // The puzzle's bitmap is still loaded right after solving; use it so there is no blank frame.
    val puzzleImage by vm.image.collectAsStateWithLifecycle()
    val stored = rememberPhoto(vm, entry.photo, thumbnail = false)
    val image = puzzleImage ?: stored
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.fillMaxWidth().height(56.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                image?.let { ZoomableImage(it, background = false) }
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = { vm.show(Screen.Home) }) { Text(stringResource(R.string.home)) }
            Button(onClick = onNext) { Text(stringResource(R.string.next_puzzle)) }
            TextButton(onClick = { vm.show(Screen.History) }) { Text(stringResource(R.string.history)) }
        }
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
private fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
        },
    )
}

@Composable
fun HistoryScreen(vm: TiloryViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar(stringResource(R.string.history), vm::back) }) { padding ->
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

            if (wallpaperColorsAvailable) {
                HorizontalDivider()
                Text(stringResource(R.string.colors_title), style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSource.entries.forEach { source ->
                        androidx.compose.material3.FilterChip(
                            selected = settings.colors == source,
                            onClick = { vm.updateSettings { it.copy(colors = source) } },
                            label = {
                                Text(
                                    stringResource(
                                        when (source) {
                                            ColorSource.TILORY -> R.string.colors_tilory
                                            ColorSource.WALLPAPER -> R.string.colors_wallpaper
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }

            HorizontalDivider()
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
