package com.voronkova.tilory.ui

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.voronkova.tilory.data.Mode
import com.voronkova.tilory.data.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The phone's theme colours: wallpaper colours on Android 12+, the default Material scheme below (#30). */
@Composable
fun TiloryTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
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

    // "Next puzzle" after a landscape photo: turn to portrait first, then open the picker (#52).
    var pickingNext by remember { mutableStateOf(false) }
    var launchNextPicker by remember { mutableStateOf(false) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Cancelled: back to the completion screen's orientation. Picked: stay portrait until the options screen.
        if (uri == null) pickingNext = false else vm.importPhoto(uri)
    }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importPhoto)
    }
    val pickGallery = {
        gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val pickFiles = { files.launch(arrayOf("image/*")) }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(vm::addSource)
    }

    BackHandler(enabled = screen != Screen.Home) { vm.back() }

    // Landscape photos turn the puzzle and completion screens to landscape, so the board is bigger (#39).
    val data by vm.data.collectAsStateWithLifecycle()
    val image by vm.image.collectAsStateWithLifecycle()
    val photoIsLandscape = when (val s = screen) {
        Screen.Puzzle -> data.current?.photo?.let { it.width > it.height } ?: image?.let { it.width > it.height }
        is Screen.Completion -> s.entry.photo.let { it.width > it.height }
        else -> null
    } ?: false
    val wantLandscape = data.settings.rotateLandscape && photoIsLandscape
    val activity = LocalContext.current as? Activity
    LaunchedEffect(screen, error) { if (screen !is Screen.Completion || error) pickingNext = false }
    val orientation = when {
        pickingNext -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        wantLandscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    LaunchedEffect(orientation) { activity?.requestedOrientation = orientation }
    // The picker opens only once the screen has actually turned, so it never shows up in landscape (#52).
    val landscapeNow = isLandscape()
    LaunchedEffect(launchNextPicker, landscapeNow) {
        if (launchNextPicker && !landscapeNow) {
            launchNextPicker = false
            pickGallery()
        }
    }

    // A Surprise puzzle stays a closed box: no picture in the recent-apps thumbnail (design §12, #4). Android 13+
    // can hide just the thumbnail, so screenshots still work.
    val secret = screen == Screen.Puzzle && data.current?.mode == Mode.SURPRISE
    LaunchedEffect(secret) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) activity?.setRecentsScreenshotEnabled(!secret)
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(vm, onPickGallery = pickGallery, onPickFiles = pickFiles)
            is Screen.Options -> OptionsScreen(vm, s.photo)
            Screen.Puzzle -> PuzzleScreen(vm)
            is Screen.Completion -> CompletionScreen(
                vm,
                s.entry,
                onNext = if (s.mode == Mode.SURPRISE) {
                    vm::startSurprise
                } else {
                    {
                        pickingNext = true
                        launchNextPicker = true
                    }
                },
            )
            Screen.History -> HistoryScreen(vm)
            is Screen.Viewer -> ViewerScreen(vm, s.entry)
            Screen.Settings -> SettingsScreen(vm)
            Screen.Sources -> SourcesScreen(vm, onAddFolder = { folder.launch(null) })
            Screen.Hidden -> HiddenScreen(vm)
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

    SkipQuestionDialog(vm)
    SurpriseProblemDialog(vm)

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

/** The EXIF shooting date ("yyyy:MM:dd HH:mm:ss") in the phone's language, e.g. "12 July 2019, 14:32" (#43). */
fun formatTaken(taken: String?): String? {
    if (taken == null) return null
    val parsed = runCatching {
        java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US).apply { isLenient = false }.parse(taken)
    }.getOrNull() ?: return null
    return java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG, java.text.DateFormat.SHORT).format(parsed)
}
