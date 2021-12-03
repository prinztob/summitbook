package de.drtobiasprinz.summitbook.dao

import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import de.drtobiasprinz.summitbook.models.ElevationData
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.models.TrackBoundingBox
import de.drtobiasprinz.summitbook.models.VelocityData


@Dao
interface SummitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSummit(summit: Summit?): Long

    @get:Query("select * from summit")
    val allSummit: List<Summit>?

    @Query("select * from summit WHERE date >= :startDate")
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

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateSummit(summit: Summit?)

    @Query("delete from summit")
    fun removeAllSummit()

    @Query("SELECT * FROM summit WHERE places like :search_query")
    fun getSummitsWithConnectedId(search_query: String): Summit?

    @Delete
    fun delete(summit: Summit?)
}