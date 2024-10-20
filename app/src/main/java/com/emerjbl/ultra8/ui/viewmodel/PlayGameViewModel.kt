package com.emerjbl.ultra8.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.emerjbl.ultra8.Ultra8Application
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.ProgramStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource


/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class PlayGameViewModel(
    val programName: String,
    private val chip8StateStore: Chip8StateStore,
    private val programStore: ProgramStore,
) : ViewModel() {
    private var machine = newMachine(byteArrayOf())

    private val runner = Chip8ThreadRunner()

    val running: StateFlow<Boolean>
        get() = runner.running

    val cyclesPerTick = MutableStateFlow(runner.cyclesPerTick).apply {
        onEach {
            runner.cyclesPerTick = it
            programStore.updateCyclesPerTick(programName, it)
        }
            .launchIn(viewModelScope)
    }

    fun keyDown(idx: Int) {
        machine.keyDown(idx)
    }

    fun keyUp(idx: Int) {
        machine.keyUp(idx)
    }

    fun pause() {
        viewModelScope.launch {
            initJob.join()
            runner.pause()
            withContext(Dispatchers.IO) {
                val state = machine.stateView.clone()
                chip8StateStore.saveState(programName, state)
                Log.i("Chip8", "(PAUSE) $programName State Saved: ${state.pc}")
            }
        }
    }

    fun resume() {
        viewModelScope.launch {
            initJob.join()
            runner.run(machine)
            withContext(Dispatchers.IO) {
                val state = machine.stateView.clone()
                chip8StateStore.saveState(programName, state)
                Log.i("Chip8", "(RESUME) $programName State Saved: ${state.pc}")
            }
        }
    }

    fun reset() {
        machine.reset()
        resume()
    }

    fun nextFrame(lastFrame: FrameManager.Frame?): FrameManager.Frame =
        machine.nextFrame(lastFrame)


    private val initJob = viewModelScope.launch {
        machine = withContext(Dispatchers.IO) {
            val savedState = chip8StateStore.findState(programName)
            val program = programStore.withData(programName)
            if (savedState != null) {
                newMachine(savedState)
            } else if (program != null) {
                cyclesPerTick.value = program.cyclesPerTick
                newMachine(program.data!!)
            } else {
                // TODO -- something useful
                newMachine(byteArrayOf())
            }
        }
        resume()
    }

    private fun newMachine(program: ByteArray): Chip8 =
        newMachine(Chip8.stateForProgram(program))

    private fun newMachine(state: Chip8.State): Chip8 {
        val sound = AudioTrackSynthSound(viewModelScope, 48000)
        return Chip8(sound, TimeSource.Monotonic, state)
    }

    companion object {
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
