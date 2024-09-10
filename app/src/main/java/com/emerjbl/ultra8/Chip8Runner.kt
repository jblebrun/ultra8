package com.emerjbl.ultra8

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.TimeSource

class Chip8Runner(val keys: Chip8Keys, val gfx: Chip8Graphics, val timeSource: TimeSource) {
    private var runThread: Thread? = null

    private var opCount: Int = 0
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var running: Boolean = false
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    var paused: Boolean = false
        set(value) {
            lock.withLock {
                field = value
                condition.signal()
            }
        }
        get() = lock.withLock { field }

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

    fun runProgram(program: ByteArray) {
        try {
            Log.i("ultra8", "resetting")
            stop()
        } catch (e: InterruptedException) {
            Log.i("ultra8", "interrupted while resetting")
            e.printStackTrace()
        }
        startThread(program)
    }

    fun runOps(machine: Chip8): Int {
        gfx.clearScreen()
        gfx.hires = false
        gfx.start()
        while (running) {
            Thread.sleep(2)
            if (paused) {
                Log.i("ultra8", "Chip8 is waiting due to pause...")
                gfx.stop()
                lock.withLock {
                    while (paused) condition.await()
                }
                Log.i("ultra8", "Chip8 is restarting after pause...")
                gfx.start()
            }
            opCount++
            running = machine.step()
        }
        return machine.pc
    }

    private inner class CPUThread(program: ByteArray) : Thread() {
        val machine = Chip8(keys, gfx, timeSource, program)
        override fun run() {
            var PC = 0
            try {
                running = true
                startTime = SystemClock.uptimeMillis()
                PC = runOps(machine)
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

    fun startThread(program: ByteArray) {
        Log.i("ultra8", "starting Chip8")
        if (runThread == null) {
            Log.i("ultra8", "creating new machine thread")
            runThread = CPUThread(program).apply {
                start()
            }
            Log.i("ultra8", "started cpu")
        }
    }
}