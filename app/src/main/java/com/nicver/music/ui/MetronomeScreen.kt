package com.nicver.music.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicver.music.MusicViewModel
import com.nicver.music.core.MetronomeMath
import com.nicver.music.core.TapTempo
import com.nicver.music.playback.MetronomeEngine
import kotlin.math.roundToInt

private fun tempoName(bpm: Int): String = when {
    bpm < 60 -> "Largo"
    bpm < 76 -> "Adagio"
    bpm < 108 -> "Andante"
    bpm < 120 -> "Moderato"
    bpm < 156 -> "Allegro"
    bpm < 176 -> "Vivace"
    else -> "Presto"
}

@Composable
fun MetronomeScreen(vm: MusicViewModel) {
    var bpm by rememberSaveable { mutableIntStateOf(100) }
    var beats by rememberSaveable { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }
    var active by remember { mutableIntStateOf(-1) }
    val engine = remember { MetronomeEngine { active = it } }
    val tap = remember { TapTempo() }

    SideEffect {
        engine.bpm = bpm
        engine.beatsPerBar = beats
    }
    DisposableEffect(Unit) { onDispose { engine.stop() } }

    Column(Modifier.fillMaxSize()) {
        ScreenBar("Metronom", onBack = { vm.pop() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Text("$bpm", fontSize = 88.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("BPM · ${tempoName(bpm)}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { bpm = MetronomeMath.clampBpm(bpm - 5) }) { Text("−5") }
                IconButton(onClick = { bpm = MetronomeMath.clampBpm(bpm - 1) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Kurangi 1")
                }
                IconButton(onClick = { bpm = MetronomeMath.clampBpm(bpm + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah 1")
                }
                OutlinedButton(onClick = { bpm = MetronomeMath.clampBpm(bpm + 5) }) { Text("+5") }
            }
            Slider(
                value = bpm.toFloat(),
                onValueChange = { bpm = MetronomeMath.clampBpm(it.roundToInt()) },
                valueRange = MetronomeMath.MIN_BPM.toFloat()..MetronomeMath.MAX_BPM.toFloat()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until beats) {
                    val on = running && active == i
                    val big = i == 0
                    Box(
                        Modifier
                            .size(if (big) 30.dp else 24.dp)
                            .clip(CircleShape)
                            .background(
                                if (on) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (n in listOf(2, 3, 4, 6)) {
                    FilterChip(selected = beats == n, onClick = { beats = n }, label = { Text("$n ketukan") })
                }
            }
            Spacer(Modifier.height(24.dp))
            FilledIconButton(
                onClick = {
                    if (running) {
                        engine.stop(); running = false; active = -1
                    } else {
                        engine.bpm = bpm; engine.beatsPerBar = beats
                        engine.start(); running = true
                    }
                },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    if (running) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (running) "Berhenti" else "Mulai",
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { tap.tap(SystemClock.elapsedRealtime())?.let { bpm = it } }) { Text("Ketuk tempo") }
            Text(
                "Ketuk beberapa kali mengikuti ketukan lagu untuk menebak BPM-nya. " +
                    "Metronom berbunyi bersamaan dengan musik, jadi bisa dipakai untuk mencocokkan tempo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
            )
        }
    }
}
