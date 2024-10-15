package com.emerjbl.ultra8.chip8.graphics

import com.emerjbl.ultra8.util.LockGuarded
import java.util.concurrent.locks.ReentrantLock

/**
 * Manages a currently active Chip8 frame.
 *
 * It manages the current graphics mode, and one currently active frame.
 * Chip8 drawing instructions alter the data here.
 *
 * It supports 2-plane rendering for Chip-XO.
 *
 * You can restore an instance with data as part of the resume sequence.
 */
class FrameManager(hires: Boolean, frame: Frame) {
    /** Create a fresh FrameManager. Starts in lo-res mode. */
    constructor() : this(false, lowRes())

    private var frame: LockGuarded<Frame> = LockGuarded(ReentrantLock(), frame)

    /**
     * Set this to change the screen mode.
     *
     * Every time you set the value, the screen is cleared.
     */
    var hires: Boolean = hires
        set(value) {
            field = value
            frame.update(if (value) hiRes() else lowRes())
        }

    /** Create a complete copy of this manager. */
    fun clone() = FrameManager(hires, frame.withLock { it.clone() })

    /** Clear the screen. */
    fun clear() {
        frame.withLock { it.data.fill(0) }
    }

    /** Scroll right by 4 pixels. (SCR instruction) */
    fun scrollRight() = frame.withLock { frame ->
        for (row in 0 until frame.width) {
            val rowStart = row * frame.width
            val startIndex = row * frame.width
            val destinationOffset = rowStart + 4
            val endIndex = rowStart + frame.width - 4
            frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
            frame.data.fill(0, 0, 4)
        }
    }

    /** Scroll left by 4 pixels. (SCL instruction). */
    fun scrollLeft() = frame.withLock { frame ->
        for (row in 0 until frame.height) {
            val rowStart = row * frame.width
            val startIndex = rowStart + 4
            val destinationOffset = row * frame.width
            val endIndex = rowStart + (frame.width)
            frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
            frame.data.fill(0, endIndex - 4, endIndex)
        }
    }

    /** Scroll down by N pixels (SCD instruction). */
    fun scrollDown(n: Int) = frame.withLock { frame ->
        val destinationOffset = n * frame.width
        val startIndex = 0
        val endIndex = (frame.height - n) * frame.width
        frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
        frame.data.fill(0, 0, n * frame.width)
    }

    /** Scroll up by N pixels (SCU instruction, Chip-XO). */
    fun scrollUp(n: Int) = frame.withLock { frame ->
        val destinationOffset = 0
        val startIndex = n * frame.width
        val endIndex = frame.width * frame.height
        frame.data.copyInto(frame.data, destinationOffset, startIndex, endIndex)
        frame.data.fill(0, 0, n * frame.width)
    }

    /** DRW instruction implementation. */
    fun putSprite(
        /** X-coordinate in Chip8 pixels for the top-left. */
        xBase: Int,
        /** Y-coordinate in Chip8 pixels for the top-left. */
        yBase: Int,
        /** The data that provides the sprite data. */
        data: ByteArray,
        /** The offset into the data. */
        offset: Int,
        /** The number of lines of sprite data. */
        linesIn: Int,
        /**
         * The plane(s) to draw to.
         *
         * Remember that drawing to plane 3 draws twice as much data.
         **/
        planes: Int,
    ): Boolean {
        val plane3Offset = if (linesIn == 0) 32 else linesIn
        return when (planes) {
            1 -> putPlaneSprite(xBase, yBase, data, offset, linesIn, 1)
            2 -> putPlaneSprite(xBase, yBase, data, offset, linesIn, 2)
            // Note: Use of non-short-circuiting `or` here, so both plans are always drawn.
            3 -> putPlaneSprite(xBase, yBase, data, offset, linesIn, 1) or
                    putPlaneSprite(xBase, yBase, data, offset + plane3Offset, linesIn, 2)

            else -> false
        }
    }

    private fun putPlaneSprite(
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

    /**
     * Get a copy of the next frame to render.
     *
     * If a [Frame] is provided, it will be filled with the current frame data.
     * Otherwise, a new [Frame] will be allocated and returned. You can provide
     * existing frames to reuse, to reduce allocations.
     **/
    fun nextFrame(lastFrame: Frame?): Frame = frame.withLock { frame ->
        if (lastFrame?.height == frame.height && lastFrame.width == frame.width) {
            frame.data.copyInto(lastFrame.data)
            lastFrame
        } else {
            frame.clone()
        }
    }

    /** An actual active frame.
     *
     *  It can be initialized with existing data, or created fresh for a given size.
     */
    class Frame(
        /** The width of the screen in Chip8 pixels. */
        val width: Int,

        /** The height of the screen in Chip8 pixels. */
        val height: Int,

        /** The packing pixel data. */
        val data: IntArray
    ) {
        /** Create a new empty frame. */
        constructor(width: Int, height: Int) : this(width, height, IntArray(width * height))

        /** Provide a complete copy of this frame, separate from this instance. */
        fun clone(): Frame = Frame(width, height, data.clone())
    }


    companion object {
        /** Create a new empty frame at Chip8 low resolution. */
        private fun lowRes() = Frame(64, 32)

        /** Create a new empty frame at Chip8 high resolution. */
        private fun hiRes() = Frame(128, 64)
    }
}
