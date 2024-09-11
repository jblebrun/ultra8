package com.emerjbl.ultra8

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import java.util.Random
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.TimeSource

private val Int.b
    get() = toByte()
private val Byte.i
    get() = toUByte().toInt()

private const val EXEC_START: Int = 0x200
private const val FONT_START: Int = 0x100

private val font: ByteArray = byteArrayOf(
    0xF0.b, 0x90.b, 0x90.b, 0x90.b, 0xF0.b,
    0x20.b, 0x60.b, 0x20.b, 0x20.b, 0x70.b,
    0xF0.b, 0x10.b, 0xF0.b, 0x80.b, 0xF0.b,
    0xF0.b, 0x10.b, 0x70.b, 0x10.b, 0xF0.b,
    0xA0.b, 0xA0.b, 0xF0.b, 0x20.b, 0x20.b,
    0xF0.b, 0x80.b, 0xF0.b, 0x10.b, 0xF0.b,
    0xF0.b, 0x80.b, 0xF0.b, 0x90.b, 0xF0.b,
    0xF0.b, 0x10.b, 0x10.b, 0x10.b, 0x10.b,
    0x60.b, 0x90.b, 0x60.b, 0x90.b, 0x60.b,
    0xF0.b, 0x90.b, 0xF0.b, 0x10.b, 0x10.b,
    0x60.b, 0x90.b, 0xF0.b, 0x90.b, 0x90.b,
    0xE0.b, 0x90.b, 0xE0.b, 0x90.b, 0xE0.b,
    0xF0.b, 0x80.b, 0x80.b, 0x80.b, 0xF0.b,
    0xE0.b, 0x90.b, 0x90.b, 0x90.b, 0xE0.b,
    0xF0.b, 0x80.b, 0xF0.b, 0x80.b, 0xF0.b,
    0xF0.b, 0x80.b, 0xE0.b, 0x80.b, 0x80.b
)

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

