package com.emerjbl.ultra8.chip8.runner

import android.util.Log
import com.emerjbl.ultra8.chip8.graphics.Chip8Graphics
import com.emerjbl.ultra8.chip8.graphics.StandardChip8Font
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.machine.Halt
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread
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
    private val _running = MutableStateFlow(false)
    override val running: Flow<Boolean>
        get() = _running.asStateFlow()

    override var period: Chip8Runner.Period = Chip8Runner.Period(2, 0)
    override var turbo: Boolean = false

    override fun load(program: ByteArray) {
        Log.i("Chip8", "Resetting machine")
        val running = runThread != null
        pause()
        halt = null
        gfx.hires = false
        gfx.clear()
        machine = Chip8(keys, gfx, sound, StandardChip8Font, timeSource, program)
        if (running) {
            resume()
        }
    }

    override fun pause() {
        Log.i("Chip8", "Pausing machine")
        runThread?.interrupt()
        runThread = null
    }

    override fun resume() {
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
                    if (turbo) {
                        Thread.sleep(0, 10)
                    } else {
                        Thread.sleep(period.millis, period.nanos)
                    }
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
