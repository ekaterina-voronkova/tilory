package com.voronkova.tilory.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Height of the game bar in portrait, in dp. */
const val BAR_HEIGHT_DP = 56

/** Width of the game bar at the side in landscape, in dp (#39). */
const val SIDE_BAR_WIDTH_DP = 136

/** Margin around the board, in dp. */
const val BOARD_MARGIN_DP = 8

@Composable
fun isLandscape() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * The layout shared by the puzzle and completion screens: the photo area, and the bar at the bottom in portrait or
 * at the side in landscape. Both screens use it, so the solved photo stays exactly where the board was (#22, #27,
 * #39). [bar] gets `vertical = true` when it is the side column.
 */
@Composable
fun GameLayout(bar: @Composable (vertical: Boolean) -> Unit, content: @Composable BoxScope.() -> Unit) {
    if (isLandscape()) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(BOARD_MARGIN_DP.dp),
                contentAlignment = Alignment.Center,
                content = content,
            )
            Box(Modifier.width(SIDE_BAR_WIDTH_DP.dp).fillMaxHeight()) { bar(true) }
        }
    } else {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(BOARD_MARGIN_DP.dp),
                contentAlignment = Alignment.Center,
                content = content,
            )
            Box(Modifier.fillMaxWidth().height(BAR_HEIGHT_DP.dp)) { bar(false) }
        }
    }
}
