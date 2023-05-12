package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
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

    @Query("SELECT * FROM $SUMMITS_TABLE where isBookmark = 0")
    fun getAllSummits(): Flow<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE where isBookmark = 1")
    fun getAllBookmarks(): Flow<MutableList<Summit>>

    @Query("SELECT * FROM $SUMMITS_TABLE WHERE name LIKE '%' || :name || '%' OR comments LIKE '%' || :name || '%'")
    fun searchSummit(name: String): Flow<MutableList<Summit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSummit(summit: Summit?): Long

    @get:Query("select * from $SUMMITS_TABLE where isBookmark = 0")
    val allSummit: List<Summit>?

    @get:Query("select * from $SUMMITS_TABLE where isBookmark = 1")
    val allBookmark: List<Summit>?
}