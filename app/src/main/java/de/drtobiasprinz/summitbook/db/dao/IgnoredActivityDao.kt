package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.IgnoredActivity


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