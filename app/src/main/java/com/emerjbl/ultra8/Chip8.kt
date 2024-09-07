package com.emerjbl.ultra8

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.Random
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val font: IntArray = intArrayOf(
    0xF0, 0x90, 0x90, 0x90, 0xF0,
    0x20, 0x60, 0x20, 0x20, 0x70,
    0xF0, 0x10, 0xF0, 0x80, 0xF0,
    0xF0, 0x10, 0x70, 0x10, 0xF0,
    0xA0, 0xA0, 0xF0, 0x20, 0x20,
    0xF0, 0x80, 0xF0, 0x10, 0xF0,
    0xF0, 0x80, 0xF0, 0x90, 0xF0,
    0xF0, 0x10, 0x10, 0x10, 0x10,
    0x60, 0x90, 0x60, 0x90, 0x60,
    0xF0, 0x90, 0xF0, 0x10, 0x10,
    0x60, 0x90, 0xF0, 0x90, 0x90,
    0xE0, 0x90, 0xE0, 0x90, 0xE0,
    0xF0, 0x80, 0x80, 0x80, 0xF0,
    0xE0, 0x90, 0x90, 0x90, 0xE0,
    0xF0, 0x80, 0xF0, 0x80, 0xF0,
    0xF0, 0x80, 0xE0, 0x80, 0x80
)

/** The registers of a Chip8 machine. */
class Chip8Machine {
    val v = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val hp = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
    val stack = IntArray(64)
    var i: Int = 0
    var sp = 0
    var pc = 0x200
}

class Chip8(val gfx: Chip8Graphics) {
    private val mem: IntArray = IntArray(4096)

    private val random: Random = Random()
    private val timer: Chip8Timer = Chip8Timer()
    private val tg: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    private var runThread: Thread? = null

    private var opCount: Int = 0
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var running: Boolean = false
    private val lock = ReentrantLock()
    private val keys = BooleanArray(16)
    private val condition = lock.newCondition()
    var paused: Boolean = false
        set(value) {
            lock.withLock {
                field = value
                condition.signal()
            }
        }
        get() = lock.withLock { field }
    private val execStart: Int = 0x200
    private val fontStart: Int = 0x100

    init {
        System.arraycopy(font, 0, mem, fontStart, font.size)
    }

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

    private fun awaitKey(): Int {
        var pressed = firstPressedKey()
        lock.withLock {
            while (pressed < 0) {
                condition.signal()
                pressed = firstPressedKey()
            }
        }
        return pressed
    }

    private fun firstPressedKey(): Int =
        keys.withIndex().firstOrNull { it.value }?.index ?: -1

    @Throws(InterruptedException::class)
    fun stop() {
        Log.i("ultra8", "stopping Chip8")
        if (runThread != null) {
            Log.i("ultra8", "there's a thread to stop")
            running = false
            runThread!!.interrupt()
            Log.i("ultra8", "waiting for run thread to complete")
            runThread!!.join()
            runThread = null
        }
        Log.i("ultra8", "reset complete")
    }

    fun reset() {
        try {
            Log.i("ultra8", "resetting")
            stop()
        } catch (e: InterruptedException) {
            Log.i("ultra8", "interrupted while resetting")
            e.printStackTrace()
        }
        startThread()
    }

    fun loadProgram(file: InputStream) {
        var at = execStart
        var next: Int
        try {
            while ((file.read().also { next = it }) != -1) {
                mem[at++] = next
            }
        } catch (ex: IOException) {
            //???
        }
    }

