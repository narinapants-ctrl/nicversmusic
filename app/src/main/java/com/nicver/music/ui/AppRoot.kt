package com.nicver.music.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nicver.music.MusicViewModel
import com.nicver.music.R
import com.nicver.music.core.GENRE_LIST
import com.nicver.music.core.OTHER_GENRE
import com.nicver.music.data.AppStore
import com.nicver.music.data.Song
import kotlinx.coroutines.delay

@Composable
fun AppRoot(vm: MusicViewModel) {
    val ctx = LocalContext.current
    var splash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1600)
        splash = false
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.refreshPermissions()
    }

    var genreSong by remember { mutableStateOf<Song?>(null) }
    var linkSong by remember { mutableStateOf<Song?>(null) }
    val pickMv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val s = linkSong
        if (uri != null && s != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            AppStore.setMv(s.key, uri.toString())
            vm.toast("MV ditautkan ke \"${s.title}\"")
        }
        linkSong = null
    }
    val actions = remember {
        SongActions(
            editGenre = { genreSong = it },
            linkMv = { linkSong = it; pickMv.launch(arrayOf("video/*")) }
        )
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        when {
            splash -> SplashScreen()
            !vm.audioGranted -> PermissionScreen(
                onAllow = {
                    val list = if (Build.VERSION.SDK_INT >= 33) listOf(
                        Manifest.permission.READ_MEDIA_AUDIO,
                        Manifest.permission.READ_MEDIA_VIDEO,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    permLauncher.launch(list.toTypedArray())
                },
                onSettings = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
            else -> MainContent(vm, actions)
        }
    }

    genreSong?.let { s -> GenreDialog(vm, s) { genreSong = null } }
}

@Composable
private fun MainContent(vm: MusicViewModel, actions: SongActions) {
    BackHandler(enabled = vm.back.isNotEmpty()) { vm.pop() }
    val route = vm.route
    val full = route is Route.Mv && vm.mvFullscreen
    Column(
        Modifier.fillMaxSize().then(if (full) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        Box(Modifier.weight(1f)) {
            when (route) {
                null -> if (vm.loaded) HomeScreen(vm, actions) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is Route.Now -> NowPlayingScreen(vm, actions)
                is Route.Mv -> MvScreen(vm)
                is Route.Metronome -> MetronomeScreen(vm)
                is Route.Settings -> SettingsScreen(vm)
                is Route.Detail -> DetailScreen(vm, route, actions)
            }
        }
        if (!full && route !is Route.Now && route !is Route.Mv) MiniPlayer(vm)
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0E0E10)), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.logo_nicvers), contentDescription = "Nicver's",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nicver's Music", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "\nAplikasi butuh izin untuk membaca lagu di perangkat. Izin video dipakai untuk menemukan MV " +
                "yang kamu simpan, dan izin notifikasi supaya kontrol lagu muncul di Panel Kontrol.\n",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAllow) { Text("Izinkan akses") }
        OutlinedButton(onClick = onSettings, modifier = Modifier.padding(top = 8.dp)) {
            Text("Buka pengaturan izin")
        }
    }
}

@Composable
private fun GenreDialog(vm: MusicViewModel, song: Song, onClose: () -> Unit) {
    val current = AppStore.genreOverride[song.key]
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Genre untuk \"${song.title}\"") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (g in GENRE_LIST + OTHER_GENRE) {
                    TextButton(
                        onClick = { AppStore.setGenre(song.key, g); onClose() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (g == current) "✓ $g" else g,
                            fontWeight = if (g == current) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                if (current != null) {
                    TextButton(
                        onClick = { AppStore.setGenre(song.key, null); onClose() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Kembali ke genre dari tag") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Tutup") } }
    )
}
