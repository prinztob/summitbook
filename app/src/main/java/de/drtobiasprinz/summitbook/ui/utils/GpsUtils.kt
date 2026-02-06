package de.drtobiasprinz.summitbook.ui.utils

import android.location.Location
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.GpsTrack
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path


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

        fun getLocationFromTrackPoint(trackPoint: TrackPoint): Location {
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

        /**
         * Prepare a GPX track from a path or summit entry
         */
        fun prepareGpxTrack(path: Path?, entry: Summit?): GpsTrack? {
            val gpsTrack = if (path != null && path.toFile().exists()) {
                GpsTrack(path)
            } else if (entry != null && entry.hasGpsTrack()) {
                entry.gpsTrack
            } else {
                null
            }
            if (gpsTrack != null && gpsTrack.hasNoTrackPoints()) {
                gpsTrack.parseTrack()
            }
            return gpsTrack
        }

        /**
         * Copy GPX file to cache
         */
        fun copyGpxFileToCache(inputStream: InputStream, file: File) {
            var out: OutputStream? = null
            try {
                out = FileOutputStream(file)
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    out?.close()
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    }

}