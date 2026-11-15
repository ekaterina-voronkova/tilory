package com.voronkova.tilory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.data.Photo
import com.voronkova.tilory.data.Preset
import com.voronkova.tilory.data.ReferenceMode
import com.voronkova.tilory.game.Grid
import com.voronkova.tilory.game.GridChooser
import kotlin.math.roundToInt

/** Screen space the puzzle screen keeps for its top bar, system bars and margins, in dp. */
private const val PUZZLE_CHROME_DP = 150f
private const val PUZZLE_SIDE_MARGIN_DP = 16f

@Composable
fun maxGridFor(photo: Photo, minTileDp: Int): Grid {
    val config = LocalConfiguration.current
    val availW = config.screenWidthDp - PUZZLE_SIDE_MARGIN_DP
    val availH = config.screenHeightDp - PUZZLE_CHROME_DP
    val aspect = photo.aspect
    val (w, h) = if (aspect > availW / availH) availW to availW / aspect else availH * aspect to availH
    return GridChooser.maxGrid(w, h, minTileDp.toFloat())
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OptionsScreen(vm: TiloryViewModel, photo: Photo) {
    val data by vm.data.collectAsStateWithLifecycle()
    val settings = data.settings
    val maxGrid = maxGridFor(photo, settings.minTileDp)
    val maxTiles = maxGrid.count.coerceAtLeast(9)

    var preset by remember {
        mutableStateOf(Preset.entries.firstOrNull { it.tiles == settings.ownTiles } ?: Preset.CUSTOM)
    }
    var custom by remember { mutableFloatStateOf(settings.ownTiles.toFloat().coerceIn(9f, maxTiles.toFloat())) }
    var reference by remember { mutableStateOf(settings.ownReference) }

    val target = if (preset == Preset.CUSTOM) custom.roundToInt() else preset.tiles
    val grid = GridChooser.choose(target.coerceAtMost(maxTiles), photo.aspect, maxGrid.rows, maxGrid.cols)
    val thumb = rememberPhoto(vm, photo, thumbnail = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.options_title)) },
                navigationIcon = {
                    IconButton(onClick = vm::back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(max = 240.dp), contentAlignment = Alignment.Center) {
                if (thumb != null) {
                    Image(
                        thumb,
                        null,
                        Modifier
                            .aspectRatio(photo.aspect)
                            .clip(MaterialTheme.shapes.medium),
                    )
                }
            }

            Text(stringResource(R.string.difficulty), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Preset.entries.forEach { p ->
                    FilterChip(
                        selected = preset == p,
                        onClick = { preset = p },
                        label = { Text(presetLabel(p)) },
                    )
                }
            }
            if (preset == Preset.CUSTOM) {
                Text(stringResource(R.string.tiles_count, custom.roundToInt()))
                Slider(
                    value = custom.coerceIn(9f, maxTiles.toFloat()),
                    onValueChange = { custom = it },
                    valueRange = 9f..maxTiles.toFloat(),
                )
            }
            Text(
                stringResource(R.string.grid_size, grid.rows, grid.cols, grid.count),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (target > maxTiles) {
                Text(
                    stringResource(R.string.limited_by_screen),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(stringResource(R.string.reference), style = MaterialTheme.typography.titleMedium)
            ReferenceChips(reference) { reference = it }

            if (data.current != null) {
                Text(
                    stringResource(R.string.replace_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = { vm.startPuzzle(photo, grid, target, reference) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.start)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReferenceChips(selected: ReferenceMode, onSelect: (ReferenceMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReferenceMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = {
                    Text(
                        stringResource(
                            when (mode) {
                                ReferenceMode.OFF -> R.string.ref_off
                                ReferenceMode.PEEK -> R.string.ref_peek
                                ReferenceMode.ALWAYS -> R.string.ref_always
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
fun presetLabel(p: Preset): String = stringResource(
    when (p) {
        Preset.EASY -> R.string.preset_easy
        Preset.MEDIUM -> R.string.preset_medium
        Preset.HARD -> R.string.preset_hard
        Preset.CHALLENGE -> R.string.preset_challenge
        Preset.CUSTOM -> R.string.preset_custom
    },
)
