package de.drtobiasprinz.summitbook.models

import androidx.room.Embedded
import androidx.room.Relation
import de.drtobiasprinz.summitbook.database.AppDatabase


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
        }
    }

    fun isDuplicate(allExistingEntries: List<Segment>?): Boolean {
        var isInList = false
        if (allExistingEntries != null) {
            for (entry in allExistingEntries) {
                if (entry == this) {
                    isInList = true
                }
            }
        }
        return isInList
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

        fun parseFromCsvFileLine(line: String, segments: MutableList<Segment>?, database: AppDatabase): Boolean {
            val segmentDetails = segments?.map { it.segmentDetails }
            val segmentEntries = segments?.flatMap { it.segmentEntries }
            val splitLine = line.split(";")
            checkValidNumberOfElements(splitLine)
//            Start Point	 End Point	 Date	activityId	startPositionInTrack	startPositionLatitude	startPositionLongitude	endPositionInTrack	endPositionLatitude	endPositionLongitude	duration	kilometers	heightMetersUp	heightMetersDown	averageHeartRate}	averagePower
            val segmentDetail = getSegmentDetail(splitLine, segmentDetails, database)
            val date = Summit.parseDate(splitLine[2])
            val segmentEntry = SegmentEntry(0, segmentDetail.segmentDetailsId, date,
                    splitLine[3].toLong(), splitLine[4].toInt(), splitLine[5].toDouble(),
                    splitLine[6].toDouble(), splitLine[7].toInt(), splitLine[8].toDouble(),
                    splitLine[9].toDouble(), splitLine[10].toDouble(), splitLine[11].toDouble(),
                    splitLine[12].toInt(), splitLine[13].toInt(), splitLine[14].toInt(), splitLine[15].toInt())
            if (segmentEntries?.contains(segmentEntry) == true) {
                return false
            } else {
                if (segmentDetails?.contains(segmentDetail) == false) {
                    segments.add(Segment(segmentDetail, mutableListOf(segmentEntry)))
                } else {
                    segments?.forEach {
                        if (it.segmentDetails == segmentDetail) {
                            it.segmentEntries.add(segmentEntry)
                        }
                    }
                }
                database.segmentsDao()?.addSegmentEntry(segmentEntry)
                return true
            }
        }

        private fun getSegmentDetail(entries: List<String>, segmentDetails: List<SegmentDetails>?, database: AppDatabase): SegmentDetails {
            var segmentDetail = SegmentDetails(0, entries[0], entries[1])
            if (segmentDetails?.contains(segmentDetail) == false) {
                segmentDetail.segmentDetailsId = database.segmentsDao()?.addSegmentDetails(segmentDetail)
                        ?: 0L
            } else {
                segmentDetail = segmentDetails?.firstOrNull { it == segmentDetail } ?: segmentDetail
            }
            return segmentDetail
        }

        private fun checkValidNumberOfElements(splitLine: List<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS) {
                throw Exception("Line ${splitLine.joinToString { ";" }} has ${splitLine.size} number " +
                        "of elements. Expected are $NUMBER_OF_ELEMENTS")
            }
        }

    }

}

