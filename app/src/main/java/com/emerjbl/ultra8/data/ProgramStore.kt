package com.emerjbl.ultra8.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.MediaColumns
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.emerjbl.ultra8.chip8.machine.Quirks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** A single program entry. */
@TypeConverters(QuirksTypeConverter::class)
@Entity
class Program(
    @PrimaryKey
    /** The display name for the program. */
    val name: String,

    /** The last chosen cycles per second. */
    val cyclesPerTick: Int,

    /** The quirks settings for this program. */
    val quirks: Quirks = Quirks(),

    /** The Chip-8 byte code. */
    val data: ByteArray? = null,
)

@Dao
interface ProgramDao {
    @Query("SELECT name, cyclesPerTick, quirks from program ORDER BY name")
    fun allFlow(): Flow<List<Program>>

    @Query("SELECT * from program where name == :name limit 1")
    fun nameFlow(name: String): Flow<Program?>

    @Query("SELECT * from program where name == :name limit 1")
    suspend fun withData(name: String): Program?

    @Query("UPDATE program set cyclesPerTick = :cyclesPerTick WHERE name = :programName")
    suspend fun updateCyclesPerTick(programName: String, cyclesPerTick: Int)

    @Query("UPDATE program set quirks = :quirks WHERE name = :programName")
    suspend fun updateQuirks(programName: String, quirks: Quirks)

    @Delete
    suspend fun remove(program: Program)

    @Upsert
    suspend fun add(program: Program)
}

/** The store of programs that Ultra8 can run. */
class ProgramStore(
    private val context: Context,
    private val programDao: ProgramDao,
) {
    fun programsFlow(): Flow<List<Program>> = programDao.allFlow()

    fun nameFlow(name: String): Flow<Program?> = programDao.nameFlow(name)

    suspend fun add(program: Program) {
        programDao.add(program)
    }

    suspend fun addForUri(uri: Uri, quirks: Quirks = Quirks()): Program =
        withContext(Dispatchers.IO) {
            val name = withContext(Dispatchers.IO) {
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use {
                    if (it.moveToNext()) {
                        it.getString(0)
                    } else {
                        null
                    }
                } ?: "Unknown name"
            }
            val data = context.contentResolver.openInputStream(uri)!!.use {
                it.readBytes()
            }
            Program(name, 10, quirks, data).also {
                programDao.add(it)
            }
        }

    suspend fun withData(name: String): Program? = programDao.withData(name)

    suspend fun updateCyclesPerTick(name: String, cyclesPerTick: Int) =
        programDao.updateCyclesPerTick(name, cyclesPerTick)

    suspend fun remove(name: String) = programDao.remove(Program(name, 0))

    suspend fun updateQuirks(name: String, quirks: Quirks) {
        programDao.updateQuirks(name, quirks)
    }
}
