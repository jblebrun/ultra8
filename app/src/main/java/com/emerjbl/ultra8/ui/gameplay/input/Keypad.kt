package com.emerjbl.ultra8.ui.gameplay.input

import androidx.collection.FloatFloatPair
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.emerjbl.ultra8.ui.theme.chip8Colors
import java.util.Locale


@Composable
fun Keypad(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit, modifier: Modifier = Modifier) {
    val keyHitManager = remember { KeyHitManager(onKeyDown, onKeyUp) }
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.chip8Colors.keypadBackground)
            .keyHitManager(keyHitManager)
                then modifier
    ) {
        ButtonRow(keyHitManager, 1, 2, 3, 12)
        ButtonRow(keyHitManager, 4, 5, 6, 13)
        ButtonRow(keyHitManager, 7, 8, 9, 14)
        ButtonRow(keyHitManager, 10, 0, 11, 15)
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

/** Unit-size polygon for the keypad key shape. */
private val unitKeyPoly = RoundedPolygon(
    vertices = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
    centerX = 0.5f,
    centerY = 0.5f,
    rounding = CornerRounding(
        0.3f,
        smoothing = 1f
    )
)

/** A shape that will draw the key poly at the correct size. */
private val keyPathShape = GenericShape { size, _ ->
    unitKeyPoly.transformed { x, y -> FloatFloatPair(x * size.width, y * size.height) }
        .toPath(this@GenericShape.asAndroidPath())
}

@Composable
fun RowScope.Chip8Button(
    value: Int,
    keyHitManager: KeyHitManager,
) {
    val text = Integer.toHexString(value).uppercase(Locale.getDefault())
    val buttonColor = MaterialTheme.chip8Colors.keyCapBackground
    val keyCapTextSize = remember { mutableStateOf(16.sp) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .padding(5.dp)
            .shadow(5.dp, clip = true, shape = keyPathShape)
            .background(buttonColor)
            .onGloballyPositioned { keyCapTextSize.value = (it.size.width / 4f).sp }
            .addKeyToKeyHitManager(value, keyHitManager)
            .aspectRatio(1.0f)
            .fillMaxSize(),
    ) {
        Text(
            text,
            color = MaterialTheme.chip8Colors.keyCapForeground,
            fontSize = keyCapTextSize.value
        )
    }
}
