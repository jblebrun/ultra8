package com.emerjbl.ultra8.ui.gameplay

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.machine.StepResult
import com.emerjbl.ultra8.chip8.machine.StepResult.Halt
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.util.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.measureTime


/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class PlayGameViewModel(
    val programName: String,
    private val chip8StateStore: Chip8StateStore,
    private val programStore: ProgramStore,
) : ViewModel() {
    private var machine = newMachine(byteArrayOf())

    val running = MutableStateFlow<Boolean>(false)
    val halted = MutableStateFlow<Halt?>(null)

    val cyclesPerTick = MutableStateFlow(10).apply {
        // Don't overwrite with default
        drop(1)
            .onEach {
                programStore.updateCyclesPerTick(programName, it)
                // Throttle updates a bit
                delay(1000)
            }.launchIn(viewModelScope)
    }

    val frames = MutableStateFlow(machine.nextFrame(null).clone())

    fun keyDown(idx: Int) {
        machine.keyDown(idx)
    }

    fun keyUp(idx: Int) {
        machine.keyUp(idx)
    }

    private suspend fun saveState(reason: String) {
        withContext(Dispatchers.IO) {
            val state = machine.stateView.clone()
            chip8StateStore.saveState(programName, state)
            Log.i("Chip8", "($reason) $programName State Saved: ${state.pc}")
        }
    }

    fun reset() {
        val lastJob = runJob
        lastJob.cancel()
        runJob = viewModelScope.launch {
            lastJob.join()
            chip8StateStore.clearState(programName)
            runMachine()
        }
        halted.value = null
    }

    private var runJob = viewModelScope.launch {
        runMachine()
    }

    suspend fun runMachine() {
        machine = withContext(Dispatchers.IO) {
            val savedState = chip8StateStore.findState(programName)
            val program = programStore.withData(programName)
            if (program != null) {
                cyclesPerTick.value = program.cyclesPerTick
            }
            if (savedState != null) {
                Log.i("Chip8", "Restored save state for $programName")
                newMachine(savedState)
            } else if (program != null) {
                newMachine(program.data!!)
            } else {
                Log.i("Chip8", "Could not find $programName at all")
                null
            }
        } ?: return

        withContext(Dispatchers.Default) {
            // Force a new frame instance to trigger certain recomposes.
            // TODO: See if we can improve this.
            frames.value = machine.nextFrame(null)

            while (isActive) {
                if (!running.value || machine.stateView.halted != null) {
                    Log.i("Chip8", "$programName paused at ${machine.stateView.pc.toHexString()}")
                    saveState("pause")
                    // Wait for halt to clear and running to go to true.
                    running
                        .combine(halted) { r, h -> r && h == null }
                        .first { it }
                    // Force a new frame instance to trigger certain recomposes.
                    // TODO: See if we can improve this.
                    frames.value = machine.nextFrame(null)
                    Log.i("Chip8", "$programName resumed at ${machine.stateView.pc.toHexString()}")
                }
                val instructionTime = measureTime {
                    val result = machine.tick(cyclesPerTick.value)
                    frames.value = machine.nextFrame(frames.value)
                    when (result) {
                        is Halt -> {
                            Log.i("Chip8", "Halted $programName: $result")
                            halted.value = result
                        }

                        is StepResult.Await -> result.await()
                        is StepResult.Continue -> {}
                    }
                }
                delay(FRAME_TIME - instructionTime)
            }
        }
    }

    private fun newMachine(program: ByteArray): Chip8 =
        newMachine(Chip8.stateForProgram(program))

    private fun newMachine(state: Chip8.State): Chip8 {
        val sound = AudioTrackSynthSound(viewModelScope, 48000)
        return Chip8(sound, TimeSource.Monotonic, state)
    }

    companion object {
        val FRAME_TIME = (1.0 / 60).seconds

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            // We share viewModels for all running games in the nav stack
            PlayGameViewModel(
                checkNotNull(extras[ViewModelProvider.VIEW_MODEL_KEY]) {
                    "Missing VIEW_MODEL_KEY for PlayGameViewModel factory"
                },
                application.provider.chip8StateStore,
                application.provider.programStore,
            )
        }
    }
}
