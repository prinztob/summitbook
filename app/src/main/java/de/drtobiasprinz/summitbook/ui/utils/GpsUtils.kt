package de.drtobiasprinz.summitbook.ui.utils

import android.annotation.SuppressLint
import android.location.Location
import de.drtobiasprinz.gpx.GPXParser
import de.drtobiasprinz.gpx.PointExtension
import de.drtobiasprinz.gpx.Gpx
import de.drtobiasprinz.gpx.Track
import de.drtobiasprinz.gpx.TrackPoint
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.abs


class GpsUtils {

    companion object {


        private fun getLastPoints(tracks: MutableList<Track>): TrackPoint? {
            val trackPoints = mutableListOf<TrackPoint>()
            for (track in tracks) {
                for (segment in track.trackSegments) {
                    val points = segment.trackPoints
                    trackPoints.addAll(points.filter { it.latitude != 0.0 && it.longitude != 0.0 })
                }
            }
            return trackPoints.lastOrNull()
        }

        @SuppressLint("CheckResult")
        private fun updatePointsWithDistanceOfLastTrack(tracks: List<Track>, distance: Double) {
            tracks.forEach { track ->
                track.trackSegments.forEach { trackSegment ->
                    trackSegment.trackPoints.forEach {
                        it.pointExtension?.distance = it.pointExtension?.distance?.plus(distance)
                    }
                }
            }
        }

        fun composeGpxFile(files: ArrayList<File>): List<Track> {
            val gpxParser = GPXParser()
            files.sortBy { it.name }
            val composedTrack: MutableList<Track> = ArrayList()
            var i = 0
            for (file in files) {
                var parsedGpx: Gpx? = null
                try {
                    val inputStream: InputStream = FileInputStream(file)
                    parsedGpx = gpxParser.parse(inputStream)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: XmlPullParserException) {
                    e.printStackTrace()
                }
                if (parsedGpx != null) {
                    val tracks = parsedGpx.tracks
                    if (i > 0) {
                        updatePointsWithDistanceOfLastTrack(
                            tracks, getLastPoints(composedTrack)?.pointExtension?.distance
                                ?: 0.0
                        )
                    }
                    composedTrack.addAll(tracks)

                }
                i += 1
            }
            return composedTrack
        }


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

        private fun getLocationFromTrackPoint(trackPoint: TrackPoint): Location {
            val location = Location(trackPoint.name)
            location.latitude = trackPoint.latitude
            location.longitude = trackPoint.longitude
            location.altitude = trackPoint.elevation ?: 0.0
            return location
        }

    }

}