/** The registers of a Chip8 machine. */
class Chip8(
    val keys: Chip8Keys,
    val gfx: Chip8Graphics,
    timeSource: TimeSource,
    program: ByteArray
) {
    val v = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val hp = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
    val stack = IntArray(64)
    var i: Int = 0
    var sp = 0
    var pc = 0x200
    private val mem: ByteArray = ByteArray(4096).apply {
        font.copyInto(this, FONT_START)
        program.copyInto(this, EXEC_START)
    }
    private val random: Random = Random()
    private val timer: Chip8Timer = Chip8Timer(timeSource)
    private val tg: ToneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)


    fun step(): Boolean {
        val b1 = mem[pc++].i
        val b2 = mem[pc++].i
        val word = (b1 shl 8) or b2
        val majOp = b1 and 0xF0
        val nnn = word and 0xFFF
        val subOp = b2 and 0x0F
        val x = b1 and 0xF
        val y = (b2 shr 4)

        when (majOp) {
            0x00 -> when (b2) {
                0xE0 -> gfx.frameBuffer.clear()

                0xEE -> {
                    if (sp < 0) {
                        Log.i("ultra8", "FATAL: RET when stack is empty")
                        return false
                    }
                    pc = stack[--sp]
                }

                0xFB -> gfx.frameBuffer.scrollRight()
                0xFC -> gfx.frameBuffer.scrollLeft()
                0xFD -> {
                    Log.i("ultra8", "Normal: EXIT instruction")
                    return false
                }

                0xFE -> gfx.hires = false

                0xFF -> gfx.hires = true

                else -> if (y == 0xC) {
                    gfx.frameBuffer.scrollDown(subOp)
                } else {
                    Log.i(
                        "ultra8",
                        "FATAL: trying to execute 0 byte, program missing halt"
                    )
                    return false
                }
            }

            0x20 -> {
                stack[sp++] = pc

                if (pc == nnn + 2) {
                    Log.i(
                        "Ultra8",
                        "normal: would spin in endless loop, stopping emulation"
                    )
                    return false
                } else {
                    pc = nnn
                }
            }

            0x10 ->
                if (pc == nnn + 2) {
                    Log.i(
                        "Ultra8",
                        "normal: would spin in endless loop, stopping emulation"
                    )
                    return false
                } else {
                    pc = nnn
                }

            0x30 -> if (v[x] == b2) pc += 2

            0x40 -> if (v[x] != b2) pc += 2

            0x50 -> {
                if (subOp != 0) {
                    Log.i("ultra8", "FATAL: Illegal opcode " + Integer.toHexString(word))
                    return false
                }
                if (v[x] == v[y]) {
                    pc += 2
                }
            }

            0x60 -> v[x] = b2

            0x70 -> {
                v[x] = v[x] + b2
                v[15] = if (((v[x] and 0x100) != 0)) 1 else 0
                v[x] = v[x] and 0xFF
            }

            0x80 -> when (subOp) {
                0x00 -> v[x] = v[y]

                0x01 -> v[x] = v[x] or v[y]

                0x02 -> v[x] = v[x] and v[y]
                0x03 -> v[x] = v[x] xor v[y]
                0x04 -> {
                    v[x] += v[y]
                    v[x] = v[x] and 0xFF
                    v[15] = if (((v[x] and 0x100) != 0)) 1 else 0
                }

                0x05 -> {
                    v[x] = (v[x] - v[y])
                    v[15] = (if ((v[x] and 0x100) == 0) 1 else 0)
                    v[x] = v[x] and 0xFF
                }

                0x06 -> {
                    v[15] = (v[x] and 0x01)
                    v[x] = v[x] ushr 1
                }

                0x07 -> {
                    v[x] = (v[y] - v[x])
                    v[15] = (if ((v[x] and 0x100) == 0) 0 else 1)
                    v[x] = v[x] and 0xFF
                }

                0x0E -> {
                    v[15] = (if ((v[x] and 0x80) == 0x80) 1 else 0)
                    v[x] = v[x] shl 1
                    v[x] = v[x] and 0xFF
                }

                else -> {
                    Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                    return false
                }
            }

            0x90 -> {
                if (subOp != 0) {
                    Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                    return false
                }
                if (v[x] != v[y]) pc += 2
            }

            0xA0 -> i = nnn
            0xB0 -> pc = v[0] + nnn
            0xC0 -> v[x] = random.nextInt(b2 + 1)
            0xD0 -> v[15] =
                if (gfx.frameBuffer.putSprite(v[x], v[y], mem, i, subOp)) 1 else 0

            0xE0 -> when (b2) {
                0x9E -> if (keys.pressed(v[x])) {
                    pc += 2
                }

                0xA1 -> if (!keys.pressed(v[x])) {
                    pc += 2
                }

                else -> {
                    Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                    return false
                }
            }

            0xF0 -> when (b2) {
                0x07 -> v[x] = timer.value
                0x0A -> {
                    Log.i("ultra8", "WAITING FOR PRESS")
                    val pressed = keys.awaitKey()
                    Log.i("ultra8", "Waited and got key $pressed")
                    v[x] = pressed
                }

                0x15 -> timer.value = v[x]

                0x18 -> {}
                0x1E -> i += v[x]
                0x29 -> i = (FONT_START + v[x] * 5)

                0x33 -> {
                    var tmp = v[x]
                    var ctr = 0
                    while (tmp > 99) {
                        tmp -= 100
                        ctr++
                    }
                    mem[i] = ctr.b
                    ctr = 0
                    while (tmp > 9) {
                        tmp -= 10
                        ctr++
                    }
                    mem[i + 1] = ctr.b
                    ctr = 0
                    while (tmp > 0) {
                        tmp--
                        ctr++
                    }
                    mem[i + 2] = ctr.b
                }

                0x55 -> {
                    for (j in 0..x) {
                        mem[i + j] = v[j].b
                    }
                }

                0x65 -> {
                    for (j in 0..x) {
                        v[j] = mem[i + j].i
                    }
                }

                0x75 -> {
                    for (j in 0..x) {
                        mem[i + j] = hp[j].b
                    }
                }

                0x85 -> {
                    for (j in 0..x) {
                        hp[j] = mem[i + j].i
                    }
                }

                else -> {
                    Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                    return false
                }
            }

            else -> {
                Log.i("Ultra8", "FATAL: Illegal opcdoe $word")
                return false
            }
        }
        return true
    }
}

