package com.emerjbl.ultra8.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emerjbl.ultra8.R
import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.runner.Chip8Runner
import com.emerjbl.ultra8.chip8.runner.Chip8ThreadRunner
import com.emerjbl.ultra8.chip8.sound.AudioTrackSynthSound
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.time.TimeSource

/** Pre-loaded program entry */
data class Program(val name: String, val id: Int)


/** A [androidx.lifecycle.ViewModel] maintaining the state of a running Chip8 machine. */
class Chip8ViewModel(application: Application) : AndroidViewModel(application) {
    private val gfx = SimpleGraphics()
    private val keys: Chip8Keys = Chip8Keys()
    private val sound = AudioTrackSynthSound(viewModelScope, 48000)
    private val runner: Chip8Runner = Chip8ThreadRunner(keys, gfx, sound, TimeSource.Monotonic)
    private val background: CoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val running: Flow<Boolean>
        get() = runner.running

    private val _loadedName = MutableStateFlow<String?>(null)
    val loadedName: StateFlow<String?>
        get() = _loadedName.asStateFlow()

    val cyclesPerSecond = MutableStateFlow(runner.cyclesPerSecond).apply {
        onEach { runner.cyclesPerSecond = it }
            .launchIn(viewModelScope)
    }

    val programs = R.raw::class.java.fields.map {
        Program(it.name, it.getInt(null))
    }

    init {
        load(programs.firstOrNull { it.id == R.raw.breakout }!!)
    }

    fun keyDown(idx: Int) {
        keys.keyDown(idx)
    }

    fun keyUp(idx: Int) {
        keys.keyUp(idx)
    }

    fun pause() {
        runner.pause()
    }

    fun resume() {
        runner.resume()
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
                getApplication<Application>().contentResolver.query(
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

                getApplication<Application>().contentResolver.openInputStream(uri)!!.use {
                    it.readBytes()
                }
            }
            Log.i("Chip8", "Program size: ${program.size}")
            runner.load(program)
        }
    }

    fun load(program: Program) {
        viewModelScope.launch {
            val data = getApplication<Application>().resources.openRawResource(program.id).use {
                it.readBytes()
            }
            Log.i("Chip8", "Program size: ${data.size}")
            _loadedName.value = program.name
            runner.load(data)
        }
    }

    fun nextFrame(lastFrame: SimpleGraphics.Frame?): SimpleGraphics.Frame =
        gfx.nextFrame(lastFrame)
}
