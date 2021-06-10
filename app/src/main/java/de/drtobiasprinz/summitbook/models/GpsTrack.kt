package de.drtobiasprinz.summitbook.models

import android.graphics.*
import android.location.Location
import com.github.mikephil.charting.data.Entry
import com.google.android.gms.maps.model.LatLng
import de.drtobiasprinz.gpx.GPXParser
import de.drtobiasprinz.gpx.Gpx
import de.drtobiasprinz.gpx.TrackPoint
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.milestones.*
import org.xmlpull.v1.XmlPullParserException
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToLong


class GpsTrack(private val gpsTrackPath: Path) {
    var osMapRoute: Polyline? = null
    var isShownOnMap: Boolean = false
    val trackGeoPoints: ArrayList<GeoPoint?> = ArrayList()
    val trackPoints: ArrayList<TrackPoint?> = ArrayList()
    private val COLOR_POLYLINE_STATIC = Color.BLUE
    private val COLOR_POLYLINE_ANIMATED = Color.GREEN
    private val COLOR_BACKGROUND = Color.WHITE
    private val LINE_WIDTH_BIG = 12f
    private val TEXT_SIZE = 20f

    fun addGpsTrack(mMapView: MapView?, isMileageShown: Boolean = false, color: Int = COLOR_POLYLINE_STATIC) {
        osMapRoute = Polyline(mMapView)
        osMapRoute?.setOnClickListener { _, _, _ ->
            true // DO NOTHING
        }
        osMapRoute?.outlinePaint?.color = color
        osMapRoute?.outlinePaint?.strokeWidth = LINE_WIDTH_BIG
        osMapRoute?.setPoints(trackGeoPoints)
        osMapRoute?.outlinePaint?.strokeCap = Paint.Cap.ROUND
        if (isMileageShown) {
            val managers: MutableList<MilestoneManager> = ArrayList()
            val slicerForPath = MilestoneMeterDistanceSliceLister()
            managers.add(getAnimatedPathManager(slicerForPath))
            managers.add(getHalfKilometerManager())
            managers.add(getKilometerManager())
            osMapRoute?.setMilestoneManagers(managers)
        }
        mMapView?.overlayManager?.add(osMapRoute)
        isShownOnMap = true
    }

    private fun getFillPaint(pColor: Int): Paint {
        val paint = Paint()
        paint.color = pColor
        paint.style = Paint.Style.FILL_AND_STROKE
        return paint
    }

    private fun getTextPaint(pColor: Int): Paint {
        val paint = Paint()
        paint.color = pColor
        paint.textSize = TEXT_SIZE
        paint.isAntiAlias = true
        return paint
    }

    private fun getKilometerManager(): MilestoneManager {
        val backgroundRadius = 30f
        val backgroundPaint1 = getFillPaint(COLOR_BACKGROUND)
        val textPaint1: Paint = getTextPaint(COLOR_POLYLINE_STATIC)
        val borderPaint = getStrokePaint(COLOR_BACKGROUND, 2f)
        return MilestoneManager(
                MilestoneMeterDistanceLister(1000.0),
                object : MilestoneDisplayer(0.0, false) {
                    override fun draw(pCanvas: Canvas, pParameter: Any) {
                        val meters = pParameter as Double
                        val kilometers = (meters / 1000).roundToLong().toInt()
                        textPaint1.textSize = 30f
                        val text = "" + kilometers + "K"
                        val rect = Rect()
                        textPaint1.getTextBounds(text, 0, text.length, rect)
                        pCanvas.drawCircle(0f, 0f, backgroundRadius, backgroundPaint1)
                        pCanvas.drawText(text, -rect.left - rect.width() / 2f, rect.height() / 2f - rect.bottom, textPaint1)
                        pCanvas.drawCircle(0f, 0f, backgroundRadius + 1, borderPaint)
                    }
                }
        )
    }

