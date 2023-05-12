package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.IgnoredActivity
import kotlinx.coroutines.flow.Flow


@Dao
interface IgnoredActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(ignoredActivity: IgnoredActivity?)

    @Transaction
    @Query("select * from ignoredactivity")
    fun getAllIgnoredActivities(): Flow<List<IgnoredActivity>>

    @Delete
    suspend fun delete(ignoredActivity: IgnoredActivity?)

}