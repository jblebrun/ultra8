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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.rememberNavController
import com.emerjbl.ultra8.Ultra8Application
import com.emerjbl.ultra8.ui.component.SideDrawer
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


@Composable
fun AppEntryPoint() {
    val navController = rememberNavController()

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed,
    )

    val scope = rememberCoroutineScope()

    val programs =
        (LocalContext.current.applicationContext as Ultra8Application)
            .provider.programStore.programs.collectAsState()

    val selectedProgram = remember { mutableStateOf("") }

    val windowFocused = LocalWindowInfo.current.isWindowFocused

    val gameShouldPause = remember { mutableStateOf(false) }

    // Pass reset events from the navigation drawer down to gameplay children.
    // This is one case where I feel like it's OK to break unidirectional dataflow.
    // But I'll probably find a better approach eventually.
    val resetEvents = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }

    LifecycleResumeEffect(windowFocused) {
        gameShouldPause.value = !windowFocused
        onPauseOrDispose {
            gameShouldPause.value = true
        }
    }

    Ultra8Theme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Allow swipe/scrim tap to close, but don't try to open it with swipes when its not.
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
                        selectedProgram.value = program.name
                        navController.navigate(PlayGame(program.name))
                    },
                    onReset = {
                        scope.launch {
                            drawerState.close()
                        }
                        resetEvents.tryEmit(Unit)
                    }
                )
            }) {
            Ultra8NavHost(
                navController,
                gameShouldPause = gameShouldPause.value || drawerState.isOpen,
                resetEvents = resetEvents,
                onDrawerOpen = { scope.launch { drawerState.open() } }
            )
        }
    }
}
