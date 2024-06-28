package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import kotlinx.coroutines.flow.Flow


@Dao
interface SegmentsDao {
    @Transaction
    @Query("select * from segmentdetails")
    fun getAllSegmentsDeprecated(): MutableList<Segment>?

    @Transaction
    @Query("select * from segmentdetails")
    fun getAllSegments(): Flow<MutableList<Segment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSegmentDetails(segmentDetails: SegmentDetails): Long
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

    suspend fun updateSegment(segment: Segment) {
        for (entry in segment.segmentEntries) {
            updateSegmentEntry(entry)
        }
        updateSegmentDetails(segment.segmentDetails)
    }

    suspend fun saveSegment(segment: Segment) {
        for (entry in segment.segmentEntries) {
            addSegmentEntry(entry)
        }
        addSegmentDetails(segment.segmentDetails)
    }

}