    fun runOps(): Int {
        val state = Chip8Machine()
        gfx.clearScreen()
        gfx.hires = false
        gfx.start()
        while (running) {
            Thread.sleep(1)
            if (paused) {
                Log.i("ultra8", "Chip8 is waiting due to pause...")
                gfx.stop()
                lock.withLock {
                    while (paused) condition.await()
                }
                Log.i("ultra8", "Chip8 is restarting after pause...")
                gfx.start()
            }
            val b1 = mem[state.pc++]
            val b2 = mem[state.pc++]
            val word = (b1 shl 8) or b2
            val majOp = b1 and 0xF0
            val nnn = word and 0xFFF
            val subOp = b2 and 0x0F
            val x = b1 and 0xF
            val y = (b2 shr 4)
            opCount++

            when (majOp) {
                0x00 -> when (b2) {
                    0xE0 -> gfx.clearScreen()

                    0xEE -> {
                        if (state.sp < 0) {
                            running = false
                            Log.i("ultra8", "FATAL: RET when state.stack is empty")
                            break
                        }
                        state.pc = state.stack[--state.sp]
                    }

                    0xFB -> gfx.rscroll()
                    0xFC -> gfx.lscroll()
                    0xFD -> {
                        running = false
                        Log.i("ultra8", "Normal: EXIT instruction")
                    }

                    0xFE -> gfx.hires = false

                    0xFF -> gfx.hires = true

                    else -> if (y == 0xC) {
                        gfx.scrolldown(subOp)
                    } else {
                        Log.i(
                            "ultra8",
                            "FATAL: trying to execute 0 byte, program missing halt"
                        )
                        running = false
                    }
                }

                0x20 -> {
                    state.stack[state.sp++] = state.pc

                    if (state.pc == nnn + 2) {
                        Log.i(
                            "Ultra8",
                            "normal: would spin in endless loop, stopping emulation"
                        )
                        running = false
                    } else {
                        state.pc = nnn
                    }
                }

                0x10 ->
                    if (state.pc == nnn + 2) {
                        Log.i(
                            "Ultra8",
                            "normal: would spin in endless loop, stopping emulation"
                        )
                        running = false
                    } else {
                        state.pc = nnn
                    }

                0x30 -> if (state.v[x] == b2) state.pc += 2

                0x40 -> if (state.v[x] != b2) state.pc += 2

                0x50 -> {
                    if (subOp != 0) {
                        Log.i("ultra8", "FATAL: Illegal opcode " + Integer.toHexString(word))
                        running = false
                    }
                    if (state.v[x] == state.v[y]) {
                        state.pc += 2
                    }
                }

                0x60 -> state.v[x] = b2

                0x70 -> {
                    state.v[x] = state.v[x] + b2
                    state.v[15] = if (((state.v[x] and 0x100) != 0)) 1 else 0
                    state.v[x] = state.v[x] and 0xFF
                }

                0x80 -> when (subOp) {
                    0x00 -> state.v[x] = state.v[y]

                    0x01 -> state.v[x] = state.v[x] or state.v[y]

                    0x02 -> state.v[x] = state.v[x] and state.v[y]
                    0x03 -> state.v[x] = state.v[x] xor state.v[y]
                    0x04 -> {
                        state.v[x] += state.v[y]
                        state.v[x] = state.v[x] and 0xFF
                        state.v[15] = if (((state.v[x] and 0x100) != 0)) 1 else 0
                    }

                    0x05 -> {
                        state.v[x] = (state.v[x] - state.v[y])
                        state.v[15] = (if ((state.v[x] and 0x100) == 0) 1 else 0)
                        state.v[x] = state.v[x] and 0xFF
                    }

                    0x06 -> {
                        state.v[15] = (state.v[x] and 0x01)
                        state.v[x] = state.v[x] ushr 1
                    }

                    0x07 -> {
                        state.v[x] = (state.v[y] - state.v[x])
                        state.v[15] = (if ((state.v[x] and 0x100) == 0) 0 else 1)
                        state.v[x] = state.v[x] and 0xFF
                    }

                    0x0E -> {
                        state.v[15] = (if ((state.v[x] and 0x80) == 0x80) 1 else 0)
                        state.v[x] = state.v[x] shl 1
                        state.v[x] = state.v[x] and 0xFF
                    }

                    else -> {
                        Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                        running = false
                    }
                }

                0x90 -> {
                    if (subOp != 0) {
                        Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                        running = false
                    }
                    if (state.v[x] != state.v[y]) state.pc += 2
                }

                0xA0 -> state.i = nnn
                0xB0 -> state.pc = state.v[0] + nnn
                0xC0 -> state.v[x] = random.nextInt(b2 + 1)
                0xD0 -> state.v[15] =
                    if (gfx.putSprite(state.v[x], state.v[y], mem, state.i, subOp)) 1 else 0

                0xE0 -> when (b2) {
                    0x9E -> if (keys[state.v[x]]) {
                        state.pc += 2
                    }

                    0xA1 -> if (!keys[state.v[x]]) {
                        state.pc += 2
                    }

                    else -> {
                        Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                        running = false
                    }
                }

                0xF0 -> when (b2) {
                    0x07 -> state.v[x] = timer.value
                    0x0A -> {
                        Log.i("ultra8", "WAITING FOR PRESS")
                        val pressed = awaitKey()
                        Log.i("ultra8", "Waited and got key $pressed")
                        state.v[x] = pressed
                    }

                    0x15 -> timer.value = state.v[x]

                    0x18 -> {}
                    0x1E -> state.i += state.v[x]
                    0x29 -> state.i = (fontStart + state.v[x] * 5)

                    0x33 -> {
                        var tmp = state.v[x]
                        var i = 0
                        while (tmp > 99) {
                            tmp -= 100
                            i++
                        }
                        mem[state.i] = i
                        i = 0
                        while (tmp > 9) {
                            tmp -= 10
                            i++
                        }
                        mem[state.i + 1] = i
                        i = 0
                        while (tmp > 0) {
                            tmp--
                            i++
                        }
                        mem[state.i + 2] = i
                    }

                    0x55 -> System.arraycopy(state.v, 0, mem, state.i, x + 1)
                    0x65 -> System.arraycopy(mem, state.i, state.v, 0, x + 1)
                    0x75 -> System.arraycopy(state.hp, 0, mem, state.i, x + 1)
                    0x85 -> System.arraycopy(mem, state.i, state.hp, 0, x + 1)

                    else -> {
                        Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                        running = false
                    }
                }

                else -> {
                    Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                    running = false
                }
            }
        }
        return state.pc
    }

