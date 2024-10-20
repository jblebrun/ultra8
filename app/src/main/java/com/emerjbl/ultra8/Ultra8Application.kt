package com.emerjbl.ultra8

import android.app.Application
import androidx.room.RoomDatabase
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.data.Ultra8Database

interface Provider {
    val chip8StateStore: Chip8StateStore
    val programStore: ProgramStore
    val db: RoomDatabase
}

class Ultra8Application : Application() {
    val provider = object : Provider {
        override val chip8StateStore by lazy { Chip8StateStore(db.chip8StateDao()) }
        override val programStore by lazy { ProgramStore(this@Ultra8Application, db.programDao()) }
        override val db by lazy {
            deleteDatabase("ultra8-database")
            Ultra8Database.newForFile(this@Ultra8Application, "ultra8-database", "seed.db")
        }
    }
}
