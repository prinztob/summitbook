package de.drtobiasprinz.summitbook.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.models.Summit


@Dao
interface SummitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSummit(summit: Summit?): Long

    @get:Query("select * from summit where isBookmark = 0")
    val allSummit: List<Summit>?

    @get:Query("select * from summit where isBookmark = 1")
    val allBookmark: List<Summit>?

    @Query("select * from summit WHERE date >= :startDate and isBookmark = 0")
    fun getAllSummitForYear(startDate: Long): List<Summit>?

    @Query("select * from summit where id = :id")
    fun getSummit(id: Long): Summit?

    @Query("select * from summit where activityId = :activityId")
    fun getSummitFromActivityId(activityId: Long): Summit?

    @Query("UPDATE summit SET imageIds=:imageIds WHERE id = :id")
    fun updateImageIds(id: Long, imageIds: List<Int>)

    @Query("UPDATE summit SET lat=:lat WHERE id = :id")
    fun updateLat(id: Long, lat: Double)

    @Query("UPDATE summit SET lng=:lng WHERE id = :id")
    fun updateLng(id: Long, lng: Double)

    @Query("UPDATE summit SET isFavorite=:isFavorite WHERE id = :id")
    fun updateIsFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE summit SET isPeak=:isPeak WHERE id = :id")
    fun updateIsPeak(id: Long, isPeak: Boolean)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateSummit(summit: Summit?)

    @Query("delete from summit")
    fun removeAllSummit()

    @Query("SELECT * FROM summit WHERE places like :search_query")
    fun getSummitsWithConnectedId(search_query: String): Summit?

    @Delete
    fun delete(summit: Summit?)
}