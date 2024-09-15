package com.emerjbl.ultra8

import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sign
import kotlin.math.sin

const val SAMPLE_RATE: Int = 44100

fun wave(f: (Double) -> Double): FloatArray {
    // Generate 5 seconds to cover 255 * 16.66ms
    val samples = 5 * SAMPLE_RATE
    val out = FloatArray(samples)
    val b = 2 * PI / SAMPLE_RATE
    for (t in out.indices) {
        val y = f(b * t.toDouble())
        out[t] = y.toFloat()
    }
    return out
}

fun sin(freq: Int): (Double) -> Double = {
    sin(freq * it)
}

fun square(freq: Int): (Double) -> Double = {
    sign(sin(freq * it))
}

class Chip8Sound {
    val data: FloatArray = wave(square(880))
    val track: AudioTrack =
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
                setPlaybackPositionUpdateListener(object :
                    AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack?) {
                        track!!.stop()
                    }

                    override fun onPeriodicNotification(track: AudioTrack?) {
                        TODO("Not yet implemented")
                    }
                })
            }

    fun play(timer: Int) {
        if (track.state == AudioTrack.STATE_INITIALIZED) {
            track.stop()
            track.reloadStaticData()
        }
        // The marker notification triggers the sound stop
        val samples = (timer * SAMPLE_RATE / 60)
        track.notificationMarkerPosition = samples
        track.play()
    }
}