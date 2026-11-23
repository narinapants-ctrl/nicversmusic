package com.nicver.music.ui

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.nicver.music.MusicViewModel
import com.nicver.music.core.formatTime
import com.nicver.music.core.genreLabel
import com.nicver.music.data.AppStore
import com.nicver.music.data.Song
import com.nicver.music.playback.SleepTimer
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(vm: MusicViewModel, actions: SongActions) {
    val song = vm.currentSong
    var sleepDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenBar("Sedang diputar", onBack = { vm.pop() })
        if (song == null) {
            EmptyHint("Belum ada lagu yang diputar.")
            return@Column
        }
        val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (landscape) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SongArt(song.artworkUri, 768, Modifier.fillMaxHeight().aspectRatio(1f), 16.dp)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    NowControls(vm, song, actions) { sleepDialog = true }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SongArt(song.artworkUri, 768, Modifier.fillMaxWidth().aspectRatio(1f), 20.dp)
                Spacer(Modifier.height(20.dp))
                NowControls(vm, song, actions) { sleepDialog = true }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (sleepDialog) SleepDialog { sleepDialog = false }
}

@Composable
private fun NowControls(vm: MusicViewModel, song: Song, actions: SongActions, onSleep: () -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val dur = vm.durationMs.coerceAtLeast(1L).toFloat()
    val pos = drag ?: vm.positionMs.toFloat().coerceIn(0f, dur)
    val isFav = AppStore.favorites.containsKey(song.key)
    val hasMv = vm.mvFor(song) != null

    Text(
        song.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
        maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
    )
    Text(
        song.artist, style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
    )
    TextButton(onClick = { actions.editGenre(song) }, modifier = Modifier.fillMaxWidth()) {
        Text("Genre: " + genreLabel(vm.genresOf(song), song.genreRaw))
    }
    Slider(
        value = pos, valueRange = 0f..dur,
        onValueChange = { drag = it },
        onValueChangeFinished = { drag?.let { vm.seekTo(it.toLong()) }; drag = null }
    )
    Row(Modifier.fillMaxWidth()) {
        Text(formatTime(pos.toLong()), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        Text(formatTime(vm.durationMs), style = MaterialTheme.typography.labelMedium)
    }
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { vm.toggleShuffle() }) {
            Icon(
                Icons.Filled.Shuffle, contentDescription = "Acak",
                tint = if (vm.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { vm.prev() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Sebelumnya", modifier = Modifier.size(34.dp))
        }
        FilledIconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(72.dp)) {
            Icon(
                if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Putar / jeda", modifier = Modifier.size(40.dp)
            )
        }
        IconButton(onClick = { vm.next() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Berikutnya", modifier = Modifier.size(34.dp))
        }
        IconButton(onClick = { vm.cycleRepeat() }) {
            Icon(
                if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Ulang",
                tint = if (vm.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { AppStore.toggleFavorite(song.key) }) {
            Icon(
                if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorit",
                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = { vm.watchMv() }, enabled = hasMv) {
            Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (hasMv) "Tonton MV" else "Tidak ada MV")
        }
        TextButton(onClick = onSleep) {
            Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(sleepLabel())
        }
    }
}

@Composable
private fun sleepLabel(): String {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(SleepTimer.endsAt) {
        while (SleepTimer.endsAt != 0L) {
            now = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }
    if (SleepTimer.endsAt == 0L) return "Pewaktu tidur"
    val left = SleepTimer.remainingMs(now)
    return "Tidur " + formatTime(left)
}

@Composable
private fun SleepDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Pewaktu tidur") },
        text = {
            Column {
                for (m in listOf(15, 30, 45, 60, 90)) {
                    TextButton(onClick = { SleepTimer.start(m); onClose() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Jeda dalam $m menit")
                    }
                }
                if (SleepTimer.endsAt != 0L) {
                    TextButton(onClick = { SleepTimer.cancel(); onClose() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Matikan pewaktu")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Tutup") } }
    )
}
