package com.emerjbl.ultra8

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.HaltTypeConverter
import com.emerjbl.ultra8.data.IntArrayTypeConverter
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.data.Ultra8Database
import kotlinx.coroutines.MainScope

interface Provider {
    val chip8StateStore: Chip8StateStore
    val programStore: ProgramStore
    val db: RoomDatabase
}


class Ultra8Application : Application() {
    val provider = object : Provider {
        override val chip8StateStore by lazy { Chip8StateStore(db) }
        override val programStore by lazy { ProgramStore(this@Ultra8Application, MainScope()) }
        override val db by lazy {
            Room.databaseBuilder(
                this@Ultra8Application,
                Ultra8Database::class.java, "ultra8-database"
            )
                .addTypeConverter(IntArrayTypeConverter())
                .addTypeConverter(HaltTypeConverter()).build()
        }
    }
}
