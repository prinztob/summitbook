package de.drtobiasprinz.summitbook.ui.utils

import android.annotation.SuppressLint
import android.location.Location
import de.drtobiasprinz.gpx.*
import io.reactivex.Observable
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import kotlin.math.abs


class GpsUtils {

    companion object {

        fun write(file: File, tracks: List<Track>, name: String) {
            val gpx = Gpx(
                    creator = "SummitBook",
                    metadata = Metadata(name = name),
                    tracks = Observable.fromIterable(tracks)
            )
            val fileWriter = FileWriter(file)
            val gpxWriter = gpx.writeTo(fileWriter)
            gpxWriter.subscribe()

        }

        private fun getLastPoints(tracks: MutableList<Track>): TrackPoint? {
            val trackPoints = mutableListOf<TrackPoint>()
            for (track in tracks) {
                for (segment in track.segments.toList().blockingGet()) {
                    val points = segment.points.toList().blockingGet()
                    trackPoints.addAll(points.filter { it.lat != 0.0 && it.lon != 0.0 })
                }
            }
            return trackPoints.lastOrNull()
        }

        @SuppressLint("CheckResult")
        private fun updatePointsWithDistanceOfLastTrack(tracks: Observable<Track>, distance: Double) {
            tracks.forEach { track ->
                track.segments.forEach { trackSegment ->
                    trackSegment.points.forEach {
                        it.extension?.distance = it.extension?.distance?.plus(distance)
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
                        updatePointsWithDistanceOfLastTrack(tracks, getLastPoints(composedTrack)?.extension?.distance
                                ?: 0.0)
                    }
                    composedTrack.addAll(tracks.toList().blockingGet())

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
                        val distance = lastTrackPoint.extension?.distance
                        if (distance != null) {
                            setDistance(trackPoint, distance + abs(getDistance(trackPoint, lastTrackPoint)).toDouble())
                            lastTrackPoint = trackPoint
                        }
                    }
                }
            }
        }

        private fun setDistance(trackPoint: TrackPoint, distance: Double = 0.0) {
            if (trackPoint.extension == null) {
                trackPoint.extension = PointExtension(distance = distance)
            } else {
                trackPoint.extension?.distance = distance
            }
        }

        fun getDistance(trackPoint1: TrackPoint, trackPoint2: TrackPoint): Float {
            val location1 = getLocationFromTrackPoint(trackPoint1)
            val location2 = getLocationFromTrackPoint(trackPoint2)
            return location1.distanceTo(location2)
        }

        private fun getLocationFromTrackPoint(trackPoint: TrackPoint): Location {
            val location = Location(trackPoint.name)
            location.latitude = trackPoint.lat
            location.longitude = trackPoint.lon
            location.altitude = trackPoint.ele ?: 0.0
            return location
        }

    }

}