package com.nicver.music

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nicver.music.core.GENRE_LIST
import com.nicver.music.core.MvEntry
import com.nicver.music.core.MvIndex
import com.nicver.music.core.OTHER_GENRE
import com.nicver.music.core.baseNameOf
import com.nicver.music.core.canonGenres
import com.nicver.music.data.AppStore
import com.nicver.music.data.Library
import com.nicver.music.data.LibraryLoader
import com.nicver.music.data.Song
import com.nicver.music.data.VideoFile
import com.nicver.music.playback.PlaybackService
import com.nicver.music.playback.isMv
import com.nicver.music.playback.toMediaItem
import com.nicver.music.ui.Kind
import com.nicver.music.ui.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(private val app: Application) : AndroidViewModel(app) {

    // ---------- perpustakaan ----------
    var library by mutableStateOf(Library())
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    private var mvIndex by mutableStateOf<MvIndex<VideoFile>?>(null)
    var videoGranted by mutableStateOf(false)
        private set
    var audioGranted by mutableStateOf(false)
        private set

    // ---------- navigasi ----------
    var query by mutableStateOf("")
    var tab by mutableIntStateOf(0)
    var mvFullscreen by mutableStateOf(false)
    val back = mutableStateListOf<Route>()
    val route: Route? get() = back.lastOrNull()

    // ---------- status pemutar ----------
    var controller by mutableStateOf<MediaController?>(null)
        private set
    var currentKey by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isMvNow by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set

    /** True selama layar MV terbuka: lagu berikutnya yang punya MV ikut diputar sebagai video. */
    private var wantMv = false
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // ---------- turunan ----------
    val songByKey by derivedStateOf { library.songs.associateBy { it.key } }
    val currentSong: Song? get() = currentKey?.let { songByKey[it] }

    val sortedSongs by derivedStateOf {
        val l = library.songs
        when (AppStore.sortMode) {
            1 -> l.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            2 -> l.sortedByDescending { it.dateAdded }
            else -> l.sortedBy { it.title.lowercase() }
        }
    }

    val albums: List<Pair<String, List<Song>>> by derivedStateOf {
        sortedSongs.groupBy { it.album }.entries.sortedBy { it.key.lowercase() }.map { it.key to it.value }
    }

    val artists: List<Pair<String, List<Song>>> by derivedStateOf {
        sortedSongs.groupBy { it.artist }.entries.sortedBy { it.key.lowercase() }.map { it.key to it.value }
    }

    val genreGroups: List<Pair<String, List<Song>>> by derivedStateOf {
        val m = LinkedHashMap<String, MutableList<Song>>()
        for (s in sortedSongs) for (g in genresOf(s)) m.getOrPut(g) { mutableListOf() }.add(s)
        val order = GENRE_LIST + OTHER_GENRE
        m.entries
            .sortedBy { e -> order.indexOf(e.key).let { if (it < 0) 999 else it } }
            .map { it.key to (it.value as List<Song>) }
    }

    val favSongs: List<Song> by derivedStateOf { sortedSongs.filter { AppStore.favorites.containsKey(it.key) } }

    val topPlayed: List<Song> by derivedStateOf {
        library.songs.filter { (AppStore.plays[it.key] ?: 0) > 0 }
            .sortedByDescending { AppStore.plays[it.key] ?: 0 }
            .take(100)
    }

    val mvSongs: List<Song> by derivedStateOf { sortedSongs.filter { mvFor(it) != null } }

    // ---------- genre ----------
    fun genresOf(song: Song): List<String> {
        val o = AppStore.genreOverride[song.key]
        return if (o != null) listOf(o) else canonGenres(song.genreRaw)
    }

    // ---------- MV ----------
    fun mvFor(song: Song): Uri? {
        val manual = AppStore.mvLinks[song.key]
        if (manual != null) return Uri.parse(manual)
        return mvIndex?.find(song.title, song.artist, baseNameOf(song.fileName), song.folder)?.uri
    }

    // ---------- pencarian / daftar ----------
    fun matches(text: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() || text.lowercase().contains(q)
    }

    fun filtered(list: List<Song>): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return list
        return list.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
    }

    fun songsFor(r: Route.Detail): List<Song> = when (r.kind) {
        Kind.Album -> sortedSongs.filter { it.album == r.key }
        Kind.Artist -> sortedSongs.filter { it.artist == r.key }
        Kind.Genre -> sortedSongs.filter { r.key in genresOf(it) }
    }

    // ---------- izin ----------
    private fun has(p: String) =
        ContextCompat.checkSelfPermission(app, p) == PackageManager.PERMISSION_GRANTED

    /** Dipanggil saat aplikasi dibuka / kembali dari dialog izin atau pengaturan. */
    fun refreshPermissions() {
        val api33 = Build.VERSION.SDK_INT >= 33
        audioGranted = has(if (api33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
        videoGranted = has(if (api33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE)
        if (audioGranted) reload()
    }

    // ---------- muat perpustakaan ----------
    fun reload() {
        if (loading) return
        loading = true
        viewModelScope.launch {
            val lib = withContext(Dispatchers.IO) { LibraryLoader.load(app) }
            library = lib
            mvIndex = MvIndex(lib.videos.map { MvEntry(it, baseNameOf(it.name), it.folder) })
            loading = false
            loaded = true
        }
    }

    // ---------- navigasi ----------
    fun go(r: Route) {
        if (route == r) return
        back.add(r)
    }

    fun pop() {
        if (route is Route.Mv && mvFullscreen) {
            mvFullscreen = false
            return
        }
        if (back.isNotEmpty()) back.removeAt(back.lastIndex)
    }

    // ---------- pemutar ----------
    private fun connect() {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val f = MediaController.Builder(app, token).buildAsync()
        controllerFuture = f
        f.addListener({
            try {
                val c = f.get()
                c.addListener(listener)
                controller = c
                sync()
            } catch (e: Exception) {
                toast("Gagal menyambung ke pemutar")
            }
        }, ContextCompat.getMainExecutor(app))
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
            if (wantMv && mediaItem != null && !mediaItem.isMv) {
                val s = songByKey[mediaItem.mediaId]
                if (s != null && mvFor(s) != null) switchMv(true)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val c = controller ?: return
            if (c.currentMediaItem?.isMv == true) {
                toast("MV tidak bisa diputar, kembali ke audio")
                wantMv = false
                switchMv(false)
                c.prepare()
                if (route is Route.Mv) pop()
            } else {
                toast("Lagu ini tidak bisa diputar")
                if (c.hasNextMediaItem()) {
                    c.seekToNextMediaItem()
                    c.prepare()
                }
            }
        }
    }

    private fun sync() {
        val c = controller ?: return
        isPlaying = c.isPlaying
        shuffle = c.shuffleModeEnabled
        repeatMode = c.repeatMode
        val item = c.currentMediaItem
        currentKey = item?.mediaId
        isMvNow = item?.isMv == true
        val d = c.duration
        durationMs = if (d == C.TIME_UNSET || d < 0) 0L else d
        positionMs = c.currentPosition.coerceAtLeast(0L)
    }

    fun playList(list: List<Song>, index: Int) {
        val c = controller ?: return toast("Pemutar belum siap")
        if (list.isEmpty()) return
        wantMv = false
        c.shuffleModeEnabled = false
        c.setMediaItems(list.map { it.toMediaItem() }, index.coerceIn(0, list.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun playShuffled(list: List<Song>) {
        val c = controller ?: return toast("Pemutar belum siap")
        if (list.isEmpty()) return
        wantMv = false
        c.shuffleModeEnabled = true
        c.setMediaItems(list.map { it.toMediaItem() }, list.indices.random(), 0L)
        c.prepare()
        c.play()
    }

    /** Putar lagu ke-[index] langsung sebagai video MV, lalu buka layar MV. */
    fun playMv(list: List<Song>, index: Int) {
        val c = controller ?: return toast("Pemutar belum siap")
        if (list.isEmpty()) return
        val i = index.coerceIn(0, list.lastIndex)
        val mv = mvFor(list[i]) ?: return toast("Lagu ini belum punya MV")
        wantMv = true
        c.shuffleModeEnabled = false
        c.setMediaItems(list.mapIndexed { k, s -> s.toMediaItem(if (k == i) mv else null) }, i, 0L)
        c.prepare()
        c.play()
        go(Route.Mv)
    }

    /** Dari layar Sedang Diputar: lanjutkan lagu yang sama sebagai video. */
    fun watchMv() {
        val s = currentSong ?: return
        if (mvFor(s) == null) return toast("Lagu ini belum punya MV")
        wantMv = true
        switchMv(true)
        go(Route.Mv)
    }

    fun enterMv() {
        wantMv = true
        switchMv(true)
    }

    fun leaveMv() {
        wantMv = false
        switchMv(false)
    }

    /** Ganti item yang sedang diputar antara versi audio dan versi MV pada posisi yang sama. */
    private fun switchMv(on: Boolean) {
        val c = controller ?: return
        val item = c.currentMediaItem ?: return
        if (item.isMv == on) return
        val song = songByKey[item.mediaId] ?: return
        val mv = if (on) (mvFor(song) ?: return) else null
        val idx = c.currentMediaItemIndex
        val pos = c.currentPosition
        c.replaceMediaItem(idx, song.toMediaItem(mv))
        c.seekTo(idx, pos)
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun prev() {
        val c = controller ?: return
        if (c.currentPosition > 3000) c.seekTo(0) else c.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toast(msg: String) {
        Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
    }

    init {
        AppStore.init(app)
        connect()
        viewModelScope.launch {
            while (isActive) {
                delay(400)
                val c = controller
                if (c != null) {
                    positionMs = c.currentPosition.coerceAtLeast(0L)
                    val d = c.duration
                    durationMs = if (d == C.TIME_UNSET || d < 0) 0L else d
                }
            }
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }
}
