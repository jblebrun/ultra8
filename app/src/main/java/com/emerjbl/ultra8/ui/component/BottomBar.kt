package com.emerjbl.ultra8.ui.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.emerjbl.ultra8.R
import kotlinx.coroutines.flow.MutableStateFlow

fun turboHold(
    cps: State<Int>,
    cyclesPerTick: MutableStateFlow<Int>
): suspend PointerInputScope.() -> Unit {
    return {
        detectTapGestures(
            onPress = {
                val oldCps = cps.value
                cyclesPerTick.value = 10
                tryAwaitRelease()
                cyclesPerTick.value = oldCps
            }
        )
    }
}

fun speedDrag(
    cyclesPerTick: MutableStateFlow<Int>
): suspend PointerInputScope.() -> Unit {
    return {
        detectHorizontalDragGestures { _, dragAmount ->
            cyclesPerTick.value =
                (cyclesPerTick.value + 2 * dragAmount.toInt())
                    .coerceIn(10..2000)
        }
    }
}

@Composable
fun TurboButton(
    cps: State<Int>, cyclesPerFrame: MutableStateFlow<Int>,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .pointerInput(Unit, turboHold(cps, cyclesPerFrame))
                then (modifier),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(
                id = R.drawable.baseline_fast_forward_24
            ),
            contentDescription = "Turbo"
        )
    }
}


@Composable
fun BottomBar(
    cyclesPerTick: MutableStateFlow<Int>
) {
    val cpf = cyclesPerTick.collectAsState(0)
    Box(modifier = Modifier.pointerInput(Unit, speedDrag((cyclesPerTick)))) {
        BottomAppBar(actions = {
            Text(
                "${cpf.value}\ncyc/frame",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                fontSize = 12.sp
            )
            Slider(
                modifier = Modifier.weight(3f),
                value = cpf.value.toFloat(),
                onValueChange = { cyclesPerTick.value = it.toInt() },
                steps = 2000,
                valueRange = 1f..2000f
            )
            TurboButton(
                cpf, cyclesPerTick, modifier = Modifier.weight(1f)
            )
        })
    }
}
