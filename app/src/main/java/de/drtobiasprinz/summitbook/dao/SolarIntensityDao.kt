package de.drtobiasprinz.summitbook.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.models.SolarIntensity


@Dao
interface SolarIntensityDao {
    @Transaction
    @Query("select * from solarintensity")
    fun getAll(): MutableList<SolarIntensity>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(solarIntensity: SolarIntensity?): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(solarIntensity: SolarIntensity?)

    @Delete
    fun delete(solarIntensity: SolarIntensity?)
}