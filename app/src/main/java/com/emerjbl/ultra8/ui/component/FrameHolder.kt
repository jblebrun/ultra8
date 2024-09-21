package com.emerjbl.ultra8.ui.component

import android.graphics.Bitmap
import android.view.animation.AccelerateInterpolator
import androidx.core.graphics.alpha
import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics

/** The amount of time to fade out an unset pixel. */
const val FADE_TIME_MILLIS: Float = 400f
const val SET_COLOR = 0xFF00FF88.toInt()
const val MAX_ALPHA = 0xFF

/** The interpolator function for the fadeout. */
private val interpolator = AccelerateInterpolator(1.5f)


fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)

/** Holds data related to a frame to render, and its fade out times. */
class FrameHolder(
    val pixelData: IntArray,
    val fadeTimes: IntArray,
    val bitmap: Bitmap,
    val frameTime: Long,
) {
    private var fadingPixels = 0
    val stillFading: Boolean
        get() = fadingPixels > 0

    /** Update this frame's data for the provided `frameDiff`. */
    fun update(frame: SimpleGraphics.Frame, frameDiff: Int) {
        var currentlyFading = 0
        for (i in frame.data.indices) {
            // If the pixel is being unset, start its fade timer
            if (frame.data[i] == 0 && pixelData[i] == SET_COLOR) {
                fadeTimes[i] = FADE_TIME_MILLIS.toInt()
            }

            // Interpolate and fade
            if (fadeTimes[i] > 0) {
                currentlyFading++
                fadeTimes[i] = maxOf(0, fadeTimes[i] - frameDiff)
                val newAlpha = if (fadeTimes[i] <= 0 || pixelData[i].alpha == 0) {
                    0
                } else {
                    val fadeFraction = fadeTimes[i] / FADE_TIME_MILLIS
                    (MAX_ALPHA * interpolator.getInterpolation(fadeFraction)).toInt()
                }
                pixelData[i] = pixelData[i].withAlpha(newAlpha)
            }

            // Set Pixel
            if (frame.data[i] == 1) {
                pixelData[i] = SET_COLOR
                fadeTimes[i] = 0
            }
        }
        fadingPixels = currentlyFading

        bitmap.setPixels(
            pixelData, 0, frame.width, 0, 0, frame.width, frame.height
        )
    }
}

/**
 * Create the next frame holder from the current one.
 *
 * If any sizes mismatch the frame size, they are recreated.
 */
fun FrameHolder?.next(frame: SimpleGraphics.Frame, frameTime: Long): FrameHolder {
    val fadeTimes = this?.fadeTimes
        ?.takeIf { it.size == frame.data.size }
        ?: IntArray(frame.data.size)

    val pixelData = this?.pixelData
        ?.takeIf { it.size == frame.data.size }
        ?: IntArray(frame.data.size)

    val bitmap = this?.bitmap
        ?.takeIf { it.width == frame.width && it.height == frame.height }
        ?: Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)

    val frameDiff = (frameTime - (this?.frameTime ?: 0)).toInt()
    return FrameHolder(pixelData, fadeTimes, bitmap, frameTime).apply {
        update(frame, frameDiff)
    }
}
