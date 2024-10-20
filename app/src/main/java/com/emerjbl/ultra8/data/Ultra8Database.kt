package com.emerjbl.ultra8.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.emerjbl.ultra8.BuildConfig

@Database(entities = [Chip8ProgramState::class, Program::class], version = 2)
abstract class Ultra8Database : RoomDatabase() {
    abstract fun chip8StateDao(): Chip8ProgramStateDao
    abstract fun programDao(): ProgramDao

    companion object {
        fun newForFile(context: Context, name: String, seed: String): Ultra8Database =
            Room.databaseBuilder(
                context,
                Ultra8Database::class.java,
                name
            )
                .createFromAsset(seed)
                .buildForApp()

        private fun Builder<Ultra8Database>.buildForApp() = apply {
            if (BuildConfig.DEBUG) {
                fallbackToDestructiveMigration()
            }
        }
            .addTypeConverter(IntArrayTypeConverter())
            .addTypeConverter(HaltTypeConverter())
            .build()
    }
}
