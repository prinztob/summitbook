package de.drtobiasprinz.gpx

import android.util.Xml
import de.drtobiasprinz.gpx.Gpx.Companion.TAG_CREATOR
import de.drtobiasprinz.gpx.Gpx.Companion.TAG_GPX
import de.drtobiasprinz.gpx.Metadata.Companion.TAG_AUTHOR
import de.drtobiasprinz.gpx.Metadata.Companion.TAG_METADATA
import de.drtobiasprinz.gpx.Point.Companion.TAG_ELEVATION
import de.drtobiasprinz.gpx.Point.Companion.TAG_LAT
import de.drtobiasprinz.gpx.Point.Companion.TAG_LON
import de.drtobiasprinz.gpx.TrackPoint.Companion.TAG_TRACK_POINT
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_ATEMP
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_CADENCE
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_DISTANCE
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_EXTENSIONS
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_HR
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_POWER
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_SLOPE
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_SPEED
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_TRACK_POINT_EXTENSIONS
import de.drtobiasprinz.gpx.PointExtension.Companion.TAG_VERT_VELOCITY
import de.drtobiasprinz.gpx.Route.Companion.TAG_ROUTE
import de.drtobiasprinz.gpx.RoutePoint.Companion.TAG_ROUTE_POINT
import de.drtobiasprinz.gpx.Time.Companion.TAG_TIME
import de.drtobiasprinz.gpx.Track.Companion.TAG_CMT
import de.drtobiasprinz.gpx.Track.Companion.TAG_NUMBER
import de.drtobiasprinz.gpx.Track.Companion.TAG_SRC
import de.drtobiasprinz.gpx.Track.Companion.TAG_TRACK
import de.drtobiasprinz.gpx.Track.Companion.TAG_TYPE
import de.drtobiasprinz.gpx.TrackSegment.Companion.TAG_SEGMENT
import de.drtobiasprinz.gpx.Waypoint.Companion.TAG_WAY_POINT
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.*

class GPXParser {
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(`in`: InputStream): Gpx {
        return `in`.use { `in` ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(`in`, null)
            parser.nextTag()
            readGpx(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readGpx(parser: XmlPullParser): Gpx {
        val wayPoints: MutableList<Waypoint> = ArrayList()
        val tracks: MutableList<Track> = ArrayList()
        val routes: MutableList<Route> = ArrayList()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_GPX)
        val builder: Gpx.Builder = Gpx.Builder()
        builder.creator = parser.getAttributeValue(namespace, TAG_CREATOR)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_METADATA -> builder.metadata = readMetadata(parser)
                TAG_WAY_POINT -> wayPoints.add(readWaypoint(parser))
                TAG_ROUTE -> routes.add(readRoute(parser))
                TAG_TRACK -> tracks.add(readTrack(parser))
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_GPX)
        builder.routes = routes
        builder.tracks = tracks
        builder.wayPoints = wayPoints
        return builder.build()
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTrack(parser: XmlPullParser): Track {
        val trackBuilder: Track.Builder = Track.Builder()
        val segments: MutableList<TrackSegment> = ArrayList()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TRACK)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_NAME -> trackBuilder.name = readName(parser)
                TAG_SEGMENT -> segments.add(readSegment(parser))
                TAG_DESC -> trackBuilder.desc = readDesc(parser)
                TAG_CMT -> trackBuilder.cmt = readCmt(parser)
                TAG_SRC -> trackBuilder.src = readString(parser, TAG_SRC)
                TAG_NUMBER -> trackBuilder.number = readNumber(parser)
                TAG_TYPE -> trackBuilder.type = readString(parser, TAG_TYPE)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TRACK)
        trackBuilder.segments = segments
        return trackBuilder.build()
    }

