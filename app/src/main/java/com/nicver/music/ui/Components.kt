package com.nicver.music.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nicver.music.MusicViewModel
import com.nicver.music.data.AppStore
import com.nicver.music.data.Art
import com.nicver.music.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sampul album dengan placeholder. [px] = ukuran bitmap yang diminta. */
@Composable
fun SongArt(uri: Uri?, px: Int, modifier: Modifier = Modifier, corner: Dp = 10.dp) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = uri?.let { Art.cached(it, px) }, uri, px) {
        value = if (uri == null) null else withContext(Dispatchers.IO) { Art.load(ctx, uri, px) }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Filled.MusicNote, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxSize(0.5f)
            )
        }
    }
}

/** Aksi yang butuh launcher / dialog di tingkat atas. */
class SongActions(
    val editGenre: (Song) -> Unit,
    val linkMv: (Song) -> Unit,
)

@Composable
fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SongRow(
    vm: MusicViewModel,
    song: Song,
    actions: SongActions,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val playing = vm.currentKey == song.key
    val hasMv = vm.mvFor(song) != null
    val isFav = AppStore.favorites.containsKey(song.key)
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SongArt(song.artworkUri, 128, Modifier.size(48.dp), 8.dp)
        Column(Modifier.weight(1f)) {
            Text(
                song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (playing) FontWeight.Bold else FontWeight.Normal,
                color = if (playing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle ?: song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (hasMv) Icon(
            Icons.Filled.Videocam, contentDescription = "Ada MV",
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
        )
        if (isFav) Icon(
            Icons.Filled.Favorite, contentDescription = "Favorit",
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
        )
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Menu") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (isFav) "Hapus dari favorit" else "Tambah ke favorit") },
                    leadingIcon = { Icon(if (isFav) Icons.Filled.FavoriteBorder else Icons.Filled.Favorite, null) },
                    onClick = { menu = false; AppStore.toggleFavorite(song.key) }
                )
                DropdownMenuItem(
                    text = { Text("Ubah genre") },
                    onClick = { menu = false; actions.editGenre(song) }
                )
                if (hasMv) DropdownMenuItem(
                    text = { Text("Tonton MV") },
                    onClick = { menu = false; vm.playMv(listOf(song), 0) }
                )
                DropdownMenuItem(
                    text = { Text("Pilih file MV manual") },
                    onClick = { menu = false; actions.linkMv(song) }
                )
                if (AppStore.mvLinks.containsKey(song.key)) DropdownMenuItem(
                    text = { Text("Lepas tautan MV") },
                    onClick = { menu = false; AppStore.setMv(song.key, null) }
                )
            }
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