    private fun getHalfKilometerManager(): MilestoneManager {
        val arrowPath = Path() // a simple arrow towards the right
        arrowPath.moveTo(-5f, -5f)
        arrowPath.lineTo(5f, 0f)
        arrowPath.lineTo(-5f, 5f)
        arrowPath.close()
        val backgroundPaint = getFillPaint(COLOR_BACKGROUND)
        return MilestoneManager( // display an arrow at 500m every 1km
                MilestoneMeterDistanceLister(500.0),
                object : MilestonePathDisplayer(0.0, true, arrowPath, backgroundPaint) {
                    override fun draw(pCanvas: Canvas, pParameter: Any) {
                        val halfKilometers = (pParameter as Double / 500).roundToLong().toInt()
                        if (halfKilometers % 2 == 0) {
                            return
                        }
                        super.draw(pCanvas, pParameter)
                    }
                }
        )
    }

    private fun getStrokePaint(pColor: Int, pWidth: Float): Paint {
        val paint = Paint()
        paint.strokeWidth = pWidth
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        paint.color = pColor
        paint.strokeCap = Paint.Cap.ROUND
        return paint
    }

    private fun getAnimatedPathManager(pMilestoneLister: MilestoneLister): MilestoneManager {
        val slicePaint = getStrokePaint(COLOR_POLYLINE_ANIMATED, LINE_WIDTH_BIG)
        return MilestoneManager(pMilestoneLister, MilestoneLineDisplayer(slicePaint))
    }

    fun parseTrack() {
        val mParser = GPXParser()
        var parsedGpx: Gpx? = null
        try {
            val inputStream: InputStream = FileInputStream(gpsTrackPath.toFile())
            parsedGpx = mParser.parse(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        if (parsedGpx != null) {
            val tracks = parsedGpx.tracks
            for (track in tracks.toList().blockingGet()) {
                for (segment in track.segments.toList().blockingGet()) {
                    val points = segment.points.toList().blockingGet()
                    trackPoints.addAll(points)
                    for (point in points) {
                        if (point.lat != 0.0 && point.lon != 0.0) {
                            trackGeoPoints.add(GeoPoint(point.lat, point.lon))
                        }
                    }
                }
            }
        }
    }

    fun getTrackPositions(): ArrayList<LatLng?> {
        val positions = ArrayList<LatLng?>()
        for (trackPoint in trackPoints) {
            if (trackPoint != null) {
                positions.add(LatLng(trackPoint.lat, trackPoint.lon))
            }
        }
        return positions
    }
    fun getTrackGraph(f: (TrackPoint) -> Float?): MutableList<Entry> {
        val graph = mutableListOf<Entry>()
        var lastTrackPoint: TrackPoint? = null
        var cumulativeDistance = 0f
        for (trackPoint in trackPoints) {
            if (trackPoint != null) {
                val value = f(trackPoint)
                val distance = trackPoint.extension?.distance?.toFloat()
                if (value != null) {
                    if (lastTrackPoint == null) {
                        lastTrackPoint = trackPoint
                    }
                    if (distance != null) {
                        cumulativeDistance = distance
                    } else {
                        cumulativeDistance += abs(getDistance(trackPoint, lastTrackPoint))
                    }
                    lastTrackPoint = trackPoint
                    graph.add(Entry(cumulativeDistance, value, trackPoint))
                }
            }
        }
        return graph
    }

    fun getHighestElevation(): TrackPoint? {
        if (trackPoints.isNotEmpty()) {
            var highestPoint = trackPoints[0]
            for (trackPoint in trackPoints) {
                if (trackPoint != null && highestPoint != null && trackPoint.ele ?: 0.0 > highestPoint.ele ?: 0.0) {
                    highestPoint = trackPoint
                }
            }
            return highestPoint
        } else {
            return null
        }
    }

    fun hasNoTrackPoints(): Boolean {
        return trackPoints.size <= 0
    }

    fun hasOnlyZeroCoordinates(): Boolean {
        return trackPoints.none { it?.lat != 0.0 && it?.lon != 0.0 }
    }

    private fun getDistance(trackPoint1: TrackPoint, trackPoint2: TrackPoint): Float {
        val location1 = getLocationFromFrackPoint(trackPoint1)
        val location2 = getLocationFromFrackPoint(trackPoint2)
        return location1.distanceTo(location2)
    }

    private fun getLocationFromFrackPoint(trackPoint: TrackPoint): Location {
        val location = Location(trackPoint.name)
        location.latitude = trackPoint.lat
        location.longitude = trackPoint.lon
        location.altitude = trackPoint.ele ?: 0.0
        return location
    }

}