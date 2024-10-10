package com.emerjbl.ultra8.data

import com.emerjbl.ultra8.chip8.graphics.SimpleGraphics
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.testutil.contentEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream

class Chip8StateStoreTest {
    @Test
    fun test_serializedState_deserializesToSameState() = runTest {
        val gfx = SimpleGraphics()
        gfx.putSprite(10, 10, byteArrayOf(2, 3, 4, 56, 7, 7), 0, 5, 2)
        val state = Chip8.State(
            v = (100..115).toList().toIntArray(),
            hp = (200..215).toList().toIntArray(),
            stack = (300..363).toList().toIntArray(),
            mem = (0..65535).map { it.toByte() }.toByteArray(),
            i = 0x0123,
            pc = 0x0343,
            sp = 0x0001,
            targetPlane = 0x03,
            gfx = gfx
        )

        val input = PipedInputStream(1000000)
        val output = PipedOutputStream(input)
        launch(Dispatchers.IO) {
            Chip8StateSerializer.writeTo(MaybeState.Yes(state), output)
        }

        val readBack = Chip8StateSerializer.readFrom(input)

        strikt.api.expectThat(readBack).contentEquals(MaybeState.Yes(state))
    }
}
