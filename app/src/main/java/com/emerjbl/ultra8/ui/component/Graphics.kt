package com.emerjbl.ultra8.ui.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.ui.helpers.FrameConfig
import com.emerjbl.ultra8.ui.helpers.FrameHolder
import com.emerjbl.ultra8.ui.helpers.next
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

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
                    frameHolder.value = frameHolder.value.next(
                        nextFrame(frameHolder.value.frame),
                        ft,
                        frameConfig
                    )
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

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(10.dp)
            .shadow(3.dp, shape = RoundedCornerShape(10.dp), clip = true)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .aspectRatio(2f)
            .drawBehind {
                drawImage(
                    frameHolder.value.bitmap.asImageBitmap(),
                    filterQuality = FilterQuality.Low,
                    dstSize = size.toIntSize(),
                )
            }
                then modifier,
    )
}
