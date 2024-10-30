package com.emerjbl.ultra8.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.emerjbl.ultra8.BuildConfig

@Database(entities = [CatalogProgram::class], version = 1)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    companion object {
        fun newForFile(
            context: Context,
            name: String,
            seed: String
        ): CatalogDatabase =
            Room.databaseBuilder(context, CatalogDatabase::class.java, name)
                .createFromAsset(seed)
                .buildForApp()

        private fun Builder<CatalogDatabase>.buildForApp() =
            fallbackToDestructiveMigration().addTypeConverter(QuirksTypeConverter())
                .build()
    }
}

@Database(entities = [Chip8ProgramState::class, Program::class], version = 5)
@TypeConverters(QuirksTypeConverter::class)
abstract class Ultra8Database : RoomDatabase() {
    abstract fun chip8StateDao(): Chip8ProgramStateDao
    abstract fun programDao(): ProgramDao

    companion object {
        fun newForFile(context: Context, name: String): Ultra8Database =
            Room.databaseBuilder(
                context,
                Ultra8Database::class.java,
                name
            )
                .buildForApp()

        private fun Builder<Ultra8Database>.buildForApp() = apply {
            if (BuildConfig.DEBUG) {
                fallbackToDestructiveMigration()
            }
        }
            .addTypeConverter(IntArrayTypeConverter())
            .addTypeConverter(HaltTypeConverter())
            .addTypeConverter(QuirksTypeConverter())
            .build()
    }
}
