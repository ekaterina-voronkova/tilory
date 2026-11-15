package com.voronkova.tilory.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.voronkova.tilory.MainActivity
import com.voronkova.tilory.R

/** Set by MainActivity: true while the app is on screen. */
object AppState {
    @Volatile
    var foreground = false
}

object UpdateNotifications {
    private const val CHANNEL = "updates"
    private const val ID = 1

    /** "Version X downloaded — tap to install". Tapping opens the app, which opens the installer on resume. */
    fun showReady(context: Context, version: String) {
        val nm = NotificationManagerCompat.from(context)
        // POST_NOTIFICATIONS exists only on API 33+; on older systems checkSelfPermission would say "denied".
        if (!nm.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL,
            context.getString(R.string.update_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.update_notif_title, version))
            .setContentText(context.getString(R.string.update_notif_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call; the resume check still opens the installer.
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
    }
}

/**
 * The system sends DOWNLOAD_COMPLETE straight to the package that queued the download, so this reaches the app even
 * when it was killed. When the app is on screen, its own polling opens the installer instead.
 */
class DownloadDoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(android.app.DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val updater = Updater(context)
        val pending = updater.pending() ?: return
        if (pending.id != id || AppState.foreground) return
        if (updater.status(pending) is Updater.Status.Done) UpdateNotifications.showReady(context, pending.version)
    }
}
