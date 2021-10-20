package de.drtobiasprinz.summitbook.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.models.IgnoredActivity


@Dao
interface IgnoredActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addActivity(ignoredActivity: IgnoredActivity?)

    @get:Query("select * from ignoredactivity")
    val allIgnoredActivities: List<IgnoredActivity>?

    @Delete
    fun delete(ignoredActivity: IgnoredActivity?)

    @Query("DELETE FROM ignoredactivity")
    fun deleteAll()

}