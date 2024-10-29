package com.emerjbl.ultra8

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emerjbl.ultra8.chip8.machine.Quirks
import com.emerjbl.ultra8.data.HaltTypeConverter
import com.emerjbl.ultra8.data.IntArrayTypeConverter
import com.emerjbl.ultra8.data.Program
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.data.QuirksTypeConverter
import com.emerjbl.ultra8.data.Ultra8Database
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@RunWith(AndroidJUnit4::class)
class ProgramStoreTest {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun nameFlow_indicatesRemovedProgram() {
        val store = store(appContext)

        runBlocking {
            val program = Program("Foo", 10, Quirks(), byteArrayOf(0x42))

            strikt.api.expectThat(
                store.nameFlow("Foo").firstOrNull()
            ).isEqualTo(null)

            store.add(program)

            strikt.api.expectThat(
                store.nameFlow(program.name).firstOrNull()?.name
            ).isEqualTo(program.name)

            store.remove(program.name)

            strikt.api.expectThat(
                store.nameFlow(program.name).firstOrNull()
            ).isNull()
        }

    }

    private fun store(context: Context): ProgramStore {
        val db = Room.inMemoryDatabaseBuilder(
            context,
            Ultra8Database::class.java,
        ).addTypeConverter(HaltTypeConverter())
            .addTypeConverter(IntArrayTypeConverter())
            .addTypeConverter(QuirksTypeConverter())
            .build()

        return ProgramStore(context, db.programDao())
    }
}
