package com.emerjbl.ultra8.ui.component

import android.graphics.Bitmap
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
import kotlinx.coroutines.isActive

/** Wrap the Bitmap in a class to trigger updates. */
class BitmapHolder(val bitmap: Bitmap)

@Composable
fun Graphics(nextFrame: (Long) -> Bitmap) {
    val bitmapHolder = remember { mutableStateOf(BitmapHolder(nextFrame(0))) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                bitmapHolder.value = BitmapHolder(nextFrame(it))
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
