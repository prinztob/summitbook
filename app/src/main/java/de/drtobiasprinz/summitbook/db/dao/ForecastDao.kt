package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.*
import kotlinx.coroutines.flow.Flow


@Dao
interface ForecastDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addForecast(forecast: Forecast?): Long

    @get:Query("select * from forecast")
    val allForecastsDeprecated: List<Forecast>?

    @Transaction
    @Query("select * from forecast")
    fun getAllForecasts(): Flow<MutableList<Forecast>>

    @Query("select * from forecast where id = :id")
    fun getForecast(id: Long): Forecast?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateForecast(Forecast: Forecast?)
}