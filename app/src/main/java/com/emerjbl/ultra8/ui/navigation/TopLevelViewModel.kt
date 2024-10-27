package com.emerjbl.ultra8.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.emerjbl.ultra8.data.Program
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.util.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class TopLevelViewModel(
    private val programStore: ProgramStore,
) : ViewModel() {
    val programs: Flow<List<Program>> = programStore.programsFlow()

    // Pass reset events from the navigation drawer down to gameplay children.
    // This is one case where I feel like it's OK to break unidirectional dataflow.
    // But I'll probably find a better approach eventually.
    val resetEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val selectedProgram = programStore.selectedProgram

    fun removeProgram(name: String) {
        viewModelScope.launch {
            programStore.remove(name)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            TopLevelViewModel(application.provider.programStore)
        }
    }
}
