package com.emerjbl.ultra8.ui.screen

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.emerjbl.ultra8.Chip8ViewModel
import com.emerjbl.ultra8.chip8.runner.Chip8Runner
import com.emerjbl.ultra8.ui.component.Graphics
import com.emerjbl.ultra8.ui.component.Keypad
import com.emerjbl.ultra8.ui.component.TopBar
import com.emerjbl.ultra8.ui.theme.Ultra8Theme

@Composable
fun MainScreen(
    viewModel: Chip8ViewModel,
) {
    val onLoadProgram = { id: Int ->
        viewModel.load(id)
        viewModel.resume()
    }

    Ultra8Theme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopBar(viewModel.programs, onLoadProgram) },
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Graphics(viewModel::nextFrame)
                }
                Box(modifier = Modifier.padding(20.dp)) {
                    Keypad(viewModel::keyDown, viewModel::keyUp)
                }

                Spacer(modifier = Modifier.weight(1f))

                Row() {
                    Button({ viewModel.period = Chip8Runner.Period(2, 0) }) {
                        Text("LowSpeed")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.turbo = true
                                tryAwaitRelease()
                                viewModel.turbo = false
                            }
                        )
                    }) {
                        Text("Turbo")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button({ viewModel.period = Chip8Runner.Period(2, 0) }) {
                        Text("HiSpeed")
                    }
                }
            }
        }
    }
}
