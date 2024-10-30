package com.emerjbl.ultra8

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emerjbl.ultra8.chip8.graphics.FrameManager
import com.emerjbl.ultra8.chip8.machine.Chip8
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.HaltTypeConverter
import com.emerjbl.ultra8.data.IntArrayTypeConverter
import com.emerjbl.ultra8.data.Program
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.data.QuirksTypeConverter
import com.emerjbl.ultra8.data.Ultra8Database
import com.emerjbl.ultra8.testutil.contentEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotNull

@RunWith(AndroidJUnit4::class)
class DataStoreTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val db = Room.inMemoryDatabaseBuilder(
        appContext,
        Ultra8Database::class.java,
    ).addTypeConverter(HaltTypeConverter())
        .addTypeConverter(IntArrayTypeConverter())
        .addTypeConverter(QuirksTypeConverter())
        .build()
    val stateStore = Chip8StateStore(db.chip8StateDao())
    val programStore = ProgramStore(appContext, db.programDao())

    @Test
    fun ensure_testData_Different() {
        strikt.api.expectThat(STATE_1).isNotEqualTo(STATE_2)
    }

    @Test
    fun stateStore_store1_retrieve1() {
        runBlocking(Dispatchers.Default) {
            programStore.add(Program(PROGRAM_NAME_1, 10))
            stateStore.saveState(PROGRAM_NAME_1, STATE_1)
            strikt.api.expectThat(
                stateStore.findState(PROGRAM_NAME_1)
            ).isNotNull().contentEquals(STATE_1)
        }
    }

    @Test
    fun stateStore_store2_retrieve2() {
        runBlocking(Dispatchers.Default) {
            programStore.add(Program(PROGRAM_NAME_1, 10))
            programStore.add(Program(PROGRAM_NAME_2, 10))
            stateStore.saveState(PROGRAM_NAME_1, STATE_1)
            strikt.api.expectThat(
                stateStore.findState(PROGRAM_NAME_1)
            ).isNotNull().contentEquals(STATE_1)

            stateStore.saveState(PROGRAM_NAME_2, STATE_2)
            strikt.api.expectThat(
                stateStore.findState(PROGRAM_NAME_2)
            ).isNotNull().contentEquals(STATE_2)
        }
    }

    @Test
    fun stateStore_store1_overwrite1() {
        runBlocking(Dispatchers.Default) {
            programStore.add(Program(PROGRAM_NAME_1, 10))
            programStore.add(Program(PROGRAM_NAME_2, 10))
            stateStore.saveState(PROGRAM_NAME_1, STATE_1)
            strikt.api.expectThat(
                stateStore.findState(PROGRAM_NAME_1)
            ).isNotNull().contentEquals(STATE_1)

            // Overwrite state
            stateStore.saveState(PROGRAM_NAME_1, STATE_2)
            strikt.api.expectThat(
                stateStore.findState(PROGRAM_NAME_1)
            ).isNotNull().contentEquals(STATE_2)
        }
    }

    companion object {
        private const val PROGRAM_NAME_1 = "test program 1"
        private const val PROGRAM_NAME_2 = "test program 2"

        private val STATE_1 = Chip8.State(
            v = (100..115).toList().toIntArray(),
            hp = (200..215).toList().toIntArray(),
            stack = (300..363).toList().toIntArray(),
            mem = (0..65535).map { it.toByte() }.toByteArray(),
            i = 0x0242,
            pc = 0x0343,
            sp = 0x0545,
            gfx = FrameManager(),
        ).apply {
            gfx.targetPlane = 0x2
            gfx.putSprite(10, 10, byteArrayOf(2, 3, 4, 56, 7, 7), 0, 5)
        }

        val STATE_2 = Chip8.State(
            v = (110..125).toList().toIntArray(),
            hp = (210..225).toList().toIntArray(),
            stack = (310..373).toList().toIntArray(),
            mem = (0..65535).map { (it % 10).toByte() }.toByteArray(),
            i = 0x0342,
            pc = 0x0443,
            sp = 0x0845,
            gfx = FrameManager(),
        ).apply {
            gfx.targetPlane = 0x3
            gfx.putSprite(15, 15, byteArrayOf(2, 3, 4, 56, 8, 8, 67, 12), 0, 4)
        }
    }
}
