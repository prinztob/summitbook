package de.drtobiasprinz.summitbook.ui.utils

import android.location.Location
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.osmdroid.util.GeoPoint


class GpsUtils {

    companion object {

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