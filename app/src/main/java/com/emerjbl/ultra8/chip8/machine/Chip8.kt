package com.emerjbl.ultra8.chip8.machine

import android.util.Log
import com.emerjbl.ultra8.chip8.graphics.Chip8Font
import com.emerjbl.ultra8.chip8.graphics.Chip8Graphics
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import java.util.Random
import kotlin.time.TimeSource

private const val EXEC_START: Int = 0x200
private const val FONT_START: Int = 0x000
private const val HIRES_FONT_START: Int = 0x100

@OptIn(ExperimentalStdlibApi::class)
private val Int.sx: String
    get() = "0x${toShort().toHexString()}"

private val Int.b: Byte
    get() = toByte()

private val Byte.i: Int
    get() = toUByte().toInt()

/** The various halt conditions that can happen during execution. */
sealed class Halt(val pc: Int) {
    /** The EXIT command was encountered. */
    class Exit(pc: Int) : Halt(pc) {
        override fun toString() = "EXIT at ${pc.sx}"
    }

    /** A spin-jump was encountered (JMP to self). */
    class Spin(pc: Int) : Halt(pc) {
        override fun toString() = "SPIN at 0x${pc.sx}"
    }

    /** Unknown or unimplemented opcode. */
    class IllegalOpcode(pc: Int, val opcode: Int) : Halt(pc) {
        override fun toString() =
            "ILLOP 0x${opcode.sx} at 0x${pc.sx}"
    }

    /** A return underflowed the stack. */
    class StackUnderflow(pc: Int) : Halt(pc) {
        override fun toString() = "UNDERFLOW at 0x${pc.sx}"
    }

    /** A call overflowed the stack. */
    class StackOverflow(pc: Int) : Halt(pc) {
        override fun toString() = "UNDERFLOW at 0x${pc.sx}"
    }
}

/** All state of a running Chip8 Machine. */
class Chip8(
    val keys: Chip8Keys,
    val gfx: Chip8Graphics,
    val sound: Chip8Sound,
    val font: Chip8Font,
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
        font.lo.copyInto(this, FONT_START)
        font.hi.copyInto(this, HIRES_FONT_START)
        program.copyInto(this, EXEC_START)
    }
    private val random: Random = Random()
    private val timer: Chip8Timer = Chip8Timer(timeSource)

    fun step(): Halt? {
        val inst = Chip8Instruction(mem[pc++].i, mem[pc++].i)
        inst.run {
            when (majOp) {
                0x00 -> when (b2) {
                    0xE0 -> gfx.clear()

                    0xEE -> {
                        if (sp < 0) return Halt.StackUnderflow(pc - 2)
                        pc = stack[--sp]
                    }

                    0xFB -> gfx.scrollRight()
                    0xFC -> gfx.scrollLeft()
                    0xFD -> return Halt.Exit(pc - 2)

                    0xFE -> gfx.hires = false
                    0xFF -> gfx.hires = true

                    else -> if (y == 0xC) {
                        gfx.scrollDown(subOp)
                    } else {
                        return Halt.IllegalOpcode(pc - 2, word)
                    }
                }

                0x20 -> {
                    if (sp == stack.size - 1) return Halt.StackOverflow(pc - 2)
                    stack[sp++] = pc

                    if (pc == nnn + 2) return Halt.Spin(pc - 2)

                    pc = nnn
                }

                0x10 -> if (pc == nnn + 2) {
                    return Halt.Spin(pc - 2)
                } else {
                    pc = nnn
                }

                0x30 -> if (v[x] == b2) pc += 2
                0x40 -> if (v[x] != b2) pc += 2

                0x50 -> {
                    if (subOp != 0) Halt.IllegalOpcode(pc - 2, word)

                    if (v[x] == v[y]) {
                        pc += 2
                    }
                }

                0x60 -> v[x] = b2

                0x70 -> {
                    // 0x70NN does *not* set the flag
                    v[x] = v[x] + b2
                    v[x] = v[x] and 0xFF
                }

                0x80 -> when (subOp) {
                    0x00 -> v[x] = v[y]

                    0x01 -> v[x] = v[x] or v[y]
                    0x02 -> v[x] = v[x] and v[y]
                    0x03 -> v[x] = v[x] xor v[y]

                    0x04 -> {
                        val result = v[x] + v[y]
                        v[x] = result and 0xFF
                        v[0xF] = if (result > 0xFF) 1 else 0
                    }

                    0x05 -> {
                        val result = v[x] - v[y]
                        v[x] = result and 0xFF
                        v[0xF] = if (result >= 0) 1 else 0
                    }

                    0x06 -> {
                        val vf = (v[x] and 0x01)
                        v[x] = v[x] ushr 1
                        v[0xF] = vf
                    }

                    0x07 -> {
                        val result = v[y] - v[x]
                        v[x] = result and 0xFF
                        v[0xF] = if (result >= 0) 1 else 0
                    }

                    0x0E -> {
                        val vf = (if ((v[x] and 0x80) == 0x80) 1 else 0)
                        v[x] = v[x] shl 1
                        v[x] = v[x] and 0xFF
                        v[0xF] = vf
                    }

                    else -> return Halt.IllegalOpcode(pc - 2, word)
                }

                0x90 -> {
                    if (subOp != 0) return Halt.IllegalOpcode(pc - 2, word)
                    if (v[x] != v[y]) pc += 2
                }

                0xA0 -> i = nnn
                0xB0 -> pc = v[0] + nnn
                0xC0 -> v[x] = random.nextInt(0xFF) and b2
                0xD0 -> v[0xF] =
                    if (gfx.putSprite(v[x], v[y], mem, i, subOp)) 1 else 0

                0xE0 -> when (b2) {
                    0x9E -> if (keys.pressed(v[x])) pc += 2
                    0xA1 -> if (!keys.pressed(v[x])) pc += 2
                    else -> return Halt.IllegalOpcode(pc - 2, word)
                }

                0xF0 -> when (b2) {
                    0x07 -> v[x] = timer.value
                    0x0A -> {
                        // If we are interrupted while waiting,
                        // come back to waiting.
                        pc -= 2
                        Log.i("ultra8", "WAITING FOR PRESS")
                        v[x] = keys.awaitKey()
                        pc += 2
                    }

                    0x15 -> timer.value = v[x]
                    0x18 -> sound.play(v[x])

                    0x1E -> i += v[x]
                    0x29 -> i = (FONT_START + v[x] * 5)
                    0x30 -> i = (HIRES_FONT_START + v[x] * 10)

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
                            hp[i + j] = v[j]
                        }
                    }

                    0x85 -> {
                        for (j in 0..x) {
                            v[j] = hp[i + j]
                        }
                    }

                    else -> return Halt.IllegalOpcode(pc - 2, word)
                }

                else -> return Halt.IllegalOpcode(pc - 2, word)
            }
        }
        return null
    }
}
