package de.drtobiasprinz.summitbook.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class SegmentEntry(
        @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
        val segmentId: Long,
        var date: Date,
        var activityId: Long,
        var startPositionInTrack: Int,
        var startPositionLatitude: Double,
        var startPositionLongitude: Double,
        var endPositionInTrack: Int,
        var endPositionLatitude: Double,
        var endPositionLongitude: Double,
        var duration: Double,
        var kilometers: Double,
        var heightMetersUp: Int,
        var heightMetersDown: Int,
        var averageHeartRate: Int,
        var averagePower: Int
)
