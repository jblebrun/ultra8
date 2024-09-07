package com.emerjbl.ultra8

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Wrap the Bitmap in a changing class to trigger updates
class BitmapHolder(val bitmap: Bitmap)

data class Program(val name: String, val id: Int)
class MainActivity : ComponentActivity() {
    val gfx: Chip8Graphics = Chip8Graphics()
    var input: Chip8Input = Chip8Input()
    var machine: Chip8 = Chip8(gfx, input)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val programs = R.raw::class.java.fields.map {
            Program(it.name, it.getInt(null))
        }

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
            }, { input.setKey(it) }, { input.resetKey(it) })
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
    var expanded by remember { mutableStateOf(false) }
    Ultra8Theme {

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
            ) {
                Box {
                    Button(onClick = { expanded = !expanded }) { Text("Programs") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { /*TODO*/ }) {
                        for (program in programs) {
                            DropdownMenuItem(text = { Text(program.name) }, onClick = {
                                onSelectProgram(program.id)
                                expanded = false
                            })
                        }
                    }
                }
                Graphics(bitmapHolder)
                Buttons(onKeyDown, onKeyUp)
            }
        }
    }
}

@Composable
fun Graphics(bitmapHolder: BitmapHolder) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
    ) {
        val bitmap = bitmapHolder.bitmap
        val scaleX = size.width / bitmap.width
        val scaleY = size.height / bitmap.height
        scale(scaleX, scaleY, Offset.Zero) {
            drawImage(bitmap.asImageBitmap())
        }
    }
}

@Composable
fun RowScope.Chip8Button(value: Int, onKeyDown: () -> Unit, onKeyUp: () -> Unit) {
    val text = Integer.toHexString(value)
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(5.dp)
            .border(1.dp, Color.Companion.Black, RoundedCornerShape(10f, 10f, 10f, 10f))
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