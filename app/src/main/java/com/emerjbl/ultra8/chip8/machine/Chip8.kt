package com.emerjbl.ultra8.chip8.machine

import android.util.Log
import com.emerjbl.ultra8.chip8.graphics.Chip8Font
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.graphics.StandardChip8Font
import com.emerjbl.ultra8.chip8.machine.StepResult.Halt
import com.emerjbl.ultra8.chip8.sound.Chip8Sound
import com.emerjbl.ultra8.chip8.sound.Pattern
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow
import kotlin.time.TimeSource

/** All state of a running Chip8 Machine. */
class Chip8(
    private val sound: Chip8Sound,
    timeSource: TimeSource,
    private val state: State,
) {
    /** Collect all Chip8 state in one place. Convenient for eventual save/restore. */
    class State(
        /** Registers V0-VF. */
        val v: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
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
        val hp: IntArray = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        /** Call stack. */
        val stack: IntArray = IntArray(64),
        /**
         * Machine memory.
         *
         * The original machine  implementation is 4k, but Chip-XO can address up to 64k.
         * For now, we just support the highest.
         **/
        val mem: ByteArray = ByteArray(65536),
        /** Index register I. */
        var i: Int = 0,
        /** Stack pointer. */
        var sp: Int = 0,
        /** Program counter. */
        var pc: Int = EXEC_START,

        /** Graphics buffer is an important part of state, too. */
        val gfx: FrameManager = FrameManager(),

        /** The last halt condition for the machine. */
        var halted: Halt? = null
    ) {
        /**
         * A read-only view of the Chip8 state.
         *
         * A given instance wraps an actual live machine state, so it will change as the machine does;
         * it's not a static copy.
         */
        @Suppress("unused", "MemberVisibilityCanBePrivate")
        class View(private val state: State) {
            val v: IntBuffer = IntBuffer.wrap(state.v).asReadOnlyBuffer()
            val hp: IntBuffer = IntBuffer.wrap(state.hp).asReadOnlyBuffer()
            val stack: IntBuffer = IntBuffer.wrap(state.stack).asReadOnlyBuffer()
            val mem: ByteBuffer = ByteBuffer.wrap(state.mem).asReadOnlyBuffer()
            val i: Int get() = state.i
            val sp: Int get() = state.sp
            val pc: Int get() = state.pc
            val halted: Halt? get() = state.halted

            /** Create a deep copy of the entire state. */
            fun clone() = State(
                state.v.clone(),
                state.hp.clone(),
                state.stack.clone(),
                state.mem.clone(),
                i,
                sp,
                pc,
                state.gfx.clone(),
                state.halted
            )
        }
    }

    val stateView = State.View(state)

    fun nextFrame(frame: FrameManager.Frame?): FrameManager.Frame = state.gfx.nextFrame(frame)

    private val random: Random = Random()
    private val timer: Chip8Timer = Chip8Timer(timeSource)

    private object Keys {
        private val keys = AtomicInteger(0)
        private fun mask(idx: Int) = 1 shl idx
        private val keyDown = MutableSharedFlow<Int>(extraBufferCapacity = 1)
        private val keyUp = MutableSharedFlow<Int>(extraBufferCapacity = 1)

        fun keyDown(idx: Int) {
            keys.getAndUpdate { it or mask(idx) }
            keyDown.tryEmit(idx)
        }

        fun keyUp(idx: Int) {
            keys.getAndUpdate { it and mask(idx).inv() }
            keyUp.tryEmit(idx)
        }

        fun isKeyPressed(idx: Int): Boolean = keys.get() and mask(idx) > 0

        suspend fun awaitKey(): Int {
            Log.i("ultra8", "WAITING FOR PRESS")
            val nextDown = keyDown.first()
            keyUp.filter { it == nextDown }.first()
            return nextDown
        }
    }

    fun keyDown(idx: Int) = Keys.keyDown(idx)
    fun keyUp(idx: Int) = Keys.keyUp(idx)

    private val currentInstruction
        get() =
            Chip8Instruction(state.mem[state.pc].i, state.mem[state.pc + 1].i)

    /**
     * Execute up to cyclesPerTick instructions.
     *
     * This allows the machine to make tick-based decisions, which is useful for some quirks,
     * and to relinquish control during key awaits.
     */
    fun tick(cyclesPerTick: Int): StepResult {
        repeat(cyclesPerTick) {
            step()
                .takeIf { it !is StepResult.Continue }
                ?.let { return it }
        }
        return StepResult.Continue
    }

    /**
     * Execute the next program instruction.
     *
     * If the instruction results in a halt, the halt is returned.
     * If the machine was already halted, the last halt is returned.
     */
    fun step(): StepResult {
        state.halted?.let { return it }
        return state.run(currentInstruction).also { if (it is Halt) state.halted = it }
    }

    private fun jump(nnn: Int): StepResult {
        return if (state.pc == nnn) {
            Halt.Spin(state.pc)
        } else {
            state.pc = nnn
            StepResult.Continue
        }
    }

    private fun State.run(inst: Chip8Instruction): StepResult {
        inst.run {
            when (majOp) {
                0x00 -> when (b2) {
                    0xE0 -> gfx.clear()

                    0xEE -> {
                        if (sp < 1) return Halt.StackUnderflow(pc)
                        pc = stack[--sp] - 2
                    }

                    0xFB -> gfx.scrollRight()
                    0xFC -> gfx.scrollLeft()
                    0xFD -> return Halt.Exit(pc)

                    0xFE -> gfx.hires = false
                    0xFF -> gfx.hires = true

                    else -> when (y) {
                        0xC -> gfx.scrollDown(subOp)
                        0xD -> gfx.scrollUp(subOp)
                        else -> return Halt.IllegalOpcode(pc, word)
                    }
                }

                0x20 -> {
                    if (sp == stack.size - 1) return Halt.StackOverflow(pc)
                    if (pc == nnn) return Halt.Spin(pc)

                    stack[sp++] = pc + 2
                    return jump(nnn)
                }

                0x10 -> return jump(nnn)

                0x30 -> if (v[x] == b2) pc += 2
                0x40 -> if (v[x] != b2) pc += 2

                0x50 -> {
                    when (subOp) {
                        0x00 -> if (v[x] == v[y]) pc += 2

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

                        else -> return Halt.IllegalOpcode(pc, word)
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
                        v[0xF] = if (result > 0) 1 else 0
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

                    else -> return Halt.IllegalOpcode(pc, word)
                }

                0x90 -> {
                    if (subOp != 0) return Halt.IllegalOpcode(pc, word)
                    if (v[x] != v[y]) pc += 2
                }

                0xA0 -> i = nnn
                0xB0 -> return jump(v[0] + nnn)
                0xC0 -> v[x] = random.nextInt(0xFF) and b2
                0xD0 -> v[0xF] =
                    if (gfx.putSprite(v[x], v[y], mem, i, subOp)) 1 else 0

                0xE0 -> when (b2) {
                    0x9E -> if (Keys.isKeyPressed(v[x])) pc += 2
                    0xA1 -> if (!Keys.isKeyPressed(v[x])) pc += 2
                    else -> return Halt.IllegalOpcode(pc, word)
                }

                0xF0 -> when (b2) {
                    0x00 -> {
                        i = mem[pc + 2].i
                        i = i shl 8
                        i = i or mem[pc + 3].i
                    }

                    0x01 -> {
                        if (inst.x > 3 || inst.x < 0) {
                            return Halt.InvalidBitPlane(pc, x)
                        }
                        gfx.targetPlane = inst.x
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
                        return StepResult.Await {
                            v[x] = Keys.awaitKey()
                            pc += 2
                        }
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
                            (4000.0 * 2.0.pow(((v[x] - 64) / 48.0))).toInt()
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

                    else -> return Halt.IllegalOpcode(pc, word)
                }

                else -> return Halt.IllegalOpcode(pc, word)
            }
        }
        pc += currentInstruction.instructionBytes
        return StepResult.Continue
    }

    companion object {
        /** Generate a new state instance initialized with the provided [program] and [font] data. */
        fun stateForProgram(program: ByteArray, font: Chip8Font = StandardChip8Font) =
            State().apply {
                font.lo.copyInto(mem, FONT_START)
                font.hi.copyInto(mem, HIRES_FONT_START)
                program.copyInto(mem, EXEC_START)
            }

        private const val EXEC_START: Int = 0x200
        private const val FONT_START: Int = 0x000
        private const val HIRES_FONT_START: Int = 0x100
    }

}

/** The various halt conditions that can happen during execution. */
sealed interface StepResult {
    data object Continue : StepResult

    /**
     * The machine is waiting for a condition and will suspend until it's met.
     *
     * The continuation of the action is returned to the runner, so it can have
     * a chance to do other things before suspending.
     */
    data class Await(val await: suspend () -> Unit) : StepResult

    sealed interface Halt : StepResult {
        val pc: Int

        /** The EXIT command was encountered. */
        data class Exit(override val pc: Int) : Halt {
            override fun toString() = "Exit at ${pc.sx}"
        }

        /** A spin-jump was encountered (JMP to self). */
        data class Spin(override val pc: Int) : Halt {
            override fun toString() = "Exit (Spin) at ${pc.sx}"
        }

        /** Unknown or unimplemented opcode. */
        data class IllegalOpcode(override val pc: Int, val opcode: Int) : Halt {
            override fun toString() =
                "Illegal Operation (${opcode.sx}) at ${pc.sx}"
        }

        /** A return underflowed the stack. */
        data class StackUnderflow(override val pc: Int) : Halt {
            override fun toString() = "Stack Underflow at ${pc.sx}"
        }

        /** A call overflowed the stack. */
        data class StackOverflow(override val pc: Int) : Halt {
            override fun toString() = "Stack Overflow at ${pc.sx}"
        }

        /** An attempt to draw to an invalid bitplane. */
        data class InvalidBitPlane(override val pc: Int, val x: Int) : Halt {
            override fun toString() = "Draw to Invalid Bitplane ($x) at ${pc.sx}"
        }
    }
}

private val Int.sx: String
    get() = "0x${toShort().toHexString()}"

private val Int.b: Byte
    get() = toByte()

private val Byte.i: Int
    get() = toUByte().toInt()
