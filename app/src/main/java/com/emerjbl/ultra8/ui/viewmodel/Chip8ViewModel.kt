package com.emerjbl.ultra8.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.emerjbl.ultra8.R
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.graphics.StandardChip8Font
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import com.emerjbl.ultra8.data.MaybeState
import com.emerjbl.ultra8.data.chip8StateStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.time.TimeSource

/** Pre-loaded program entry */
data class Program(val name: String, val id: Int)

/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class Chip8ViewModel(
    val application: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val keys = Chip8Keys()
    private fun newMachine(program: ByteArray): Chip8 {
        val sound = AudioTrackSynthSound(viewModelScope, 48000)
        return Chip8(keys, sound, StandardChip8Font, TimeSource.Monotonic, program = program)
    }

    private fun newMachine(state: Chip8.State): Chip8 {
        val sound = AudioTrackSynthSound(viewModelScope, 48000)
        return Chip8(keys, sound, TimeSource.Monotonic, state)
    }

    private var machine = newMachine(byteArrayOf())

    private val runner = Chip8ThreadRunner()
    private val background: CoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    val running: StateFlow<Boolean>
        get() = runner.running

    private val _loadedName = MutableStateFlow<String?>(null)
    val loadedName: StateFlow<String?>
        get() = _loadedName.asStateFlow()

    val cyclesPerTick = MutableStateFlow(runner.cyclesPerTick).apply {
        onEach { runner.cyclesPerTick = it }
            .launchIn(viewModelScope)
    }

    val programs = R.raw::class.java.fields.map {
        Program(it.name, it.getInt(null))
    }

    init {
        Log.i("Chip8", "Saved State Keys: ${savedStateHandle.keys()}")

        viewModelScope.launch(Dispatchers.IO) {
            when (val savedState = application.chip8StateStore.data.firstOrNull()) {
                is MaybeState.Yes -> {
                    Log.i("Chip8", "Restoring saved state: ${savedState.state.pc}")
                    machine = newMachine(savedState.state)
                    resume()
                    return@launch
                }

                else -> {
                    val program: ByteArray? = savedStateHandle["program"]
                    if (program != null) {
                        machine = newMachine(program)
                        resume()
                    } else {
                        load(programs.firstOrNull { it.id == R.raw.breakout }!!)
                    }
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
            application.chip8StateStore.updateData {
                MaybeState.Yes(state)
            }
            Log.i("Chip8", "State Saved: ${state.pc}")
        }
    }

    fun resume() {
        runner.run(machine)
    }

    fun load(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MAIN -> {}
            Intent.ACTION_VIEW -> {
                val data = intent.data
                if (data == null) {
                    Log.i("Chip8", "View intent had no data")
                } else {
                    loadContent(data)
                }
            }

            else -> {
                Log.i("Chip8", "Don't know what to do with $intent")
            }
        }
    }

    private fun loadContent(uri: Uri) {
        Log.i("Chip8", "Opening program from URI: $uri")
        viewModelScope.launch {
            val program = withContext(background) {
                application.contentResolver.query(
                    uri,
                    arrayOf(MediaColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use {
                    if (it.moveToNext()) {
                        val name = it.getString(0)
                        Log.i("Chip8", "Program file name: $name")
                        _loadedName.value = name
                    }
                }

                application.contentResolver.openInputStream(uri)!!.use {
                    it.readBytes()
                }
            }
            Log.i("Chip8", "Program size: ${program.size}")
            savedStateHandle["program"] = program
            machine = newMachine(program)
            resume()
        }
    }

    fun load(program: Program) {
        viewModelScope.launch {
            val data = application.resources.openRawResource(program.id).use {
                it.readBytes()
            }
            Log.i("Chip8", "Program size: ${data.size}")
            _loadedName.value = program.name
            savedStateHandle["program"] = data
            machine = newMachine(data)
            resume()
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
                Chip8ViewModel(
                    checkNotNull(extras[APPLICATION_KEY]),
                    extras.createSavedStateHandle()
                ) as T
        }
    }
}
