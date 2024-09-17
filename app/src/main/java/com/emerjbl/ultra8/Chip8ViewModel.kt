package com.emerjbl.ultra8

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import kotlin.time.TimeSource

/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class Chip8ViewModel(application: Application) : AndroidViewModel(application) {
    private val gfx = FadeBitmapChip8Graphics()
    private val keys: Chip8Keys = Chip8Keys()
    private val sound = AudioTrackSynthSound()
    private val runner: Chip8Runner =
        Chip8Runner(keys, gfx, sound, TimeSource.Monotonic).apply {
            load(R.raw.breakout)
        }

    var turbo: Boolean by runner::turbo

    var period: Chip8Runner.Period by runner::period

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
