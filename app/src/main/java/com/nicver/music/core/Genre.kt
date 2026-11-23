package com.nicver.music.core

/** Daftar genre resmi. Genre kosong / tidak dikenal masuk ke tempat khusus [OTHER_GENRE]. */
val GENRE_LIST: List<String> = listOf(
    "Pop", "Rock", "Hip-Hop", "R&B", "Jazz", "Klasik", "Elektronik", "Dangdut",
    "Indie", "Metal", "Country", "Reggae", "Folk", "Soundtrack"
)

const val OTHER_GENRE = "Lainnya"

/** Kata kunci -> genre resmi. Urutan = prioritas (yang lebih spesifik di atas). */
private val GENRE_WORDS: List<Pair<String, String>> = listOf(
    "metal" to "Metal", "dangdut" to "Dangdut", "koplo" to "Dangdut", "jazz" to "Jazz",
    "rap" to "Hip-Hop", "hip" to "Hip-Hop", "hiphop" to "Hip-Hop", "rnb" to "R&B", "soul" to "R&B",
    "electronic" to "Elektronik", "electronica" to "Elektronik", "elektronik" to "Elektronik",
    "edm" to "Elektronik", "techno" to "Elektronik", "house" to "Elektronik", "trance" to "Elektronik",
    "classical" to "Klasik", "klasik" to "Klasik", "reggae" to "Reggae", "country" to "Country",
    "folk" to "Folk", "soundtrack" to "Soundtrack", "ost" to "Soundtrack",
    "indie" to "Indie", "rock" to "Rock", "pop" to "Pop"
)

private val GENRE_ALIAS: Map<String, String> = mapOf(
    "r&b" to "R&B", "r and b" to "R&B", "rhythm and blues" to "R&B", "hip-hop" to "Hip-Hop",
    "alternative" to "Indie", "alt" to "Indie", "dance" to "Elektronik", "lo-fi" to "Elektronik",
    "lofi" to "Elektronik", "acoustic" to "Folk", "singer-songwriter" to "Folk", "k-pop" to "Pop",
    "j-pop" to "Pop", "kpop" to "Pop", "jpop" to "Pop"
)

/** Nama genre standar ID3v1 (tag lama menyimpan angka, mis. "(13)"). */
private val ID3_GENRES: List<String> = listOf(
    "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal",
    "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno", "Industrial",
    "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop",
    "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game",
    "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative",
    "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial",
    "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta",
    "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave",
    "Psychadelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz",
    "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock"
)

private val NUMERIC_GENRE = Regex("^\\(?(\\d{1,3})\\)?$")
private val SPACES = Regex("\\s+")
private val TOKENS = Regex("[a-z0-9&]+")

/** Satu teks genre dari tag -> nama genre resmi, atau null kalau tidak ada di daftar. */
fun canonGenre(raw: String): String? {
    var k = raw.lowercase().replace('_', ' ').trim().replace(SPACES, " ")
    if (k.isEmpty()) return null
    val num = NUMERIC_GENRE.matchEntire(k)
    if (num != null) {
        val i = num.groupValues[1].toInt()
        if (i >= ID3_GENRES.size) return null
        k = ID3_GENRES[i].lowercase()
    }
    GENRE_ALIAS[k]?.let { return it }
    for (g in GENRE_LIST) if (k == g.lowercase()) return g
    val toks = TOKENS.findAll(k).map { it.value }.toSet()
    for ((word, g) in GENRE_WORDS) if (word in toks) return g
    return null
}

/** Daftar tag mentah -> daftar genre resmi (tanpa duplikat). Kosong / tidak dikenal -> [OTHER_GENRE]. */
fun canonGenres(raws: List<String>?): List<String> {
    val out = LinkedHashSet<String>()
    if (raws != null) {
        for (raw in raws) {
            for (part in raw.split(';', '/', ',')) {
                val g = canonGenre(part)
                if (g != null) out.add(g)
            }
        }
    }
    return if (out.isEmpty()) listOf(OTHER_GENRE) else out.toList()
}

/** Teks genre untuk ditampilkan. Untuk 'Lainnya' tampilkan tag mentahnya kalau ada. */
fun genreLabel(canon: List<String>, raws: List<String>?): String {
    if (canon.isNotEmpty() && canon[0] != OTHER_GENRE) return canon.joinToString(", ")
    val raw = raws?.firstOrNull { it.isNotBlank() }
    return raw ?: "Tanpa genre"
}
