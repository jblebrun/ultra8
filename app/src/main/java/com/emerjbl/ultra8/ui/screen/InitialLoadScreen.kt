package com.emerjbl.ultra8.ui.screen

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.flowOf

@Composable
fun InitialLoadScreen(selectedProgram: String, onReady: () -> Unit) {
    if (selectedProgram != "") onReady()
    // Just show a blank play screen while loading.
    PlayScreen("", false, flowOf(), {})
}
