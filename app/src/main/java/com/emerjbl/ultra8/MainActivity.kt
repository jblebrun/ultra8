package com.emerjbl.ultra8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.emerjbl.ultra8.ui.screen.MainScreen

class MainActivity : ComponentActivity() {
    private val chip8ViewModel: Chip8ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        enableEdgeToEdge()
        setContent {
            MainScreen(chip8ViewModel)
        }
    }

    override fun onPause() {
        super.onPause()
        chip8ViewModel.pause()
    }

    override fun onResume() {
        super.onResume()
        chip8ViewModel.resume()
    }
}
