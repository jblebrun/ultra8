package com.emerjbl.ultra8

class Chip8Input {
    var keys: BooleanArray = BooleanArray(16)

    fun keyPressed(x: Int): Boolean {
        return keys[x]
    }

    private val monitor = Any()

    @Synchronized
    fun setKey(x: Int) {
        keys[x] = true
        (this as Object).notify()
    }

    fun resetKey(x: Int) {
        keys[x] = false
    }

    fun awaitPress(): Int {
        val pressed = checkForPress()
        if (pressed >= 0) {
            return pressed
        }

        try {
            (monitor as Object).wait()
        } catch (ex: InterruptedException) {
            return -1
        }
        return checkForPress()
    }

    fun checkForPress(): Int {
        for (i in 15 downTo 0) {
            if (keys[i]) {
                return i
            }
        }
        return -1
    }
}
