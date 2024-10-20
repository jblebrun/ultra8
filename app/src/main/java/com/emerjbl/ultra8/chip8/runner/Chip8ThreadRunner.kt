package com.emerjbl.ultra8.chip8.runner

import android.util.Log
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.machine.StepResult
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
            try {
                while (machine.stateView.halted == null) {
                    val instructionTime = measureTime {
                        when (val result = machine.tick(cyclesPerTick)) {
                            is StepResult.Await -> result.await()
                            else -> Unit
                        }
                    }
                    val remaining = FRAME_TIME - instructionTime
                    val millisRemaining =
                        maxOf(0, remaining.inWholeMilliseconds)
                    Thread.sleep(millisRemaining)
                }
                Log.i("Chip8", "Finished with: ${machine.stateView.halted}")
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
