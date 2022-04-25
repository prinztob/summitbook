package de.drtobiasprinz.summitbook.models

import android.graphics.Rect
import androidx.room.Ignore
import de.drtobiasprinz.gpx.TrackPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

class TrackBoundingBox(
        var latNorth: Double, var latSouth: Double, var lonWest: Double, var lonEast: Double
) {
    @Ignore
    private var factor = 1000

    @Ignore
    var trackBoundingRect = Rect(floor(lonWest * factor).toInt(), ceil( latNorth * factor).toInt(), ceil(lonEast * factor).toInt(), floor(latSouth * factor).toInt())

    fun intersects(boundingBox: BoundingBox): Boolean {
        val boundingRect = Rect(floor(boundingBox.lonWest * factor).toInt(), ceil(boundingBox.latNorth * factor).toInt(), ceil(boundingBox.lonEast * factor).toInt(), floor(boundingBox.latSouth * factor).toInt())
        return boundingRect.intersect(trackBoundingRect)
    }

    fun contains(geoPoint: GeoPoint): Boolean {
        return trackBoundingRect.left <= (geoPoint.longitude * factor).toInt() && trackBoundingRect.right >= (geoPoint.longitude * factor).toInt() && trackBoundingRect.bottom <= (geoPoint.latitude * factor).toInt() && trackBoundingRect.top >= (geoPoint.latitude * factor).toInt()
    }

    fun getGeoPoints(): List<GeoPoint> {
        return listOf(
                GeoPoint(latNorth, lonWest),
                GeoPoint(latNorth, lonEast),
                GeoPoint(latSouth, lonEast),
                GeoPoint(latSouth, lonWest),
                GeoPoint(latNorth, lonWest)
        )
    }

}