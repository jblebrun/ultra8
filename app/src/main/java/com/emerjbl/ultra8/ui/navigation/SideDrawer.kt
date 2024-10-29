package com.emerjbl.ultra8.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emerjbl.ultra8.data.Program

@Composable
fun SideDrawer(
    drawerState: DrawerState,
    programs: List<Program>,
    selectedProgram: String,
    onProgramSelected: (Program) -> Unit,
    onReset: () -> Unit,
    onCatalog: () -> Unit,
    onRemoveProgram: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.sizeIn(maxWidth = 300.dp)
    ) {
        LazyColumn {
            item {
                Text(
                    "Ultra 8", modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
            }
            item {
                NavigationDrawerItem(
                    onClick = onReset,
                    label = {
                        Text(
                            "Reset Machine", modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = false
                )
                HorizontalDivider()
            }
            item {
                NavigationDrawerItem(
                    onClick = onCatalog,
                    label = {
                        Text(
                            "Catalog", modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    selected = false
                )
                HorizontalDivider()
            }
            items(programs.size,
                contentType = { 1 },
                key = { programs[it].name }
            ) {
                val program = programs[it]
                NavigationDrawerItem(
                    onClick = { onProgramSelected(program) },
                    label = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(program.name)
                            IconButton({ onRemoveProgram(program.name) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove ${program.name}"
                                )
                            }
                        }
                    },
                    selected = program.name == selectedProgram
                )
            }
        }
    }
}
