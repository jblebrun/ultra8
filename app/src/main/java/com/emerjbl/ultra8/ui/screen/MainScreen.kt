package com.emerjbl.ultra8.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.ui.component.BottomBar
import com.emerjbl.ultra8.ui.component.Graphics
import com.emerjbl.ultra8.ui.component.Keypad
import com.emerjbl.ultra8.ui.component.TopBar
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import com.emerjbl.ultra8.ui.viewmodel.Chip8ViewModel


@Composable
fun MainScreen() {
    val viewModel = viewModel<Chip8ViewModel>()

    LifecycleResumeEffect(Unit) {
        viewModel.resume()

        onPauseOrDispose {
            viewModel.pause()
        }
    }

    val running by viewModel.running.collectAsState(initial = false)

    Ultra8Theme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopBar(viewModel.programs, viewModel::load) },
            bottomBar = {
                BottomBar(
                    viewModel::lowSpeed,
                    viewModel::hiSpeed,
                    viewModel::turboOn,
                    viewModel::turboOff
                )
            }
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    Graphics(running, viewModel::nextFrame)
                }
                Box(modifier = Modifier.padding(20.dp)) {
                    Keypad(viewModel::keyDown, viewModel::keyUp)
                }
            }
        }
    }
}
