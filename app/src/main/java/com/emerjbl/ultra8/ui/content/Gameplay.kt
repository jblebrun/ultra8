package com.emerjbl.ultra8.ui.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.StepResult.Halt
import com.emerjbl.ultra8.ui.component.BottomBar
import com.emerjbl.ultra8.ui.component.Graphics
import com.emerjbl.ultra8.ui.component.Keypad
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PortraitGameplay(
    running: Boolean,
    frame: FrameManager.Frame,
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit,
    halt: Halt?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
                then modifier
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Graphics(
                running,
                frame = frame,
            )
            if (halt != null) {
                Text(halt.toString(), style = MaterialTheme.typography.labelSmall)
            }
        }

        Keypad(
            modifier = Modifier.padding(10.dp),
            onKeyDown = onKeyDown,
            onKeyUp = onKeyUp
        )
    }
}

@Composable
fun LandscapeGameplay(
    innerPadding: PaddingValues,
    running: Boolean,
    frame: FrameManager.Frame,
    cyclesPerTick: MutableStateFlow<Int>,
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Keypad(
            onKeyDown,
            onKeyUp,
            modifier = Modifier.weight(1f)
        )
        Column(
            modifier = Modifier.weight(2f),
        ) {
            Graphics(running, frame = frame)
            BottomBar(cyclesPerTick)
        }
    }
}
