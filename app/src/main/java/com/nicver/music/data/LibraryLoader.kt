package com.nicver.music.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

object LibraryLoader {

    fun load(ctx: Context): Library {
        val cr = ctx.contentResolver
        return Library(loadSongs(cr), loadVideos(cr))
    }

    private fun parentOf(path: String?): String {
        if (path == null) return ""
        val i = path.lastIndexOf('/')
        return if (i > 0) path.substring(0, i) else ""
    }

    /** audioId -> daftar nama genre mentah dari tag. */
    private fun loadGenres(cr: ContentResolver): Map<Long, List<String>> {
        val out = HashMap<Long, MutableList<String>>()
        try {
            val gUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            cr.query(gUri, arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME), null, null, null)?.use { g ->
                while (g.moveToNext()) {
                    val gid = g.getLong(0)
                    val name = g.getString(1) ?: continue
                    val mUri = MediaStore.Audio.Genres.Members.getContentUri("external", gid)
                    cr.query(mUri, arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null)?.use { m ->
                        while (m.moveToNext()) out.getOrPut(m.getLong(0)) { mutableListOf() }.add(name)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun loadSongs(cr: ContentResolver): List<Song> {
        val genres = loadGenres(cr)
        val list = ArrayList<Song>()
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
        )
        try {
            cr.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 30000",
                null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(7) ?: continue
                    val artist = c.getString(2)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Artis tidak diketahui"
                    list.add(
                        Song(
                            id = id,
                            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            title = c.getString(1)?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.'),
                            artist = artist,
                            album = c.getString(3)?.takeIf { it.isNotBlank() } ?: "Album tidak diketahui",
                            albumId = c.getLong(4),
                            durationMs = c.getLong(5),
                            dateAdded = c.getLong(6),
                            fileName = name,
                            folder = parentOf(c.getString(8)),
                            genreRaw = genres[id] ?: emptyList(),
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
        }
        return list
    }

    private fun loadVideos(cr: ContentResolver): List<VideoFile> {
        val list = ArrayList<VideoFile>()
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
        )
        try {
            cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val name = c.getString(1) ?: continue
                    list.add(
                        VideoFile(
                            ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                            name, parentOf(c.getString(2))
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
        }
        return list
    }
}
