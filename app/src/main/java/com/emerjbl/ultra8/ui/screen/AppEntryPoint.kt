package com.emerjbl.ultra8.ui.screen

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.emerjbl.ultra8.ui.component.SideDrawer
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import com.emerjbl.ultra8.ui.viewmodel.TopLevelViewModel
import kotlinx.coroutines.launch

@Composable
fun AppEntryPoint() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val topLevelViewModel = viewModel<TopLevelViewModel>(factory = TopLevelViewModel.Factory)
    val programs = topLevelViewModel.programs.collectAsState(emptyList())
    val selectedProgram = topLevelViewModel.selectedProgram.collectAsState("")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val activityState = (LocalContext.current as LifecycleOwner).lifecycle.currentState

    val gameShouldRun = remember { mutableStateOf(false) }

    gameShouldRun.value =
        windowFocused
                && activityState.isAtLeast(Lifecycle.State.RESUMED)
                && drawerState.isClosed
                && !drawerState.isAnimationRunning

    Ultra8Theme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Allow swipe/scrim tap to close, but don't try to open it with swipes when it's not.
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                SideDrawer(
                    drawerState,
                    programs.value,
                    selectedProgram.value,
                    onProgramSelected = { program ->
                        scope.launch {
                            drawerState.close()
                        }
                        if (topLevelViewModel.selectedProgram.value != program.name) {
                            topLevelViewModel.selectedProgram.value = program.name
                            navController.navigate(PlayGame(program.name))
                        }
                    },
                    onRemoveProgram = {
                        topLevelViewModel.removeProgram(it)
                    },
                    onReset = {
                        scope.launch {
                            drawerState.close()
                        }
                        topLevelViewModel.resetEvents.tryEmit(Unit)
                    }
                )
            }) {
            Ultra8NavHost(
                navController,
                selectedProgram = selectedProgram.value,
                gameShouldRun = gameShouldRun.value,
                resetEvents = topLevelViewModel.resetEvents,
                onDrawerOpen = { scope.launch { drawerState.open() } }
            )
        }
    }
}
