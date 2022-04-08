package de.drtobiasprinz.gpx

import android.util.Pair
import android.util.Xml
import de.drtobiasprinz.gpx.Time.Companion.parse
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.*

class TCXToGPXParser {
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(tcsInputStream: InputStream): Gpx {
        return tcsInputStream.use {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(it, null)
            parser.nextTag()
            readTcx(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTcx(parser: XmlPullParser): Gpx {
        val tracks: MutableList<Track> = ArrayList()
        val waypoints: List<Waypoint> = ArrayList()
        val routes: List<Route> = ArrayList()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TCX)
        val builder = Gpx.Builder()
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_ACTIVITIES -> tracks.addAll(readActivities(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TCX)
        builder.tracks = tracks
        builder.wayPoints = waypoints
        builder.routes = routes
        return builder.build()
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readActivities(parser: XmlPullParser): List<Track> {
        val tracks: MutableList<Track> = ArrayList()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_ACTIVITIES)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_ACTIVITY -> tracks.addAll(readActivity(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_ACTIVITIES)
        return tracks
    }

    // Processes summary tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readActivity(parser: XmlPullParser): List<Track> {
        val tracks: MutableList<Track> = ArrayList()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_ACTIVITY)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (TAG_LAP == name) {
                tracks.addAll(readLap(parser))
            } else {
                skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_ACTIVITY)
        return tracks
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLap(parser: XmlPullParser): List<Track> {
        val tracks: MutableList<Track> = ArrayList()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_LAP)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (TAG_TRACK == name) {
                tracks.add(readTrack(parser))
            } else {
                skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_LAP)
        return tracks
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTrack(parser: XmlPullParser): Track {
        val trackBuilder = Track.Builder()
        val trackPoints: MutableList<TrackPoint> = ArrayList()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TRACK)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_NAME -> trackBuilder.name = readName(parser)
                TAG_TRACK_POINT -> trackPoints.add(readTrackPoint(parser))
                TAG_DESC -> trackBuilder.desc = readDesc(parser)
                TAG_CMT -> trackBuilder.cmt = readCmt(parser)
                TAG_SRC -> trackBuilder.src = readString(parser, TAG_SRC)
                TAG_NUMBER -> trackBuilder.number = readNumber(parser)
                TAG_TYPE -> trackBuilder.type = readString(parser, TAG_TYPE)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TRACK)
        val segments: MutableList<TrackSegment> = ArrayList()
        val trackSegmentBuilder = TrackSegment.Builder()
        trackSegmentBuilder.trackPoints = trackPoints
        segments.add(trackSegmentBuilder.build())
        trackBuilder.segments = segments
        return trackBuilder.build()
    }

    /**
     * Reads a single point, which can either be a [TrackPoint], [RoutePoint] or [Waypoint].
     *
     * @param builder The prepared builder, one of [TrackPoint.Builder], [RoutePoint.Builder] or [Waypoint.Builder].
     * @param parser  Parser
     * @param tagName Tag name, e.g. trkpt, rtept, wpt
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readPoint(builder: Point.Builder, parser: XmlPullParser, tagName: String): Point {
        parser.require(XmlPullParser.START_TAG, namespace, tagName)
        val extensionBuilder: PointExtension.Builder = PointExtension.Builder()
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_NAME -> builder.name = readName(parser)
                TAG_ELEVATION -> builder.elevation = readElevation(parser)
                TAG_TIME -> builder.time = readTime(parser)
                TAG_HEART_RATE -> extensionBuilder.heartRate = readHeartRate(parser)
                TAG_DISTANCE -> extensionBuilder.distanceMeters = readText(parser).toDouble()
                TAG_CADENCE -> extensionBuilder.cadence = readText(parser).toInt()
                TAG_POSITION -> {
                    val position = readPosition(parser)
                    builder.latitude = position.first.toDouble()
                    builder.longitude = position.second.toDouble()
                }
                TAG_EXTENSIONS -> readExtension(parser, extensionBuilder)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, tagName)
        builder.extension = extensionBuilder.build()
        return builder.build()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPosition(parser: XmlPullParser): Pair<String, String> {
        var latitude = "0"
        var longitude = "0"
        parser.require(XmlPullParser.START_TAG, namespace, TAG_POSITION)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_LAT -> {
                    latitude = readString(parser, TAG_LAT)
                }
                TAG_LON -> {
                    longitude = readString(parser, TAG_LON)
                }
                else -> {
                    skip(parser)
                }
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_POSITION)
        return Pair(latitude, longitude)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTrackPoint(parser: XmlPullParser): TrackPoint {
        return readPoint(TrackPoint.Builder(), parser, TAG_TRACK_POINT) as TrackPoint
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readName(parser: XmlPullParser): String {
        return readString(parser, TAG_NAME)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readDesc(parser: XmlPullParser): String {
        return readString(parser, TAG_DESC)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readExtension(parser: XmlPullParser, extensionBuilder: PointExtension.Builder) {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_EXTENSIONS)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.prefix != "ns3") {
                continue
            }
            parser.require(XmlPullParser.START_TAG, namespace, TAG_TPX)
            while (loopMustContinue(parser.next())) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                if (parser.prefix != "ns3") {
                    continue
                }
                when (parser.name) {
                    TAG_POWER -> extensionBuilder.power = readString(parser, TAG_POWER).toInt()
                    TAG_SPEED -> extensionBuilder.speed = readString(parser, TAG_SPEED).toDouble()
                    else -> skip(parser)
                }
            }
            parser.require(XmlPullParser.END_TAG, namespace, TAG_TPX)
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_EXTENSIONS)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readCmt(parser: XmlPullParser): String {
        return readString(parser, TAG_CMT)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readString(parser: XmlPullParser, tag: String): String {
        parser.require(XmlPullParser.START_TAG, namespace, tag)
        val value = readText(parser)
        parser.require(XmlPullParser.END_TAG, namespace, tag)
        return value
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readElevation(parser: XmlPullParser): Double {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_ELEVATION)
        val ele = readText(parser).toDouble()
        parser.require(XmlPullParser.END_TAG, namespace, TAG_ELEVATION)
        return ele
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readHeartRate(parser: XmlPullParser): Int {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_HEART_RATE)
        var heartRate = 0
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (TAG_VALUE == name) {
                heartRate = readString(parser, TAG_VALUE).toInt()
            } else {
                skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_HEART_RATE)
        return heartRate
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTime(parser: XmlPullParser): Long? {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TIME)
        val (date) = parse(readText(parser))
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TIME)
        return date
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(IOException::class, XmlPullParserException::class, NumberFormatException::class)
    private fun readNumber(parser: XmlPullParser): Int {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_NUMBER)
        val number = readText(parser).toInt()
        parser.require(XmlPullParser.END_TAG, namespace, TAG_NUMBER)
        return number
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun loopMustContinue(next: Int): Boolean {
        return next != XmlPullParser.END_TAG && next != XmlPullParser.END_DOCUMENT
    }

    companion object {
        private const val TAG_TCX = "TrainingCenterDatabase"
        private const val TAG_TPX = "TPX"
        private const val TAG_ACTIVITIES = "Activities"
        private const val TAG_ACTIVITY = "Activity"
        private const val TAG_LAP = "Lap"
        private const val TAG_TRACK = "Track"
        private const val TAG_TRACK_POINT = "Trackpoint"
        private const val TAG_LAT = "LatitudeDegrees"
        private const val TAG_LON = "LongitudeDegrees"
        private const val TAG_ELEVATION = "AltitudeMeters"
        private const val TAG_TIME = "Time"
        private const val TAG_POSITION = "Position"
        private const val TAG_NAME = "name"
        private const val TAG_DESC = "desc"
        private const val TAG_CMT = "cmt"
        private const val TAG_SRC = "src"
        private const val TAG_NUMBER = "number"
        private const val TAG_TYPE = "type"
        private const val TAG_EXTENSIONS = "Extensions"
        private const val TAG_DISTANCE = "DistanceMeters"
        const val TAG_CADENCE = "Cadence"
        const val TAG_HEART_RATE = "HeartRateBpm"
        const val TAG_SPEED = "Speed"
        const val TAG_POWER = "Watts"
        const val TAG_VALUE = "Value"
        private val namespace: String? = null
    }
}