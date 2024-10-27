package com.emerjbl.ultra8.ui.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.data.SelectedProgram
import com.emerjbl.ultra8.ui.gameplay.PlayScreen
import kotlinx.coroutines.flow.flowOf

@Composable
fun InitialLoadScreen(
    onDrawerOpen: () -> Unit,
    onReady: (String) -> Unit,
) {
    val viewModel = viewModel<InitialLoadViewModel>(factory = InitialLoadViewModel.Factory)
    val selectedProgram = viewModel.selectedProgram.collectAsState(SelectedProgram.Loading).value
    println("SELECTED PROGRAM: $selectedProgram")
    when (selectedProgram) {
        is SelectedProgram.None -> onDrawerOpen()
        is SelectedProgram.Program -> onReady(selectedProgram.programName)
        else -> Unit
    }
    val title = when (selectedProgram) {
        is SelectedProgram.Loading -> "Loading..."
        is SelectedProgram.None -> "<- Choose a game"
        is SelectedProgram.Program -> selectedProgram.programName
    }

    // Just show a blank play screen while loading.
    PlayScreen("", false, flowOf(), onDrawerOpen, title)

}
