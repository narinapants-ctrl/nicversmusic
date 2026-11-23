package com.nicver.music.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nicver.music.MusicViewModel
import com.nicver.music.R
import com.nicver.music.core.OTHER_GENRE
import com.nicver.music.data.Song

private val TABS = listOf("Lagu", "Album", "Artis", "Genre", "Favorit", "Sering", "MV")

@Composable
fun ScreenBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
        Text(
            title, style = MaterialTheme.typography.titleLarge, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
fun HomeScreen(vm: MusicViewModel, actions: SongActions) {
    var searching by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(R.drawable.logo_nicvers), contentDescription = "Nicver's",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.height(24.dp)
            )
            Box(Modifier.weight(1f))
            IconButton(onClick = { searching = !searching; if (!searching) vm.query = "" }) {
                Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Cari")
            }
            IconButton(onClick = { vm.go(Route.Metronome) }) {
                Icon(Icons.Filled.Speed, contentDescription = "Metronom")
            }
            IconButton(onClick = { vm.go(Route.Settings) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Pengaturan")
            }
        }
        if (searching) {
            OutlinedTextField(
                value = vm.query, onValueChange = { vm.query = it }, singleLine = true,
                placeholder = { Text("Cari judul, artis, atau album") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        ScrollableTabRow(selectedTabIndex = vm.tab.coerceIn(0, TABS.lastIndex), edgePadding = 8.dp) {
            TABS.forEachIndexed { i, t ->
                Tab(selected = vm.tab == i, onClick = { vm.tab = i }, text = { Text(t) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (vm.tab) {
                0 -> SongList(vm, vm.filtered(vm.sortedSongs), actions, "Belum ada lagu di perangkat ini.")
                1 -> GroupList(
                    vm.albums.filter { vm.matches(it.first) }.map { (name, songs) ->
                        GroupItem(name, "${songs[0].artist} · ${songs.size} lagu", songs[0], false) {
                            vm.go(Route.Detail(Kind.Album, name, name))
                        }
                    }, "Belum ada album."
                )
                2 -> GroupList(
                    vm.artists.filter { vm.matches(it.first) }.map { (name, songs) ->
                        GroupItem(name, "${songs.size} lagu", songs[0], false) {
                            vm.go(Route.Detail(Kind.Artist, name, name))
                        }
                    }, "Belum ada artis."
                )
                3 -> GroupList(
                    vm.genreGroups.filter { vm.matches(it.first) }.map { (name, songs) ->
                        val sub = if (name == OTHER_GENRE) "${songs.size} lagu · genre kosong / tidak dikenal"
                        else "${songs.size} lagu"
                        GroupItem(name, sub, null, true) { vm.go(Route.Detail(Kind.Genre, name, name)) }
                    }, "Belum ada genre."
                )
                4 -> SongList(vm, vm.filtered(vm.favSongs), actions, "Belum ada favorit. Buka menu ⋮ pada lagu lalu pilih favorit.")
                5 -> SongList(
                    vm, vm.filtered(vm.topPlayed), actions,
                    "Belum ada riwayat. Lagu dihitung setelah diputar 30 detik.",
                    subtitle = { "${com.nicver.music.data.AppStore.plays[it.key] ?: 0}× diputar · ${it.artist}" }
                )
                else -> SongList(
                    vm, vm.filtered(vm.mvSongs), actions,
                    if (vm.videoGranted) "Belum ada MV yang cocok. Simpan video dengan nama yang sama dengan lagunya, atau pilih manual lewat menu ⋮ pada lagu."
                    else "Izin video belum diberikan, jadi MV tidak bisa dicari otomatis. Kamu tetap bisa memilih file MV manual lewat menu ⋮ pada lagu.",
                    playAsMv = true
                )
            }
        }
    }
}

@Composable
fun SongList(
    vm: MusicViewModel,
    list: List<Song>,
    actions: SongActions,
    empty: String,
    subtitle: ((Song) -> String)? = null,
    playAsMv: Boolean = false,
) {
    if (list.isEmpty()) {
        EmptyHint(empty)
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                FilledTonalButton(onClick = { vm.playShuffled(list) }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Putar acak (${list.size})")
                }
            }
        }
        itemsIndexed(list, key = { _, s -> s.id }) { i, s ->
            SongRow(vm, s, actions, subtitle?.invoke(s)) {
                if (playAsMv) vm.playMv(list, i) else vm.playList(list, i)
            }
        }
    }
}

class GroupItem(
    val title: String,
    val subtitle: String,
    val art: Song?,
    val genre: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun GroupList(items: List<GroupItem>, empty: String) {
    if (items.isEmpty()) {
        EmptyHint(empty)
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(items, key = { it.title }) { g ->
            Row(
                Modifier.fillMaxWidth().clickable(onClick = g.onClick).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (g.genre) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Category, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    SongArt(g.art?.artworkUri, 128, Modifier.size(48.dp), 8.dp)
                }
                Column(Modifier.weight(1f)) {
                    Text(g.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        g.subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DetailScreen(vm: MusicViewModel, route: Route.Detail, actions: SongActions) {
    val list = vm.songsFor(route)
    Column(Modifier.fillMaxSize()) {
        ScreenBar(route.title, onBack = { vm.pop() })
        Box(Modifier.weight(1f)) { SongList(vm, list, actions, "Tidak ada lagu.") }
    }
}
