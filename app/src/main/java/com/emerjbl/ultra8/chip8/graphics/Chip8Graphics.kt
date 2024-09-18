package com.emerjbl.ultra8.chip8.graphics

/** The interface provided to Chip8 to control graphics. */
interface Chip8Graphics {
    fun clear()
    fun scrollRight()
    fun scrollLeft()
    fun scrollDown(n: Int)
    fun putSprite(xBase: Int, yBase: Int, data: ByteArray, offset: Int, linesIn: Int): Boolean
    var hires: Boolean
}

/** The interface provided to renderers of Chip8 graphics. */
interface Chip8Render<FT> {
    fun nextFrame(frameTime: Long): FT
}
