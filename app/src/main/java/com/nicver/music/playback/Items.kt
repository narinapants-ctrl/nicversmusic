package com.nicver.music.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.nicver.music.data.Song

const val EXTRA_MV = "mv"

/** Item media untuk satu lagu. Kalau [mvUri] diisi, yang diputar adalah file video MV-nya. */
fun Song.toMediaItem(mvUri: Uri? = null): MediaItem {
    val extras = Bundle().apply { putBoolean(EXTRA_MV, mvUri != null) }
    val md = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId(key)
        .setUri(mvUri ?: uri)
        .setMediaMetadata(md)
        .build()
}

val MediaItem.isMv: Boolean get() = mediaMetadata.extras?.getBoolean(EXTRA_MV, false) == true
