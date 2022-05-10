package de.drtobiasprinz.summitbook.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Entity
data class SegmentEntry(
        @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
        var segmentId: Long,
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
) {
    fun getDateAsString(): String? {
        val dateFormat: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
        return dateFormat.format(date)
    }

    fun getStringRepresentation(): String {
        return "${getDateAsString()};${activityId};${startPositionInTrack};${startPositionLatitude};" +
                "${startPositionLongitude};${endPositionInTrack};${endPositionLatitude};" +
                "${endPositionLongitude};${duration};${kilometers};${heightMetersUp};" +
                "${heightMetersDown};${averageHeartRate};${averagePower}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SegmentEntry

        if (date != other.date) return false
        if (activityId != other.activityId) return false
        if (startPositionInTrack != other.startPositionInTrack) return false
        if (startPositionLatitude != other.startPositionLatitude) return false
        if (startPositionLongitude != other.startPositionLongitude) return false
        if (endPositionInTrack != other.endPositionInTrack) return false
        if (endPositionLatitude != other.endPositionLatitude) return false
        if (endPositionLongitude != other.endPositionLongitude) return false
        if (duration != other.duration) return false
        if (kilometers != other.kilometers) return false
        if (heightMetersUp != other.heightMetersUp) return false
        if (heightMetersDown != other.heightMetersDown) return false
        if (averageHeartRate != other.averageHeartRate) return false
        if (averagePower != other.averagePower) return false

        return true
    }

    override fun hashCode(): Int {
        var result = segmentId.hashCode()
        result = 31 * result + date.hashCode()
        result = 31 * result + activityId.hashCode()
        result = 31 * result + startPositionInTrack
        result = 31 * result + startPositionLatitude.hashCode()
        result = 31 * result + startPositionLongitude.hashCode()
        result = 31 * result + endPositionInTrack
        result = 31 * result + endPositionLatitude.hashCode()
        result = 31 * result + endPositionLongitude.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + kilometers.hashCode()
        result = 31 * result + heightMetersUp
        result = 31 * result + heightMetersDown
        result = 31 * result + averageHeartRate
        result = 31 * result + averagePower
        return result
    }

    companion object {
        fun getCsvHeadline(): String {
            return "Date;activityId;startPositionInTrack;startPositionLatitude;" +
                    "startPositionLongitude;endPositionInTrack;endPositionLatitude;" +
                    "endPositionLongitude;duration;kilometers;heightMetersUp;" +
                    "heightMetersDown;averageHeartRate};averagePower"
        }
    }

}
