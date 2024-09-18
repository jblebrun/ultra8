package com.emerjbl.ultra8.chip8.machine

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Manages the Chip8 real-time timer state. */
class Chip8Timer(val timeSource: TimeSource) {
    // The value currently left to count down.
    private var tickCount: Int = 0
    private var timeSetAt: TimeMark = timeSource.markNow()

    var value: Int
        get() {
            val elapsed = timeSetAt.elapsedNow()
            //16.66ms for 60Hz
            val elapsedTicks = (elapsed / 16.66.milliseconds).toInt()
            return maxOf(tickCount - elapsedTicks, 0).also {
                tickCount = it
            }
        }
        set(ticks) {
            tickCount = ticks
            timeSetAt = timeSource.markNow()
        }
}
