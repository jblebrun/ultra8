package com.emerjbl.ultra8.chip8.sound

/** A component that can handle the sound commands on Chip8. */
interface Chip8Sound {
    /** Play a sound for `ticks` number of 60Hz ticks. */
    fun play(ticks: Int)

    fun setPattern(pattern: Pattern)
    fun setPatternRate(patternRate: Int)
}
