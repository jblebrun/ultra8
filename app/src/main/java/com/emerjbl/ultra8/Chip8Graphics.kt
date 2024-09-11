package com.emerjbl.ultra8

import android.graphics.Bitmap
import android.graphics.Color
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Chip8Graphics {
    class FrameBuffer(val width: Int, val height: Int, density: Int) {
        private val bitmap: Bitmap =
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                this.density = density
            }

        private val lock = ReentrantLock()

        private val pixels: IntArray = IntArray(width * height)

        // Time buffers track the fade time for the interpolator.
        // They store the amount of time remaining for the pixel to be on.
        private val timeBuffer = IntArray(width * height)

        private var lastFrameTime: Long = 0

        private val interpolator = AccelerateInterpolator(2f)

        fun scrollRight() {}
        fun scrollLeft() {}
        fun scrollDown(n: Int) {}


        fun nextFrame(frameTime: Long): Bitmap {
            val frameDiff = (frameTime - lastFrameTime).toInt()
            lastFrameTime = frameTime

            lock.withLock {
                for (i in timeBuffer.indices) {
                    if (timeBuffer[i] > 0) {
                        timeBuffer[i] = maxOf(0, timeBuffer[i] - frameDiff)
                        val newAlpha = if (timeBuffer[i] <= 0) {
                            0
                        } else {
                            val frameFrac = timeBuffer[i].toFloat() / FADE_TIME_MILLIS
                            val frac = interpolator.getInterpolation(frameFrac)
                            (pixels[i].alpha * frac).toInt()
                        }
                        pixels[i] = Color.argb(
                            newAlpha,
                            pixels[i].red,
                            pixels[i].green,
                            pixels[i].blue
                        )
                    }
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            }
            return bitmap
        }

        fun putSprite(xBase: Int, yBase: Int, data: ByteArray, offset: Int, linesIn: Int): Boolean {
            val lines = if (linesIn == 0) 16 else linesIn
            val bytesPerRow = if (linesIn == 0) 2 else 1
            var unset = false

            lock.withLock {
                for (yOffset in 0 until lines) {
                    for (rowByte in 0 until bytesPerRow) {
                        val row = data[offset + (yOffset * bytesPerRow) + rowByte].toInt()
                        for (xOffset in 0..7) {
                            val mask = 0x80 shr xOffset
                            if ((row and mask) != 0) {
                                val x = ((xBase + xOffset + rowByte * 8) and (width - 1))
                                val y = ((yBase + yOffset) and height - 1)
                                val i = y * width + x
                                unset = unset or (pixels[i] == SET_COLOR)
                                pixels[i] = if (pixels[i] == SET_COLOR) CLEAR_COLOR else SET_COLOR
                                timeBuffer[i] =
                                    if (pixels[i] == CLEAR_COLOR) FADE_TIME_MILLIS else 0
                            }
                        }
                    }
                }
            }
            return unset
        }

        fun clear() {
            lock.withLock {
                pixels.fill(0)
                bitmap.eraseColor(0)
            }
        }

        companion object {
            fun LowRes() = FrameBuffer(64, 32, 2)
            fun HiRes() = FrameBuffer(128, 64, 4)
        }
    }

    var frameBuffer: FrameBuffer = FrameBuffer.LowRes()
        private set

    var hires: Boolean = false
        set(value) {
            field = value
            frameBuffer = if (field) FrameBuffer.HiRes() else FrameBuffer.LowRes()
        }

    companion object {
        const val SET_COLOR: Int = 0xFF00FF00.toInt()
        const val CLEAR_COLOR: Int = 0xFE00FF00.toInt()

        // The number of frames to fade out a pixel
        const val FADE_TIME_MILLIS: Int = 1000
    }
}
