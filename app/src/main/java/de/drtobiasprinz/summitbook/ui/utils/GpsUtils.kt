package de.drtobiasprinz.summitbook.ui.utils

import co.beeline.gpx.*
import io.reactivex.Observable
import io.ticofab.androidgpxparser.parser.GPXParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import io.ticofab.androidgpxparser.parser.domain.Gpx as AndroidgpxparserParserDomainGpx


class GpsUtils {

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

    fun composeGpxFile(files: ArrayList<File>, name: String): List<Track> {
        val gpxParser = GPXParser()
        files.sortBy { it.name }
        val segmentPoints: ArrayList<TrackSegment> = ArrayList()
        for (file in files) {
            var parsedGpx: AndroidgpxparserParserDomainGpx? = null
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
                for (track in tracks) {
                    for (segment in track.trackSegments) {
                        val points: ArrayList<TrackPoint> = ArrayList()
                        for (point in segment.trackPoints) {
                            points.add(TrackPoint(point.latitude, point.longitude, point.time.millis, point.elevation, point.name))
                        }
                        segmentPoints.add(TrackSegment(
                                points = Observable.fromIterable(points)
                        ))
                    }
                }
            }
        }

        val composedTrack: ArrayList<Track> = ArrayList()
        composedTrack.add(Track(
                name = name,
                segments = Observable.fromIterable(segmentPoints)
        ))
        return composedTrack
    }
}