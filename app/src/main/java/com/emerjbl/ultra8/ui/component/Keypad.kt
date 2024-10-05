package com.emerjbl.ultra8.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.emerjbl.ultra8.ui.theme.chip8Colors
import java.util.Locale
import kotlin.math.sqrt


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Keypad(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit) {
    val keyHitManager = remember { KeyHitManager(onKeyDown, onKeyUp) }

    Box(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()
            .background(color = MaterialTheme.chip8Colors.keypadBackground)
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

@Composable
fun ButtonRow(onPositioned: (Int, Rect) -> Unit, vararg buttons: Int) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (value in buttons) {
            Chip8Button(value) { onPositioned(value, it) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RowScope.Chip8Button(value: Int, onPositioned: (Rect) -> Unit) {
    val text = Integer.toHexString(value).uppercase(Locale.getDefault())
    val buttonColor = MaterialTheme.chip8Colors.keyCapBackground
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
        ) {
            Text(
                text,
                color = MaterialTheme.chip8Colors.keyCapForeground,
                fontSize = keyCapTextSize.value
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OverlayKeypad(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit) {
    val keyHitManager = remember { KeyHitManager(onKeyDown, onKeyUp) }

    Box(
        modifier = Modifier
            .alpha(0.2f)
            .padding(5.dp)
            .aspectRatio(1.0f)
            .fillMaxSize()
            .background(color = MaterialTheme.chip8Colors.keypadBackground)
            .pointerInteropFilter { keyHitManager.onTouchEvent(it) }
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ButtonRow(keyHitManager::setKeyPosition, 1, 2, 3, 12)
            ButtonRow(keyHitManager::setKeyPosition, 4, 5, 6, 13)
            ButtonRow(keyHitManager::setKeyPosition, 7, 8, 9, 14)
            ButtonRow(keyHitManager::setKeyPosition, 10, 0, 11, 15)
        }
    }
}
