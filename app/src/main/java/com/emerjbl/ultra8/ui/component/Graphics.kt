package com.emerjbl.ultra8.ui.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import com.emerjbl.ultra8.util.SimpleStats
import kotlinx.coroutines.isActive
import kotlin.time.measureTimedValue


private val frameStats = SimpleStats()

@Composable
fun Graphics(nextFrame: () -> SimpleGraphics.Frame) {
    val bitmapHolder =
        remember { mutableStateOf(null.next(nextFrame(), 0)) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { ft ->
                measureTimedValue {
                    bitmapHolder.value = bitmapHolder.value.next(nextFrame(), ft)
                }.let {
                    frameStats.add(it.duration.inWholeMicroseconds)
                    frameStats.run_every(300) {
                        Log.i("Chip8", "Frame update: $it")
                    }
                }
            }
        }
    }
    Image(
        bitmap = bitmapHolder.value.bitmap.asImageBitmap(),
        contentDescription = "Main Screen",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f),
        filterQuality = FilterQuality.Low
    )
}
