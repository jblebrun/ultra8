package com.emerjbl.ultra8.ui.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.util.SimpleStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.time.measureTimedValue


private val frameStats = SimpleStats(unit = "us", actionInterval = 300) {
    Log.i("Chip8", "Frame update: $it")
}


@Composable
fun frameGrabber(
    running: Boolean,
    frameConfig: FrameConfig,
    nextFrame: (FrameManager.Frame?) -> FrameManager.Frame
): State<FrameHolder> {
    val frameHolder =
        remember { mutableStateOf(null.next(nextFrame(null), 0, frameConfig)) }

    if (running || frameHolder.value.stillFading) {
        LaunchedEffect(true) {
            Log.i("Chip8", "Begin Render Loop")
            coroutineContext[Job]?.invokeOnCompletion { Log.i("Chip8", "End Render Loop") }
            while (isActive) {
                withFrameMillis { ft ->
                    measureTimedValue {
                        frameHolder.value = frameHolder.value.next(
                            nextFrame(frameHolder.value.frame),
                            ft,
                            frameConfig
                        )
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
fun Graphics(
    running: Boolean,
    frameConfig: FrameConfig,
    modifier: Modifier = Modifier,
    nextFrame: (FrameManager.Frame?) -> FrameManager.Frame
) {
    val frameHolder = frameGrabber(running, frameConfig, nextFrame)

    Image(
        bitmap = frameHolder.value.bitmap.asImageBitmap(),
        contentDescription = "Main Screen",
        modifier = Modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(5.dp)
            .aspectRatio(2f) then modifier,
        filterQuality = FilterQuality.Low
    )
}
