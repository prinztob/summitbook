package de.drtobiasprinz.summitbook.db.entities

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentDetails

        if (startPointName != other.startPointName) return false
        if (endPointName != other.endPointName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startPointName.hashCode()
        result = 31 * result + endPointName.hashCode()
        return result
    }

    companion object {
        const val SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER = "SEGMENT_DETAILS_ID"
    }
}
