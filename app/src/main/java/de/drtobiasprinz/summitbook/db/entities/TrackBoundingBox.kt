package de.drtobiasprinz.summitbook.db.entities

import androidx.room.Ignore
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

class TrackBoundingBox(
        var latNorth: Double, var latSouth: Double, var lonWest: Double, var lonEast: Double
) {

    @Ignore
    var trackBoundingRect = Rectangle(lonWest, latNorth, lonEast, latSouth)

    fun intersects(boundingBox: BoundingBox): Boolean {
        val boundingRect = Rectangle(boundingBox.lonWest , boundingBox.latNorth , boundingBox.lonEast , boundingBox.latSouth )
        return boundingRect.intersect(trackBoundingRect)
    }

    fun intersects(boundingBox: TrackBoundingBox): Boolean {
        val boundingRect = Rectangle(boundingBox.lonWest , boundingBox.latNorth , boundingBox.lonEast , boundingBox.latSouth )
        return boundingRect.intersect(trackBoundingRect)
    }

    fun contains(geoPoint: GeoPoint): Boolean {
        return trackBoundingRect.left <= geoPoint.longitude
                && trackBoundingRect.right >= geoPoint.longitude
                && trackBoundingRect.bottom <= geoPoint.latitude
                && trackBoundingRect.top >= geoPoint.latitude
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

    class Rectangle(var left: Double, var top: Double, var right: Double, var bottom: Double) {
        fun intersect(rec1: Rectangle): Boolean {
            return if (left == right || top == bottom || rec1.left == rec1.right || rec1.top == rec1.bottom) {
                false
            } else !(right <= rec1.left ||
                    top <= rec1.bottom ||
                    left >= rec1.right ||
                    bottom >= rec1.top)
        }
    }

}