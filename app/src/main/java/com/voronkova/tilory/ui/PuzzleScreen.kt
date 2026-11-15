package com.voronkova.tilory.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.data.ReferenceMode
import com.voronkova.tilory.game.Board
import kotlin.math.roundToInt

@Composable
fun PuzzleScreen(vm: TiloryViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val board by vm.board.collectAsStateWithLifecycle()
    val image by vm.image.collectAsStateWithLifecycle()
    val solved by vm.solved.collectAsStateWithLifecycle()
    var peek by remember { mutableStateOf(false) }
    // After the last drop the puzzle state is gone, but the reference controls are hidden by then anyway.
    val reference = data.current?.reference ?: ReferenceMode.OFF

    // The solve time counts only while the puzzle is on screen (design §6).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.resumeTimer()
                Lifecycle.Event.ON_PAUSE -> vm.pauseTimer()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.resumeTimer()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.pauseTimer()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = vm::back) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
            board?.let { Text("${it.rows} × ${it.cols}", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.weight(1f))
            val img = image
            if (!solved && img != null) {
                when (reference) {
                    ReferenceMode.PEEK -> PeekButton { peek = it }
                    ReferenceMode.ALWAYS -> Image(
                        img,
                        null,
                        Modifier
                            .size(44.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop,
                    )
                    ReferenceMode.OFF -> Unit
                }
            }
            if (!solved) {
                TextButton(onClick = vm::skip) { Text(stringResource(R.string.skip)) }
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val b = board
            val img = image
            if (b == null || img == null) {
                CircularProgressIndicator()
            } else {
                BoardView(
                    board = b,
                    image = img,
                    solved = solved,
                    peek = peek,
                    vibration = data.settings.vibration,
                    onMove = vm::move,
                )
            }
        }
    }
}

