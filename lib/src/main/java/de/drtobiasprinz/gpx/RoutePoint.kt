package de.drtobiasprinz.gpx

/**
 * A route point (rtept) element.
 */
class RoutePoint private constructor(builder: Builder) : Point(builder) {
    class Builder : Point.Builder() {
        override fun build(): RoutePoint {
            return RoutePoint(this)
        }
    }
}
