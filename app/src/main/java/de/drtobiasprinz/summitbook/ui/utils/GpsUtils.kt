package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.gpx.*
import io.reactivex.Observable
import de.drtobiasprinz.gpx.GPXParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import de.drtobiasprinz.gpx.Gpx


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
    fun composeTcxFile(files: ArrayList<File>): List<Track> {
        val tcxToGPXParser = TCXToGPXParser()
        files.sortBy { it.name }
        val composedTrack: MutableList<Track> = ArrayList()
        for (file in files) {
            var parsedTcx: Gpx? = null
            try {
                val inputStream: InputStream = FileInputStream(file)
                parsedTcx = tcxToGPXParser.parse(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            }
            if (parsedTcx != null) {
                val tracks = parsedTcx.tracks
                composedTrack.addAll(tracks.toList().blockingGet())

            }
        }
        return composedTrack
    }
    fun composeGpxFile(files: ArrayList<File>): List<Track> {
        val gpxParser = GPXParser()
        files.sortBy { it.name }
        val composedTrack: MutableList<Track> = ArrayList()
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
                composedTrack.addAll(tracks.toList().blockingGet())

            }
        }
        return composedTrack
    }
}