/** Hold to see the picture over the board, release to hide it (design §4). */
@Composable
private fun PeekButton(onPeek: (Boolean) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onPeek(true)
                    tryAwaitRelease()
                    onPeek(false)
                })
            },
    ) {
        Text(
            stringResource(R.string.ref_peek),
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun BoardView(
    board: Board,
    image: ImageBitmap,
    solved: Boolean,
    peek: Boolean,
    vibration: Boolean,
    onMove: (Int, Int, Int) -> Boolean,
) {
    val context = LocalContext.current
    val haptics = remember(context) { Haptics(context) }
    val density = LocalDensity.current
    val seamAlpha by animateFloatAsState(if (solved) 0f else 1f, tween(900), label = "seams")
    val seamColor = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary
    val slotColor = MaterialTheme.colorScheme.surfaceVariant

    var dragCell by remember { mutableIntStateOf(-1) }
    var dragGroup by remember { mutableStateOf(emptyList<Int>()) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val aspect = image.width.toFloat() / image.height
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val (boardW, boardH) = if (aspect > maxW / maxH) maxW to maxW / aspect else maxH * aspect to maxH
        val boardSize = with(density) { androidx.compose.ui.unit.DpSize(boardW.toDp(), boardH.toDp()) }

        Canvas(
            Modifier
                .size(boardSize)
                .pointerInput(board, solved) {
                    if (solved) return@pointerInput
                    val tw = size.width.toFloat() / board.cols
                    val th = size.height.toFloat() / board.rows
                    fun reset() {
                        dragCell = -1
                        dragGroup = emptyList()
                        dragOffset = Offset.Zero
                    }
                    detectDragGestures(
                        onDragStart = { pos ->
                            val c = (pos.x / tw).toInt().coerceIn(0, board.cols - 1)
                            val r = (pos.y / th).toInt().coerceIn(0, board.rows - 1)
                            dragCell = board.cell(r, c)
                            dragGroup = board.groupOf(dragCell)
                            dragOffset = Offset.Zero
                        },
                        onDragEnd = {
                            val cell = dragCell
                            val dr = (dragOffset.y / th).roundToInt()
                            val dc = (dragOffset.x / tw).roundToInt()
                            reset()
                            if (cell >= 0 && onMove(cell, dr, dc) && vibration) haptics.glue()
                        },
                        onDragCancel = { reset() },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount
                        },
                    )
                },
        ) {
            val tw = size.width / board.cols
            val th = size.height / board.rows
            val dragging = dragGroup.toHashSet()
            val seam = 1.dp.toPx()
            val outline = 2.5.dp.toPx()

            // Tiles that are not being dragged; the dragged group's cells show as empty slots.
            for (cell in 0 until board.size) {
                val x = board.col(cell) * tw
                val y = board.row(cell) * th
                if (cell in dragging) {
                    drawRect(slotColor, Offset(x, y), androidx.compose.ui.geometry.Size(tw, th))
                } else {
                    drawTile(image, board, board.tiles[cell], x, y, tw, th)
                }
            }

            // Seams between tiles that are not glued; they fade out when the puzzle is solved.
            if (seamAlpha > 0f) {
                val color = seamColor.copy(alpha = seamAlpha)
                for (cell in 0 until board.size) {
                    val r = board.row(cell)
                    val c = board.col(cell)
                    if (c < board.cols - 1 && !board.gluedRight(cell)) {
                        val x = (c + 1) * tw
                        drawLine(color, Offset(x, r * th), Offset(x, (r + 1) * th), seam)
                    }
                    if (r < board.rows - 1 && !board.gluedDown(cell)) {
                        val y = (r + 1) * th
                        drawLine(color, Offset(c * tw, y), Offset((c + 1) * tw, y), seam)
                    }
                }
            }

            if (dragGroup.isNotEmpty()) {
                // Where the group will land, already shifted inwards at the edges.
                val (dr, dc) = board.clampShift(
                    dragGroup,
                    (dragOffset.y / th).roundToInt(),
                    (dragOffset.x / tw).roundToInt(),
                )
                val target = dragGroup.map { board.row(it) + dr to board.col(it) + dc }.toHashSet()
                drawOutline(target, Offset.Zero, tw, th, accent.copy(alpha = 0.6f), outline)

                for (cell in dragGroup) {
                    drawTile(
                        image, board, board.tiles[cell],
                        board.col(cell) * tw + dragOffset.x, board.row(cell) * th + dragOffset.y, tw, th,
                    )
                }
                val cells = dragGroup.map { board.row(it) to board.col(it) }.toHashSet()
                drawOutline(cells, dragOffset, tw, th, accent, outline)
            }

            if (peek) {
                drawImage(
                    image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    alpha = 0.7f,
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
    }
}

/** Draws [tile] (its region of the picture) with its top-left corner at ([x], [y]). */
private fun DrawScope.drawTile(
    image: ImageBitmap,
    board: Board,
    tile: Int,
    x: Float,
    y: Float,
    tw: Float,
    th: Float,
) {
    val sc = tile % board.cols
    val sr = tile / board.cols
    val sx0 = sc * image.width / board.cols
    val sx1 = (sc + 1) * image.width / board.cols
    val sy0 = sr * image.height / board.rows
    val sy1 = (sr + 1) * image.height / board.rows
    val dx0 = x.roundToInt()
    val dy0 = y.roundToInt()
    drawImage(
        image,
        srcOffset = IntOffset(sx0, sy0),
        srcSize = IntSize(sx1 - sx0, sy1 - sy0),
        dstOffset = IntOffset(dx0, dy0),
        dstSize = IntSize((x + tw).roundToInt() - dx0, (y + th).roundToInt() - dy0),
        filterQuality = FilterQuality.Medium,
    )
}

/** Outlines the outer edges of a set of (row, col) cells. */
private fun DrawScope.drawOutline(
    cells: Set<Pair<Int, Int>>,
    offset: Offset,
    tw: Float,
    th: Float,
    color: Color,
    width: Float,
) {
    for ((r, c) in cells) {
        val x0 = c * tw + offset.x
        val y0 = r * th + offset.y
        val x1 = x0 + tw
        val y1 = y0 + th
        if ((r - 1 to c) !in cells) drawLine(color, Offset(x0, y0), Offset(x1, y0), width)
        if ((r + 1 to c) !in cells) drawLine(color, Offset(x0, y1), Offset(x1, y1), width)
        if ((r to c - 1) !in cells) drawLine(color, Offset(x0, y0), Offset(x0, y1), width)
        if ((r to c + 1) !in cells) drawLine(color, Offset(x1, y0), Offset(x1, y1), width)
    }
}
