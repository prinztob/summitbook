package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.Peak
import kotlinx.coroutines.flow.Flow


@Dao
interface PeakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(peak: Peak)

    @Transaction
    @Query("select * from peak")
    fun getAllPeaks(): Flow<List<Peak>>

    @Delete
    suspend fun delete(peak: Peak)

    @Update
    suspend fun update(peak: Peak)

    @Query("SELECT * FROM peak WHERE name = :name")
    suspend fun getPeakByName(name: String): Peak?

    @Query("SELECT name FROM peak GROUP BY name HAVING COUNT(*) > 1")
    suspend fun getDuplicatePeakNames(): List<String>

    @Query("SELECT * FROM peak WHERE name = :name ORDER BY id DESC")
    suspend fun getPeaksByName(name: String): List<Peak>

}