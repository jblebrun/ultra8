package com.emerjbl.ultra8.ui.gameplay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.ui.gameplay.graphics.Graphics
import com.emerjbl.ultra8.ui.gameplay.input.Keypad
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun LandscapeGameplay(
    running: Boolean,
    frame: FrameManager.Frame,
    cyclesPerTick: MutableStateFlow<Int>,
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Companion.CenterVertically,
        modifier = Modifier.Companion
            .fillMaxSize()
    ) {
        Keypad(
            onKeyDown,
            onKeyUp,
            modifier = Modifier.Companion.weight(1f)
        )
        Column(
            modifier = Modifier.Companion.weight(2f),
        ) {
            Graphics(running, frame = frame)
            BottomBar(cyclesPerTick)
        }
    }
}
