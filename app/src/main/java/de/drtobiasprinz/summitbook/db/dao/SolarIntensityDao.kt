package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.SolarIntensity
import kotlinx.coroutines.flow.Flow


@Dao
interface SolarIntensityDao {
    @Transaction
    @Query("select * from solarintensity")
    fun getAllSolarIntensities(): Flow<MutableList<SolarIntensity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(solarIntensity: SolarIntensity?): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(solarIntensity: SolarIntensity?)

    @Delete
    suspend fun delete(solarIntensity: SolarIntensity?)
}