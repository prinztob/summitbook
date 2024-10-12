package de.drtobiasprinz.summitbook.db.entities

import androidx.room.Embedded
import androidx.room.Relation
import de.drtobiasprinz.summitbook.ui.MainActivity
import java.io.File


data class Segment(
    @Embedded val segmentDetails: SegmentDetails,
    @Relation(
        parentColumn = "segmentDetailsId",
        entityColumn = "segmentId"
    )
    val segmentEntries: MutableList<SegmentEntry>

) {



    fun getStringRepresentation(): String {
        return segmentEntries.joinToString("\n") {
            "${segmentDetails.startPointName};${segmentDetails.endPointName};${it.getStringRepresentation()}"
        } + "\n"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Segment

        if (segmentDetails != other.segmentDetails) return false
        if (segmentEntries != other.segmentEntries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = segmentDetails.hashCode()
        result = 31 * result + segmentEntries.hashCode()
        return result
    }

    companion object {
        const val NUMBER_OF_ELEMENTS = 16
        fun getCsvHeadline(): String {
            return "Start Point; " +
                    "End Point; " +
                    SegmentEntry.getCsvHeadline() + "\n"
        }

        fun getMapScreenshotFile(segmentDetailsId: Long): File {
            return File(MainActivity.segmentScreenshotDir, "id_${segmentDetailsId}.jpg")
        }

        fun parseFromCsvFileLine(
            line: String,
            segments: MutableList<Segment>?,
        ) {
            val splitLine = line.split(";")
            checkValidNumberOfElements(splitLine)
//            Start Point	 End Point	 Date	activityId	startPositionInTrack	startPositionLatitude	startPositionLongitude	endPositionInTrack	endPositionLatitude	endPositionLongitude	duration	kilometers	heightMetersUp	heightMetersDown	averageHeartRate}	averagePower
            val segmentDetail = SegmentDetails(0, splitLine[0], splitLine[1])
            addSegmentEntries(
                splitLine,
                segmentDetail,
                segments
            )
        }

        private fun addSegmentEntries(
            splitLine: List<String>,
            segmentDetail: SegmentDetails,
            segments: MutableList<Segment>?,
        ): Boolean {
            val date = Summit.parseDate(splitLine[2])
            val segmentEntry = SegmentEntry(
                0,
                segmentDetail.segmentDetailsId,
                date,
                splitLine[3].toLong(),
                splitLine[4].toInt(),
                splitLine[5].toDouble(),
                splitLine[6].toDouble(),
                splitLine[7].toInt(),
                splitLine[8].toDouble(),
                splitLine[9].toDouble(),
                splitLine[10].toDouble(),
                splitLine[11].toDouble(),
                splitLine[12].toInt(),
                splitLine[13].toInt(),
                splitLine[14].toInt(),
                splitLine[15].toInt()
            )
            return if (segments?.flatMap { it.segmentEntries }?.contains(segmentEntry) == true) {
                false
            } else {
                if (segments?.map { it.segmentDetails }?.contains(segmentDetail) == false) {
                    segments.add(Segment(segmentDetail, mutableListOf(segmentEntry)))
                } else {
                    segments?.forEach {
                        if (it.segmentDetails == segmentDetail) {
                            it.segmentEntries.add(segmentEntry)
                        }
                    }
                }
                true
            }
        }


        private fun checkValidNumberOfElements(splitLine: List<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS) {
                throw Exception(
                    "Line ${splitLine.joinToString { ";" }} has ${splitLine.size} number " +
                            "of elements. Expected are $NUMBER_OF_ELEMENTS"
                )
            }
        }

    }

}

