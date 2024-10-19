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
class FrameManager(hires: Boolean, var targetPlane: Int, frame: Frame) {
    /** Create a fresh FrameManager. Starts in lo-res mode. */
    constructor() : this(false, 0x1, lowRes())

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
    fun clone() = FrameManager(hires, targetPlane, frame.withLock { it.clone() })

    /** Clear the screen. */
    fun clear() {
        frame.withLock { frame ->
            frame.operate(targetPlane) {
                it.data.fill(0)
            }
        }
    }

    /** Scroll right by 4 pixels. (SCR instruction) */
    fun scrollRight() = frame.withLock { frame ->
        frame.operate(targetPlane) { it.scrollRight() }
    }

    private fun Plane.scrollRight() {
        for (row in 0 until height) {
            val rowStart = row * width
            val startIndex = row * width
            val destinationOffset = rowStart + 4
            val endIndex = rowStart + width - 4
            data.copyInto(data, destinationOffset, startIndex, endIndex)
            data.fill(0, rowStart, rowStart + 4)
        }
    }

    /** Scroll left by 4 pixels. (SCL instruction) */
    fun scrollLeft() = frame.withLock { frame ->
        frame.operate(targetPlane) { it.scrollLeft() }
    }

    private fun Plane.scrollLeft() {
        for (row in 0 until height) {
            val rowStart = row * width
            val startIndex = rowStart + 4
            val destinationOffset = row * width
            val endIndex = rowStart + width
            data.copyInto(data, destinationOffset, startIndex, endIndex)
            data.fill(0, endIndex - 4, endIndex)
        }
    }

    /** Scroll up by N pixels (SCD instruction, Chip-XO). */
    fun scrollDown(n: Int) = frame.withLock { frame ->
        frame.operate(targetPlane) { it.scrollDown(n) }
    }

    private fun Plane.scrollDown(n: Int) {
        val destinationOffset = n * width
        val startIndex = 0
        val endIndex = (height - n) * width
        data.copyInto(data, destinationOffset, startIndex, endIndex)
        data.fill(0, 0, n * width)
    }

    /** Scroll up by N pixels (SCU instruction, Chip-XO). */
    fun scrollUp(n: Int) = frame.withLock { frame ->
        frame.operate(targetPlane) { it.scrollUp(n) }
    }

    private fun Plane.scrollUp(n: Int) {
        val destinationOffset = 0
        val startIndex = n * width
        val endIndex = width * height
        data.copyInto(data, destinationOffset, startIndex, endIndex)
        data.fill(0, (height - n) * width, height * width)
    }

    /** DRW instruction implementation. */
    fun putSprite(
        /** X-coordinate in Chip8 pixels for the top-left. */
        xBase: Int,
        /** Y-coordinate in Chip8 pixels for the top-left. */
        yBase: Int,
        /** The data that provides the sprite data. */
        src: ByteArray,
        /** The offset into the data. */
        offset: Int,
        /** The number of lines of sprite data. */
        linesIn: Int,
    ): Boolean {
        val plane3Offset = offset + if (linesIn == 0) 32 else linesIn
        return frame.withLock { frame ->
            when (targetPlane) {
                1 -> frame.plane1.putSprite(xBase, yBase, src, offset, linesIn)
                2 -> frame.plane2.putSprite(xBase, yBase, src, offset, linesIn)
                // Note: Use of non-short-circuiting `or` here, so both plans are always drawn.
                3 -> frame.plane1.putSprite(xBase, yBase, src, offset, linesIn) or
                        frame.plane2.putSprite(xBase, yBase, src, plane3Offset, linesIn)

                else -> false
            }
        }
    }

    private fun Plane.putSprite(
        xBase: Int,
        yBase: Int,
        src: ByteArray,
        offset: Int,
        linesIn: Int,
    ): Boolean {
        var unset = false
        val bytesPerRow = if (linesIn == 0) 2 else 1
        val lines = if (linesIn == 0) 16 else linesIn

        for (yOffset in 0 until lines) {
            for (rowByte in 0 until bytesPerRow) {
                var row = src[offset + (yOffset * bytesPerRow) + rowByte].toInt()
                val y = ((yBase + yOffset) and height - 1)
                for (xOffset in 0..7) {
                    if ((row and 0x80) != 0) {
                        val x = ((xBase + xOffset + rowByte * 8) and (width - 1))
                        val i = y * width + x
                        unset = unset || (data[i].toInt() == 1)
                        data[i] = (data[i].toInt() xor 1).toByte()
                    }
                    row = row shl 1
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
            frame.plane1.data.copyInto(lastFrame.plane1.data)
            frame.plane2.data.copyInto(lastFrame.plane2.data)
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

        /** The backing pixel data. */
        plane1: ByteArray = ByteArray(width * height),
        plane2: ByteArray = ByteArray(width * height)
    ) {
        val plane1 = Plane(width, height, plane1)
        val plane2 = Plane(width, height, plane2)

        inline fun operate(targetPlane: Int, action: (Plane) -> Unit) {
            when (targetPlane) {
                1 -> action(plane1)
                2 -> action(plane2)
                3 -> {
                    action(plane1)
                    action(plane2)
                }
            }

        }

        /** Provide a complete copy of this frame, separate from this instance. */
        fun clone(): Frame = Frame(width, height, plane1.data.clone(), plane2.data.clone())
    }

    class Plane(val width: Int, val height: Int, val data: ByteArray)


    companion object {
        /** Create a new empty frame at Chip8 low resolution. */
        private fun lowRes() = Frame(64, 32)

        /** Create a new empty frame at Chip8 high resolution. */
        private fun hiRes() = Frame(128, 64)
    }
}
