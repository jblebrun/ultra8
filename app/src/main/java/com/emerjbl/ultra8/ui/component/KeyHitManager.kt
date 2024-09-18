package com.emerjbl.ultra8.ui.component

import android.util.Log
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** A helper to manage tracking key down/up events across the Chip8 key grid.
 *
 * The idea is to track movements across the key grid, so if your finger slides from one key to a
 * neighboring key, it's tracked as the appropriate keydown/keyup event.
 *
 * If two or more fingers move over the same key, only one onKeyDown should fire.
 * If two fingers move over the same key and then go up, only one onKeyUp should fire.
 */
class KeyHitManager(val onKeyDown: (Int) -> Unit, val onKeyUp: (Int) -> Unit) {
    /** Track the position and pressed state of a key. */
    class ButtonState(
        /** Window coordinates of this key's view */
        val bounds: Rect
    ) {
        /** Bitmask of pointers over this button. */
        var pointers: Int = 0
            private set

        fun pointerDown(pointerIdx: Int): Boolean {
            val alreadyDown = down()
            pointers = pointers or (1 shl pointerIdx)
            return !alreadyDown
        }

        fun pointerUp(pointerIdx: Int): Boolean {
            pointers = pointers and (1 shl pointerIdx).inv()
            return pointers == 0
        }

        fun downFor(pointerIdx: Int) = pointers and (1 shl pointerIdx) != 0

        fun down() = pointers != 0
    }

    /** The keystates for the 16 keys on the Chip8 hex keypad. */
    private val keyBounds = Array(16) { ButtonState(Rect.Zero) }

    /**
     *  Set the position on-screen for the key with the provided values.
     *
     *  The provided [Rect] should be in screen coordinates. The [value] parameter corresponds to
     *  the hex value on the Chip8 keycap.
     * */
    fun setKeyPosition(value: Int, position: Rect) {
        keyBounds[value] = ButtonState(position)
    }

    /** A touch event handler.
     *
     *  Appropriate for use as the `touchEvent` parameter for
     *  [androidx.ui.compose.Modifier.pointerInteropFilter].
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointer = event.getPointerId(event.actionIndex)
                val windowOffset = event.windowOffset(event.actionIndex)
                hit(windowOffset)?.let { (index, value) ->
                    if (value.pointerDown(pointer)) {
                        Log.i("Chip8", "KEYDOWN $index")
                        onKeyDown(index)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointer = event.getPointerId(event.actionIndex)
                val windowOffset = event.windowOffset(event.actionIndex)
                hit(windowOffset)?.also { (index, value) ->
                    if (value.downFor(pointer)) {
                        if (value.pointerUp(pointer)) {
                            Log.i("Chip8", "KEYUP $index ($pointer)")
                            onKeyUp(index)
                        }
                    } else {
                        Log.i("Chip8", "KEYUP BUG $index ($pointer)")
                    }
                }

                // Last pointer going up, so everything should be up now.
                if (event.pointerCount == 1) {
                    val stillDown = keyBounds.withIndex().filter { it.value.down() }
                    if (stillDown.isNotEmpty()) {
                        Log.e(
                            "Chip8",
                            "BUG: KEYS ARE STILL DOWN: ${stillDown.joinToString { "${it.index}" }}"
                        )
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (actionIndex in 0 until event.pointerCount) {
                    val pointer = event.getPointerId(actionIndex)
                    val windowOffset = event.windowOffset((actionIndex))
                    downForPointer(pointer)?.let {
                        if (!it.value.bounds.contains(windowOffset)) {
                            Log.i("Chip8", "MOVE KEYUP ${it.index} ($pointer)")
                            if (it.value.pointerUp(pointer)) {
                                onKeyUp(it.index)
                            }
                        }
                    }

                    hit(windowOffset)?.also {
                        if (!it.value.downFor(pointer)) {
                            if (it.value.pointerDown(pointer)) {
                                Log.i("Chip8", "MOVE KEYDOWN ${it.index} ($pointer)")
                                onKeyDown(it.index)
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    private fun hit(point: Offset) =
        keyBounds.withIndex().firstOrNull { it.value.bounds.contains(point) }

    private fun downForPointer(pointer: Int) =
        keyBounds.withIndex().firstOrNull { it.value.downFor(pointer) }

    private fun MotionEvent.windowOffset(actionIndex: Int) =
        Offset(getRawX(actionIndex), getRawY(actionIndex))
}
