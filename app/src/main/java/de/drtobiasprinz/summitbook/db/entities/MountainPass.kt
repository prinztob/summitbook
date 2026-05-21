package de.drtobiasprinz.summitbook.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.drtobiasprinz.summitbook.utils.Constants.DATE_FORMAT
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Entity
data class MountainPass(
    var name: String,
    var activityId: Long,
    var startPositionInTrack: Int,
    var startPositionLatitude: Double,
    var startPositionLongitude: Double,
    var endPositionInTrack: Int,
    var endPositionLatitude: Double,
    var endPositionLongitude: Double,
    var elevationGain: Double = 0.0,
    var elevationLoss: Double = 0.0,
    var avgGradient: Double = 0.0,
    var maxGradeInWindow: Double = 0.0,
    var windowDistanceMeters: Double = 500.0,
    var kilometers: Double = 0.0,
    var duration: Double = 0.0,
    var date: Date = Date()
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    fun getDateAsString(): String? {
        val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        return dateFormat.format(date)
    }

    fun getDisplayName(): String {
        return name.ifBlank { "Mountain Pass" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MountainPass

        if (activityId != other.activityId) return false
        if (startPositionInTrack != other.startPositionInTrack) return false
        if (startPositionLatitude != other.startPositionLatitude) return false
        if (startPositionLongitude != other.startPositionLongitude) return false
        if (endPositionInTrack != other.endPositionInTrack) return false
        if (endPositionLatitude != other.endPositionLatitude) return false
        if (endPositionLongitude != other.endPositionLongitude) return false

        return true
    }

    override fun hashCode(): Int {
        var result = activityId.hashCode()
        result = 31 * result + startPositionInTrack
        result = 31 * result + startPositionLatitude.hashCode()
        result = 31 * result + startPositionLongitude.hashCode()
        result = 31 * result + endPositionInTrack
        result = 31 * result + endPositionLatitude.hashCode()
        result = 31 * result + endPositionLongitude.hashCode()
        return result
    }
}
