package com.emerjbl.ultra8.chip8.input

import com.emerjbl.ultra8.util.LockGuarded
import java.util.concurrent.locks.ReentrantLock

/** Manage and share the state of the Chip8 key pad. */
class Chip8Keys {
    private val lock = ReentrantLock()
    private val keys = LockGuarded(lock, BooleanArray(16))
    private val condition = lock.newCondition()

    /** Put the key at index `idx` in the down state. */
    fun keyDown(idx: Int) {
        keys.withLock {
            it[idx] = true
            condition.signal()
        }
    }

    /** Put the key at index `idx` in the up state. */
    fun keyUp(idx: Int) {
        keys.withLock {
            it[idx] = false
            condition.signal()
        }
    }

    /** Return whether or not the key [idx] is pressed */
    fun pressed(idx: Int) = keys.withLock { it[idx] }

    /** Wait until the next key is pressed, and return its index. */
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
