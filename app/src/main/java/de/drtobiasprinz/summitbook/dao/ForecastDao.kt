package de.drtobiasprinz.summitbook.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.models.*


@Dao
interface ForecastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addForecast(forecast: Forecast?): Long

    @get:Query("select * from forecast")
    val allForecasts: List<Forecast>?

    @Query("select * from forecast where id = :id")
    fun getForecast(id: Long): Forecast?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateForecast(Forecast: Forecast?)

    @Query("delete from forecast")
    fun removeAllForecast()

    @Delete
    fun delete(Forecast: Forecast?)
}