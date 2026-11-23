package com.nicver.music.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** Penyimpanan ringan di SharedPreferences. Satu instance untuk seluruh proses (UI dan service). */
object AppStore {
    private lateinit var sp: SharedPreferences

    val favorites = mutableStateMapOf<String, Boolean>()
    val plays = mutableStateMapOf<String, Int>()
    val genreOverride = mutableStateMapOf<String, String>()
    val mvLinks = mutableStateMapOf<String, String>()

    var accent by mutableIntStateOf(0)
    var themeMode by mutableIntStateOf(0)   // 0 sistem, 1 terang, 2 gelap
    var sortMode by mutableIntStateOf(0)    // indeks SORT_OPTIONS

    private var ready = false

    fun init(ctx: Context) {
        if (ready) return
        ready = true
        sp = ctx.applicationContext.getSharedPreferences("nicver_music", Context.MODE_PRIVATE)
        loadMap("fav") { k, v -> if (v is Boolean && v) favorites[k] = true }
        loadMap("plays") { k, v -> if (v is Number) plays[k] = v.toInt() }
        loadMap("genre") { k, v -> if (v is String) genreOverride[k] = v }
        loadMap("mv") { k, v -> if (v is String) mvLinks[k] = v }
        accent = sp.getInt("accent", 0)
        themeMode = sp.getInt("themeMode", 0)
        sortMode = sp.getInt("sortMode", 0)
    }

    private fun loadMap(name: String, put: (String, Any) -> Unit) {
        val s = sp.getString(name, null) ?: return
        try {
            val o = JSONObject(s)
            for (k in o.keys()) put(k, o.get(k))
        } catch (_: Exception) {
        }
    }

    private fun saveMap(name: String, m: Map<String, Any>) {
        val o = JSONObject()
        for ((k, v) in m) o.put(k, v)
        sp.edit().putString(name, o.toString()).apply()
    }

    fun toggleFavorite(key: String) {
        if (favorites.containsKey(key)) favorites.remove(key) else favorites[key] = true
        saveMap("fav", favorites.toMap())
    }

    fun addPlay(key: String) {
        plays[key] = (plays[key] ?: 0) + 1
        saveMap("plays", plays.toMap())
    }

    fun setGenre(key: String, genre: String?) {
        if (genre == null) genreOverride.remove(key) else genreOverride[key] = genre
        saveMap("genre", genreOverride.toMap())
    }

    fun setMv(key: String, uri: String?) {
        if (uri == null) mvLinks.remove(key) else mvLinks[key] = uri
        saveMap("mv", mvLinks.toMap())
    }

    fun saveSettings() {
        sp.edit().putInt("accent", accent).putInt("themeMode", themeMode).putInt("sortMode", sortMode).apply()
    }
}
