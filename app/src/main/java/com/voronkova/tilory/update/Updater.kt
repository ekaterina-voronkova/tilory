package com.voronkova.tilory.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Self-update from GitHub releases (#20). The system DownloadManager does the download, so it goes on when the app
 * is minimised or killed. The download id and the expected version live in SharedPreferences, so a restarted app
 * can still pick up the finished file.
 */
class Updater(context: Context) {
    private val app = context.applicationContext
    private val dm = app.getSystemService(DownloadManager::class.java)
    private val prefs = app.getSharedPreferences("update", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    val installedVersion: String =
        runCatching { app.packageManager.getPackageInfo(app.packageName, 0).versionName }.getOrNull() ?: "0"

    /** One request to the releases API. Blocking: call off the main thread. Null on any failure. */
    fun check(): AvailableUpdate? = runCatching {
        val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Tilory-Android")
            if (conn.responseCode != 200) return@runCatching null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            pickUpdate(json.decodeFromString(ListSerializer(GhRelease.serializer()), body), installedVersion)
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    data class Pending(val id: Long, val version: String, val size: Long)

    sealed interface Status {
        data class Running(val done: Long, val total: Long) : Status
        data class Done(val file: File) : Status
        data class Failed(val reason: Int) : Status
        /** Cancelled from the notification shade or the Downloads app. */
        data object Gone : Status
    }

    fun pending(): Pending? {
        val id = prefs.getLong(KEY_ID, -1L)
        val version = prefs.getString(KEY_VERSION, null)
        if (id < 0 || version == null) return null
        return Pending(id, version, prefs.getLong(KEY_SIZE, 0L))
    }

    /** Queues the download, replacing any older one. A running download for the same version is kept. */
    fun enqueue(update: AvailableUpdate) {
        pending()?.let { p ->
            if (p.version == update.version && status(p) is Status.Running) return
        }
        forget()
        val request = DownloadManager.Request(Uri.parse(update.url))
            .setMimeType(APK_MIME)
            .setTitle("Tilory ${update.version}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalFilesDir(app, DIR, "tilory-${update.version}.apk")
        val id = dm.enqueue(request)
        prefs.edit()
            .putLong(KEY_ID, id)
            .putString(KEY_VERSION, update.version)
            .putLong(KEY_SIZE, update.size)
            .putBoolean(KEY_OPENED, false)
            .commit()
    }

    fun status(p: Pending): Status {
        dm.query(DownloadManager.Query().setFilterById(p.id))?.use { c ->
            if (!c.moveToFirst()) return Status.Gone
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val local = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    val path = local?.let { Uri.parse(it).path }
                    if (path == null) Status.Failed(DownloadManager.ERROR_FILE_ERROR) else Status.Done(File(path))
                }
                DownloadManager.STATUS_FAILED ->
                    Status.Failed(c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)))
                else -> Status.Running(
                    c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0),
                    c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        .takeIf { it > 0 } ?: p.size,
                )
            }
        }
        return Status.Gone
    }

    /** True when [file] is a Tilory package of the promised version; catches truncated or wrong files early. */
    fun verify(file: File, version: String): Boolean {
        @Suppress("DEPRECATION")
        val info = app.packageManager.getPackageArchiveInfo(file.path, 0) ?: return false
        return info.packageName == app.packageName && info.versionName == version
    }

    fun installIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Whether the installer was already opened automatically for the current download. */
    var openedOnce: Boolean
        get() = prefs.getBoolean(KEY_OPENED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_OPENED, value).commit()
        }

    /** Removes the download (and its file) and the saved id. */
    fun forget() {
        pending()?.let { dm.remove(it.id) }
        prefs.edit().clear().commit()
    }

    /** After the update is installed, the stored download is no longer needed. */
    fun cleanUpIfInstalled() {
        val p = pending() ?: return
        if (Versions.compare(p.version, installedVersion) <= 0) forget()
    }

    companion object {
        const val RELEASES_URL = "https://api.github.com/repos/ekaterina-voronkova/tilory/releases?per_page=10"
        const val APK_MIME = "application/vnd.android.package-archive"
        const val DIR = "update"
        private const val KEY_ID = "id"
        private const val KEY_VERSION = "version"
        private const val KEY_SIZE = "size"
        private const val KEY_OPENED = "opened"
    }
}
