package com.emerjbl.ultra8.ui.screen

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.emerjbl.ultra8.ui.helpers.onKeyEvent
import com.emerjbl.ultra8.ui.viewmodel.PlayGameViewModel
import kotlinx.coroutines.flow.Flow

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
        viewModel.running.value = gameShouldRun
        onDispose {
            viewModel.running.value = false
        }
    }

    val running by viewModel.running.collectAsState(initial = false)
    val frame by viewModel.frames.collectAsState()
    val halt by viewModel.halted.collectAsState()

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
                programName,
                openDrawer = {
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
            LandscapeGameplay(
                innerPadding,
                running,
                frame,
                viewModel.cyclesPerTick,
                viewModel::keyDown,
                viewModel::keyUp
            )
        } else {
            PortraitGameplay(
                running,
                frame,
                viewModel::keyDown,
                viewModel::keyUp,
                halt,
                modifier = Modifier
                    .padding(innerPadding)
            )
        }
    }
}
