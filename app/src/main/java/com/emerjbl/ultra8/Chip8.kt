package com.emerjbl.ultra8

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.Arrays
import java.util.Random

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

class Chip8(val gfx: Chip8Graphics, val input: Chip8Input) {
    private val mem: IntArray = IntArray(4096)

    private val random: Random = Random()
    private val timer: Chip8Timer = Chip8Timer()
    private val tg: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)


    private var runThread: Thread? = null

    private var opCount: Int = 0
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var running: Boolean = false
    private var calling: Boolean = false
    private val execStart: Int = 0x200
    private val fontStart: Int = 0x100

    init {
        System.arraycopy(font, 0, mem, fontStart, font.size)
    }

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
        var regX: Int
        var regY: Int
        var i: Int
        var b1: Int
        var b2: Int
        var word: Int
        var majOp: Int
        var subOp: Int
        var nnn: Int
        var tmp: Int
        val regV = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val regHP = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
        val stack = IntArray(64)
        val timeCounts = IntArray(100)
        var opTime: Int
        Arrays.fill(timeCounts, 0, 99, 0)
        var regSP = 0
        var regI = 0
        var regPC = 0x200
        var opEndMillis: Long
        var opStartMillis: Long = 0
        gfx.clearScreen()
        gfx.hires = false
        gfx.start()
        while (running) {
            opEndMillis = SystemClock.uptimeMillis()
            opTime = (opEndMillis - opStartMillis).toInt()
            timeCounts[if (opTime > 99) 99 else opTime]++
            opStartMillis = SystemClock.uptimeMillis()
            Thread.sleep(0, if (input.hurry) 100 else 50000)
            if (input.reset) {
                running = false
                continue
            }
            if (input.pause) {
                Log.i("ultra8", "Chip8 is waiting due to pause...")
                gfx.stop()
                input.awaitPress()
                Log.i("ultra8", "Chip8 is restarting after pause...")
                gfx.start()
            }
            calling = false
            b1 = mem[regPC++]
            b2 = mem[regPC++]
            word = (b1 shl 8) or b2
            majOp = b1 and 0xF0
            nnn = word and 0xFFF
            subOp = b2 and 0x0F
            regX = b1 and 0xF
            regY = (b2 shr 4)
            opCount++

            when (majOp) {
                0x00 -> when (b2) {
                    0xE0 -> gfx.clearScreen()

                    0xEE -> {
                        if (regSP < 0) {
                            running = false
                            Log.i("ultra8", "FATAL: RET when stack is empty")
                            break
                        }
                        regPC = stack[--regSP]
                    }

                    0xFB -> gfx.rscroll()
                    0xFC -> gfx.lscroll()
                    0xFD -> {
                        running = false
                        Log.i("ultra8", "Normal: EXIT instruction")
                    }

                    0xFE -> gfx.hires = false

                    0xFF -> gfx.hires = true

                    else -> if (regY == 0xC) {
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
                    calling = true
                    stack[regSP++] = regPC

                    if (regPC == nnn + 2) {
                        Log.i(
                            "Ultra8",
                            "normal: would spin in endless loop, stopping emulation"
                        )
                        running = false
                    } else {
                        regPC = nnn
                    }
                }

                0x10 ->
                    if (regPC == nnn + 2) {
                        Log.i(
                            "Ultra8",
                            "normal: would spin in endless loop, stopping emulation"
                        )
                        running = false
                    } else {
                        regPC = nnn
                    }

                0x30 -> if (regV[regX] == b2) regPC += 2

                0x40 -> if (regV[regX] != b2) regPC += 2

                0x50 -> {
                    if (subOp != 0) {
                        Log.i("ultra8", "FATAL: Illegal opcode " + Integer.toHexString(word))
                        running = false
                    }
                    if (regV[regX] == regV[regY]) {
                        regPC += 2
                    }
                }

                0x60 -> regV[regX] = b2

                0x70 -> {
                    regV[regX] = regV[regX] + b2
                    regV[15] = if (((regV[regX] and 0x100) != 0)) 1 else 0
                    regV[regX] = regV[regX] and 0xFF
                }

                0x80 -> when (subOp) {
                    0x00 -> regV[regX] = regV[regY]

                    0x01 -> regV[regX] = regV[regX] or regV[regY]

                    0x02 -> regV[regX] = regV[regX] and regV[regY]
                    0x03 -> regV[regX] = regV[regX] xor regV[regY]
                    0x04 -> {
                        regV[regX] += regV[regY]
                        regV[regX] = regV[regX] and 0xFF
                        regV[15] = if (((regV[regX] and 0x100) != 0)) 1 else 0
                    }

                    0x05 -> {
                        regV[regX] = (regV[regX] - regV[regY])
                        regV[15] = (if ((regV[regX] and 0x100) == 0) 1 else 0)
                        regV[regX] = regV[regX] and 0xFF
                    }

                    0x06 -> {
                        regV[15] = (regV[regX] and 0x01)
                        regV[regX] = regV[regX] ushr 1
                    }

                    0x07 -> {
                        regV[regX] = (regV[regY] - regV[regX])
                        regV[15] = (if ((regV[regX] and 0x100) == 0) 0 else 1)
                        regV[regX] = regV[regX] and 0xFF
                    }

                    0x0E -> {
                        regV[15] = (if ((regV[regX] and 0x80) == 0x80) 1 else 0)
                        regV[regX] = regV[regX] shl 1
                        regV[regX] = regV[regX] and 0xFF
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
                    if (regV[regX] != regV[regY]) regPC += 2
                }

                0xA0 -> regI = nnn
                0xB0 -> regPC = regV[0] + nnn
                0xC0 -> regV[regX] = random.nextInt(b2 + 1)
                0xD0 -> regV[15] =
                    if (gfx.putSprite(regV[regX], regV[regY], mem, regI, subOp)) 1 else 0

                0xE0 -> when (b2) {
                    0x9E -> if (input.keyPressed(regV[regX])) {
                        regPC += 2
                    }

                    0xA1 -> if (!input.keyPressed(regV[regX])) {
                        regPC += 2
                    }

                    else -> {
                        Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                        running = false
                    }
                }

                0xF0 -> when (b2) {
                    0x07 -> regV[regX] = timer.value
                    0x0A -> {
                        synchronized(input) {
                            val pressed = input.awaitPress()
                            Log.i("ultra8", "Waited and got key $pressed")
                            regV[regX] = pressed
                        }
                    }

                    0x15 -> timer.value = regV[regX]

                    0x18 -> {}
                    0x1E -> regI += regV[regX]
                    0x29 -> regI = (fontStart + regV[regX] * 5)

                    0x33 -> {
                        tmp = regV[regX]
                        i = 0
                        while (tmp > 99) {
                            tmp -= 100
                            i++
                        }
                        mem[regI] = i
                        i = 0
                        while (tmp > 9) {
                            tmp -= 10
                            i++
                        }
                        mem[regI + 1] = i
                        i = 0
                        while (tmp > 0) {
                            tmp--
                            i++
                        }
                        mem[regI + 2] = i
                    }

                    0x55 -> System.arraycopy(regV, 0, mem, regI, regX + 1)
                    0x65 -> System.arraycopy(mem, regI, regV, 0, regX + 1)
                    0x75 -> System.arraycopy(regHP, 0, mem, regI, regX + 1)
                    0x85 -> System.arraycopy(mem, regI, regHP, 0, regX + 1)

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
        Log.i("ultra8", timeCounts.toString())
        return regPC
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
