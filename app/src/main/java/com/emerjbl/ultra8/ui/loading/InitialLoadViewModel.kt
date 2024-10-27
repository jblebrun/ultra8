package com.emerjbl.ultra8.ui.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.data.SelectedProgram
import com.emerjbl.ultra8.util.viewModelFactory
import kotlinx.coroutines.flow.Flow

class InitialLoadViewModel(
    programStore: ProgramStore,
) : ViewModel() {
    val selectedProgram: Flow<SelectedProgram> = programStore.selectedProgram

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            InitialLoadViewModel(
                application.provider.programStore,
            )
        }
    }
}
