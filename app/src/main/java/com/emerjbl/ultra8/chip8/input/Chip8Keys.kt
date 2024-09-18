package com.emerjbl.ultra8.chip8.input

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Manage and share the state of the Chip8 key pad. */
class Chip8Keys {
    private val lock = ReentrantLock()
    private val keys = BooleanArray(16)
    private val condition = lock.newCondition()

    fun keyDown(idx: Int) {
        lock.withLock {
            keys[idx] = true
            condition.signal()
        }
    }

    fun keyUp(idx: Int) {
        lock.withLock {
            keys[idx] = false
            condition.signal()
        }
    }

    fun pressed(idx: Int) = keys[idx]

    fun awaitKey(): Int {
        var pressed = firstPressedKey()
        lock.withLock {
            while (pressed < 0) {
                condition.await()
                pressed = firstPressedKey()
            }
        }
        return pressed
    }

    private fun firstPressedKey(): Int =
        keys.withIndex().firstOrNull { it.value }?.index ?: -1
}
