package com.voronkova.tilory.ui

import android.Manifest
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.update.FailReason
import com.voronkova.tilory.update.UpdateController
import com.voronkova.tilory.update.UpdateUi

/** "Version X is available · 25 MB" on the main screen, with download progress and install (#20). */
@Composable
fun UpdateRow(update: UpdateController) {
    val state by update.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val size = { bytes: Long -> Formatter.formatShortFileSize(context, bytes) }

    // Ask for notifications (API 33+ only) when the download starts, so "downloaded, tap to install" can show.
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        update.download()
    }
    val startDownload = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            update.download()
        }
    }

    val s = state
    if (s == UpdateUi.None) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (s) {
                is UpdateUi.Available -> ActionLine(
                    stringResource(R.string.update_available, s.version, size(s.size)),
                    stringResource(R.string.update_download),
                    startDownload,
                )
                is UpdateUi.Downloading -> {
                    Text(
                        stringResource(R.string.update_downloading, size(s.done), size(s.total)),
                        Modifier.padding(vertical = 8.dp),
                    )
                    LinearProgressIndicator(
                        progress = { s.percent / 100f },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                is UpdateUi.Ready -> ActionLine(
                    stringResource(R.string.update_ready, s.version),
                    stringResource(R.string.update_install),
                    update::install,
                )
                is UpdateUi.Failed -> ActionLine(
                    stringResource(
                        when (s.reason) {
                            FailReason.SPACE -> R.string.update_failed_space
                            FailReason.NETWORK -> R.string.update_failed_network
                            FailReason.BAD_FILE -> R.string.update_failed_file
                            FailReason.OTHER -> R.string.update_failed_other
                        },
                    ),
                    stringResource(R.string.update_retry),
                    startDownload,
                )
                UpdateUi.None -> Unit
            }
        }
    }
}

@Composable
private fun ActionLine(text: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onClick) { Text(action) }
    }
}
