package com.emerjbl.ultra8.ui.component

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

fun onKeyEvent(event: KeyEvent, keyDown: (Int) -> Unit, keyUp: (Int) -> Unit) {
    if (event.nativeKeyEvent.repeatCount > 0) {
        return
    }
    val idx = when (event.key) {
        Key.One -> 1
        Key.Two -> 2
        Key.Three -> 3
        Key.Four -> 12
        Key.Q -> 4
        Key.W -> 5
        Key.E -> 6
        Key.R -> 13
        Key.A -> 7
        Key.S -> 8
        Key.D -> 9
        Key.F -> 14
        Key.Z -> 10
        Key.X -> 0
        Key.C -> 11
        Key.V -> 15
        else -> return
    }
    when (event.type) {
        KeyEventType.KeyDown -> keyDown(idx)
        KeyEventType.KeyUp -> keyUp(idx)
    }
    return
}
