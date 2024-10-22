package com.emerjbl.ultra8

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.RoomDatabase
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.data.Ultra8Database

interface Provider {
    val chip8StateStore: Chip8StateStore
    val programStore: ProgramStore
    val db: RoomDatabase

}

val Context.preferences: DataStore<Preferences> by preferencesDataStore(name = "settings")

class Ultra8Application : Application() {
    val provider = object : Provider {
        override val chip8StateStore by lazy { Chip8StateStore(db.chip8StateDao()) }
        override val programStore by lazy { ProgramStore(this@Ultra8Application, db.programDao()) }
        override val db by lazy {
            Ultra8Database.newForFile(this@Ultra8Application, "ultra8-database", "seed.db")
        }
    }
}
