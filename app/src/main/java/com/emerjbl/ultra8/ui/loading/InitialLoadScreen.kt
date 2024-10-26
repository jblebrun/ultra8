package com.emerjbl.ultra8.ui.loading

import androidx.compose.runtime.Composable
import com.emerjbl.ultra8.ui.gameplay.PlayScreen
import kotlinx.coroutines.flow.flowOf

@Composable
fun InitialLoadScreen(selectedProgram: String, onReady: () -> Unit) {
    if (selectedProgram != "") onReady()
    // Just show a blank play screen while loading.
    PlayScreen("", false, flowOf(), {})
}
