package com.emerjbl.ultra8.ui.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import com.emerjbl.ultra8.util.SimpleStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.time.measureTimedValue


private val frameStats = SimpleStats(unit = "us", actionInterval = 300) {
    Log.i("Chip8", "Frame update: $it")
}

@Composable
fun frameGrabber(running: Boolean, nextFrame: () -> SimpleGraphics.Frame): State<FrameHolder> {
    val frameHolder =
        remember { mutableStateOf(null.next(nextFrame(), 0)) }

    if (running) {
        LaunchedEffect(true) {
            Log.i("Chip8", "Begin Render Loop")
            coroutineContext[Job]?.invokeOnCompletion { Log.i("Chip8", "End Render Loop") }
            while (isActive) {
                withFrameMillis { ft ->
                    measureTimedValue {
                        frameHolder.value = frameHolder.value.next(nextFrame(), ft)
                    }.let {
                        frameStats.add(it.duration.inWholeMicroseconds)
                    }
                }
            }
        }
    }
    return frameHolder
}

@Composable
fun Graphics(running: Boolean, nextFrame: () -> SimpleGraphics.Frame) {
    val frameHolder = frameGrabber(running, nextFrame)

    Image(
        bitmap = frameHolder.value.bitmap.asImageBitmap(),
        contentDescription = "Main Screen",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f),
        filterQuality = FilterQuality.Low
    )
}
