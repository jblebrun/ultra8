package com.emerjbl.ultra8.chip8.sound

import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.emerjbl.ultra8.util.SimpleStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.Executors
import kotlin.time.measureTime

const val SAMPLE_RATE: Int = 44100

class AudioTrackSynthSound(parentScope: CoroutineScope) : Chip8Sound {
    val playStats = SimpleStats("ms", 10) {
        Log.i("Chip8", "Beep times: $it")
    }

    private val data: FloatArray = wave(square(730), 730f)
    private fun newTrack(data: FloatArray): AudioTrack =
        AudioTrack.Builder().setTransferMode(AudioTrack.MODE_STATIC)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(data.size * 4)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            ).build().apply {
                write(data, 0, data.size, AudioTrack.WRITE_BLOCKING)
            }

    private val track = newTrack(data)

    // AudioTrack is managed on a different thread, because calls to play sometimes take 10s of ms.
    private val executor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val ticksToPlay = MutableSharedFlow<Int>(replay = 1).apply {
        onEach { ticks ->
            val playTime = measureTime {
                val loops = (ticks * SAMPLE_RATE / 60) / track.bufferSizeInFrames
                track.pause()
                track.setLoopPoints(0, track.bufferSizeInFrames, loops)
                track.play()
            }
            playStats.add(playTime.inWholeMilliseconds)
        }
            .flowOn(executor)
            .launchIn(parentScope)
    }

    override fun play(ticks: Int) {
        ticksToPlay.tryEmit(ticks)
    }
}
