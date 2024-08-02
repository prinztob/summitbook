package de.drtobiasprinz.summitbook.ui.utils

import android.location.Location
import de.drtobiasprinz.gpx.PointExtension
import de.drtobiasprinz.gpx.TrackPoint
import org.osmdroid.util.GeoPoint
import kotlin.math.abs


class GpsUtils {

    companion object {

        fun setDistanceFromPoints(trackPoints: List<TrackPoint?>) {
            var lastTrackPoint: TrackPoint? = null
            for (trackPoint in trackPoints) {
                if (trackPoint != null) {
                    if (lastTrackPoint == null) {
                        setDistance(trackPoint)
                        lastTrackPoint = trackPoint
                    } else {
                        val distance = lastTrackPoint.pointExtension?.distance
                        if (distance != null) {
                            setDistance(
                                trackPoint,
                                distance + abs(getDistance(trackPoint, lastTrackPoint)).toDouble()
                            )
                            lastTrackPoint = trackPoint
                        }
                    }
                }
            }
        }

        private fun setDistance(trackPoint: TrackPoint, distance: Double = 0.0) {
            if (trackPoint.pointExtension == null) {
                trackPoint.pointExtension = PointExtension.Builder().setDistance(distance).build()
            } else {
                trackPoint.pointExtension?.distance = distance
            }
        }

        fun getDistance(trackPoint1: TrackPoint, trackPoint2: TrackPoint): Float {
            val location1 = getLocationFromTrackPoint(trackPoint1)
            val location2 = getLocationFromTrackPoint(trackPoint2)
            return location1.distanceTo(location2)
        }

        fun getDistance(trackPoint1: GeoPoint, trackPoint2: GeoPoint): Float {
            val location1 = getLocationFromTrackPoint(trackPoint1)
            val location2 = getLocationFromTrackPoint(trackPoint2)
            return location1.distanceTo(location2)
        }

        private fun getLocationFromTrackPoint(trackPoint: TrackPoint): Location {
            val location = Location(trackPoint.name)
            location.latitude = trackPoint.latitude
            location.longitude = trackPoint.longitude
            location.altitude = trackPoint.elevation ?: 0.0
            return location
        }

        private fun getLocationFromTrackPoint(trackPoint: GeoPoint): Location {
            val location = Location("")
            location.latitude = trackPoint.latitude
            location.longitude = trackPoint.longitude
            location.altitude = trackPoint.altitude
            return location
        }

    }

}