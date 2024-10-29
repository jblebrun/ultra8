package com.emerjbl.ultra8.ui.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.util.viewModelFactory

class InitialLoadViewModel(
    programStore: ProgramStore,
) : ViewModel() {
    val programs = programStore.programsFlow()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            InitialLoadViewModel(
                application.provider.programStore,
            )
        }
    }
}
