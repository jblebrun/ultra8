package com.emerjbl.ultra8.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

@Composable
fun TopBar(
    loadedProgram: String?,
    openDrawer: () -> Unit

) {
    val title = if (loadedProgram == null) "Ultra8" else "Ultra8: $loadedProgram"
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Default.Menu, "Menu")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { Text(title) },
    )
}
