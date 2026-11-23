package com.nicver.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.nicver.music.ui.AppRoot
import com.nicver.music.ui.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme { AppRoot(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPermissions()
    }
}
