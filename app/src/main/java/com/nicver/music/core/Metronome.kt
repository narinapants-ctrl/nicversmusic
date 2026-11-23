package com.nicver.music.core

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

object MetronomeMath {
    const val MIN_BPM = 30
    const val MAX_BPM = 300

    fun clampBpm(bpm: Int): Int = bpm.coerceIn(MIN_BPM, MAX_BPM)

    fun framesPerBeat(sampleRate: Int, bpm: Int): Int = (sampleRate * 60.0 / clampBpm(bpm)).toInt()

    /** Bunyi 'tik': gelombang sinus yang meredam cepat. */
    fun click(sampleRate: Int, freqHz: Double, durationMs: Int, amplitude: Double): ShortArray {
        val n = sampleRate * durationMs / 1000
        val out = ShortArray(n)
        for (i in 0 until n) {
            val env = exp(-6.0 * i / n)
            val v = sin(2.0 * PI * freqHz * i / sampleRate) * env * amplitude
            out[i] = (v * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
        }
        return out
    }

    /** Satu ketukan penuh: bunyi di awal, sisanya hening. Panjangnya persis satu ketukan. */
    fun beatBuffer(sampleRate: Int, bpm: Int, click: ShortArray): ShortArray {
        val n = framesPerBeat(sampleRate, bpm)
        val buf = ShortArray(n)
        System.arraycopy(click, 0, buf, 0, minOf(click.size, n))
        return buf
    }
}

/** Ketuk berulang untuk menentukan tempo. Jeda > 2 detik memulai hitungan baru. */
class TapTempo {
    private val taps = ArrayDeque<Long>()

    fun tap(nowMs: Long): Int? {
        if (taps.isNotEmpty() && nowMs - taps.last() > 2000) taps.clear()
        taps.addLast(nowMs)
        while (taps.size > 6) taps.removeFirst()
        if (taps.size < 2) return null
        val avg = (taps.last() - taps.first()).toDouble() / (taps.size - 1)
        if (avg <= 0.0) return null
        return MetronomeMath.clampBpm((60000.0 / avg).roundToInt())
    }
}
