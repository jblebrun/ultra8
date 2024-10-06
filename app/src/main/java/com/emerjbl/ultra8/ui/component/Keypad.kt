package com.emerjbl.ultra8.ui.component

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.emerjbl.ultra8.ui.theme.chip8Colors
import java.util.Locale
import kotlin.math.sqrt


@Composable
fun Keypad(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit) {
    val keyHitManager = remember { KeyHitManager(onKeyDown, onKeyUp) }

    Box(
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()
            .background(color = MaterialTheme.chip8Colors.keypadBackground)
            .keyHitManager(keyHitManager)
            .padding(5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ButtonRow(keyHitManager, 1, 2, 3, 12)
            ButtonRow(keyHitManager, 4, 5, 6, 13)
            ButtonRow(keyHitManager, 7, 8, 9, 14)
            ButtonRow(keyHitManager, 10, 0, 11, 15)
        }
    }
}

@Composable
fun ButtonRow(
    keyHitManager: KeyHitManager,
    vararg buttons: Int
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (value in buttons) {
            Chip8Button(value, keyHitManager)
        }
    }
}

@Composable
fun RowScope.Chip8Button(
    value: Int,
    keyHitManager: KeyHitManager,
) {
    val text = Integer.toHexString(value).uppercase(Locale.getDefault())
    val buttonColor = MaterialTheme.chip8Colors.keyCapBackground
    val keyCapTextSize = remember { mutableStateOf(16.sp) }

    // Outer box to get position including all padding so there are no touch gaps.
    Box(
        modifier = Modifier
            .weight(1f)
            .onGloballyPositioned { keyCapTextSize.value = (it.size.width / 4f).sp }
            .addKeyToKeyHitManager(value, keyHitManager)
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
            .padding(5.dp)
            .keyHitManager(keyHitManager)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ButtonRow(keyHitManager, 1, 2, 3, 12)
            ButtonRow(keyHitManager, 4, 5, 6, 13)
            ButtonRow(keyHitManager, 7, 8, 9, 14)
            ButtonRow(keyHitManager, 10, 0, 11, 15)
        }
    }
}