    // Processes summary tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readSegment(parser: XmlPullParser): TrackSegment {
        val points: MutableList<TrackPoint> = ArrayList()
        val trackSegmentBuilder = TrackSegment.Builder()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_SEGMENT)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            val name = parser.name
            if (TAG_TRACK_POINT == name) {
                points.add(readTrackPoint(parser))
            } else {
                skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_SEGMENT)
        trackSegmentBuilder.trackPoints = points
        return trackSegmentBuilder.build()
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readRoute(parser: XmlPullParser): Route {
        val points: MutableList<RoutePoint> = ArrayList<RoutePoint>()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_ROUTE)
        val routeBuilder: Route.Builder = Route.Builder()
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_ROUTE_POINT -> points.add(readRoutePoint(parser))
                TAG_NAME -> routeBuilder.routeName = readName(parser)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_ROUTE)
        routeBuilder.routePoints = points
        return routeBuilder.build()
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
        builder.latitude = java.lang.Double.valueOf(parser.getAttributeValue(namespace, TAG_LAT))
        builder.longitude = java.lang.Double.valueOf(parser.getAttributeValue(namespace, TAG_LON))
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_NAME -> builder.name = readName(parser)
                TAG_ELEVATION -> builder.elevation = readElevation(parser)
                TAG_TIME -> builder.time = readTime(parser)
                TAG_EXTENSIONS -> builder.extension = readExtension(parser)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, tagName)
        return builder.build()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readMetadata(parser: XmlPullParser): Metadata {
        val metadataBuilder: Metadata.Builder = Metadata.Builder()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_METADATA)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_NAME -> metadataBuilder.name = readName(parser)
                TAG_DESC -> metadataBuilder.desc = readDesc(parser)
                TAG_AUTHOR -> metadataBuilder.author = readAuthor(parser)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_METADATA)
        return metadataBuilder.build()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readAuthor(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_AUTHOR)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_NAME -> return readString(parser, TAG_NAME)
                else -> skip(parser)
            }
        }
         TAG_AUTHOR
        parser.require(XmlPullParser.END_TAG, namespace, TAG_AUTHOR)
        return "unknown"
    }


    @Throws(XmlPullParserException::class, IOException::class)
    private fun readWaypoint(parser: XmlPullParser): Waypoint {
        return readPoint(Waypoint.Builder(), parser, TAG_WAY_POINT) as Waypoint
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTrackPoint(parser: XmlPullParser): TrackPoint {
        return readPoint(TrackPoint.Builder(), parser, TAG_TRACK_POINT) as TrackPoint
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readRoutePoint(parser: XmlPullParser): RoutePoint {
        return readPoint(RoutePoint.Builder(), parser, TAG_ROUTE_POINT) as RoutePoint
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
    private fun readExtension(parser: XmlPullParser): PointExtension {
        val extensionBuilder: PointExtension.Builder = PointExtension.Builder()
        parser.require(XmlPullParser.START_TAG, namespace, TAG_EXTENSIONS)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                TAG_TRACK_POINT_EXTENSIONS -> readTrackPointExtensions(parser, extensionBuilder, parser.prefix)
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_EXTENSIONS)
        return extensionBuilder.build()
    }

    private fun readTrackPointExtensions(parser: XmlPullParser, extensionBuilder: PointExtension.Builder, extensionNamespace: String) {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TRACK_POINT_EXTENSIONS)
        while (loopMustContinue(parser.next())) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.prefix != extensionNamespace) {
                continue
            }
            when (parser.name) {
                TAG_CADENCE -> extensionBuilder.cadence = readString(parser, TAG_CADENCE).toInt()
                TAG_DISTANCE -> extensionBuilder.distanceMeters = readString(parser, TAG_DISTANCE).toDouble()
                TAG_HR -> extensionBuilder.heartRate = readString(parser, TAG_HR).toInt()
                TAG_POWER -> extensionBuilder.power = readString(parser, TAG_POWER).toInt()
                TAG_SPEED -> extensionBuilder.speed = readString(parser, TAG_SPEED).toDouble()
                TAG_ATEMP -> extensionBuilder.temp = readString(parser, TAG_ATEMP).toDouble()
                TAG_SLOPE -> extensionBuilder.slope = readString(parser, TAG_SLOPE).toDouble()
                TAG_VERT_VELOCITY -> extensionBuilder.verticalVelocity = readString(parser, TAG_VERT_VELOCITY).toDouble()
                else -> skip(parser)
            }
        }
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TRACK_POINT_EXTENSIONS)
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
        val ele = java.lang.Double.valueOf(readText(parser))
        parser.require(XmlPullParser.END_TAG, namespace, TAG_ELEVATION)
        return ele
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTime(parser: XmlPullParser): Long {
        parser.require(XmlPullParser.START_TAG, namespace, TAG_TIME)
        val time: Time = Time.parse(readText(parser))
        parser.require(XmlPullParser.END_TAG, namespace, TAG_TIME)
        return time.date ?: 0
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
        val number = Integer.valueOf(readText(parser))
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
        private const val TAG_NAME = "name"
        private const val TAG_DESC = "desc"
        private val namespace: String? = null
    }
}