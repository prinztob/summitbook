package de.drtobiasprinz.summitbook.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.drtobiasprinz.summitbook.core.Constants.DATE_FORMAT
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
        @ColumnInfo(defaultValue = "0") var heightMetersUp: Double = 0.0,
        @ColumnInfo(defaultValue = "0") var heightMetersDown: Double = 0.0,
        var averageHeartRate: Int,
        var averagePower: Int,
        @ColumnInfo(defaultValue = "0") var isMountainPass: Boolean = false,
        @ColumnInfo(defaultValue = "") var name: String = "",
        @ColumnInfo(defaultValue = "0.0") var avgGradient: Double = 0.0,
        @ColumnInfo(defaultValue = "0.0") var maxGradeInWindow: Double = 0.0,
        @ColumnInfo(defaultValue = "0.0") var windowDistanceMeters: Double = 0.0,
        @ColumnInfo(defaultValue = "0.0") var durationInMotion: Double = 0.0
) {
    fun getDateAsString(): String? {
        val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        return dateFormat.format(date)
    }

    fun getDisplayName(): String {
        return name.ifBlank { if (isMountainPass) "Mountain Pass" else "Segment" }
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
        result = 31 * result + heightMetersUp.hashCode()
        result = 31 * result + heightMetersDown.hashCode()
        result = 31 * result + averageHeartRate
        result = 31 * result + averagePower
        return result
    }

    companion object {
        const val SEGMENT_ENTRY_ID_EXTRA_IDENTIFIER = "SEGMENT_ENTRY_ID"

        fun getCsvHeadline(): String {
            return "Date;activityId;startPositionInTrack;startPositionLatitude;" +
                    "startPositionLongitude;endPositionInTrack;endPositionLatitude;" +
                    "endPositionLongitude;duration;kilometers;heightMetersUp;" +
                    "heightMetersDown;averageHeartRate};averagePower"
        }

        const val MOUNTAIN_PASS_NUMBER_OF_ELEMENTS = 20

        fun getMountainPassCsvHeadline(): String {
            return "Date;activityId;startPositionInTrack;startPositionLatitude;" +
                    "startPositionLongitude;endPositionInTrack;endPositionLatitude;" +
                    "endPositionLongitude;duration;kilometers;heightMetersUp;" +
                    "heightMetersDown;averageHeartRate;averagePower;isMountainPass;" +
                    "avgGradient;maxGradeInWindow;windowDistanceMeters;durationInMotion;name"
        }

        fun parseMountainPassFromCsvFileLine(line: String): SegmentEntry {
            val splitLine = line.split(";", limit = MOUNTAIN_PASS_NUMBER_OF_ELEMENTS)
            if (splitLine.size != MOUNTAIN_PASS_NUMBER_OF_ELEMENTS) {
                throw Exception(
                    "Line ${splitLine.joinToString { ";" }} has ${splitLine.size} number " +
                            "of elements. Expected are $MOUNTAIN_PASS_NUMBER_OF_ELEMENTS"
                )
            }
            return SegmentEntry(
                entryId = 0,
                segmentId = 0,
                date = Summit.parseDate(splitLine[0]),
                activityId = splitLine[1].toLong(),
                startPositionInTrack = splitLine[2].toInt(),
                startPositionLatitude = splitLine[3].toDouble(),
                startPositionLongitude = splitLine[4].toDouble(),
                endPositionInTrack = splitLine[5].toInt(),
                endPositionLatitude = splitLine[6].toDouble(),
                endPositionLongitude = splitLine[7].toDouble(),
                duration = splitLine[8].toDouble(),
                kilometers = splitLine[9].toDouble(),
                heightMetersUp = splitLine[10].toDouble(),
                heightMetersDown = splitLine[11].toDouble(),
                averageHeartRate = splitLine[12].toInt(),
                averagePower = splitLine[13].toInt(),
                isMountainPass = splitLine[14].toBoolean(),
                name = splitLine[19],
                avgGradient = splitLine[15].toDouble(),
                maxGradeInWindow = splitLine[16].toDouble(),
                windowDistanceMeters = splitLine[17].toDouble(),
                durationInMotion = splitLine[18].toDouble()
            )
        }

    }

    fun getMountainPassStringRepresentation(): String {
        val sanitizedName = name
            .replace("\n", " ")
            .replace("\r", " ")
        return "${getDateAsString()};${activityId};${startPositionInTrack};${startPositionLatitude};" +
                "${startPositionLongitude};${endPositionInTrack};${endPositionLatitude};" +
                "${endPositionLongitude};${duration};${kilometers};${heightMetersUp};" +
                "${heightMetersDown};${averageHeartRate};${averagePower};${isMountainPass};" +
                "${avgGradient};${maxGradeInWindow};${windowDistanceMeters};${durationInMotion};" +
                sanitizedName
    }

}
