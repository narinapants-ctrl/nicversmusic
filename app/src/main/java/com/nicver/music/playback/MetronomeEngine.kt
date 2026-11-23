package com.nicver.music.playback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.nicver.music.core.MetronomeMath

/** Metronom: AudioTrack mode stream, satu buffer per ketukan, ketukan 1 diberi aksen. */
class MetronomeEngine(private val onBeat: (beatIndex: Int) -> Unit) {
    private val sr = 44100
    private val ui = Handler(Looper.getMainLooper())
    private var thread: Thread? = null

    @Volatile var bpm = 100
    @Volatile var beatsPerBar = 4
    @Volatile private var running = false

    val isRunning: Boolean get() = running

    fun start() {
        if (running) return
        running = true
        val t = Thread {
            val accent = MetronomeMath.click(sr, 1500.0, 40, 0.9)
            val normal = MetronomeMath.click(sr, 1000.0, 40, 0.7)
            val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val bufBytes = maxOf(minBuf, sr / 5 * 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            var beat = 0
            val bufMs = (bufBytes / 2) * 1000L / sr
            while (running) {
                val b = MetronomeMath.beatBuffer(sr, bpm, if (beat == 0) accent else normal)
                val idx = beat
                track.write(b, 0, b.size)   // blok sampai ada ruang
                val beatMs = b.size * 1000L / sr
                ui.postDelayed({ if (running) onBeat(idx) }, maxOf(0L, bufMs - beatMs))
                beat = (beat + 1) % beatsPerBar.coerceAtLeast(1)
            }
            try {
                track.pause(); track.flush(); track.release()
            } catch (_: Exception) {
            }
        }
        t.isDaemon = true
        thread = t
        t.start()
    }

    fun stop() {
        running = false
        try {
            thread?.join(500)
        } catch (_: InterruptedException) {
        }
        thread = null
    }
}
