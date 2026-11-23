package com.nicver.music.core

/**
 * Pencocokan otomatis lagu <-> file video MV (tanpa dependensi Android, jadi bisa diuji di JVM).
 *
 * Aturan (berurutan):
 * 1. Folder sama + nama file sama (tanpa ekstensi), huruf besar/kecil diabaikan.
 * 2. Nama video (setelah dinormalkan) sama dengan nama file lagu, judul, "artis judul", "judul artis".
 *    Teks dalam kurung seperti (Official Video) atau [HD] diabaikan.
 */
private val BRACKETS = Regex("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}")
private val NON_ALNUM = Regex("[^\\p{L}\\p{Nd}]+")

/** Nama video yang sering diberi embel-embel; dibuang hanya pada sisi VIDEO. */
private val VIDEO_NOISE = setOf("official", "video", "music", "mv", "hd", "4k", "lyric", "lyrics", "lirik", "clip")

/** Huruf kecil, buang isi kurung, simbol -> spasi, spasi ganda dirapikan. */
fun normName(s: String): String =
    NON_ALNUM.replace(BRACKETS.replace(s.lowercase(), " "), " ").trim()

fun normVideo(s: String): String =
    normName(s).split(' ').filter { it.isNotEmpty() && it !in VIDEO_NOISE }.joinToString(" ")

fun baseNameOf(fileName: String): String {
    val i = fileName.lastIndexOf('.')
    return if (i > 0) fileName.substring(0, i) else fileName
}

fun folderKey(folder: String, base: String): String =
    folder.lowercase().replace('\\', '/').trim('/') + "|" + base.lowercase()

class MvEntry<T>(val ref: T, val base: String, val folder: String)

class MvIndex<T>(entries: List<MvEntry<T>>) {
    private val byFolderBase = HashMap<String, T>()
    private val byNorm = HashMap<String, T>()

    init {
        for (e in entries) {
            byFolderBase.putIfAbsent(folderKey(e.folder, e.base), e.ref)
            for (n in listOf(normName(e.base), normVideo(e.base))) {
                if (n.isNotEmpty()) byNorm.putIfAbsent(n, e.ref)
            }
        }
    }

    fun find(title: String, artist: String, audioBase: String, audioFolder: String): T? {
        byFolderBase[folderKey(audioFolder, audioBase)]?.let { return it }
        val cands = LinkedHashSet<String>()
        cands.add(normName(audioBase))
        cands.add(normName(title))
        cands.add(normName("$artist $title"))
        cands.add(normName("$title $artist"))
        for (c in cands) {
            if (c.isEmpty()) continue
            val hit = byNorm[c]
            if (hit != null) return hit
        }
        return null
    }
}
