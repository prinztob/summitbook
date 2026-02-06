package de.drtobiasprinz.summitbook.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.Surface
import de.drtobiasprinz.summitbook.utils.Constants.SUMMITS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface SummitsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSummit(entity: Summit): Long

    @Insert
    suspend fun insertAll(summits: List<Summit>)

    @Update
    suspend fun updateSummit(entity: Summit)

    @Query("UPDATE $SUMMITS_TABLE SET ignoreSimplifyingTrack = :ignoreSimplifyingTrack WHERE id = :summitId")
    suspend fun updateIgnoreSimplifyingTrack(summitId: Long, ignoreSimplifyingTrack: Boolean)

    @Query("UPDATE $SUMMITS_TABLE SET distancePerSurface = :distancePerSurface, distancePerRoadType = :distancePerRoadType WHERE id = :summitId")
    suspend fun updateDistanceData(summitId: Long, distancePerSurface: Map<Surface, Int>, distancePerRoadType: Map<RoadType, Int>)

    @Query("DELETE FROM $SUMMITS_TABLE")
    suspend fun deleteAll()

    @Delete
    suspend fun deleteSummit(entity: Summit)

    @Query("SELECT * FROM $SUMMITS_TABLE WHERE id ==:id")
    fun getSummit(id: Long): Flow<Summit>

    @Query("SELECT * FROM $SUMMITS_TABLE")
    fun getAllSummits(): Flow<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE")
    fun getAllSummitsLiveData(): LiveData<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE WHERE name LIKE '%' || :name || '%' OR comments LIKE '%' || :name || '%'")
    fun searchSummit(name: String): Flow<MutableList<Summit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSummit(summit: Summit): Long

    @Update
    fun updateSummitDeprecated(entity: Summit)

    @get:Query("select * from $SUMMITS_TABLE where isBookmark = 0")
    val allSummit: List<Summit>

    @get:Query("select * from $SUMMITS_TABLE where isBookmark = 1")
    val allBookmark: List<Summit>
}