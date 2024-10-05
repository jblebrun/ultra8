package com.emerjbl.ultra8.ui.component

import android.animation.TimeInterpolator
import android.graphics.Bitmap
import android.view.animation.AccelerateInterpolator
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.alpha
import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_ALPHA = 0xFF

data class FrameConfig(
    /** The color of an "on" pixel. */
    val color: Color = Color.Green,
    val color2: Color = Color.Blue,
    val color3: Color = Color.DarkGray,

    /** The amount of time to fade out an unset pixel. */
    val fadeTime: Duration = 400.milliseconds,

    /** The interpolator function for the fadeout. */
    val interpolator: TimeInterpolator = AccelerateInterpolator(1.5f)
) {
    val fadeMillisInt: Int = fadeTime.inWholeMilliseconds.toInt()
    val fadeMillisFloat: Float = fadeTime.inWholeMilliseconds.toFloat()

    @ColorInt
    val colorInt: Int = color.toArgb()
    val color2Int: Int = color2.toArgb()
    val color3Int: Int = color3.toArgb()
}

fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)

/** Holds data related to a frame to render, and its fade out times. */
class FrameHolder(
    val frame: SimpleGraphics.Frame?,
    val pixelData: IntArray,
    val fadeTimes: IntArray,
    val bitmap: Bitmap,
    val frameTime: Long,
) {
    private var fadingPixels = 0
    val stillFading: Boolean
        get() = fadingPixels > 0

    /** Update this frame's data for the provided `frameDiff`. */
    fun update(frame: SimpleGraphics.Frame, frameDiff: Int, frameConfig: FrameConfig) {
        var currentlyFading = 0
        for (i in frame.data.indices) {
            // If the pixel is being unset, start its fade timer
            if (frame.data[i] == 0 && pixelData[i].alpha == 0xFF) {
                fadeTimes[i] = frameConfig.fadeMillisInt
            }

            // Interpolate and fade
            if (fadeTimes[i] > 0) {
                currentlyFading++
                fadeTimes[i] = maxOf(0, fadeTimes[i] - frameDiff)
                val newAlpha = if (fadeTimes[i] <= 0 || pixelData[i].alpha == 0) {
                    0
                } else {
                    val fadeFraction = fadeTimes[i] / frameConfig.fadeMillisFloat
                    (MAX_ALPHA * frameConfig.interpolator.getInterpolation(fadeFraction)).toInt()
                }
                pixelData[i] = pixelData[i].withAlpha(newAlpha)
            }

            // Set Pixel
            if (frame.data[i] == 1) {
                pixelData[i] = frameConfig.colorInt
                fadeTimes[i] = 0
            }
            if (frame.data[i] == 2) {
                pixelData[i] = frameConfig.color2Int
                fadeTimes[i] = 0
            }
            if (frame.data[i] == 3) {
                pixelData[i] = frameConfig.color3Int
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
fun FrameHolder?.next(
    frame: SimpleGraphics.Frame,
    frameTime: Long,
    frameConfig: FrameConfig
): FrameHolder {
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
    return FrameHolder(frame, pixelData, fadeTimes, bitmap, frameTime).apply {
        update(frame, frameDiff, frameConfig)
    }
}
