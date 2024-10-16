package com.emerjbl.ultra8.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Chip8ProgramState::class], version = 1)
abstract class Ultra8Database : RoomDatabase() {
    abstract fun chip8StateDao(): Chip8ProgramStateDao
}
