package com.emerjbl.ultra8.chip8.machine

/**
 * The various quirks to consider when running a program.
 *
 * Most of the descriptions are from Timendus's:
 * https://github.com/chip-8/chip-8-database/blob/master/database/quirks.json
 *
 * The names of the classes indicate the behavior when the quirk is enabled. Refer to the
 * description text for clarification.
 */
sealed interface Quirk {
    val enabled: Boolean
    val description: String

    @JvmInline
    value class ShiftXOnly(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "On most systems the shift opcodes take `vY` as input and stores the shifted " +
                    "version of `vY` into `vX`.The interpreters for the HP48 took `vX` as both " +
                    "the input and the output, introducing the shift quirk. " +
                    "NOT DONE."
    }

    @JvmInline
    value class MemoryIncrementByX(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "On most systems storing and retrieving data between registers and memory " +
                    "increments the `i` register with `X + 1` (the number of registers read or " +
                    "written). So for each register read or writen, the index register would be " +
                    "incremented. The CHIP-48 interpreter for the HP48 would only increment the " +
                    "`i` register by `X`, introducing the first load/store quirk. " +
                    "NOT DONE."
    }

    @JvmInline
    value class MemoryIUnchanged(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "On most systems storing and retrieving data between registers and memory " +
                    "increments the `i` register relative to the number of registers read or " +
                    "written. The Superchip 1.1 interpreter for the HP48 however did not " +
                    "increment the `i` register at all, introducing the second load/store quirk. " +
                    "NOT DONE"
    }

    @JvmInline
    value class SpriteWrapQuirk(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "Most systems, when drawing sprites to the screen, will clip sprites at the " +
                    "edges of the screen. The Octo interpreter, which spawned the XO-CHIP " +
                    "variant of CHIP-8, instead wraps the sprite around to the other side of the " +
                    "screen. This introduced the wrap quirk." +
                    "NOT DONE"
    }

    @JvmInline
    value class BXNNJumpQuirk(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "The jump to `<address> + v0` opcode was wronly implemented on all the HP48 " +
                    "interpreters as jump to `<address> + vX`, introducing the jump quirk." +
                    "NOT DONE"
    }

    @JvmInline
    value class VSyncDraw(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "The original Cosmac VIP interpreter would wait for vertical blank before " +
                    "each sprite draw. This was done to prevent sprite tearing on the display, " +
                    "but it would also act as an accidental limit on the execution speed of the " +
                    "program. Some programs rely on this speed limit to be playable. Vertical " +
                    "blank happens at 60Hz, and as such its logic be combined with the timers." +
                    "NOT DONE"
    }

    @JvmInline
    value class CosmacLogicQuirk(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "On the original Cosmac VIP interpreter, `vF` would be reset after each " +
                    "opcode that would invoke the maths coprocessor. Later interpreters have not " +
                    "copied this behaviour." +
                    "NOT DONE"
    }

    @JvmInline
    value class OverwriteVFQuirk(override val enabled: Boolean) : Quirk {
        override val description: String
            get() = "Most implementations unconditionally set the vF flag register after " +
                    "performing a mathematical operation, even if the operation's destination " +
                    "register was vF. With this quirk enabled, the operation result will " +
                    "overwrite the carry flag result." +
                    "NOT DONE"
    }
}

data class Quirks(
    val shiftXOnly: Quirk.ShiftXOnly = Quirk.ShiftXOnly(false),
    val memoryIncrementByX: Quirk.MemoryIncrementByX = Quirk.MemoryIncrementByX(false),
    val memoryIUnchanged: Quirk.MemoryIUnchanged = Quirk.MemoryIUnchanged(false),
    val spriteWrapQuirk: Quirk.SpriteWrapQuirk = Quirk.SpriteWrapQuirk(false),
    val bxnnJumpQuirk: Quirk.BXNNJumpQuirk = Quirk.BXNNJumpQuirk(false),
    val vSyncDraw: Quirk.VSyncDraw = Quirk.VSyncDraw(false),
    val cosmacLogicQuirk: Quirk.CosmacLogicQuirk = Quirk.CosmacLogicQuirk(false),
    val overwriteVFQuirk: Quirk.OverwriteVFQuirk = Quirk.OverwriteVFQuirk(false)
)
