package com.emerjbl.ultra8.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emerjbl.ultra8.R
import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.runner.Chip8Runner
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.time.TimeSource

/** Pre-loaded program entry */
data class Program(val name: String, val id: Int)

/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class Chip8ViewModel(application: Application) : AndroidViewModel(application) {
    private val gfx = SimpleGraphics()
    private val keys: Chip8Keys = Chip8Keys()
    private val sound = AudioTrackSynthSound(viewModelScope)
    private val runner: Chip8Runner = Chip8ThreadRunner(keys, gfx, sound, TimeSource.Monotonic)
    private val background: CoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val running: Flow<Boolean>
        get() = runner.running

    val cyclesPerSecond = MutableStateFlow(runner.cyclesPerSecond).apply {
        onEach { runner.cyclesPerSecond = it }
            .launchIn(viewModelScope)
    }

    val programs = R.raw::class.java.fields.map {
        Program(it.name, it.getInt(null))
    }

    init {
        load(R.raw.breakout)
    }

    fun keyDown(idx: Int) {
        keys.keyDown(idx)
    }

    fun keyUp(idx: Int) {
        keys.keyUp(idx)
    }

    fun pause() {
        runner.pause()
    }

    fun resume() {
        runner.resume()
    }

    fun load(id: Int) {
        viewModelScope.launch {
            val program = withContext(background) {
                getApplication<Application>().resources.openRawResource(id).readBytes()
            }
            runner.load(program)
        }
    }

    fun nextFrame(): SimpleGraphics.Frame = gfx.nextFrame()
}
