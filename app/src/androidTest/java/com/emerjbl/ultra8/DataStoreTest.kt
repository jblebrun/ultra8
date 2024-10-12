package com.emerjbl.ultra8

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.testutil.contentEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.Test
import org.junit.runner.RunWith
import strikt.assertions.isNotNull

@RunWith(AndroidJUnit4::class)
class DataStoreTest {

    @Test
    fun test_dataStoreReload() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val gfx = FrameManager()
        gfx.putSprite(10, 10, byteArrayOf(2, 3, 4, 56, 7, 7), 0, 5, 2)

        val state = Chip8.State(
            v = (100..115).toList().toIntArray(),
            hp = (200..215).toList().toIntArray(),
            stack = (300..363).toList().toIntArray(),
            mem = (0..65535).map { it.toByte() }.toByteArray(),
            i = 0x0242,
            pc = 0x0343,
            sp = 0x0545,
            targetPlane = 0x03,
            gfx = gfx,
        )

        runBlocking(Dispatchers.Default) {
            supervisorScope {
                Chip8StateStore(appContext, this).run {
                    saveSate(state)
                }
                // Cause the store to shut down so we can exit this scope.
                coroutineContext.cancelChildren()
            }

            val readBack = supervisorScope {
                val result = Chip8StateStore(appContext, this).run {
                    lastSavedState()
                }
                coroutineContext.cancelChildren()
                result
            }
            strikt.api.expectThat(readBack).isNotNull().contentEquals(state)
        }
    }
}
