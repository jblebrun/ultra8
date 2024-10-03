package com.emerjbl.ultra8.chip8.input

import com.emerjbl.ultra8.util.LockGuarded
import java.util.concurrent.locks.ReentrantLock

/** Manage and share the state of the Chip8 key pad. */
class Chip8Keys {
    private val lock = ReentrantLock()
    private val keys = LockGuarded(lock, BooleanArray(16))
    private val condition = lock.newCondition()

    fun keyDown(idx: Int) {
        keys.withLock {
            it[idx] = true
            condition.signal()
        }
    }

    fun keyUp(idx: Int) {
        keys.withLock {
            it[idx] = false
            condition.signal()
        }
    }

    fun pressed(idx: Int) = keys.withLock { it[idx] }

    fun awaitKey(): Int = keys.withLock {
        var pressed = it.firstPressedKey()
        while (pressed < 0) {
            condition.await()
            pressed = it.firstPressedKey()
        }
        pressed
    }

    private fun BooleanArray.firstPressedKey(): Int =
        withIndex().firstOrNull { it.value }?.index ?: -1
}
