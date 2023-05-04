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
    fun addSegmentDetails(segmentDetails: SegmentDetails?): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSegmentEntry(segmentEntry: SegmentEntry?): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateSegmentDetails(segmentDetails: SegmentDetails?)
    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateSegmentEntry(segmentEntry: SegmentEntry?)

    @Delete
    fun deleteSegmentDetails(segmentDetails: SegmentDetails?)
    @Delete
    suspend fun deleteSegmentEntry(segmentEntry: SegmentEntry?)
    @Delete
    fun deleteSegmentEntryDeprecated(segmentEntry: SegmentEntry?)

    fun delete(segment: Segment) {
        for (entry in segment.segmentEntries) {
            deleteSegmentEntryDeprecated(entry)
        }
        deleteSegmentDetails(segment.segmentDetails)
    }

    @Query("select * from segmentdetails where segmentDetailsId = :id")
    fun getSegmentDetails(id: Long): SegmentDetails?

    @Query("select * from segmententry where entryId = :id")
    fun getSegmentEntry(id: Long): SegmentEntry?
}