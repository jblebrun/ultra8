package com.emerjbl.ultra8.ui.viewmodel

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.emerjbl.ultra8.Ultra8Application
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.Program
import com.emerjbl.ultra8.data.ProgramStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.TimeSource


/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class Chip8ViewModel(
    private val chip8StateStore: Chip8StateStore,
    private val programStore: ProgramStore,
) : ViewModel() {
    private val keys = Chip8Keys()
    private fun newMachine(program: ByteArray): Chip8 =
        newMachine(Chip8.stateForProgram(program))

    private fun newMachine(state: Chip8.State): Chip8 {
        val sound = AudioTrackSynthSound(viewModelScope, 48000)
        return Chip8(keys, sound, TimeSource.Monotonic, state)
    }

    val programs: StateFlow<List<Program>> = programStore.programs

    private var machine = newMachine(byteArrayOf())

    private val runner = Chip8ThreadRunner()

    val running: StateFlow<Boolean>
        get() = runner.running

    private val _loadedProgram = MutableStateFlow<Program?>(null)
    val loadedProgram: StateFlow<Program?>
        get() = _loadedProgram.asStateFlow()

    val cyclesPerTick = MutableStateFlow(runner.cyclesPerTick).apply {
        onEach { runner.cyclesPerTick = it }
            .launchIn(viewModelScope)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            when (val savedState = chip8StateStore.lastSavedState()) {
                null -> load(programStore.forName("breakout")!!)
                else -> {
                    Log.i("Chip8", "Restoring saved state: ${savedState.pc}")
                    machine = newMachine(savedState)
                    resume()
                }
            }
        }
    }

    fun keyDown(idx: Int) {
        keys.keyDown(idx)
    }

    fun keyUp(idx: Int) {
        keys.keyUp(idx)
    }

    fun pause() {
        runner.pause()
        val state = machine.stateView.clone()
        viewModelScope.launch(Dispatchers.IO) {
            chip8StateStore.saveSate(state)
            Log.i("Chip8", "State Saved: ${state.pc}")
        }
    }

    fun resume() {
        runner.run(machine)
        val state = machine.stateView.clone()
        viewModelScope.launch(Dispatchers.IO) {
            chip8StateStore.saveSate(state)
            Log.i("Chip8", "State Saved: ${state.pc}")
        }
    }

    fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MAIN -> {}
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data == null) {
                    Log.i("Chip8", "View intent had no data")
                } else {
                    Log.i("Chip8", "Opening program from URI: $data")
                    viewModelScope.launch {
                        val program = programStore.addForUri(data)
                        if (program != loadedProgram.value) {
                            loadInternal(program)
                        }
                    }
                }
            }

            else -> {
                Log.i("Chip8", "Don't know what to do with $intent")
            }
        }
    }

    private suspend fun loadInternal(program: Program) {
        Log.i("Chip8", "Program size: ${program.data.bytes.size}")
        _loadedProgram.value = program
        machine = newMachine(program.data.bytes)
        resume()
    }

    fun reset() {
        machine.reset()
        resume()
    }

    fun load(program: Program) {
        viewModelScope.launch {
            loadInternal(program)
        }
    }

    fun nextFrame(lastFrame: FrameManager.Frame?): FrameManager.Frame =
        machine.nextFrame(lastFrame)

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T =
                checkNotNull(extras[APPLICATION_KEY] as? Ultra8Application).let { application ->
                    Chip8ViewModel(
                        application.provider.chip8StateStore,
                        application.provider.programStore,
                    ) as T
                }
        }
    }
}
