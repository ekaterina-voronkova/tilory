package com.voronkova.tilory.ui

import android.text.format.Formatter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** "Clear history?" with the number of photos and the space they take; cannot be undone (#38). */
@Composable
fun ClearHistoryDialog(vm: TiloryViewModel, onDismiss: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val count = data.history.size
    var bytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(count) { bytes = withContext(Dispatchers.IO) { vm.historyBytes() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clear_history_title)) },
        text = {
            Text(
                stringResource(
                    R.string.clear_history_text,
                    pluralStringResource(R.plurals.history_photos, count, count),
                    Formatter.formatShortFileSize(context, bytes),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                vm.clearHistory()
                onDismiss()
            }) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
