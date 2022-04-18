package de.drtobiasprinz.summitbook.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.models.*


@Dao
interface SegmentsDao {
    @Transaction
    @Query("select * from segmentdetails")
    fun getAllSegments(): MutableList<Segment>?

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
    fun deleteSegmentEntry(segmentEntry: SegmentEntry?)

    fun delete(segment: Segment) {
        for (entry in segment.segmentEntries) {
            deleteSegmentEntry(entry)
        }
        deleteSegmentDetails(segment.segmentDetails)
    }

    @Query("select * from segmentdetails where segmentDetailsId = :id")
    fun getSegmentDetails(id: Long): SegmentDetails?

    @Query("select * from segmententry where entryId = :id")
    fun getSegmentEntry(id: Long): SegmentEntry?
}