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
import kotlin.time.TimeSource

/** Provide Chip8 sound output using an AudioTrack. */
class AudioTrackSynthSound(
    parentScope: CoroutineScope,
    private val sampleRate: Int
) : Chip8Sound {
    private val playStats = SimpleStats("ms", 1) {
        Log.i("Chip8", "Beep play times: $it")
    }

    private sealed interface Action {
        data class Play(val ticks: Int) : Action
        data object UpdatePattern : Action
    }

    private var pattern = Pattern(
        0xFF00FF00FF00FF00UL,
        0xFF00FF00FF00FF00UL,
    )
    private var patternRate: Int = 4000

    private fun newTrack(): AudioTrack {
        val data = pattern.render(patternRate, sampleRate)
        val track = AudioTrack.Builder().setTransferMode(AudioTrack.MODE_STATIC)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setBufferSizeInBytes(data.size * 4)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            ).build()
        track.write(data, 0, data.size, AudioTrack.WRITE_BLOCKING)
        return track
    }

    private var track = newTrack()

    // AudioTrack is managed on a different thread, because calls to play sometimes take 10s of ms.
    private val executor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val soundActions =
        MutableSharedFlow<Action>(replay = 1, extraBufferCapacity = 10).apply {
            onEach { action ->
                when (action) {
                    is Action.Play -> {
                        // This implementation gives reasonable results, but some details are wrong:
                        // 1) If the pattern is longer than frame, longer sounds may be a bit too
                        //    long. But for sounds shorter than one pattern, we are good. We could
                        //    resolve this, for example, by rendering the pattern enough times that
                        //    it doesn't need to be looped. But this would still hit problem #2.
                        // 2) According to the XO spec, updating the tick timer should not restart
                        //    the sound. But currently, we will pause and restart the sound, so
                        //    you may hear clicks.
                        val started = TimeSource.Monotonic.markNow()
                        val sampleTime = action.ticks / 60.0 // 60 Hz ticks
                        val samplesToPlay = sampleTime * sampleRate
                        track.pause()
                        val loops = (samplesToPlay / track.bufferSizeInFrames).toInt()
                        // If loops is 0, we may only need to play a partial sample.
                        val playSamples = minOf(samplesToPlay.toInt(), track.bufferSizeInFrames)
                        if (loops == 0) {
                            track.playbackHeadPosition = 0
                        }
                        track.setLoopPoints(0, playSamples, loops)
                        track.play()
                        val timeToPlay = started.elapsedNow()
                        playStats.add(timeToPlay.inWholeMilliseconds)
                    }

                    is Action.UpdatePattern -> {
                        val data: FloatArray = pattern.render(patternRate, sampleRate)
                        track.pause()
                        track.setBufferSizeInFrames(data.size)
                        track.write(data, 0, data.size, AudioTrack.WRITE_BLOCKING)
                    }
                }
            }
                .flowOn(executor)
                .launchIn(parentScope)
                .invokeOnCompletion {
                    track.stop()
                    track.release()
                }
        }

    override fun play(ticks: Int) {
        soundActions.tryEmit(Action.Play(ticks))
    }

    override fun setPattern(pattern: Pattern) {
        this.pattern = pattern
        soundActions.tryEmit(Action.UpdatePattern)
    }

    override fun setPatternRate(patternRate: Int) {
        this.patternRate = patternRate
        soundActions.tryEmit(Action.UpdatePattern)
    }
}
