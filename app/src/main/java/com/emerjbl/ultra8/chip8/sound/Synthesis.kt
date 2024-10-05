package com.emerjbl.ultra8.chip8.sound

class Pattern(
    val first: ULong,
    val second: ULong
) {
    fun bit(patternBit: Int): Double {
        val word = if (patternBit < 64) first else second
        val wordBit = (64 - patternBit % 64)
        // Take the bit for the sample
        val bit = word shr wordBit and 0x1UL
        // Scale to -1..1 float space, but
        // Scale volume to half.
        // So -0.5-0.5
        return (bit.toLong() - 0.5)
    }
}

/**
 * Render a Chip-XO sound pattern into PCM samples.
 *
 * This will render one complete cycle of the given pattern, at the give patternRate, so that it
 * plays properly at the provided output sampleRate.
 *
 * We output floats rather than the smallest possible sample size, in case we want to do some
 * smoothing later.
 **/
fun Pattern.render(
    /** The pattern sample rate to use (the "pitch" value from a program). */
    patternRate: Int,

    /** The actual waveform output playback sample rate. */
    sampleRate: Int
): FloatArray {
    // Calculate the total number of samples to hold one pattern repetition.
    val samples = (128.0 * sampleRate / patternRate).toInt()
    val out = FloatArray(samples)

    // Now populate each sample.
    for (i in out.indices) {
        // What ratio of the output are we at?
        val ratio = i.toFloat() / out.size
        // Scale that to 128 to find the right bit.
        val bitIdx = (128 * ratio).toInt()
        // Ask the pattern for that bit.
        out[i] = this.bit(bitIdx).toFloat()
    }
    return out
}
