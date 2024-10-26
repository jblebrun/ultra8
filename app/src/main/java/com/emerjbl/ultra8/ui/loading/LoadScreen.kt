package com.emerjbl.ultra8.ui.loading

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoadScreen(onProgramLoaded: (String) -> Unit) {
    val loadGameViewModel =
        viewModel<LoadGameViewModel>(factory = LoadGameViewModel.Factory)
            .loadResult.collectAsState(LoadResult.Loading)

    val result = loadGameViewModel.value

    when (result) {
        is LoadResult.Success -> onProgramLoaded(result.programName)
        else -> Text("Still loading...")
    }
}
