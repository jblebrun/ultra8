package com.emerjbl.ultra8.ui.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.ui.gameplay.PlayScreen
import kotlinx.coroutines.flow.flowOf

@Composable
fun InitialLoadScreen(
    onDrawerOpen: () -> Unit,
    onCatalog: () -> Unit,
) {
    val viewModel = viewModel<InitialLoadViewModel>(factory = InitialLoadViewModel.Factory)
    val programCount = viewModel.programs.collectAsState(null).value
    when {
        programCount == null -> Unit
        programCount.size == 0 -> {
            println("No programs, show catalog")
            onCatalog()
        }

        else -> {
            println("Some programs, show drawer")
            onDrawerOpen()
        }
    }

    // Just show a blank play screen while loading.
    PlayScreen("", false, flowOf(), onDrawerOpen, {}, "<- Choose a game")

}
