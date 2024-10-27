package com.emerjbl.ultra8.ui.gameplay

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.ui.gameplay.input.onKeyEvent
import kotlinx.coroutines.flow.Flow

@Composable
fun PlayScreen(
    programName: String,
    gameShouldRun: Boolean,
    resetEvents: Flow<Unit>,
    onDrawerOpen: () -> Unit,
    title: String = programName,
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

    val frame by viewModel.frames.collectAsState()
    val halt by viewModel.halted.collectAsState()

    val gameShouldRun = gameShouldRun && halt == null

    val focusRequester = remember { FocusRequester() }

    @Composable
    fun isLandscape(): Boolean = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onKeyEvent {
                onKeyEvent(it, viewModel::keyDown, viewModel::keyUp)
                false
            },
    ) {
        LaunchedEffect(true) {
            if (gameShouldRun) {
                focusRequester.requestFocus()
            }
        }

        if (isLandscape()) {
            LandscapeGameplay(
                gameShouldRun,
                frame,
                viewModel.cyclesPerTick,
                viewModel::keyDown,
                viewModel::keyUp
            )
        } else {
            PortraitGameplay(
                gameShouldRun,
                title,
                frame,
                viewModel.cyclesPerTick,
                viewModel::keyDown,
                viewModel::keyUp,
                halt,
                onDrawerOpen
            )
        }
    }
}
