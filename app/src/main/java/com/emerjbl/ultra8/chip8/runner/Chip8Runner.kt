package com.emerjbl.ultra8.chip8.runner

import kotlinx.coroutines.flow.Flow

interface Chip8Runner {
    val running: Flow<Boolean>

    /** The number of instructions to run per second. */
    var cyclesPerSecond: Int

    /** When true, run really really fast. */
    var turbo: Boolean

    fun load(program: ByteArray)
    fun pause()
    fun resume()
}
