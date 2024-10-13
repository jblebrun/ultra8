package com.emerjbl.ultra8.chip8.machine

/** Helper for a Chip8 instruction
 *
 * Convenience properties for the various subfields.
 * A nice printing of known instructions.
 */
@JvmInline
value class Chip8Instruction(val word: Int) {
    constructor(b1: Int, b2: Int) : this((b1 shl 8) or b2)

    val b1
        get() = word shr 8
    val b2
        get() = word and 0xFF
    val majOp
        get() = b1 and 0xF0
    val nnn
        get() = word and 0xFFF
    val subOp
        get() = b2 and 0x0F
    val x
        get() = b1 and 0xF
    val y
        get() = (b2 shr 4)
    val n
        get() = b2 and 0x0F

    override fun toString(): String {
        return asm ?: "?? 0x${b1.b}${b2.b}"
    }

    val asm: String?
        get() = when (majOp) {
            0x00 -> when (b2) {
                0xE0 -> "CLS"

                0xEE -> "RET"

                0xFB -> "SCR"
                0xFC -> "SCL"
                0xFD -> "EXIT"

                0xFE -> "LOW"
                0xFF -> "HIGH"

                else -> if (y == 0xC) {
                    "SCD $subOp"
                } else {
                    null
                }
            }

            0x10 -> "JP ${nnn.sx}"

            0x20 -> "CALL 0x${nnn.sx}"

            0x30 -> "SE ${x.v}, ${b2.b}"
            0x40 -> "SNE ${x.v}, ${b2.b}"

            0x50 -> "SE ${x.v}, ${y.v}"
            0x60 -> "LD ${x.v}, ${b2.b}"

            0x70 -> "ADD ${x.v}, ${b2.b}"

            0x80 -> when (subOp) {
                0x00 -> "LD ${x.v}, ${y.v}"
                0x01 -> "OR ${x.v}, ${y.v}"
                0x02 -> "AND ${x.v}, ${y.v}"
                0x03 -> "XOR ${x.v}, ${y.v}"
                0x04 -> "ADD ${x.v}, ${y.v}"
                0x05 -> "SUB ${x.v}, ${y.v}"
                0x06 -> "SHR ${x.v}, ${y.v}"
                0x07 -> "SUBN ${x.v}, ${y.v}"
                0x0E -> "SHL ${x.v}, ${y.v}"
                else -> null
            }

            0x90 -> {
                when (n) {
                    0 -> "SNE ${x.v} ${y.b}"
                    else -> null
                }
            }

            0xA0 -> "LD I, ${nnn.sx}"
            0xB0 -> "JP V0, ${nnn.sx}"
            0xC0 -> "RND ${x.v}, ${b2.b}"
            0xD0 -> "DRW ${x.v}, ${y.v}"
            0xE0 -> when (b2) {
                0x9E -> "SKP ${x.v}"
                0xA1 -> "SKNP ${x.v}"
                else -> null
            }

            0xF0 -> when (b2) {
                0x00 -> "long NNNNN"
                0x07 -> "LD ${x.v}, DT"
                0x0A -> "LD ${x.v}, K"

                0x15 -> "LD DT, ${x.v}"
                0x18 -> "LD ST, ${x.v}"

                0x1E -> "ADD I,${x.v}"
                0x29 -> "LD F,${x.v}"
                0x30 -> "LD HF, ${x.v}"

                0x33 -> "LD B,${x.v}"

                0x55 -> "LD [I], ${x.v}"
                0x65 -> "LD ${x.v}, [I]"

                0x75 -> "LD R, ${x.v}"
                0x85 -> "LD ${x.v}, R"

                else -> null
            }

            else -> null
        }

    companion object {
        private val Int.b: String
            get() = "0x${toByte().toHexString()}"

        private val Int.sx: String
            get() = "0x${toShort().toHexString()}"

        private val V_FORMAT = HexFormat {
            number.removeLeadingZeros = true
        }

        val Int.v: String
            get() = "V${toByte().toHexString(V_FORMAT)}"
    }

}
