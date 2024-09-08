package com.emerjbl.ultra8

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Wrap the Bitmap in a class to trigger updates. */
class BitmapHolder(val bitmap: Bitmap)

/** Pre-loaded program entry */
data class Program(val name: String, val id: Int)

class MainActivity : ComponentActivity() {
    private val gfx: Chip8Graphics = Chip8Graphics()
    private val machine: Chip8 = Chip8(gfx)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val programs = R.raw::class.java.fields.map {
            Program(it.name, it.getInt(null))
        }

        actionBar?.hide()
        enableEdgeToEdge()
        setContent {
            val bitmap = remember { mutableStateOf(BitmapHolder(gfx.b)) }
            LaunchedEffect(Unit) {
                while (isActive) {
                    bitmap.value = BitmapHolder(gfx.b)
                    delay(33)
                }
            }
            Screen(bitmap.value, programs, {
                machine.loadProgram(resources.openRawResource(it))
                machine.reset()
            }, { machine.keyDown(it) }, { machine.keyUp(it) })
        }
        machine.loadProgram(resources.openRawResource(R.raw.blinky))
        machine.reset()
    }

    override fun onPause() {
        super.onPause()
        machine.paused = true
    }

    override fun onResume() {
        super.onResume()
        machine.paused = false
    }
}

@Composable
fun Screen(
    bitmapHolder: BitmapHolder,
    programs: List<Program>,
    onSelectProgram: (Int) -> Unit,
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit
) {

    Ultra8Theme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopBar(programs, onSelectProgram) },
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                Graphics(bitmapHolder)
                Buttons(onKeyDown, onKeyUp)
            }
        }
    }
}

@Composable
fun ProgramsDropdown(programs: List<Program>, onSelectProgram: (Int) -> Unit) {
    var programsExpanded by remember { mutableStateOf(false) }
    Button(
        onClick = { programsExpanded = !programsExpanded }
    ) { Text("Programs") }
    DropdownMenu(
        expanded = programsExpanded,
        onDismissRequest = { programsExpanded = false }) {
        for (program in programs) {
            DropdownMenuItem(text = { Text(program.name) }, onClick = {
                onSelectProgram(program.id)
                programsExpanded = false
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(programs: List<Program>, onSelectProgram: (Int) -> Unit) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary,
        ),
        title = { Text("Ultra8") },
        actions = { ProgramsDropdown(programs, onSelectProgram) },
    )
}

@Composable
fun Graphics(bitmapHolder: BitmapHolder) {
    Image(
        bitmap = bitmapHolder.bitmap.asImageBitmap(),
        contentDescription = "Main Screen",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f),
        filterQuality = FilterQuality.Low
    )
}

@Composable
fun RowScope.Chip8Button(value: Int, onKeyDown: () -> Unit, onKeyUp: () -> Unit) {
    val text = Integer.toHexString(value)
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(5.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10f, 10f, 10f, 10f))
            .aspectRatio(1.0f)
            .pointerInput(Unit) {
                interceptOutOfBoundsChildEvents = true
                detectTapGestures(onPress = {
                    Log.i("Chip8", "DOWN $value")
                    onKeyDown()
                    val released = tryAwaitRelease()
                    Log.i("Chip8", "UP $value ($released)")
                    onKeyUp()
                })

            }, contentAlignment = Alignment.Center
    ) { Text(text) }
}

@Composable
fun ButtonRow(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit, vararg buttons: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (value in buttons) {
            Chip8Button(value, { onKeyDown(value) }, { onKeyUp(value) })
        }
    }
}

@Composable
fun Buttons(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ButtonRow(onKeyDown, onKeyUp, 1, 2, 3, 12)
            ButtonRow(onKeyDown, onKeyUp, 4, 5, 6, 13)
            ButtonRow(onKeyDown, onKeyUp, 7, 8, 9, 14)
            ButtonRow(onKeyDown, onKeyUp, 10, 0, 11, 15)
        }
    }
}