package com.emerjbl.ultra8.ui.screen

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.ui.component.BottomBar
import com.emerjbl.ultra8.ui.component.TopBar
import com.emerjbl.ultra8.ui.content.LandscapeGameplay
import com.emerjbl.ultra8.ui.content.PortraitGameplay
import com.emerjbl.ultra8.ui.helpers.FrameConfig
import com.emerjbl.ultra8.ui.helpers.onKeyEvent
import com.emerjbl.ultra8.ui.theme.chip8Colors
import com.emerjbl.ultra8.ui.viewmodel.PlayGameViewModel
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayScreen(
    programName: String,
    gameShouldRun: Boolean,
    resetEvents: Flow<Unit>,
    onDrawerOpen: () -> Unit
) {
    val viewModel =
        viewModel<PlayGameViewModel>(
            // We share game viewModels for a given game across all models in the
            // stack. So we use the Activity-scoped ViewModelStoreOwner.
            viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
            key = programName,
            factory = PlayGameViewModel.Factory
        )

    // Collect reset events from above
    LaunchedEffect(resetEvents) {
        resetEvents.collect {
            viewModel.reset()
        }
    }

    DisposableEffect(gameShouldRun) {
        if (gameShouldRun) {
            viewModel.resume()
        } else {
            viewModel.pause()
        }
        onDispose {
            viewModel.pause()
        }
    }

    val frameConfig = FrameConfig(
        color1 = MaterialTheme.chip8Colors.pixel1Color,
        color2 = MaterialTheme.chip8Colors.pixel2Color,
        color3 = MaterialTheme.chip8Colors.pixel3Color,
        fadeTime = 400.milliseconds,
    )

    val loadedProgram = remember { viewModel.programName }
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
            if (!isLandscape()) TopBar(
                loadedProgram,
                openDrawer = {
                    viewModel.pause()
                    onDrawerOpen()
                }
            )
        },
        bottomBar = {
            if (!isLandscape()) BottomBar(cyclesPerTick = viewModel.cyclesPerTick)
        }
    ) { innerPadding ->
        if (running) {
            focusRequester.requestFocus()
        }


        if (isLandscape()) {
            LandscapeGameplay(innerPadding, running, frameConfig, viewModel)
        } else {
            PortraitGameplay(
                running,
                frameConfig,
                viewModel,
                modifier = Modifier
                    .padding(innerPadding)
            )
        }
    }
}
