package com.emerjbl.ultra8

import android.app.Application
import androidx.room.RoomDatabase
import com.emerjbl.ultra8.data.CatalogDatabase
import com.emerjbl.ultra8.data.CatalogStore
import com.emerjbl.ultra8.data.Chip8StateStore
import com.emerjbl.ultra8.data.ProgramStore
import com.emerjbl.ultra8.data.Ultra8Database

interface Provider {
    val chip8StateStore: Chip8StateStore
    val programStore: ProgramStore
    val catalogStore: CatalogStore
    val userDb: RoomDatabase
    val catalogDb: RoomDatabase
}

class Ultra8Application : Application() {
    val provider = object : Provider {
        override val chip8StateStore by lazy { Chip8StateStore(userDb.chip8StateDao()) }
        override val programStore by lazy {
            ProgramStore(
                this@Ultra8Application,
                userDb.programDao()
            )
        }
        override val catalogStore by lazy {
            CatalogStore(catalogDb.catalogDao())
        }
        override val userDb by lazy {
            Ultra8Database.newForFile(this@Ultra8Application, "ultra8-user-database")
        }
        override val catalogDb by lazy {
            CatalogDatabase.newForFile(this@Ultra8Application, "ultra8-catalog-database", "seed.db")
        }
    }
}
