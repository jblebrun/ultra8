package com.emerjbl.ultra8

import android.util.Log
import kotlin.concurrent.thread
import kotlin.time.TimeSource

class Chip8Runner(val keys: Chip8Keys, val gfx: Chip8Graphics, val timeSource: TimeSource) {
    private var machine: Chip8? = null
    var runThread: Thread? = null

    fun load(program: ByteArray) {
        Log.i("Chip8", "Resetting machine")
        gfx.hires = false
        gfx.clear()
        machine = Chip8(keys, gfx, StandardChip8Font, timeSource, program)
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
                    Thread.sleep(2)
                }
                Log.i("Chip8", "Finished with: ${halt}")
            } catch (ex: InterruptedException) {
                Log.i("Chip8", "Thread interrupted at ${machine.pc}")
            }
        }
    }
}