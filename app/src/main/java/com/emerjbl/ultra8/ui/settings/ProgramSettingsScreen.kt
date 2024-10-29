package com.emerjbl.ultra8.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProgramSettingsScreen(
    programName: String,
    closeDialog: () -> Unit,
) {
    val viewModel = viewModel<ProgramSettingsViewModel>(
        key = programName,
        factory = ProgramSettingsViewModel.Factory
    )
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = closeDialog) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = { Text("Settings for $programName") },
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding((innerPadding))
        ) {
            SpeedSettings()
            HorizontalDivider()
            ColorSettings()
            HorizontalDivider()
            QuirkSettings()
            HorizontalDivider()
            Spacer(modifier = Modifier.weight(2.0f))
            DeleteSettings {
                viewModel.removeProgram(programName)
                closeDialog()
            }
        }
    }
}

@Composable
fun QuirkSettings() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text("Coming Soon: Quirks", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ColorSettings() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text("Coming Soon: Colors", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SpeedSettings() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text("Coming Soon: Speed", style = MaterialTheme.typography.labelLarge)
    }
}


@Composable
fun DeleteSettings(onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(top = 20.dp),
        onClick = onClick,
        colors = ButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceDim,
            disabledContentColor = MaterialTheme.colorScheme.primary,
        )
    ) {
        Text("Delete Program")
    }
}
