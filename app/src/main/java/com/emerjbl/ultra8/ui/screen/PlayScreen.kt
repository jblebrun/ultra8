package com.emerjbl.ultra8.ui.screen

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.ui.component.BottomBar
import com.emerjbl.ultra8.ui.component.SideDrawer
import com.emerjbl.ultra8.ui.component.TopBar
import com.emerjbl.ultra8.ui.content.LandscapeGameplay
import com.emerjbl.ultra8.ui.content.PortraitGameplay
import com.emerjbl.ultra8.ui.helpers.FrameConfig
import com.emerjbl.ultra8.ui.helpers.onKeyEvent
import com.emerjbl.ultra8.ui.theme.chip8Colors
import com.emerjbl.ultra8.ui.viewmodel.PlayGameViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayScreen(onSelectProgram: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<PlayGameViewModel>(factory = PlayGameViewModel.Factory)

    val windowFocused = LocalWindowInfo.current.isWindowFocused

    LifecycleResumeEffect(windowFocused) {
        // If window loses focus (recent tasks view), pause machine.
        if (windowFocused) {
            viewModel.resume()
        } else {
            viewModel.pause()
        }

        onPauseOrDispose {
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
    val programs by viewModel.programs.collectAsState(initial = emptyList())
    val focusRequester = remember { FocusRequester() }

    fun onKeyEvent(event: KeyEvent): Boolean {
        onKeyEvent(event, viewModel::keyDown, viewModel::keyUp)
        return false
    }

    @Composable
    fun isLandscape(): Boolean = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed,
        confirmStateChange = {
            if (it == DrawerValue.Closed) {
                viewModel.resume()
            }
            true
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Allow swipe/scrim tap to close, but don't try to open it with swipes when its not.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            SideDrawer(
                drawerState,
                programs,
                viewModel.programName,
                onProgramSelected = { program ->
                    scope.launch {
                        drawerState.close()
                    }
                    onSelectProgram(program.name)
                },
                onReset = {
                    scope.launch {
                        drawerState.close()
                    }
                    viewModel.reset()
                }
            )
        }) {
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
                        scope.launch { drawerState.open() }
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
            val extraPadding = animateDpAsState(
                targetValue = if (drawerState.isAnimationRunning) {
                    if (drawerState.isOpen) 0.dp else 10.dp
                } else {
                    if (drawerState.isOpen) 10.dp else 0.dp
                },
                label = "scrimPadding"
            )

            if (isLandscape()) {
                LandscapeGameplay(innerPadding, running, frameConfig, viewModel)
            } else {
                PortraitGameplay(
                    running,
                    frameConfig,
                    viewModel,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(extraPadding.value)
                )
            }
        }
    }
}
