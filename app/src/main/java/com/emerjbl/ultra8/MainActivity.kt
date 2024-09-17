package com.emerjbl.ultra8

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.emerjbl.ultra8.ui.theme.Gray20
import com.emerjbl.ultra8.ui.theme.Gray200
import com.emerjbl.ultra8.ui.theme.Gray40
import com.emerjbl.ultra8.ui.theme.Ultra8Theme
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.sqrt

/** Wrap the Bitmap in a class to trigger updates. */
class BitmapHolder(val bitmap: Bitmap)

/** Pre-loaded program entry */
data class Program(val name: String, val id: Int)

class MainActivity : ComponentActivity() {

    private val chip8ViewModel: Chip8ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val programs = R.raw::class.java.fields.map {
            Program(it.name, it.getInt(null))
        }

        actionBar?.hide()
        enableEdgeToEdge()
        setContent {
            Screen(
                programs,
                onSelectProgram = {
                    chip8ViewModel.load(it)
                    chip8ViewModel.resume()
                },
                nextFrame = chip8ViewModel::nextFrame,
                onKeyDown = chip8ViewModel::keyDown,
                onKeyUp = chip8ViewModel::keyUp,
                onStartTurbo = { chip8ViewModel.turbo = true },
                onStopTurbo = { chip8ViewModel.turbo = false },
                onLowSpeed = { chip8ViewModel.period = Chip8Runner.Period(2, 0) },
                onHiSpeed = { chip8ViewModel.period = Chip8Runner.Period(1, 0) }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        chip8ViewModel.pause()
    }

    override fun onResume() {
        super.onResume()
        chip8ViewModel.resume()
    }
}

@Composable
fun Screen(
    programs: List<Program>,
    onSelectProgram: (Int) -> Unit,
    nextFrame: (Long) -> Bitmap,
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit,
    onStartTurbo: () -> Unit,
    onStopTurbo: () -> Unit,
    onLowSpeed: () -> Unit,
    onHiSpeed: () -> Unit,
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
                Box(modifier = Modifier.padding(20.dp)) {
                    Graphics(nextFrame)
                }
                Box(modifier = Modifier.padding(20.dp)) {
                    Buttons(onKeyDown, onKeyUp)
                }

                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onStartTurbo()
                            awaitRelease()
                            onStopTurbo()
                        }
                    )
                }) {
                    Row() {
                        Button(onLowSpeed) {
                            Text("LowSpeed")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Turbo")
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onHiSpeed) {
                            Text("HiSpeed")
                        }
                    }
                }
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
        actions = {
            ProgramsDropdown(programs, onSelectProgram)
        },
    )
}

@Composable
fun Graphics(nextFrame: (Long) -> Bitmap) {
    val bitmapHolder = remember { mutableStateOf(BitmapHolder(nextFrame(0))) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis {
                bitmapHolder.value = BitmapHolder(nextFrame(it))
            }
        }
    }
    Image(
        bitmap = bitmapHolder.value.bitmap.asImageBitmap(),
        contentDescription = "Main Screen",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f),
        filterQuality = FilterQuality.Low
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.Chip8Button(value: Int, onPositioned: (Rect) -> Unit) {
    val text = Integer.toHexString(value).uppercase(Locale.getDefault())
    val buttonColor = Gray20
    val keyCapTextSize = remember { mutableStateOf(16.sp) }

    // Outer box to get position including all padding so there are no touch gaps.
    Box(modifier = Modifier
        .weight(1f)
        .onGloballyPositioned {
            onPositioned(Rect(it.positionInWindow(), it.size.toSize()))
            keyCapTextSize.value = (it.size.width / 4f).sp
        }
        .aspectRatio(1.0f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .drawWithCache {
                    val path = RoundedPolygon(
                        numVertices = 4,
                        radius = sqrt(2f) * size.minDimension / 2,
                        centerX = size.width / 2,
                        centerY = size.height / 2,
                        rounding = CornerRounding(
                            size.minDimension / 3f,
                            smoothing = 0.2f
                        )
                    )
                        .toPath()
                        .asComposePath()
                    onDrawBehind {
                        rotate(45f) {
                            drawPath(path, color = buttonColor)
                        }
                    }
                }, contentAlignment = Alignment.Center
        ) { Text(text, color = Gray200, fontSize = keyCapTextSize.value) }
    }
}

@Composable
fun ButtonRow(onPositioned: (Int, Rect) -> Unit, vararg buttons: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (value in buttons) {
            Chip8Button(value) { onPositioned(value, it) }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Buttons(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit) {
    val keyHitManager = remember { KeyHitManager(onKeyDown, onKeyUp) }

    Box(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()
            .background(color = Gray40)
            .pointerInteropFilter { keyHitManager.onTouchEvent(it) }
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ButtonRow(keyHitManager::setKeyPosition, 1, 2, 3, 12)
            ButtonRow(keyHitManager::setKeyPosition, 4, 5, 6, 13)
            ButtonRow(keyHitManager::setKeyPosition, 7, 8, 9, 14)
            ButtonRow(keyHitManager::setKeyPosition, 10, 0, 11, 15)
        }
    }
}
