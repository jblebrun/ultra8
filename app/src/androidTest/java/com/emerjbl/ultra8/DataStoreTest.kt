package com.emerjbl.ultra8

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.data.Chip8StateSerializer
import com.emerjbl.ultra8.data.MaybeState
import com.emerjbl.ultra8.testutil.contentEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.assertions.isNotNull

class DataStoreWrapper(scope: CoroutineScope) {
    val Context.testStore: DataStore<MaybeState> by dataStore(
        fileName = "chip8state.u8b",
        serializer = Chip8StateSerializer,
        scope = scope
    )
}

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
            val scope1 = CoroutineScope(Dispatchers.IO)
            DataStoreWrapper(scope1).run {
                appContext.testStore.updateData { MaybeState.Yes(state) }
            }
            scope1.cancel()

            val scope2 = CoroutineScope(Dispatchers.IO)
            val readBack = DataStoreWrapper(scope2).run {
                appContext.testStore.data.firstOrNull()
            }
            scope2.cancel()

            strikt.api.expectThat(readBack).isNotNull().contentEquals(MaybeState.Yes(state))
        }
    }
}
