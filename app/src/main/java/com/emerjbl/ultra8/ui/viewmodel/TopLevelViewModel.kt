package com.emerjbl.ultra8.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.emerjbl.ultra8.Ultra8Application
import com.emerjbl.ultra8.data.Program
import com.emerjbl.ultra8.data.ProgramStore
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

    fun setSelectedProgram(name: String) {
        viewModelScope.launch {
            programStore.setSelectedProgram(name)
        }
    }

    fun removeProgram(name: String) {
        viewModelScope.launch {
            programStore.remove(name)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T =
                checkNotNull(extras[APPLICATION_KEY] as? Ultra8Application).let { application ->
                    TopLevelViewModel(
                        application.provider.programStore,
                    ) as T
                }
        }

    }
}
