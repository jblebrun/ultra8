package com.emerjbl.ultra8.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity
class CatalogProgram(
    @PrimaryKey
    /** The display name for the program. */
    val name: String,

    /** The Chip-8 byte code. */
    val data: ByteArray? = null,
)

@Dao
interface CatalogDao {
    @Query("SELECT name, NULL as data from catalogProgram ORDER BY name")
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