    private inner class CPUThread : Thread() {
        override fun run() {
            var PC = 0
            try {
                running = true
                startTime = SystemClock.uptimeMillis()
                PC = runOps()
                //Wait for last pixels to fade.
                sleep(2000)
                gfx.stop()
                runThread = null
            } catch (ex: InterruptedException) {
                Log.i("ultra8", "machine was interrupted. That's fine.")
                gfx.stop()
            }
            endTime = SystemClock.uptimeMillis()
            Log.i("ultra8", "Finished at PC $PC")
            Log.i("ultra8", "Executed " + opCount + " ops in " + (endTime - startTime) + "ms")
            Log.i("ultra8", (1000L * opCount / (endTime - startTime)).toString() + "ops/sec")

            val SETCOLOR = -0xff0100
            val FADERATE = 0x08000000
            val temp = SETCOLOR - FADERATE
            val lmask = 0xFFFFFFFFL
            Log.i("utlra8", "btw, temp is " + Integer.toHexString(temp))
            Log.i("utlra8", "btw, temp > FADERATE is " + (temp > FADERATE))
            Log.i(
                "utlra8",
                "btw, cast temp > FADERATE is " + ((temp.toLong() and lmask) > (FADERATE.toLong() and lmask))
            )
            Log.i("utlra8", "btw, temp < SETCOLOR is " + (temp > SETCOLOR))
            Log.i(
                "utlra8",
                "btw, cast temp < SETCOLOR is " + ((temp.toLong() and lmask) > (SETCOLOR.toLong() and lmask))
            )
        }
    }

    fun startThread() {
        Log.i("ultra8", "starting Chip8")
        if (runThread == null) {
            Log.i("ultra8", "creating new machine thread")
            runThread = CPUThread().apply {
                start()
            }
            Log.i("ultra8", "started cpu")
        }
    }
}
