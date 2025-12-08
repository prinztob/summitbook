package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.DailyActivitySummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivitySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(dailyActivitySummary: DailyActivitySummary): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(dailyActivitySummaries: List<DailyActivitySummary>)

    @Transaction
    @Query("SELECT * FROM daily_report ORDER BY date DESC")
    fun getAllDailyActivitySummary(): Flow<MutableList<DailyActivitySummary>>

    @Query("SELECT * FROM daily_report WHERE id = :id")
    fun getDailyActivitySummary(id: Long): Flow<DailyActivitySummary?>

    @Query("SELECT * FROM daily_report WHERE date = :date")
    fun getDailyActivitySummariesByDate(date: String): Flow<List<DailyActivitySummary>>

    @Query("SELECT * FROM daily_report WHERE activityId = :activityId LIMIT 1")
    suspend fun getDailyActivitySummaryByDateSync(activityId: Long): DailyActivitySummary?

}