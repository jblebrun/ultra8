package com.emerjbl.ultra8.util

import java.util.concurrent.locks.Lock
import kotlin.concurrent.withLock

/** Wrap an item so that it must be accessed via a lock. */
class LockGuarded<T>(val lock: Lock, initial: T) {
    private var item = initial
    fun update(newItem: T) {
        lock.withLock { item = newItem }
    }

    fun <R> withLock(action: (T) -> R): R = lock.withLock { action(item) }
}
