package com.emerjbl.ultra8.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.emerjbl.ultra8.R
import com.emerjbl.ultra8.chip8.graphics.FadeBitmapChip8Graphics
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.runner.Chip8Runner
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import kotlin.time.TimeSource

/** Pre-loaded program entry */
data class Program(val name: String, val id: Int)

/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class Chip8ViewModel(application: Application) : AndroidViewModel(application) {
    private val gfx = FadeBitmapChip8Graphics()
    private val keys: Chip8Keys = Chip8Keys()
    private val sound = AudioTrackSynthSound()
    private val runner: Chip8Runner = Chip8ThreadRunner(keys, gfx, sound, TimeSource.Monotonic)

    val programs = R.raw::class.java.fields.map {
        Program(it.name, it.getInt(null))
    }

    init {
        load(R.raw.breakout)
    }

    fun turboOn() {
        runner.turbo = true
    }

    fun turboOff() {
        runner.turbo = false
    }

    fun lowSpeed() {
        runner.period = Chip8Runner.Period(2, 0)
    }

    fun hiSpeed() {
        runner.period = Chip8Runner.Period(1, 0)
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
        val program = getApplication<Application>().resources.openRawResource(id).readBytes()
        runner.load(program)
    }

    fun nextFrame(frameTime: Long): Bitmap =
        gfx.nextFrame(frameTime)
}
