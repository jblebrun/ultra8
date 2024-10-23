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
import com.emerjbl.ultra8.ui.helpers.FrameHolder
import com.emerjbl.ultra8.ui.helpers.next
import com.emerjbl.ultra8.ui.theme.LocalFrameConfig
import kotlinx.coroutines.Job

@Composable
fun frameGrabber(
    running: Boolean,
    frame: FrameManager.Frame,
): State<FrameHolder> {
    val frameConfig = LocalFrameConfig.current
    val frameHolder =
        remember { mutableStateOf(null.next(frame, 0, frameConfig)) }

    // Frame is fairly constant (mutable arrays)
    // Relaunch to render a frame at least once
    // TOOD: change this to a frametime ticker, remove constant frameholder allocation
    LaunchedEffect(frame) {
        Log.i("Chip8", "Begin Render Loop (frame $frame)")
        coroutineContext[Job]?.invokeOnCompletion { Log.i("Chip8", "End Render Loop") }
        do {
            withFrameMillis { ft ->
                frameHolder.value = frameHolder.value.next(
                    frame,
                    ft,
                    frameConfig
                )
            }
        } while (running || frameHolder.value.stillFading)
    }
    return frameHolder
}

@Composable
fun Graphics(
    running: Boolean,
    modifier: Modifier = Modifier,
    frame: FrameManager.Frame,
) {
    // TODO: See if we can improve this.
    // Using a frame timer instead of re-allocating holders.
    val frameHolder = frameGrabber(running, frame)

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
