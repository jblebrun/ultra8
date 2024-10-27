package com.emerjbl.ultra8.ui.catalog

import androidx.lifecycle.ViewModel
import com.emerjbl.ultra8.data.CatalogStore
import com.emerjbl.ultra8.data.Program
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.util.viewModelFactory

class CatalogViewModel(
    val catalogStore: CatalogStore,
    val programStore: ProgramStore
) : ViewModel() {
    val catalogPrograms = catalogStore.programsFlow()

    suspend fun loadCatalogProgram(name: String) {
        if (programStore.withData(name) != null) return

        val programWithData = checkNotNull(catalogStore.withData(name)) {
            "Requested a catalog program that doesn't exist"
        }
        println("Storing new program: $name")
        programStore.add(Program(name, 10, programWithData.data))
    }

    companion object {
        val Factory = viewModelFactory {
            CatalogViewModel(
                application.provider.catalogStore,
                application.provider.programStore
            )
        }
    }

}
