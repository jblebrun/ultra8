package com.emerjbl.ultra8.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emerjbl.ultra8.chip8.machine.Quirk
import com.emerjbl.ultra8.chip8.machine.Quirks
import com.emerjbl.ultra8.data.Program

@Composable
fun ProgramSettingsScreen(
    programName: String,
    closeDialog: () -> Unit,
) {
    val viewModel = viewModel<ProgramSettingsViewModel>(
        key = programName,
        factory = ProgramSettingsViewModel.Factory
    )

    val program = viewModel.program.collectAsState(null)

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
            QuirkSettings(program.value, { viewModel.updateQuirks(it) })
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
fun Quirk(name: String, quirk: Quirk, onQuirkChanged: (Boolean) -> Unit) {
    val showInfo = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(quirk.enabled, onQuirkChanged)
        Text(name, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.weight(1.0f))
        IconButton({ showInfo.value = true }) {
            Icon(Icons.Default.Info, contentDescription = "Describe Quirk")
        }
    }

    if (showInfo.value) {
        BasicAlertDialog({ showInfo.value = false }) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation

            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                ) {
                    Text(
                        name,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(quirk.description)
                }
            }
        }
    }

}

@Composable
fun QuirkSettings(program: Program?, updateQuirks: (Quirks) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column {
            Text("Quirks", style = MaterialTheme.typography.labelLarge)
            program?.quirks?.let { quirks ->
                Quirk("Shift X not Y", quirks.shiftXOnly, {
                    updateQuirks(quirks.copy(shiftXOnly = Quirk.ShiftXOnly(it)))
                })
                Quirk("Memory Inc I by X", quirks.memoryIncrementByX, {
                    updateQuirks(quirks.copy(memoryIncrementByX = Quirk.MemoryIncrementByX(it)))

                })
                Quirk("Memory I Unchanged", quirks.memoryIUnchanged, {
                    updateQuirks(quirks.copy(memoryIUnchanged = Quirk.MemoryIUnchanged(it)))

                })
                Quirk("Sprite Wrap Quirk", quirks.spriteWrapQuirk, {
                    updateQuirks(quirks.copy(spriteWrapQuirk = Quirk.SpriteWrapQuirk(it)))
                })
                Quirk("BXNN Jump Quirk", quirks.bxnnJumpQuirk, {
                    updateQuirks(quirks.copy(bxnnJumpQuirk = Quirk.BXNNJumpQuirk(it)))
                })
                Quirk("VSync DRW", quirks.vSyncDraw, {
                    updateQuirks(quirks.copy(vSyncDraw = Quirk.VSyncDraw(it)))
                })
                Quirk("COSMAC Logic Quirk", quirks.cosmacLogicQuirk, {
                    updateQuirks(quirks.copy(cosmacLogicQuirk = Quirk.CosmacLogicQuirk(it)))
                })
                Quirk("Overwrite VF Quirk", quirks.overwriteVFQuirk, {
                    updateQuirks(quirks.copy(overwriteVFQuirk = Quirk.OverwriteVFQuirk(it)))
                })
            }
        }
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
