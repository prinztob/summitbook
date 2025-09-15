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

}