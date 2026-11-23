package com.nicver.music.data

import android.net.Uri

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val folder: String,
    val fileName: String,
    val dateAdded: Long,
    val genreRaw: List<String>,
) {
    /** Kunci stabil untuk favorit / hitungan putar / tautan MV. */
    val key: String get() = "$folder/$fileName"
    /** URI lagu itu sendiri; sampulnya diambil lewat [Art] (thumbnail MediaStore / tag tertanam). */
    val artworkUri: Uri get() = uri
}

data class VideoFile(val uri: Uri, val name: String, val folder: String)

data class Library(
    val songs: List<Song> = emptyList(),
    val videos: List<VideoFile> = emptyList(),
)
