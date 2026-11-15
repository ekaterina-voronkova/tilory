package com.voronkova.tilory.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class FailReason { SPACE, NETWORK, BAD_FILE, OTHER }

/** What the update row on the main screen shows. */
sealed interface UpdateUi {
    data object None : UpdateUi
    data class Available(val version: String, val size: Long) : UpdateUi
    data class Downloading(val version: String, val done: Long, val total: Long) : UpdateUi {
        val percent get() = if (total > 0) (done * 100 / total).toInt() else 0
    }
    data class Ready(val version: String) : UpdateUi
    data class Failed(val reason: FailReason) : UpdateUi
}

/**
 * Drives the update row (#20): checks once per launch, polls the download every 0.5 s while the app is on screen,
 * and asks the activity to open the installer when the file is ready.
 */
class UpdateController(context: Context, private val scope: CoroutineScope) {
    private val updater = Updater(context)
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow<UpdateUi>(UpdateUi.None)
    val state: StateFlow<UpdateUi> = _state.asStateFlow()

    private val _installRequests = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val installRequests: SharedFlow<Intent> = _installRequests.asSharedFlow()

    @Volatile
    private var available: AvailableUpdate? = null
    private var pollJob: Job? = null
    private var resumed = false

    init {
        scope.launch(Dispatchers.IO) {
            updater.cleanUpIfInstalled()
            refresh(auto = false)
            available = updater.check()
            refresh(auto = false)
        }
    }

    fun onResume() {
        resumed = true
        startPolling()
    }

    fun onPause() {
        resumed = false
        pollJob?.cancel()
        pollJob = null
    }

    fun download() {
        val update = available ?: return
        scope.launch(Dispatchers.IO) {
            updater.enqueue(update)
            refresh(auto = false)
            if (resumed) startPolling()
        }
    }

    fun install() {
        scope.launch(Dispatchers.IO) {
            val p = updater.pending() ?: return@launch
            val s = updater.status(p)
            if (s is Updater.Status.Done) openInstaller(p, s.file)
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                refresh(auto = true)
                if (_state.value !is UpdateUi.Downloading) break
                delay(500)
            }
        }
    }

    private fun refresh(auto: Boolean) {
        val p = updater.pending()
        if (p == null) {
            if (_state.value !is UpdateUi.Failed) showAvailable()
            return
        }
        when (val s = updater.status(p)) {
            is Updater.Status.Running -> {
                val next = UpdateUi.Downloading(p.version, s.done, s.total)
                val old = _state.value
                // Only redraw when the percentage changes.
                if (old !is UpdateUi.Downloading || old.percent != next.percent) _state.value = next
            }
            is Updater.Status.Done -> {
                _state.value = UpdateUi.Ready(p.version)
                if (auto && resumed && !updater.openedOnce) openInstaller(p, s.file)
            }
            is Updater.Status.Failed -> {
                updater.forget()
                _state.value = UpdateUi.Failed(reasonOf(s.reason))
            }
            Updater.Status.Gone -> {
                updater.forget()
                showAvailable()
            }
        }
    }

    private fun showAvailable() {
        _state.value = available?.let { UpdateUi.Available(it.version, it.size) } ?: UpdateUi.None
    }

    private fun openInstaller(p: Updater.Pending, file: File) {
        if (!updater.verify(file, p.version)) {
            updater.forget()
            _state.value = UpdateUi.Failed(FailReason.BAD_FILE)
            return
        }
        UpdateNotifications.cancel(appContext)
        _installRequests.tryEmit(updater.installIntent(file))
        // Keep the id until the new version is installed, so "Install" stays available if the installer is closed.
        updater.openedOnce = true
    }

    private fun reasonOf(code: Int) = when (code) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> FailReason.SPACE
        DownloadManager.ERROR_HTTP_DATA_ERROR,
        DownloadManager.ERROR_CANNOT_RESUME,
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE,
        DownloadManager.ERROR_TOO_MANY_REDIRECTS,
        in 400..599,
        -> FailReason.NETWORK
        else -> FailReason.OTHER
    }
}
