package com.example.nt_05_thienthach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.nt_05_thienthach.ui.MeteorDashApp

class MainActivity : ComponentActivity() {
    private val game: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(0xFF050B1C.toInt()), navigationBarStyle = SystemBarStyle.dark(0xFF050B1C.toInt()))
        setContent { MeteorDashApp(game) }
    }
    override fun onResume() { super.onResume(); game.setForeground(true) }
    override fun onPause() { game.setForeground(false); super.onPause() }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) game.pause()
    }
}

