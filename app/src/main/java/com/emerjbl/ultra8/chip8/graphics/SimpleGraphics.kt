package com.emerjbl.ultra8.chip8.graphics

import com.emerjbl.ultra8.util.LockGuarded
import java.util.concurrent.locks.ReentrantLock


class SimpleGraphics : Chip8Graphics, Chip8Render<SimpleGraphics.Frame> {
    class Frame private constructor(
        val width: Int,
        val height: Int,
        val data: IntArray
    ) {
        constructor(width: Int, height: Int) : this(width, height, IntArray(width * height))

        fun clone(): Frame = Frame(width, height, data.clone())
    }

    private var frame: LockGuarded<Frame> = LockGuarded(ReentrantLock(), lowRes())

    override var hires: Boolean = false
        set(value) {
            field = value
            frame.update(if (value) hiRes() else lowRes())
        }

    private fun lowRes() = Frame(64, 32)
    private fun hiRes() = Frame(128, 64)

    override fun clear() {
        frame.withLock { it.data.fill(0) }
    }

    override fun scrollRight() = frame.withLock { frame ->
        for (row in 0 until frame.width) {
            val rowStart = row * frame.width
            val startIndex = rowStart
            val destinationOffset = rowStart + 4
            val endIndex = rowStart + frame.width - 4
            frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
            frame.data.fill(0, 0, 4)
        }
    }

    override fun scrollLeft() = frame.withLock { frame ->
        for (row in 0 until frame.height) {
            val rowStart = row * frame.width
            val startIndex = rowStart + 4
            val destinationOffset = rowStart
            val endIndex = rowStart + (frame.width)
            frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
            frame.data.fill(0, endIndex - 4, endIndex)
        }
    }

    override fun scrollDown(n: Int) = frame.withLock { frame ->
        val destinationOffset = n * frame.width
        val startIndex = 0
        val endIndex = (frame.height - n) * frame.width
        frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
        frame.data.fill(0, 0, n * frame.width)
    }

    override fun putSprite(
        xBase: Int,
        yBase: Int,
        data: ByteArray,
        offset: Int,
        linesIn: Int
    ): Boolean {
        val lines = if (linesIn == 0) 16 else linesIn
        val bytesPerRow = if (linesIn == 0) 2 else 1
        var unset = false

        frame.withLock { frame ->
            for (yOffset in 0 until lines) {
                for (rowByte in 0 until bytesPerRow) {
                    var row = data[offset + (yOffset * bytesPerRow) + rowByte].toInt()
                    val y = ((yBase + yOffset) and frame.height - 1)
                    for (xOffset in 0..7) {
                        if ((row and 0x80) != 0) {
                            val x = ((xBase + xOffset + rowByte * 8) and (frame.width - 1))
                            val i = y * frame.width + x
                            unset = unset || (frame.data[i] == 1)
                            frame.data[i] = frame.data[i] xor 1
                        }
                        row = row shl 1
                    }
                }
            }
        }
        return unset
    }

    override fun nextFrame(lastFrame: Frame?): Frame = frame.withLock { frame ->
        if (lastFrame?.height == frame.height && lastFrame.width == frame.width) {
            frame.data.copyInto(lastFrame.data)
            lastFrame
        } else {
            frame.clone()
        }
    }
}
