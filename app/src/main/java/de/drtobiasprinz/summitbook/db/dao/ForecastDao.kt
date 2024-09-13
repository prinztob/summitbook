package de.drtobiasprinz.summitbook.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.Forecast
import kotlinx.coroutines.flow.Flow


@Dao
interface ForecastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addForecast(forecast: Forecast): Long

    @get:Query("select * from forecast")
    val allForecastsDeprecated: List<Forecast>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addForecastDeprecated(forecast: Forecast): Long

    @Transaction
    @Query("select * from forecast")
    fun getAllForecasts(): Flow<MutableList<Forecast>>

    @Transaction
    @Query("select * from forecast")
    fun getAllForecastsLiveData(): LiveData<MutableList<Forecast>>

    @Query("select * from forecast where id = :id")
    fun getForecast(id: Long): Forecast?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateForecast(forecast: Forecast)
}