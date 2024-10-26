package com.emerjbl.ultra8.ui.loading

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.NavController
import com.emerjbl.ultra8.data.Program
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.ui.loading.LoadResult.Companion.toLoadResult
import com.emerjbl.ultra8.util.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface LoadResult {
    data object Loading : LoadResult
    data class Success(val programName: String) : LoadResult
    data object Failure : LoadResult

    companion object {
        fun Program?.toLoadResult(): LoadResult = when (this) {
            is Program -> Success(name)
            else -> Failure
        }
    }
}

class LoadGameViewModel(
    programStore: ProgramStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val loadResult =
        savedStateHandle.getStateFlow(NavController.KEY_DEEP_LINK_INTENT, Intent())
            .map {
                val dataUri = it.data
                if (dataUri == null) {
                    LoadResult.Loading
                } else {
                    withContext(Dispatchers.IO) {
                        programStore.addForUri(dataUri)
                    }.toLoadResult()
                }
            }


    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            LoadGameViewModel(
                application.provider.programStore,
                extras.createSavedStateHandle()
            )
        }
    }
}
