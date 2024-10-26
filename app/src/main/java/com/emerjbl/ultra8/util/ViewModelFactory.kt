package com.emerjbl.ultra8.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.emerjbl.ultra8.Ultra8Application

class FactoryCreatorScope(val application: Ultra8Application, val extras: CreationExtras)

inline fun <reified T> viewModelFactory(crossinline creator: FactoryCreatorScope.() -> T) =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T = FactoryCreatorScope(
            checkNotNull(extras[APPLICATION_KEY] as? Ultra8Application),
            extras
        ).creator() as T
    }
