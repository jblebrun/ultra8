package com.emerjbl.ultra8.chip8.runner

import kotlinx.coroutines.flow.Flow

interface Chip8Runner {
    data class Period(val millis: Long, val nanos: Int)

    val running: Flow<Boolean>

    /** The amount of time per instruction */
    var period: Period

    /** When true, run really really fast. */
    var turbo: Boolean

    fun load(program: ByteArray)
    fun pause()
    fun resume()
}
