package com.emerjbl.ultra8.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import com.emerjbl.ultra8.chip8.machine.Quirks
import kotlinx.coroutines.flow.Flow

@Entity
@TypeConverters(QuirksTypeConverter::class)
class CatalogProgram(
    @PrimaryKey
    /** The display name for the program. */
    val name: String,

    /** Recommended cycle speed for the program. */
    val cyclesPerSecond: Int = 10,

    /** Recommended quriks for the program. */
    val quirks: Quirks = Quirks(),

    /** The Chip-8 byte code. */
    val data: ByteArray? = null,
)

@Dao
interface CatalogDao {
    @Query("SELECT name, cyclesPerSecond, quirks from catalogProgram ORDER BY name")
    fun allFlow(): Flow<List<CatalogProgram>>

    @Query("SELECT * from catalogProgram where name == :name limit 1")
    suspend fun withData(name: String): CatalogProgram?
}

class CatalogStore(
    private val catalogDao: CatalogDao,
) {
    fun programsFlow(): Flow<List<CatalogProgram>> = catalogDao.allFlow()

    suspend fun withData(name: String): CatalogProgram? = catalogDao.withData(name)
}
