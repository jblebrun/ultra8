package com.emerjbl.ultra8.chip8.sound

import kotlin.math.PI
import kotlin.math.sign

fun wave(f: (Double) -> Double, freq: Float): FloatArray {
    // Generate 1 cycle to loop
    val samples = (SAMPLE_RATE / freq).toInt()
    val out = FloatArray(samples)
    val b = 2 * PI / SAMPLE_RATE
    for (t in out.indices) {
        val y = f(b * t.toDouble())
        out[t] = y.toFloat()
    }
    return out
}

fun sin(freq: Int): (Double) -> Double = {
    kotlin.math.sin(freq * it)
}

fun square(freq: Int): (Double) -> Double = {
    sign(kotlin.math.sin(freq * it))
}
