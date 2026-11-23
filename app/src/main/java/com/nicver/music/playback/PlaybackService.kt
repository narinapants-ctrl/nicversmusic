package com.nicver.music.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.BitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.nicver.music.MainActivity
import com.nicver.music.data.AppStore
import com.nicver.music.data.Art
import java.util.concurrent.Executors

/**
 * Service pemutar. Karena ini MediaSessionService, Android otomatis menampilkan
 * kartu pemutar di Panel Kontrol / layar kunci (sampul, judul, artis, sebelumnya / jeda / berikutnya).
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())

    private var countedKey: String? = null
    private var playedMs = 0L
    private var counted = false

    private val tick = object : Runnable {
        override fun run() {
            val p = session?.player
            if (p != null && p.isPlaying) {
                val id = p.currentMediaItem?.mediaId
                if (id != countedKey) {
                    countedKey = id; playedMs = 0; counted = false
                }
                playedMs += 1000
                if (!counted && id != null && playedMs >= 30_000) {
                    counted = true
                    AppStore.addPlay(id)
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppStore.init(this)
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(open)
            .setBitmapLoader(ArtBitmapLoader(this))
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(mediaItems)
            })
            .build()
        handler.postDelayed(tick, 1000)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        instance = null
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: PlaybackService? = null

        fun pauseNow() {
            instance?.session?.player?.pause()
        }
    }
}

/** Pemuat gambar untuk notifikasi / Panel Kontrol: memakai [Art] supaya sampul tertanam ikut tampil. */
class ArtBitmapLoader(private val ctx: Context) : BitmapLoader {
    private val executor = Executors.newSingleThreadExecutor()

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val f = SettableFuture.create<Bitmap>()
        executor.execute {
            val b = Art.decode(data, 512)
            if (b != null) f.set(b) else f.setException(IllegalStateException("decode gagal"))
        }
        return f
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val f = SettableFuture.create<Bitmap>()
        executor.execute {
            val b = Art.load(ctx, uri, 512)
            if (b != null) f.set(b) else f.setException(IllegalStateException("tanpa sampul"))
        }
        return f
    }
}
