package com.voronkova.tilory.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voronkova.tilory.R
import com.voronkova.tilory.data.ColorSource
import com.voronkova.tilory.data.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Wallpaper colours exist only on Android 12+. */
val wallpaperColorsAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun TiloryTheme(colors: ColorSource, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        colors == ColorSource.WALLPAPER && wallpaperColorsAvailable ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> TiloryDark
        else -> TiloryLight
    }
    MaterialTheme(colorScheme = scheme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

@Composable
fun TiloryApp(vm: TiloryViewModel) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::importPhoto)
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importPhoto)
    }
    val pickGallery = {
        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val pickFiles = { files.launch(arrayOf("image/*")) }

    BackHandler(enabled = screen != Screen.Home) { vm.back() }

    Box(Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(vm, onPickGallery = pickGallery, onPickFiles = pickFiles)
            is Screen.Options -> OptionsScreen(vm, s.photo)
            Screen.Puzzle -> PuzzleScreen(vm)
            is Screen.Completion -> CompletionScreen(vm, s.entry, onNext = pickGallery)
            Screen.History -> HistoryScreen(vm)
            is Screen.Viewer -> ViewerScreen(vm, s.entry)
            Screen.Settings -> SettingsScreen(vm)
        }

        if (busy) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.loading))
                    }
                }
            }
        }
    }

    if (error) {
        AlertDialog(
            onDismissRequest = vm::clearError,
            confirmButton = { TextButton(onClick = vm::clearError) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.import_failed)) },
        )
    }
}

/** Loads a stored photo off the main thread; [thumbnail] gives a smaller copy for lists. */
@Composable
fun rememberPhoto(vm: TiloryViewModel, photo: Photo, thumbnail: Boolean): ImageBitmap? {
    var bitmap by remember(photo, thumbnail) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photo, thumbnail) {
        bitmap = withContext(Dispatchers.IO) {
            (if (thumbnail) vm.photos.loadThumbnail(photo) else vm.photos.load(photo))?.asImageBitmap()
        }
    }
    return bitmap
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
