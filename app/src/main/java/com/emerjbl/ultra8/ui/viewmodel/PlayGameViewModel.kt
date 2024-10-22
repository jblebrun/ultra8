package com.emerjbl.ultra8.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.emerjbl.ultra8.Ultra8Application
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.machine.StepResult
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.ProgramStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val halted = MutableStateFlow(false)

    val cyclesPerTick = MutableStateFlow(10).apply {
        onEach {
            programStore.updateCyclesPerTick(programName, it)
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
        machine.reset()
        halted.value = false
    }

    private val runJob = viewModelScope.launch {
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
                newMachine(byteArrayOf())
            }
        }

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
                        .combine(halted) { r, h -> r && !h }
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
                        is StepResult.Halt -> {
                            Log.i("Chip8", "Halted $programName: $result")
                            halted.value = true
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

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T =
                checkNotNull(extras[APPLICATION_KEY] as? Ultra8Application).let { application ->
                    // We share viewModels for all running games in the nav stack
                    val programName = extras[ViewModelProvider.VIEW_MODEL_KEY]!!
                    PlayGameViewModel(
                        programName,
                        application.provider.chip8StateStore,
                        application.provider.programStore,
                    ) as T
                }
        }
    }
}
