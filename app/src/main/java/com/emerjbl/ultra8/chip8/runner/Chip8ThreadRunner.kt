package com.emerjbl.ultra8.chip8.runner

import android.util.Log
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.machine.Halt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class Chip8ThreadRunner {
    private var runThread: Thread? = null
    private val _running = MutableStateFlow(false)

    val running: StateFlow<Boolean>
        get() = _running.asStateFlow()

    var cyclesPerTick: Int = 10

    fun pause() {
        runThread?.interrupt()
        runThread?.join()
        runThread = null
    }

    fun run(machine: Chip8) {
        runThread?.interrupt()
        runThread?.join()
        runThread = thread(name = "Chip8Runner") {
            _running.value = true
            var halt: Halt? = null
            try {
                while (halt == null) {
                    val instructionTime = measureTime {
                        repeat(cyclesPerTick) {
                            halt = machine.step()
                            if (halt != null) {
                                return@measureTime
                            }
                        }
                    }
                    val remaining = FRAME_TIME - instructionTime
                    val millisRemaining =
                        maxOf(0, remaining.inWholeMilliseconds)
                    Thread.sleep(millisRemaining)
                }
                Log.i("Chip8", "Finished with: $halt")
            } catch (ex: InterruptedException) {
                Log.i("Chip8", "Thread interrupted at ${machine.stateView.pc}")
            }
            _running.value = false
        }
    }

    companion object {
        val FRAME_TIME = 16.6666.milliseconds
    }
}
