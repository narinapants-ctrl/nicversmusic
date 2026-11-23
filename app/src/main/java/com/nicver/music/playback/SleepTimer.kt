package com.nicver.music.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/** Pewaktu tidur: menjeda musik setelah N menit. Hidup selama proses aplikasi hidup. */
object SleepTimer {
    private val handler = Handler(Looper.getMainLooper())

    /** Waktu (elapsedRealtime) saat timer habis; 0 = mati. */
    var endsAt by mutableLongStateOf(0L)
        private set

    private val fire = Runnable {
        endsAt = 0L
        PlaybackService.pauseNow()
    }

    fun start(minutes: Int) {
        handler.removeCallbacks(fire)
        endsAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        handler.postDelayed(fire, minutes * 60_000L)
    }

    fun cancel() {
        handler.removeCallbacks(fire)
        endsAt = 0L
    }

    fun remainingMs(now: Long = SystemClock.elapsedRealtime()): Long =
        if (endsAt == 0L) 0L else (endsAt - now).coerceAtLeast(0L)
}
