package com.nicver.music.ui

enum class Kind { Album, Artist, Genre }

sealed interface Route {
    data object Now : Route
    data object Mv : Route
    data object Metronome : Route
    data object Settings : Route
    data class Detail(val kind: Kind, val key: String, val title: String) : Route
}
