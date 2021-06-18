package de.drtobiasprinz.gpx

/**
 * A point containing a location, time and name.
 */
abstract class Point internal constructor(
        time: Long? = null,
        ele: Double? = null,
        name: String? = null,
        extension: PointExtension? = null
) {

    abstract class Builder(
            var latitude: Double? = null,
            var longitude: Double? = null,
            var elevation: Double? = null,
            var name: String? = null,
            var time: Long? = null,
            var extension: PointExtension? = null
    ) {
        abstract fun build(): Point
    }

        companion object {
            const val TAG_LAT = "lat"
            const val TAG_LON = "lon"
            const val TAG_ELEVATION = "ele"
            const val TAG_NAME = "name"
        }


}