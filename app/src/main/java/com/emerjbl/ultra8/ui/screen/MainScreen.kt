package com.emerjbl.ultra8.ui.screen

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.ui.component.BottomBar
import com.emerjbl.ultra8.ui.component.FrameConfig
import com.emerjbl.ultra8.ui.component.Graphics
import com.emerjbl.ultra8.ui.component.Keypad
import com.emerjbl.ultra8.ui.component.OverlayKeypad
import com.emerjbl.ultra8.ui.component.TopBar
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import com.emerjbl.ultra8.ui.theme.chip8Colors
import com.emerjbl.ultra8.ui.viewmodel.Chip8ViewModel
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun MainScreen() {
    val viewModel = viewModel<Chip8ViewModel>()

    (LocalContext.current as? Activity)?.intent?.let {
        viewModel.load(it)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.resume()

        onPauseOrDispose {
            viewModel.pause()
        }
    }


    Ultra8Theme {
        val frameConfig = FrameConfig(
            color1 = MaterialTheme.chip8Colors.pixel1Color,
            color2 = MaterialTheme.chip8Colors.pixel2Color,
            color3 = MaterialTheme.chip8Colors.pixel3Color,
            fadeTime = 400.milliseconds,
        )
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE ->
                LandscapeMain(frameConfig, viewModel)

            else -> PortraitMain(frameConfig, viewModel)
        }
    }
}

@Composable
fun PortraitMain(
    frameConfig: FrameConfig,
    viewModel: Chip8ViewModel
) {
    val loadedName by viewModel.loadedName.collectAsState(null)
    val running by viewModel.running.collectAsState(initial = false)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopBar(loadedName, viewModel.programs, viewModel::load) },
        bottomBar = {
            BottomBar(
                cyclesPerSecond = viewModel.cyclesPerSecond,
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
                Graphics(running, frameConfig, viewModel::nextFrame)
            }
            Box(modifier = Modifier.padding(20.dp)) {
                Keypad(viewModel::keyDown, viewModel::keyUp)
            }
        }
    }
}


@Composable
fun LandscapeMain(
    frameConfig: FrameConfig,
    viewModel: Chip8ViewModel
) {
    val running by viewModel.running.collectAsState(initial = false)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier.padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Graphics(running, frameConfig, viewModel::nextFrame)
                OverlayKeypad(viewModel::keyDown, viewModel::keyUp)
            }
        }
    }
}
