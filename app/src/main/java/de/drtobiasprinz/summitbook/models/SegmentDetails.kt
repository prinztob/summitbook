package de.drtobiasprinz.summitbook.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class SegmentDetails(
        @PrimaryKey(autoGenerate = true) var segmentDetailsId: Long = 0,
        var startPointName: String,
        var endPointName: String
) {
    fun getDisplayName(): String {
        return "$startPointName - $endPointName"
    }
}
