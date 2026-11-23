package com.nicver.music

import android.app.Application
import com.nicver.music.data.AppStore

class MusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppStore.init(this)
    }
}
