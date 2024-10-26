package com.emerjbl.ultra8.ui.gameplay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.StepResult
import com.emerjbl.ultra8.ui.gameplay.graphics.Graphics
import com.emerjbl.ultra8.ui.gameplay.input.Keypad
import com.emerjbl.ultra8.ui.navigation.TopBar
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PortraitGameplay(
    running: Boolean,
    programName: String,
    frame: FrameManager.Frame,
    cyclesPerTick: MutableStateFlow<Int>,
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit,
    halt: StepResult.Halt?,
    onDrawerOpen: () -> Unit,
    modifier: Modifier = Modifier.Companion
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            TopBar(
                programName,
                openDrawer = {
                    onDrawerOpen()
                }
            )
        },
        bottomBar = { BottomBar(cyclesPerTick = cyclesPerTick) }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(innerPadding)
                    then modifier
        ) {
            Box(contentAlignment = Alignment.Companion.BottomCenter) {
                Graphics(
                    running,
                    frame = frame,
                )
                if (halt != null) {
                    Text(halt.toString(), style = MaterialTheme.typography.labelSmall)
                }
            }

            Keypad(
                modifier = Modifier.Companion.padding(10.dp),
                onKeyDown = onKeyDown,
                onKeyUp = onKeyUp
            )
        }
    }
}
