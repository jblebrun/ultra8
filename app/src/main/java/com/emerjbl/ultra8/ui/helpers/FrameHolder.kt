package com.emerjbl.ultra8.ui.helpers

import android.animation.TimeInterpolator
import android.graphics.Bitmap
import android.view.animation.AccelerateInterpolator
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.alpha
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_ALPHA = 0xFF

/**
 * Filter padding in Chip8 pixels.
 *
 * We add some empty base around the borer of the graphics bitmap, so that wehn the image
 * filter is applied, there's no sharp edge at the borders where the filtering is clipped
 * by the image bounds. I couldn't find a way to allow the image to draw out of bounds,
 * but this approach works nicely.
 */
private const val FILTER_PADDING_PX = 1;

data class FrameConfig(
    /** The color of an "on" pixel. */
    val color1: Color = Color.Green,
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
    val colorInt: Int = color1.toArgb()
    val color2Int: Int = color2.toArgb()
    val color3Int: Int = color3.toArgb()
}

fun Int.withAlpha(alpha: Int) = (this and 0x00FFFFFF) or (alpha shl 24)

/** Holds data related to a frame to render, and its fade out times. */
class FrameHolder(
    val frame: FrameManager.Frame?,
    val pixelData: IntArray,
    val fadeTimes: IntArray,
    val bitmap: Bitmap,
    val frameTime: Long,
) {
    private var fadingPixels = 0

    /** Let the renderer know that we still have pixels to fade out. */
    val stillFading: Boolean
        get() = fadingPixels > 0

    /** Update this frame's data for the provided `frameDiff`. */
    fun update(frame: FrameManager.Frame, frameDiff: Int, frameConfig: FrameConfig) {
        var currentlyFading = 0
        for (i in frame.plane1.data.indices) {
            val pixel = frame.plane1.data[i].toInt() or (frame.plane2.data[i].toInt() shl 1);
            // If the pixel is being unset, start its fade timer
            if (pixel == 0 && pixelData[i].alpha == 0xFF) {
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
            when (pixel) {
                1 -> pixelData[i] = frameConfig.colorInt
                2 -> pixelData[i] = frameConfig.color2Int
                3 -> pixelData[i] = frameConfig.color3Int
            }
            if (pixel != 0) fadeTimes[i] = 0
        }

        fadingPixels = currentlyFading

        bitmap.setPixels(
            pixelData,
            0,
            frame.width,
            FILTER_PADDING_PX,
            FILTER_PADDING_PX,
            frame.width,
            frame.height
        )
    }
}

/**
 * Create the next frame holder from the current one.
 *
 * If any sizes mismatch the frame size, they are recreated.
 */
fun FrameHolder?.next(
    frame: FrameManager.Frame,
    frameTime: Long,
    frameConfig: FrameConfig
): FrameHolder {
    val fadeTimes = this?.fadeTimes
        ?.takeIf { it.size == frame.plane1.data.size }
        ?: IntArray(frame.plane1.data.size)

    val pixelData = this?.pixelData
        ?.takeIf { it.size == frame.plane1.data.size }
        ?: IntArray(frame.plane1.data.size)

    // Add some space for filter blur to extend into.
    val bitmapWidth = frame.width + 2 * FILTER_PADDING_PX
    val bitmapHeight = frame.height + 2 * FILTER_PADDING_PX

    val bitmap = this?.bitmap
        ?.takeIf { it.width == bitmapWidth && it.height == bitmapHeight }
        ?: Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)

    val frameDiff = (frameTime - (this?.frameTime ?: 0)).toInt()
    return FrameHolder(frame, pixelData, fadeTimes, bitmap, frameTime).apply {
        update(frame, frameDiff, frameConfig)
    }
}
