package com.emerjbl.ultra8.ui.gameplay.graphics

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.ui.theme.LocalFrameConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicInteger

private val activeRenderLoops = AtomicInteger(0)

@Composable
fun frameTicker(
    running: Boolean
): LongState {
    val frameTime = remember { mutableLongStateOf(0L) }
    val onFrame: (Long) -> Unit = { ft -> frameTime.longValue = ft }
    LaunchedEffect(running) {
        val active = activeRenderLoops.addAndGet(1)
        Log.i("Chip8", "Begin Render Loop (Active now: $active)")
        coroutineContext[Job]?.invokeOnCompletion {
            val active = activeRenderLoops.addAndGet(-1)
            Log.i("Chip8", "End Render Loop (Active now: $active)")
        }
        do {
            withFrameMillis(onFrame)
        } while (isActive && (running))
    }
    return frameTime
}

@Composable
fun Graphics(
    running: Boolean,
    modifier: Modifier = Modifier,
    frame: FrameManager.Frame,
) {
    val frameConfig = LocalFrameConfig.current
    val lastDrawTime = remember { mutableLongStateOf(0L) }
    val frameHolder =
        remember { mutableStateOf(null.next(frame, 0, frameConfig)) }
    val frameTime = frameTicker(running || frameHolder.value.stillFading)
    val next = frameHolder.value.next(
        frame,
        (frameTime.longValue - lastDrawTime.longValue).toInt(),
        frameConfig
    )

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp)
            .shadow(3.dp, shape = RoundedCornerShape(10.dp), clip = true)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .aspectRatio(2f)
            .drawBehind {
                drawImage(
                    next.imageBitmap,
                    filterQuality = FilterQuality.Low,
                    dstSize = size.toIntSize(),
                )
                frameHolder.value = next
                lastDrawTime.longValue = frameTime.longValue
            }
                then modifier,
    )
}
