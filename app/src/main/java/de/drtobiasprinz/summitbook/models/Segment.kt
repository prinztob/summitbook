package de.drtobiasprinz.summitbook.models

import androidx.room.Embedded
import androidx.room.Relation


data class Segment(
        @Embedded val segmentDetails: SegmentDetails,
        @Relation(
                parentColumn = "segmentDetailsId",
                entityColumn = "segmentId"
        )
        val segmentEntries: MutableList<SegmentEntry>
)

