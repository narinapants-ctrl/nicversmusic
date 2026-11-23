package com.nicver.music.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size

/** Pemuat sampul album: thumbnail MediaStore (API 29+), lalu gambar tertanam di tag. */
object Art {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun cached(uri: Uri, px: Int): Bitmap? = cache.get("$uri@$px")

    fun load(ctx: Context, uri: Uri, px: Int): Bitmap? {
        val key = "$uri@$px"
        cache.get(key)?.let { return it }
        var bmp: Bitmap? = null
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                bmp = ctx.contentResolver.loadThumbnail(uri, Size(px, px), null)
            } catch (_: Exception) {
            }
        }
        if (bmp == null) bmp = fromTag(ctx, uri, px)
        if (bmp != null) cache.put(key, bmp)
        return bmp
    }

    fun decode(bytes: ByteArray, px: Int): Bitmap? {
        return try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
            var sample = 1
            while (o.outWidth / (sample * 2) >= px && o.outHeight / (sample * 2) >= px) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: Exception) {
            null
        }
    }

    private fun fromTag(ctx: Context, uri: Uri, px: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            r.embeddedPicture?.let { decode(it, px) }
        } catch (_: Exception) {
            null
        } finally {
            try {
                r.release()
            } catch (_: Exception) {
            }
        }
    }
}
