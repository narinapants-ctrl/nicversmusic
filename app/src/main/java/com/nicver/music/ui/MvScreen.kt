package com.nicver.music.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import com.nicver.music.MusicViewModel

@Composable
fun MvScreen(vm: MusicViewModel) {
    val activity = LocalContext.current as? Activity
    val view = LocalView.current

    LaunchedEffect(Unit) { vm.enterMv() }
    DisposableEffect(Unit) {
        onDispose {
            vm.leaveMv()
            vm.mvFullscreen = false
        }
    }
    DisposableEffect(vm.mvFullscreen) {
        val window = activity?.window
        val ic = if (window != null) WindowCompat.getInsetsController(window, view) else null
        if (vm.mvFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ic?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ic?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ic?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val song = vm.currentSong
    if (vm.mvFullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoArea(vm, Modifier.fillMaxSize())
            IconButton(onClick = { vm.mvFullscreen = false }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.FullscreenExit, contentDescription = "Keluar layar penuh", tint = Color.White)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        ScreenBar("Tonton MV", onBack = { vm.pop() })
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            VideoArea(vm, Modifier.fillMaxSize())
            if (vm.isMvNow) {
                IconButton(onClick = { vm.mvFullscreen = true }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Layar penuh", tint = Color.White)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                song?.title ?: "—", style = MaterialTheme.typography.titleLarge,
                maxLines = 2, textAlign = TextAlign.Center
            )
            Text(
                song?.artist ?: "", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { vm.prev() }) { Icon(Icons.Filled.SkipPrevious, contentDescription = "Sebelumnya") }
                IconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Putar / jeda", modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.next() }) { Icon(Icons.Filled.SkipNext, contentDescription = "Berikutnya") }
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { vm.pop() }) { Text("Kembali ke audio saja") }
            Text(
                "Keluar dari layar ini, lagu tetap berlanjut sebagai audio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun VideoArea(vm: MusicViewModel, modifier: Modifier) {
    val c = vm.controller
    if (vm.isMvNow && c != null) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx) },
            update = { it.player = c },
            onRelease = { it.player = null },
            modifier = modifier
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            val song = vm.currentSong
            Text(
                if (song != null && vm.mvFor(song) == null) "Lagu ini belum punya MV"
                else "Memuat MV…",
                color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)
            )
        }
    }
}
