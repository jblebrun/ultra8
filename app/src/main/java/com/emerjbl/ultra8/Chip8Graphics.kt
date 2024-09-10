package com.emerjbl.ultra8

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.util.Arrays

class Chip8Graphics {
    val b: Bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).apply {
        density = 4
    }
    val hb: Bitmap = Bitmap.createBitmap(HWIDTH, HHEIGHT, Bitmap.Config.ARGB_8888).apply {
        density = 2
    }

    val framebuffer: IntArray = IntArray(WIDTH * HEIGHT)
    val hframebuffer: IntArray = IntArray(HWIDTH * HHEIGHT)

    // Time buffers track the fade time for the interpolator.
    // They store the number of frames before the pixel is fully off.
    val timebuffer = ByteArray(WIDTH * HEIGHT)
    val htimebuffer = ByteArray(HWIDTH * HHEIGHT)

    private var stopped: Boolean = true

    private var lastDraw: Long = SystemClock.uptimeMillis()
    private var h: Handler = Handler()
    var hires: Boolean = false
    private var r: Runnable = DrawRunnable()

    fun rscroll() {}

    fun lscroll() {}

    fun scrolldown(n: Int) {}

    internal inner class DrawRunnable : Runnable {
        var counter: Int = 0
        var frameTime: Int = 0

        override fun run() {
            val start = SystemClock.uptimeMillis()
            val bitmap: Bitmap
            val fb: IntArray
            val tb: ByteArray
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
                    tb[i] = (tb[i] - 1).toByte()
                    val newAlpha = if (tb[i] <= 0) {
                        0
                    } else {
                        val frameFrac = tb[i].toFloat() / FADE_FRAMES
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

            lastDraw += FRAME_PERIOD
            if (!stopped) {
                h.postAtTime(this, lastDraw + FRAME_PERIOD)
            }
            val end = SystemClock.uptimeMillis()
            frameTime += (end - start).toInt()
            if (counter++ > 100) {
                Log.i("ultra8", "still drawing... " + frameTime / 100 + " ms/frame")
                counter = 0
                frameTime = 0
            }
        }
    }

    fun stop() {
        stopped = true
    }

    fun start() {
        stopped = false
        h.post(r)
    }

    fun putSprite(xbase: Int, ybase: Int, data: ByteArray, offset: Int, linesIn: Int): Boolean {
        val lines = if (linesIn == 0) 16 else linesIn
        val bytesPerRow = if (linesIn == 0) 2 else 1
        var unset = false
        val height = if (hires) HHEIGHT else HEIGHT
        val width = if (hires) HWIDTH else WIDTH
        val fb: IntArray = if (hires) hframebuffer else framebuffer
        val tb: ByteArray = if (hires) htimebuffer else timebuffer

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
                        tb[i] = if (fb[i] == CLEARCOLOR) FADE_FRAMES else 0.toByte()
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

        // The inverse of the render frame rate
        const val FRAME_PERIOD = 33

        // The number of frames to fade out a pixel
        const val FADE_FRAMES: Byte = 30
    }
}
