package com.emerjbl.ultra8

import android.graphics.Bitmap
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import java.util.Arrays

class Chip8Graphics {
    val b: Bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888).apply {
        density = 4
    }
    val hb: Bitmap = Bitmap.createBitmap(HWIDTH, HHEIGHT, Bitmap.Config.ARGB_8888).apply {
        density = 2
    }

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
            val len: Int
            val width: Int
            val height: Int
            if (hires) {
                len = hfblen
                fb = hframebuffer
                bitmap = hb
                width = HWIDTH
                height = HHEIGHT
            } else {
                bitmap = b
                fb = framebuffer
                len = fblen
                width = WIDTH
                height = HEIGHT
            }

            for (i in 0 until len) {
                if (fb[i] != SETCOLOR && fb[i] != 0) {
                    if ((fb[i].toLong() and lmask) > (FADEFLOOR and lmask)) {
                        fb[i] -= FADERATE
                    } else {
                        fb[i] = 0
                    }
                }
            }
            bitmap.setPixels(fb, 0, width, 0, 0, width, height)

            lastDraw = lastDraw + 15
            if (!stopped) {
                h.postAtTime(this, lastDraw + 15)
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
        h.postAtTime(r, lastDraw + 50)
    }

    fun putSprite(xbase: Int, ybase: Int, data: ByteArray, offset: Int, linesIn: Int): Boolean {
        val lines = if (linesIn == 0) 16 else linesIn
        val bytesPerRow = if (linesIn == 0) 2 else 1
        var unset = false
        val height = if (hires) HHEIGHT else HEIGHT
        val width = if (hires) HWIDTH else WIDTH
        val fb: IntArray = if (hires) hframebuffer else framebuffer

        for (yoffset in 0 until lines step bytesPerRow) {
            for (bpr in 0 until bytesPerRow) {
                val row = data[offset + yoffset + bpr].toInt()
                //Log.i("ultra8","doing sprite byte "+Integer.toHexString(row)+" from "+(offset+yoffset+bpr));
                for (xoffset in 0..7) {
                    val mask = 0x80 shr xoffset
                    if ((row and mask) != 0) {
                        val i =
                            (ybase + yoffset and height - 1) * width + ((xbase + xoffset + bpr * 8) and (width - 1))
                        unset = unset or (fb[i] == SETCOLOR)
                        fb[i] = if (fb[i] == SETCOLOR) SETCOLOR - FADERATE else SETCOLOR
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
        const val fblen: Int = WIDTH * HEIGHT
        const val HWIDTH: Int = 128
        const val HHEIGHT: Int = 64
        const val hfblen: Int = HWIDTH * HHEIGHT
        const val FADERATE: Int = 0x08000000
        const val SETCOLOR: Int = -0xff0100
        const val lmask: Long = 0xFFFFFFFFL
        const val FADEFLOOR: Long = (SETCOLOR - 2 * FADERATE).toLong()
        val framebuffer: IntArray = IntArray(WIDTH * HEIGHT)
        val hframebuffer: IntArray = IntArray(HWIDTH * HHEIGHT)
    }
}
