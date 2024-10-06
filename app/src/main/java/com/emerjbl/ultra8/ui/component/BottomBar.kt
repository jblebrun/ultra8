package com.emerjbl.ultra8.ui.component

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
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
import kotlin.math.abs

fun turboHold(
    cps: State<Int>,
    cyclesPerTick: MutableStateFlow<Int>
): suspend PointerInputScope.() -> Unit {
    return {
        detectTapGestures(
            onPress = {
                val oldCps = cps.value
                cyclesPerTick.value = oldCps * 20
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


private val speeds = arrayOf(
    1, 2, 3, 4, 5, 6, 7, 8, 9,
    10, 20, 30, 40, 50, 60, 70, 80, 90,
    100, 200, 300, 400, 500, 600, 700, 800, 900,
    1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000
)


@Composable
fun BottomBar(
    cyclesPerTick: MutableStateFlow<Int>
) {
    Box(modifier = Modifier.pointerInput(Unit, speedDrag((cyclesPerTick)))) {
        BottomAppBar(actions = { BottomBarActions(cyclesPerTick) })
    }
}

@Composable
fun RowScope.BottomBarActions(
    cyclesPerTick: MutableStateFlow<Int>
) {
    val cpf = cyclesPerTick.collectAsState(0)
    Text(
        "${cpf.value}\ncyc/frame",
        textAlign = TextAlign.Center,
        lineHeight = 12.sp,
        fontSize = 12.sp
    )
    Slider(
        value = speeds.withIndex().minBy { abs(it.value - cpf.value) }.index.toFloat(),
        onValueChange = { cyclesPerTick.value = speeds[it.toInt()] },
        steps = speeds.size,
        valueRange = 0f..(speeds.size -1).toFloat()
    )
    TurboButton(
        cpf, cyclesPerTick, modifier = Modifier.weight(1f)
    )
}
