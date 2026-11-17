package com.voronkova.tilory.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.data.Mode
import com.voronkova.tilory.game.Board
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HomeScreen(vm: TiloryViewModel, onPickGallery: () -> Unit, onPickFiles: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val skipped by vm.skipped.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pendingPick by remember { mutableStateOf<(() -> Unit)?>(null) }

    val skippedText = stringResource(R.string.skipped)
    val undoText = stringResource(R.string.undo)
    LaunchedEffect(skipped) {
        if (skipped == null) return@LaunchedEffect
        val result = withTimeoutOrNull(5_000) {
            snackbar.showSnackbar(skippedText, actionLabel = undoText, duration = SnackbarDuration.Indefinite)
        }
        if (result == SnackbarResult.ActionPerformed) vm.undoSkip() else vm.skipExpired()
    }

    val current = data.current
    // Starting a new puzzle replaces the unfinished one, so ask first (design §8).
    val guarded: (() -> Unit) -> () -> Unit = { pick -> { if (current != null) pendingPick = pick else pick() } }

    // Background: the owner's watercolour, cropped at the sides, never stretched; dimmed in dark mode (#34).
    Box(Modifier.fillMaxSize()) {
    Image(
        painterResource(R.drawable.home_background),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
    if (isSystemInDarkTheme()) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = Color.Transparent) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The picture title, centred in the upper third of the screen (#32).
            val thirdHeight = (LocalConfiguration.current.screenHeightDp / 3).dp
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(thirdHeight - padding.calculateTopPadding()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painterResource(R.drawable.title_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
                Spacer(Modifier.height(8.dp))
                // Part of the logo: English in every language, explains the name (tile + story) (#36).
                Text(
                    stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Everything below the title sits at the bottom of the screen (#35).
            Spacer(Modifier.weight(1f))
            UpdateRow(vm.update)

            if (current != null) {
                val board = remember(current) { Board(current.rows, current.cols, current.tiles) }
                val percent = board.largestGroupSize() * 100 / board.size
                Card(onClick = vm::continuePuzzle, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Own pick may show the photo; Surprise never does (design §2).
                        if (current.mode == Mode.OWN) {
                            val thumb = rememberPhoto(vm, current.photo, thumbnail = true)
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            ) {
                                if (thumb != null) {
                                    Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                            Spacer(Modifier.size(16.dp))
                        }
                        Column {
                            Text(stringResource(R.string.continue_title), style = MaterialTheme.typography.titleLarge)
                            Text(
                                stringResource(R.string.continue_detail, current.rows, current.cols, percent),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // River-blue buttons, the colour of the river in the title logo, with dark text for contrast (#44).
            // Random photo (Surprise) is the main mode (design §2); with no folders yet it offers to add them.
            val river = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.river),
                contentColor = colorResource(R.color.on_river),
            )
            Button(onClick = guarded(vm::startSurprise), modifier = Modifier.fillMaxWidth(), colors = river) {
                Text(stringResource(R.string.surprise_title))
            }
            Button(onClick = guarded(onPickGallery), modifier = Modifier.fillMaxWidth(), colors = river) {
                Text(stringResource(R.string.pick_gallery))
            }
            Button(onClick = guarded(onPickFiles), modifier = Modifier.fillMaxWidth(), colors = river) {
                Text(stringResource(R.string.pick_files))
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        RoundedCornerShape(12.dp),
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = { vm.show(Screen.History) }) { Text(stringResource(R.string.history)) }
                TextButton(onClick = { vm.show(Screen.Settings) }) { Text(stringResource(R.string.settings)) }
            }
        }
    }
    }

    pendingPick?.let { pick ->
        AlertDialog(
            onDismissRequest = { pendingPick = null },
            title = { Text(stringResource(R.string.replace_title)) },
            text = { Text(stringResource(R.string.replace_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingPick = null
                    pick()
                }) { Text(stringResource(R.string.replace_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingPick = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
