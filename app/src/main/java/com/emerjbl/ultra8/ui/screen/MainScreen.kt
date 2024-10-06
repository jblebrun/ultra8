package com.emerjbl.ultra8.ui.screen

import android.app.Activity
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.ui.component.BottomBar
import com.emerjbl.ultra8.ui.component.FrameConfig
import com.emerjbl.ultra8.ui.component.Graphics
import com.emerjbl.ultra8.ui.component.Keypad
import com.emerjbl.ultra8.ui.component.TopBar
import com.emerjbl.ultra8.ui.component.onKeyEvent
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import com.emerjbl.ultra8.ui.theme.chip8Colors
import com.emerjbl.ultra8.ui.viewmodel.Chip8ViewModel
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalComposeUiApi::class)
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

        val loadedName by viewModel.loadedName.collectAsState(null)
        val running by viewModel.running.collectAsState(initial = false)
        val focusRequester = remember { FocusRequester() }

        fun onKeyEvent(event: KeyEvent): Boolean {
            onKeyEvent(event, viewModel::keyDown, viewModel::keyUp)
            return false
        }

        @Composable
        fun isLandscape(): Boolean = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onKeyEvent(::onKeyEvent),
            topBar = {
                if (!isLandscape()) TopBar(loadedName, viewModel.programs, viewModel::load)
            },
            bottomBar = {
                if (!isLandscape()) BottomBar(cyclesPerTick = viewModel.cyclesPerTick)
            }
        ) { innerPadding ->
            if (running) {
                focusRequester.requestFocus()
            }
            if (isLandscape()) {
                LandscapeContent(innerPadding, running, frameConfig, viewModel)
            } else {
                PortraitContent(innerPadding, running, frameConfig, viewModel)
            }
        }
    }
}


@Composable
fun PortraitContent(
    innerPadding: PaddingValues,
    running: Boolean,
    frameConfig: FrameConfig,
    viewModel: Chip8ViewModel
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(innerPadding)
    ) {
        Graphics(
            running,
            frameConfig,
            nextFrame = viewModel::nextFrame
        )
        Keypad(
            modifier = Modifier.padding(10.dp),
            onKeyDown = viewModel::keyDown,
            onKeyUp = viewModel::keyUp
        )

    }
}

@Composable
fun LandscapeContent(
    innerPadding: PaddingValues,
    running: Boolean,
    frameConfig: FrameConfig,
    viewModel: Chip8ViewModel
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Keypad(
            viewModel::keyDown,
            viewModel::keyUp,
            modifier = Modifier.weight(1f)
        )
        Column(
            modifier = Modifier.weight(2f),
        ) {
            Graphics(running, frameConfig, nextFrame = viewModel::nextFrame)
            BottomBar(viewModel.cyclesPerTick)
        }
    }
}
