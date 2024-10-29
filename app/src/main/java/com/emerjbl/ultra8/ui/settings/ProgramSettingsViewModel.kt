package com.emerjbl.ultra8.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.util.viewModelFactory
import kotlinx.coroutines.launch

class ProgramSettingsViewModel(
    val programName: String,
    val programStore: ProgramStore,
) : ViewModel() {
    fun removeProgram(name: String) {
        viewModelScope.launch {
            programStore.remove(name)
        }

    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            ProgramSettingsViewModel(
                checkNotNull(extras[ViewModelProvider.VIEW_MODEL_KEY]) {
                    "Missing VIEW_MODEL_KEY for PlayGameViewModel factory"
                },
                application.provider.programStore,
            )
        }

    }
}
