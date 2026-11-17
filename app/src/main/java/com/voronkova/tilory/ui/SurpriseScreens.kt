package com.voronkova.tilory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.data.HiddenPhoto
import com.voronkova.tilory.data.SurpriseMemory
import com.voronkova.tilory.data.SurpriseSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Surprise sources (design §11, #4) ---

@Composable
fun SourcesScreen(vm: TiloryViewModel, onAddFolder: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val sources = data.surprise.sources
    Scaffold(topBar = { BackTopBar(stringResource(R.string.sources_title), vm::back) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.sources_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(sources, key = { it.tree }) { source -> SourceCard(vm, source, data.surprise) }
            item {
                Button(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sources_add))
                }
            }
        }
    }
}

@Composable
private fun SourceCard(vm: TiloryViewModel, source: SurpriseSource, memory: SurpriseMemory) {
    // Counting walks the folder, so it runs off the main thread; it is redone when Surprise memory changes.
    var counts by remember(source) { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(source, memory) {
        counts = withContext(Dispatchers.IO) { runCatching { vm.sourceCounts(source) }.getOrNull() }
    }
    var askRemove by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(source.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    counts?.let { (all, left) -> stringResource(R.string.sources_count, all, left) }
                        ?: stringResource(R.string.sources_counting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { askRemove = true }) {
                Icon(Icons.Filled.Delete, stringResource(R.string.sources_remove))
            }
        }
    }
    if (askRemove) {
        AlertDialog(
            onDismissRequest = { askRemove = false },
            title = { Text(stringResource(R.string.sources_remove_title, source.name)) },
            text = { Text(stringResource(R.string.sources_remove_text)) },
            confirmButton = {
                TextButton(onClick = {
                    askRemove = false
                    vm.removeSource(source.tree)
                }) { Text(stringResource(R.string.sources_remove)) }
            },
            dismissButton = { TextButton(onClick = { askRemove = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// --- Hidden photos (design §5) ---

@Composable
fun HiddenScreen(vm: TiloryViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val hidden = data.surprise.hidden
    var restore by remember { mutableStateOf<HiddenPhoto?>(null) }
    Scaffold(topBar = { BackTopBar(stringResource(R.string.hidden_title), vm::back) }) { padding ->
        if (hidden.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.hidden_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(hidden, key = { it.document }) { item ->
                    val thumb = rememberPhoto(vm, item.photo, thumbnail = true)
                    Box(
                        Modifier
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { restore = item },
                    ) {
                        thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    }
                }
            }
        }
    }
    restore?.let { item ->
        AlertDialog(
            onDismissRequest = { restore = null },
            text = { Text(stringResource(R.string.hidden_restore_text)) },
            confirmButton = {
                TextButton(onClick = {
                    restore = null
                    vm.restoreHidden(item)
                }) { Text(stringResource(R.string.hidden_restore)) }
            },
            dismissButton = { TextButton(onClick = { restore = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

// --- Dialogs shown over any screen ---

/** After a Surprise skip: Undo, or why, then the next photo (design §5). Dismissing counts as "just another one". */
@Composable
fun SkipQuestionDialog(vm: TiloryViewModel) {
    val state by vm.skipQuestion.collectAsStateWithLifecycle()
    if (state == null) return
    val answer = { a: TiloryViewModel.SkipAnswer -> { vm.answerSkip(a) } }
    AlertDialog(
        onDismissRequest = answer(TiloryViewModel.SkipAnswer.ANOTHER),
        title = { Text(stringResource(R.string.skipped)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = answer(TiloryViewModel.SkipAnswer.ANOTHER), Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.skip_another))
                }
                TextButton(onClick = answer(TiloryViewModel.SkipAnswer.NEVER_SHOW), Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.skip_never))
                }
                TextButton(onClick = answer(TiloryViewModel.SkipAnswer.NOT_THIS_FOLDER), Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.skip_folder))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = answer(TiloryViewModel.SkipAnswer.UNDO)) { Text(stringResource(R.string.undo)) }
        },
    )
}

/** Surprise could not start: no folders yet, every photo used, or nothing readable. */
@Composable
fun SurpriseProblemDialog(vm: TiloryViewModel) {
    val problem by vm.surpriseProblem.collectAsStateWithLifecycle()
    val p = problem ?: return
    AlertDialog(
        onDismissRequest = vm::clearSurpriseProblem,
        title = { Text(stringResource(R.string.surprise_title)) },
        text = {
            Text(
                stringResource(
                    when (p) {
                        SurpriseProblem.NO_SOURCES -> R.string.surprise_no_sources
                        SurpriseProblem.ALL_USED -> R.string.surprise_all_used
                        SurpriseProblem.UNREADABLE -> R.string.surprise_unreadable
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                vm.clearSurpriseProblem()
                vm.show(Screen.Sources)
            }) { Text(stringResource(R.string.sources_title)) }
        },
        dismissButton = { TextButton(onClick = vm::clearSurpriseProblem) { Text(stringResource(R.string.cancel)) } },
    )
}
