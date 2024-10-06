package com.emerjbl.ultra8.chip8.machine

import android.util.Log
import com.emerjbl.ultra8.chip8.graphics.Chip8Font
import com.emerjbl.ultra8.chip8.graphics.Chip8Graphics
import com.emerjbl.ultra8.chip8.input.Chip8Keys
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import com.emerjbl.ultra8.chip8.sound.Pattern
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.Random
import kotlin.math.pow
import kotlin.time.TimeSource

private const val EXEC_START: Int = 0x200
private const val FONT_START: Int = 0x000
private const val HIRES_FONT_START: Int = 0x100

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

    class InvalidBitPlane(pc: Int, val x: Int) : Halt(pc) {
        override fun toString() = "BADPLANE ($x) at 0x${pc.sx}"
    }
}

/** All state of a running Chip8 Machine. */
class Chip8(
    private val keys: Chip8Keys,
    private val gfx: Chip8Graphics,
    private val sound: Chip8Sound,
    private val font: Chip8Font,
    timeSource: TimeSource,
    program: ByteArray
) {
    /** Collect all Chip8 state in one place. Convenient for eventual save/restore. */
    class State(
        /** Registers V0-VF. */
        internal val v: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        /**
         * Special HP flag registers.
         *
         * In the HP-48 implementation, these were hardware-persisted across runs by a special 64
         * bit register file.
         *
         * Chip-XO supports up to 16 registers.
         *
         * These *should* be persisted, but we don't currently do that.
         **/
        internal val hp: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        /** Call stack. */
        internal val stack: IntArray = IntArray(64),
        /**
         * Machine memory.
         *
         * The original machine  implementation is 4k, but Chip-XO can address up to 64k.
         * For now, we just support the highest.
         **/
        internal val mem: ByteArray = ByteArray(65536),
        /** Index register I. */
        internal var i: Int = 0,
        /** Stack pointer. */
        internal var sp: Int = 0,
        /** Program counter. */
        internal var pc: Int = 0x200,
        /** Chip-XO target plane for drawing (0-3). */
        internal var targetPlane: Int = 0x1
    ) {
        /**
         * A read-only view of the Chip8 state.
         *
         * A given instance wraps an actual live machine state, so it will change as the machine does;
         * it's not a static copy.
         */
        interface View {
            val v: IntBuffer
            val hp: IntBuffer
            val stack: IntBuffer
            val mem: ByteBuffer
            val i: Int
            val sp: Int
            val pc: Int
            val targetPlane: Int
        }

        val stateView = object : View {
            override val v: IntBuffer = IntBuffer.wrap(this@State.v).asReadOnlyBuffer()
            override val hp: IntBuffer = IntBuffer.wrap(this@State.hp).asReadOnlyBuffer()
            override val stack: IntBuffer = IntBuffer.wrap(this@State.stack).asReadOnlyBuffer()
            override val mem: ByteBuffer = ByteBuffer.wrap(this@State.mem).asReadOnlyBuffer()
            override val i: Int
                get() = this@State.i
            override val sp: Int
                get() = this@State.sp
            override val pc: Int
                get() = this@State.pc
            override val targetPlane: Int
                get() = this@State.targetPlane
        }

        /** Create a deep copy of the entire state. */
        fun clone() = State(
            v.clone(), hp.clone(), stack.clone(), mem.clone(),
            i, sp, pc, targetPlane,
        )

    }

    private val state = State().apply {
        font.lo.copyInto(mem, FONT_START)
        font.hi.copyInto(mem, HIRES_FONT_START)
        program.copyInto(mem, EXEC_START)
    }
    val stateView = state.stateView

    private val random: Random = Random()
    private val timer: Chip8Timer = Chip8Timer(timeSource)

    fun step(): Halt? {
        val inst = Chip8Instruction(state.mem[state.pc++].i, state.mem[state.pc++].i)
        return state.run(inst)
    }

    private fun State.run(inst: Chip8Instruction): Halt? {
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

                    else -> when (y) {
                        0xC -> gfx.scrollDown(subOp)
                        0xD -> gfx.scrollUp(subOp)
                        else -> return Halt.IllegalOpcode(pc - 2, word)
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
                    when (subOp) {
                        0x00 -> if (v[x] == v[y]) {
                            pc += 2
                        }

                        0x02 -> {
                            // Load vx-vy into memory starting at i, don't change i
                            for (vi in x..y) {
                                mem[i + vi - x] = v[vi].b
                            }
                        }

                        0x03 -> {
                            // Load vx-vy from memory starting at i, don't change i
                            for (vi in x..y) {
                                v[vi] = mem[i + vi - x].i
                            }
                        }

                        else -> return Halt.IllegalOpcode(pc - 2, word)
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
                    if (gfx.putSprite(v[x], v[y], mem, i, subOp, targetPlane)) 1 else 0

                0xE0 -> when (b2) {
                    0x9E -> if (keys.pressed(v[x])) pc += 2
                    0xA1 -> if (!keys.pressed(v[x])) pc += 2
                    else -> return Halt.IllegalOpcode(pc - 2, word)
                }

                0xF0 -> when (b2) {
                    0x00 -> {
                        i = mem[pc++].i
                        i = i shl 8
                        i = i or mem[pc++].i
                    }

                    0x01 -> {
                        if (inst.x > 3 || inst.x < 0) {
                            return Halt.InvalidBitPlane(pc - 2, x)
                        }
                        targetPlane = inst.x
                    }

                    0x02 -> {
                        var low = 0UL
                        var hi = 0UL
                        for (offset in 0 until 8) {
                            low = low shl 8
                            hi = hi shl 8
                            low = low or mem[i + offset].toUByte().toULong()
                            hi = hi or mem[i + 8 + offset].toUByte().toULong()
                        }
                        sound.setPattern(Pattern(low, hi))
                    }

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

                    0x3A -> {
                        sound.setPatternRate(
                            (4000 * 2.0).pow((((v[x] - 64) / 48.0))).toInt()
                        )
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
