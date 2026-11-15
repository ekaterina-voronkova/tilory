package com.voronkova.tilory

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.voronkova.tilory.update.AppState
import kotlinx.coroutines.launch
import com.voronkova.tilory.ui.TiloryApp
import com.voronkova.tilory.ui.TiloryTheme
import com.voronkova.tilory.ui.TiloryViewModel

class MainActivity : ComponentActivity() {
    private val vm: TiloryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleShare(intent)
        // Open the system installer when a downloaded update is ready (#20).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.update.installRequests.collect { runCatching { startActivity(it) } }
            }
        }
        setContent {
            TiloryTheme {
                TiloryApp(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AppState.foreground = true
        vm.update.onResume()
    }

    override fun onPause() {
        AppState.foreground = false
        vm.update.onPause()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** "Share → Tilory" from any app (design §9). */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return
        vm.importPhoto(uri)
    }
}
