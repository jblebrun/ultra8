package com.emerjbl.ultra8.ui.gameplay.input

import android.util.Log
import android.view.MotionEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize

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
    class ButtonState<T>(
        /** Window coordinates of this key's view */
        val bounds: T
    ) {
        /** Bitmask of pointers over this button. */
        private var pointers: Int = 0

        /** Track pointer as holding the key down. Return true if it's the first one going down. */
        fun pointerDown(pointerIdx: Int): Boolean {
            val alreadyDown = down()
            pointers = pointers or (1 shl pointerIdx)
            return !alreadyDown
        }

        /** Track pointer as off of this key. Return true if all pointers are off of the key. */
        fun pointerUp(pointerIdx: Int): Boolean {
            pointers = pointers and (1 shl pointerIdx).inv()
            return pointers == 0
        }

        /** Return true if the specified pointer is holding this button down. */
        fun downFor(pointerIdx: Int) = pointers and (1 shl pointerIdx) != 0

        /** Return true if any pointer is down on this button. */
        fun down() = pointers != 0
    }

    /** The key states for the 16 keys on the Chip8 hex keypad. */
    private val keyBounds = Array(16) { ButtonState(Rect.Zero) }

    /** The bounds of the parent that (hopefully) encloses all added keys. */
    internal var parentCoordinates: LayoutCoordinates? = null

    /**
     *  Set the position on-screen for the key with the provided values.
     *
     *  The provided [Rect] should be in screen coordinates. The [value] parameter corresponds to
     *  the hex value on the Chip8 key cap.
     * */
    internal fun setKeyPosition(value: Int, keyCoords: LayoutCoordinates) {
        val pc = parentCoordinates
        if (pc == null) {
            Log.i("Chip8", "Trying to set key position with no parent coords.")
            return
        }
        val keyOffset = pc.localPositionOf(keyCoords, Offset.Zero)
        keyBounds[value] = ButtonState(Rect(keyOffset, keyCoords.size.toSize()))
    }

    /** A touch event handler.
     *
     *  Appropriate for use as the `touchEvent` parameter for
     *  [pointerInteropFilter].
     */
    @Suppress("SameReturnValue")
    internal fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            // A pointer is going down. If it is inside of a key bound, associate it with the key
            // and possible trigger a keydown event (if first one on that key).
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointer = event.getPointerId(event.actionIndex)
                val offsetInParent = event.offsetInParent(event.actionIndex)
                hit(offsetInParent)?.let { (index, value) ->
                    if (value.pointerDown(pointer)) {
                        Log.i("Chip8", "KEYDOWN $index")
                        onKeyDown(index)
                    }
                }
            }

            // The pointer is canceled, we won't receive any more events.
            // So take it off of any keys it is on.
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                val pointer = event.getPointerId(event.actionIndex)
                downForPointer(pointer).map { (index, value) ->
                    if (value.pointerUp(pointer)) {
                        Log.i("Chip8", "KEYUP $index ($pointer)")
                        onKeyUp(index)
                    }
                }
            }

            // A pointer is doing up; take it off of any keys that it was associated with, and
            // possibly emit keyUp events (if it was the last one up on a key).
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointer = event.getPointerId(event.actionIndex)
                downForPointer(pointer).forEach { (index, value) ->
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

            // As we move, we may:
            //   * leave key bounds, possibly triggering a key up (if last out).
            //   * enter key bounds, possibly triggering a key down (if first in).
            MotionEvent.ACTION_MOVE -> {
                for (actionIndex in 0 until event.pointerCount) {
                    val pointer = event.getPointerId(actionIndex)
                    val offsetInParent = event.offsetInParent(actionIndex)
                    downForPointer(pointer).forEach {
                        if (!it.value.bounds.contains(offsetInParent)) {
                            Log.i("Chip8", "MOVE KEYUP ${it.index} ($pointer)")
                            if (it.value.pointerUp(pointer)) {
                                onKeyUp(it.index)
                            }
                        }
                    }

                    hit(offsetInParent)?.also {
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

    /** Return the key that the point [Offset] is inside, if any. */
    private fun hit(point: Offset) =
        keyBounds.withIndex().firstOrNull { it.value.bounds.contains(point) }

    /** Return all keys that are considered down for the given pointer. */
    private fun downForPointer(pointer: Int) =
        keyBounds.withIndex().filter { it.value.downFor(pointer) }

    private fun MotionEvent.offsetInParent(actionIndex: Int) =
        Offset(getX(actionIndex), getY(actionIndex))
}

/**
 * Set the parent for the provided [KeyHitManager].
 *
 * This should be a parent fully enclosing any keys that are added  with [addKeyToKeyHitManager].
 **/
fun Modifier.keyHitManager(manager: KeyHitManager) =
    onGloballyPositioned { manager.parentCoordinates = it }
        .pointerInteropFilter { manager.onTouchEvent(it) }

/**
 * Add a key to the hit manager.
 *
 * The key should be fully contained inside of the parent that was added with [keyHitManager].
 */
fun Modifier.addKeyToKeyHitManager(value: Int, manager: KeyHitManager) =
    onGloballyPositioned { manager.setKeyPosition(value, it) }
