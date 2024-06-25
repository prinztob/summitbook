package de.drtobiasprinz.gpx

/**
 * A track point (trkpt) element.
 */
class TrackPoint private constructor(builder: Builder) : Point(builder) {
    class Builder : Point.Builder() {
        override fun build(): TrackPoint {
            return TrackPoint(this)
        }
    }
}
