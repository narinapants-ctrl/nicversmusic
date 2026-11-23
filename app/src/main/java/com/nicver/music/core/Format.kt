package com.nicver.music.core

/** 83000 -> "1:23". Nilai tidak valid -> "0:00". */
fun formatTime(ms: Long): String {
    val total = if (ms < 0) 0L else ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Urutan lagu yang tersedia di Pengaturan. */
val SORT_OPTIONS: List<String> = listOf("Judul", "Artis", "Terbaru")
