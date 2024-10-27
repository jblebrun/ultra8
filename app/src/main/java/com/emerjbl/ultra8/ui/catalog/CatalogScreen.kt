package com.emerjbl.ultra8.ui.catalog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun CatalogScreen(
    onSelectProgram: (String) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<CatalogViewModel>(factory = CatalogViewModel.Factory)

    val programs = viewModel.catalogPrograms.collectAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "go back")
                    }
                },
                title = { Text("Built-in Catalog") }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(programs.value.size) {
                val program = programs.value[it]
                Surface(onClick = {
                    scope.launch {
                        viewModel.loadCatalogProgram(program.name)
                        onSelectProgram(program.name)
                    }
                }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            Text(program.name)
                        }
                    }
                }
            }
        }
    }
}
