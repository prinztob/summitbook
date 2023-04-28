package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.utils.Constants.SUMMITS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface SummitsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSummit(entity: Summit): Long

    @Update
    suspend fun updateSummit(entity: Summit)

    @Delete
    suspend fun deleteSummit(entity: Summit)

    @Query("SELECT * FROM $SUMMITS_TABLE WHERE id ==:id")
    fun getSummit(id: Long): Flow<Summit>

    @Query("SELECT * FROM $SUMMITS_TABLE WHERE id ==:id")
    fun getSummitDeprecated(id: Long): Summit

    @Query("SELECT * FROM $SUMMITS_TABLE where isBookmark = 0")
    fun getAllSummits(): Flow<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE where isBookmark = 1")
    fun getAllBookmarks(): Flow<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE ORDER BY name ASC")
    fun sortedASC(): Flow<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE ORDER BY name DESC")
    fun sortedDESC(): Flow<MutableList<Summit>>

    @RawQuery(observedEntities = [Summit::class])
    fun getSortedAndFilteredSummits(query: SupportSQLiteQuery): Flow<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE WHERE name LIKE '%' || :name || '%' OR comments LIKE '%' || :name || '%'")
    fun searchSummit(name: String): Flow<MutableList<Summit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSummit(summit: Summit?): Long

    @get:Query("select * from $SUMMITS_TABLE where isBookmark = 0")
    val allSummit: List<Summit>?

    @get:Query("select * from $SUMMITS_TABLE where isBookmark = 1")
    val allBookmark: List<Summit>?

    @Query("SELECT COUNT(id) FROM $SUMMITS_TABLE where isBookmark = 0")
    fun getCountSummits(): Int

    @Query("SELECT COUNT(id) FROM $SUMMITS_TABLE where isBookmark = 1")
    fun getCountBookmarks(): Int

    @RawQuery
    fun get(query: SupportSQLiteQuery): Double

    @Query("select * from $SUMMITS_TABLE where sportType == :sportType and isBookmark = 0")
    fun getAllSummitWithSameSportType(sportType: SportType): List<Summit>?

    @Query("select * from $SUMMITS_TABLE where date >= :startDate and isBookmark = 0")
    fun getAllSummitForYear(startDate: Long): List<Summit>?

    @Query("select * from $SUMMITS_TABLE where activityId = :activityId")
    fun getSummitFromActivityId(activityId: Long): Summit?

    @Query("UPDATE $SUMMITS_TABLE SET imageIds=:imageIds WHERE id = :id")
    fun updateImageIds(id: Long, imageIds: List<Int>)

    @Query("UPDATE $SUMMITS_TABLE SET lat=:lat WHERE id = :id")
    fun updateLat(id: Long, lat: Double)

    @Query("UPDATE $SUMMITS_TABLE SET lng=:lng WHERE id = :id")
    fun updateLng(id: Long, lng: Double)

    @Query("UPDATE $SUMMITS_TABLE SET isFavorite=:isFavorite WHERE id = :id")
    fun updateIsFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE $SUMMITS_TABLE SET isPeak=:isPeak WHERE id = :id")
    fun updateIsPeak(id: Long, isPeak: Boolean)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateSummit(summit: Summit?)

    @Query("delete from $SUMMITS_TABLE")
    fun removeAllSummit()

    @Query("SELECT * FROM $SUMMITS_TABLE WHERE places like :search_query")
    fun getSummitsWithConnectedId(search_query: String): Summit?

    @Delete
    fun delete(summit: Summit?)
}