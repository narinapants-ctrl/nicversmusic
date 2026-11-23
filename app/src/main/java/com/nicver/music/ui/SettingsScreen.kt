package com.nicver.music.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nicver.music.MusicViewModel
import com.nicver.music.core.SORT_OPTIONS
import com.nicver.music.data.AppStore

@Composable
fun SettingsScreen(vm: MusicViewModel) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenBar("Pengaturan", onBack = { vm.pop() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            SectionLabel("Tema")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                THEME_MODES.forEachIndexed { i, n ->
                    FilterChip(
                        selected = AppStore.themeMode == i,
                        onClick = { AppStore.themeMode = i; AppStore.saveSettings() },
                        label = { Text(n) }
                    )
                }
            }
            SectionLabel("Warna aksen")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ACCENTS.forEachIndexed { i, a ->
                    val sel = AppStore.accent == i
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(a.color)
                            .border(
                                if (sel) 3.dp else 0.dp,
                                if (sel) MaterialTheme.colorScheme.onSurface else a.color,
                                CircleShape
                            )
                            .clickable { AppStore.accent = i; AppStore.saveSettings() }
                    )
                }
            }
            Text(
                ACCENTS[AppStore.accent.coerceIn(0, ACCENTS.lastIndex)].name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            SectionLabel("Urutan lagu")
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SORT_OPTIONS.forEachIndexed { i, n ->
                    FilterChip(
                        selected = AppStore.sortMode == i,
                        onClick = { AppStore.sortMode = i; AppStore.saveSettings() },
                        label = { Text(n) }
                    )
                }
            }
            SectionLabel("Perpustakaan")
            Text(
                "${vm.library.songs.size} lagu · ${vm.library.videos.size} video terdeteksi",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { vm.reload() }, enabled = !vm.loading) {
                    Text(if (vm.loading) "Memuat…" else "Muat ulang")
                }
                OutlinedButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text("Izin aplikasi") }
            }
            Text(
                "Genre yang kamu ubah hanya tersimpan di aplikasi ini, file lagu tidak diubah.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SectionLabel("Tentang")
            Text("Nicver's Music · versi 1.0", modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}
