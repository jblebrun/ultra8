package com.emerjbl.ultra8.chip8.runner

import android.util.Log
import com.emerjbl.ultra8.chip8.graphics.Chip8Graphics
import com.emerjbl.ultra8.chip8.graphics.StandardChip8Font
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.machine.Halt
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class Chip8ThreadRunner(
    val keys: Chip8Keys,
    val gfx: Chip8Graphics,
    val sound: Chip8Sound,
    val timeSource: TimeSource,
) : Chip8Runner {
    private var machine: Chip8? = null
    private var runThread: Thread? = null
    private var halt: Halt? = null
    private var paused: Boolean = true
    private val _running = MutableStateFlow(false)
    override val running: StateFlow<Boolean>
        get() = _running.asStateFlow()

    override var cyclesPerSecond: Int = 500
        set(value) {
            field = value
            period = (1.0 / value).seconds
        }
    private var period: Duration = (1.0 / cyclesPerSecond).seconds
    override var turbo: Boolean = false

    override fun load(program: ByteArray) {
        Log.i("Chip8", "Resetting machine")
        val shouldResume = !paused
        pause()
        halt = null
        gfx.hires = false
        gfx.clear()
        machine = Chip8(keys, gfx, sound, StandardChip8Font, timeSource, program)
        if (shouldResume) {
            resume()
        }
    }

    override fun pause() {
        Log.i("Chip8", "Pausing machine")
        paused = true
        runThread?.interrupt()
        runThread?.join()
        runThread = null
    }

    override fun resume() {
        paused = false
        machine?.let {
            Log.i("Chip8", "Resuming machine at ${it.pc}")
            run(it)
        }
    }

    private fun run(machine: Chip8) {
        runThread?.interrupt()
        runThread?.join()
        runThread = thread {
            _running.value = true
            try {
                while (halt == null) {
                    halt = machine.step()
                    val nanos = (period.inWholeNanoseconds % 1000000).toInt()
                    val millis = period.inWholeNanoseconds / 1000000
                    Thread.sleep(millis, nanos)
                }
                Log.i("Chip8", "Finished with: ${halt}")
                _running.value = false
            } catch (ex: InterruptedException) {
                Log.i("Chip8", "Thread interrupted at ${machine.pc}")
                _running.value = false
            }
        }
    }
}
