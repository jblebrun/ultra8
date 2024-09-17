package com.emerjbl.ultra8

import android.util.Log
import kotlin.concurrent.thread
import kotlin.time.TimeSource

class Chip8Runner(
    val keys: Chip8Keys,
    val gfx: Chip8Graphics,
    val sound: Chip8Sound,
    val timeSource: TimeSource
) {
    data class Period(val millis: Long, val nanos: Int)

    private var machine: Chip8? = null
    var runThread: Thread? = null
    var period: Period = Period(2, 0)
    var turbo: Boolean = false

    fun load(program: ByteArray) {
        Log.i("Chip8", "Resetting machine")
        pause()
        gfx.hires = false
        gfx.clear()
        machine = Chip8(keys, gfx, sound, StandardChip8Font, timeSource, program)
    }

    fun pause() {
        Log.i("Chip8", "Pausing machine")
        runThread?.interrupt()
        runThread = null
    }

    fun resume() {
        machine?.let {
            Log.i("Chip8", "Resuming machine at ${it.pc}")
            run(it)
        }
    }

    private fun run(machine: Chip8) {
        runThread?.interrupt()
        runThread?.join()
        runThread = thread {
            try {
                var halt: Halt? = null
                while (halt == null) {
                    halt = machine.step()
                    if (turbo) {
                        Thread.sleep(0, 10)
                    } else {
                        Thread.sleep(period.millis, period.nanos)
                    }
                }
                Log.i("Chip8", "Finished with: ${halt}")
            } catch (ex: InterruptedException) {
                Log.i("Chip8", "Thread interrupted at ${machine.pc}")
            }
        }
    }
}
