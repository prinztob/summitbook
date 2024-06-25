package de.drtobiasprinz.gpx

import java.util.Collections

class TrackSegment private constructor(builder: Builder) {
    val trackPoints: List<TrackPoint> =
        builder.mTrackPoints ?: emptyList()

    class Builder {
        var mTrackPoints: List<TrackPoint>? = null

        fun setTrackPoints(trackPoints: List<TrackPoint>?): Builder {
            mTrackPoints = trackPoints
            return this
        }

        fun build(): TrackSegment {
            return TrackSegment(this)
        }
    }
}
