package com.emerjbl.ultra8.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.emerjbl.ultra8.data.Program

@Composable
fun TopBar(loadedName: String?, programs: List<Program>, onSelectProgram: (Program) -> Unit) {
    val title = if (loadedName == null) "Ultra8" else "Ultra8: $loadedName"
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { Text(title) },
        actions = {
            ProgramsDropdown(programs, onSelectProgram)
        },
    )
}

@Composable
fun ProgramsDropdown(programs: List<Program>, onSelectProgram: (Program) -> Unit) {
    var programsExpanded by remember { mutableStateOf(false) }
    Button(
        onClick = { programsExpanded = !programsExpanded }
    ) { Text("Programs") }
    DropdownMenu(
        expanded = programsExpanded,
        onDismissRequest = { programsExpanded = false }) {
        for (program in programs) {
            DropdownMenuItem(text = { Text(program.name) }, onClick = {
                onSelectProgram(program)
                programsExpanded = false
            })
        }
    }
}
