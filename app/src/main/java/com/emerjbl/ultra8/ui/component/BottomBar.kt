package com.emerjbl.ultra8.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun BottomBar(
    onLowSpeed: () -> Unit,
    onHiSpeed: () -> Unit,
    onTurboOn: () -> Unit,
    onTurboOff: () -> Unit,
) {
    BottomAppBar(actions = {
        Button(onLowSpeed) {
            Text("LowSpeed")
        }
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    onTurboOn()
                    tryAwaitRelease()
                    onTurboOff()
                }
            )
        }) {
            Text("Turbo")
        }
        Spacer(Modifier.weight(1f))
        Button(onHiSpeed) {
            Text("HiSpeed")
        }
    })
}
