package de.drtobiasprinz.gpx

/**
 * A way point (wpt element).
 */
class WayPoint private constructor(builder: Builder) : Point(builder) {
    class Builder : Point.Builder() {
        override fun build(): WayPoint {
            return WayPoint(this)
        }
    }
}
