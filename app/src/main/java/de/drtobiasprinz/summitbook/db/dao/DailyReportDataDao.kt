package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.DailyReportData
import kotlinx.coroutines.flow.Flow


@Dao
interface DailyReportDataDao {
    @Transaction
    @Query("select * from dailyreportdata")
    fun getAllDailyReportData(): Flow<MutableList<DailyReportData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(dailyReportData: DailyReportData?): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(dailyReportData: DailyReportData?)

    @Delete
    suspend fun delete(dailyReportData: DailyReportData?)
}