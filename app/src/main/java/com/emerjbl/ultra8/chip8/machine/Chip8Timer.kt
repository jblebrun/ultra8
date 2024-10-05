package com.emerjbl.ultra8.chip8.machine

import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Manages the Chip8 real-time timer state. */
class Chip8Timer(private val timeSource: TimeSource) {
    private var timerActiveUntil: TimeMark = timeSource.markNow()

    var value: Int
        get() {
            // If timer is still active, negative time has elapsed.
            val elapsed = maxOf(Duration.ZERO, -timerActiveUntil.elapsedNow())
            // Ceil because we don't decrement tick until the entire tick time
            // has passed; toInt would decrement tick right away.
            return ceil(elapsed / TICK_TIME).toInt()
        }
        set(ticks) {
            // Mark the time that the timer will be expired.
            timerActiveUntil = timeSource.markNow() + TICK_TIME * ticks
        }

    companion object {
        private val TICK_TIME = (1.0 / 60).seconds
    }
}
