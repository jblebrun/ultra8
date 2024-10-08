package com.emerjbl.ultra8.chip8.graphics

import com.emerjbl.ultra8.util.LockGuarded
import java.util.concurrent.locks.ReentrantLock


class SimpleGraphics constructor(hires: Boolean, frame: SimpleGraphics.Frame) {
    class Frame constructor(
        val width: Int,
        val height: Int,
        val data: IntArray
    ) {
        constructor(width: Int, height: Int) : this(width, height, IntArray(width * height))

        fun clone(): Frame = Frame(width, height, data.clone())
    }

    constructor() : this(false, lowRes())

    fun clone() = SimpleGraphics(hires, frame.withLock { it.clone() })

    private var frame: LockGuarded<Frame> = LockGuarded(ReentrantLock(), frame)

    var hires: Boolean = hires
        set(value) {
            field = value
            frame.update(if (value) hiRes() else lowRes())
        }


    fun clear() {
        frame.withLock { it.data.fill(0) }
    }

    fun scrollRight() = frame.withLock { frame ->
        for (row in 0 until frame.width) {
            val rowStart = row * frame.width
            val startIndex = rowStart
            val destinationOffset = rowStart + 4
            val endIndex = rowStart + frame.width - 4
            frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
            frame.data.fill(0, 0, 4)
        }
    }

    fun scrollLeft() = frame.withLock { frame ->
        for (row in 0 until frame.height) {
            val rowStart = row * frame.width
            val startIndex = rowStart + 4
            val destinationOffset = rowStart
            val endIndex = rowStart + (frame.width)
            frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
            frame.data.fill(0, endIndex - 4, endIndex)
        }
    }

    fun scrollDown(n: Int) = frame.withLock { frame ->
        val destinationOffset = n * frame.width
        val startIndex = 0
        val endIndex = (frame.height - n) * frame.width
        frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
        frame.data.fill(0, 0, n * frame.width)
    }

    fun scrollUp(n: Int) = frame.withLock { frame ->
        val destinationOffset = 0
        val startIndex = n * frame.width
        val endIndex = frame.width * frame.height
        frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
        frame.data.fill(0, 0, n * frame.width)
    }

    fun putSprite(
        xBase: Int,
        yBase: Int,
        data: ByteArray,
        offset: Int,
        linesIn: Int,
        planes: Int,
    ): Boolean {
        val lines = if (linesIn == 0) 16 else linesIn
        return when (planes) {
            1 -> putPlaneSprite(xBase, yBase, data, offset, linesIn, 1)
            2 -> putPlaneSprite(xBase, yBase, data, offset, linesIn, 2)
            3 -> putPlaneSprite(xBase, yBase, data, offset, linesIn, 1) ||
                    putPlaneSprite(xBase, yBase, data, offset + lines, linesIn, 2)

            else -> false
        }
    }

    fun putPlaneSprite(
        xBase: Int,
        yBase: Int,
        data: ByteArray,
        offset: Int,
        linesIn: Int,
        plane: Int,
    ): Boolean {
        var unset = false
        val bytesPerRow = if (linesIn == 0) 2 else 1
        val lines = if (linesIn == 0) 16 else linesIn

        frame.withLock { frame ->
            for (yOffset in 0 until lines) {
                for (rowByte in 0 until bytesPerRow) {
                    var row = data[offset + (yOffset * bytesPerRow) + rowByte].toInt()
                    val y = ((yBase + yOffset) and frame.height - 1)
                    for (xOffset in 0..7) {
                        if ((row and 0x80) != 0) {
                            val x = ((xBase + xOffset + rowByte * 8) and (frame.width - 1))
                            val i = y * frame.width + x
                            unset = unset || (frame.data[i] and plane != 0)
                            frame.data[i] = frame.data[i] xor plane
                        }
                        row = row shl 1
                    }
                }
            }
        }
        return unset
    }

    fun nextFrame(lastFrame: Frame?): Frame = frame.withLock { frame ->
        if (lastFrame?.height == frame.height && lastFrame.width == frame.width) {
            frame.data.copyInto(lastFrame.data)
            lastFrame
        } else {
            frame.clone()
        }
    }

    companion object {
        private fun lowRes() = Frame(64, 32)
        private fun hiRes() = Frame(128, 64)
    }
}
