package com.emerjbl.ultra8.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
    val groupedPrograms = programs.value.groupBy { it.category }
    val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }

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
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            groupedPrograms.forEach { (category, categoryPrograms) ->
                val isCollapsed = collapsedCategories[category] == true

                // Category Header
                item(key = "header_$category") {
                    Surface(
                        onClick = {
                            collapsedCategories[category] = !isCollapsed
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.replaceFirstChar { it.uppercase() } + " (${categoryPrograms.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Category Items (if not collapsed)
                if (!isCollapsed) {
                    items(
                        count = categoryPrograms.size,
                        key = { index -> "${category}_${categoryPrograms[index].name}" }
                    ) { index ->
                        val program = categoryPrograms[index]
                        Surface(onClick = {
                            scope.launch {
                                viewModel.loadCatalogProgram(program.name)
                                onSelectProgram(program.name)
                            }
                        }) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 0.dp,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column {
                                    Row(modifier = Modifier.padding(10.dp)) {
                                        Text(program.name, style = MaterialTheme.typography.titleLarge)
                                    }
                                    if (program.description.isNotBlank()) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                                            Text(
                                                program.description,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
