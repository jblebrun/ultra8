package com.emerjbl.ultra8.chip8.graphics

/** Convenience for byte literals below. */
private val Int.b: Byte
    get() = toByte()

/** Something that provides the two Chip8 fonts. */
interface Chip8Font {
    val lo: ByteArray
    val hi: ByteArray
}

/** The standard font in most Chip-8 engines. */
object StandardChip8Font : Chip8Font {
    override val lo = font
    override val hi = fonthi
}

private val font: ByteArray = byteArrayOf(
    // 0
    0b11110000.b,
    0b10010000.b,
    0b10010000.b,
    0b10010000.b,
    0b11110000.b,

    // 1
    0b00100000.b,
    0b01100000.b,
    0b00100000.b,
    0b00100000.b,
    0b01110000.b,

    // 2
    0b11110000.b,
    0b00010000.b,
    0b11110000.b,
    0b10000000.b,
    0b11110000.b,

    // 3
    0b11110000.b,
    0b00010000.b,
    0b01110000.b,
    0b00010000.b,
    0b11110000.b,

    // 4
    0b10100000.b,
    0b10100000.b,
    0b11110000.b,
    0b00100000.b,
    0b00100000.b,

    // 5
    0b11110000.b,
    0b10000000.b,
    0b11110000.b,
    0b00010000.b,
    0b11110000.b,

    // 6
    0b11110000.b,
    0b10000000.b,
    0b11110000.b,
    0b10010000.b,
    0b11110000.b,

    // 7
    0b11110000.b,
    0b00010000.b,
    0b00010000.b,
    0b00010000.b,
    0b00010000.b,

    // 8
    0b01100000.b,
    0b10010000.b,
    0b01100000.b,
    0b10010000.b,
    0b01100000.b,

    // 9
    0b11110000.b,
    0b10010000.b,
    0b11110000.b,
    0b00010000.b,
    0b00010000.b,

    // A
    0b01100000.b,
    0b10010000.b,
    0b11110000.b,
    0b10010000.b,
    0b10010000.b,

    // B
    0b11100000.b,
    0b10010000.b,
    0b11100000.b,
    0b10010000.b,
    0b11100000.b,

    // C
    0b11110000.b,
    0b10000000.b,
    0b10000000.b,
    0b10000000.b,
    0b11110000.b,

    // D
    0b11100000.b,
    0b10010000.b,
    0b10010000.b,
    0b10010000.b,
    0b11100000.b,

    // E
    0b11110000.b,
    0b10000000.b,
    0b11110000.b,
    0b10000000.b,
    0b11110000.b,

    // E
    0b11110000.b,
    0b10000000.b,
    0b11100000.b,
    0b10000000.b,
    0b10000000.b,
)

private val fonthi = byteArrayOf(
    // 0
    0b00111100.b,
    0b01100110.b,
    0b11000011.b,
    0b11000011.b,
    0b11000011.b,
    0b11000011.b,
    0b11000011.b,
    0b11000011.b,
    0b01100110.b,
    0b00111100.b,

    // 1
    0b00001100.b,
    0b00011100.b,
    0b00101100.b,
    0b00001100.b,
    0b00001100.b,
    0b00001100.b,
    0b00001100.b,
    0b00001100.b,
    0b00001100.b,
    0b00011110.b,

    // 2
    0b00111110.b,
    0b01111111.b,
    0b11000011.b,
    0b00000110.b,
    0b00001100.b,
    0b00011000.b,
    0b00110000.b,
    0b01100000.b,
    0b11111111.b,
    0b11111111.b,

    // 3
    0b00111100.b,
    0b01100110.b,
    0b11000011.b,
    0b00000011.b,
    0b00001110.b,
    0b00001110.b,
    0b00000011.b,
    0b11000011.b,
    0b01100110.b,
    0b00111100.b,


    // 4
    0b00000110.b,
    0b00001110.b,
    0b00011110.b,
    0b00110110.b,
    0b01100110.b,
    0b11000110.b,
    0b11111111.b,
    0b11111111.b,
    0b00000110.b,
    0b00000110.b,

    // 5
    0b11111111.b,
    0b11111111.b,
    0b11000000.b,
    0b11000000.b,
    0b11111100.b,
    0b11111110.b,
    0b00000111.b,
    0b11000111.b,
    0b01111100.b,
    0b00111000.b,

    // 6
    0b00111111.b,
    0b01111110.b,
    0b11000000.b,
    0b11000000.b,
    0b11111100.b,
    0b11111110.b,
    0b11000111.b,
    0b11000011.b,
    0b01111110.b,
    0b00111100.b,

    // 7
    0b11111111.b,
    0b11111111.b,
    0b00000011.b,
    0b00000110.b,
    0b00001100.b,
    0b00011000.b,
    0b00110000.b,
    0b01100000.b,
    0b11000000.b,
    0b11000000.b,

    // 8
    0b00111100.b,
    0b01100110.b,
    0b11000011.b,
    0b11000011.b,
    0b01111110.b,
    0b01111110.b,
    0b11000011.b,
    0b11000011.b,
    0b01100110.b,
    0b00111100.b,

    // 9
    0b00111100.b,
    0b01100110.b,
    0b11000011.b,
    0b11000011.b,
    0b01111111.b,
    0b00111111.b,
    0b00000011.b,
    0b00000011.b,
    0b01111110.b,
    0b11111100.b,
)
