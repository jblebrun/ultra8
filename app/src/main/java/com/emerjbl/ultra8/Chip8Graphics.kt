package com.emerjbl.ultra8

import android.graphics.Bitmap
import android.graphics.Color
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.util.Arrays

class Chip8Graphics {
    private val b: Bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).apply {
        density = 4
    }
    private val hb: Bitmap = Bitmap.createBitmap(HWIDTH, HHEIGHT, Bitmap.Config.ARGB_8888).apply {
        density = 2
    }

    private val framebuffer: IntArray = IntArray(WIDTH * HEIGHT)
    private val hframebuffer: IntArray = IntArray(HWIDTH * HHEIGHT)

    // Time buffers track the fade time for the interpolator.
    // They store the amount of time remaining for the pixel to be on.
    private val timebuffer = IntArray(WIDTH * HEIGHT)
    private val htimebuffer = IntArray(HWIDTH * HHEIGHT)

    private var lastFrameTime: Long = 0

    var hires: Boolean = false

    fun rscroll() {}

    fun lscroll() {}

    fun scrolldown(n: Int) {}

    fun getFrame(frameTime: Long): Bitmap {
        val frameDiff = (frameTime - lastFrameTime).toInt()
        lastFrameTime = frameTime

        val bitmap: Bitmap
        val fb: IntArray
        val tb: IntArray
        val width: Int
        val height: Int
        if (hires) {
            fb = hframebuffer
            tb = htimebuffer
            bitmap = hb
            width = HWIDTH
            height = HHEIGHT
        } else {
            bitmap = b
            fb = framebuffer
            tb = timebuffer
            width = WIDTH
            height = HEIGHT
        }

        val interpolator = AccelerateInterpolator(2f)

        for (i in tb.indices) {
            if (tb[i] > 0) {
                tb[i] = maxOf(0, tb[i] - frameDiff)
                val newAlpha = if (tb[i] <= 0) {
                    0
                } else {
                    val frameFrac = tb[i].toFloat() / FADE_TIME_MILLIS
                    val frac = interpolator.getInterpolation(frameFrac)
                    (fb[i].alpha * frac).toInt()
                }
                fb[i] = Color.argb(
                    newAlpha,
                    fb[i].red,
                    fb[i].green,
                    fb[i].blue
                )
            }
        }
        bitmap.setPixels(fb, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun putSprite(xbase: Int, ybase: Int, data: ByteArray, offset: Int, linesIn: Int): Boolean {
        val lines = if (linesIn == 0) 16 else linesIn
        val bytesPerRow = if (linesIn == 0) 2 else 1
        var unset = false
        val height = if (hires) HHEIGHT else HEIGHT
        val width = if (hires) HWIDTH else WIDTH
        val fb: IntArray = if (hires) hframebuffer else framebuffer
        val tb: IntArray = if (hires) htimebuffer else timebuffer

        for (yoffset in 0 until lines step bytesPerRow) {
            for (bpr in 0 until bytesPerRow) {
                val row = data[offset + yoffset + bpr].toInt()
                for (xoffset in 0..7) {
                    val mask = 0x80 shr xoffset
                    if ((row and mask) != 0) {
                        val i =
                            (ybase + yoffset and height - 1) * width + ((xbase + xoffset + bpr * 8) and (width - 1))
                        unset = unset or (fb[i] == SETCOLOR)
                        fb[i] = if (fb[i] == SETCOLOR) CLEARCOLOR else SETCOLOR
                        tb[i] = if (fb[i] == CLEARCOLOR) FADE_TIME_MILLIS else 0
                    }
                }
            }
        }
        return unset
    }

    fun clearScreen() {
        Arrays.fill(framebuffer, 0)
        Arrays.fill(hframebuffer, 0)
        b.eraseColor(0)
        hb.eraseColor(0)
    }

    companion object {
        const val WIDTH: Int = 64
        const val HEIGHT: Int = 32
        const val HWIDTH: Int = 128
        const val HHEIGHT: Int = 64
        const val SETCOLOR: Int = 0xFF00FF00.toInt()
        const val CLEARCOLOR: Int = 0xFE00FF00.toInt()

        // The number of frames to fade out a pixel
        const val FADE_TIME_MILLIS: Int = 1000
    }
}
