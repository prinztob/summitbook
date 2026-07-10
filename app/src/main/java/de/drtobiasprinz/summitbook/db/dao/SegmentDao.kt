package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import kotlinx.coroutines.flow.Flow


@Dao
interface SegmentDao {
    @Transaction
    @Query("select * from segmentdetails")
    fun getAllSegmentsDeprecated(): MutableList<Segment>

    @Transaction
    @Query("select * from segmentdetails")
    fun getAllSegments(): Flow<MutableList<Segment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSegmentDetails(segmentDetails: SegmentDetails): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSegmentDetailsDeprecated(segmentDetails: SegmentDetails): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSegmentEntry(segmentEntry: SegmentEntry): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSegmentDetails(segmentDetails: SegmentDetails)
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSegmentEntry(segmentEntry: SegmentEntry)

    @Delete
    suspend fun deleteSegmentDetails(segmentDetails: SegmentDetails)
    @Delete
    suspend fun deleteSegmentEntry(segmentEntry: SegmentEntry)

    suspend fun deleteSegment(segment: Segment) {
        for (entry in segment.segmentEntries) {
            deleteSegmentEntry(entry)
        }
        deleteSegmentDetails(segment.segmentDetails)
    }

    @Query("SELECT * FROM SegmentEntry WHERE isMountainPass = 1")
    fun getAllMountainPasses(): Flow<List<SegmentEntry>>

    @Query("SELECT * FROM SegmentEntry WHERE isMountainPass = 1 AND activityId = :activityId")
    fun getMountainPassesForActivity(activityId: Long): Flow<List<SegmentEntry>>

}
