package de.drtobiasprinz.summitbook.models

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.gpx.GPXParser
import de.drtobiasprinz.gpx.Gpx
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.getDistance
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.setDistanceFromPoints
import de.drtobiasprinz.summitbook.ui.utils.SummitSlope
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.ColorMapping
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList
import org.osmdroid.views.overlay.milestones.*
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.NumberFormatException
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToLong


class GpsTrack(private val gpsTrackPath: Path, private val simplifiedGpsTrackPath: Path? = null) {
    var osMapRoute: Polyline? = null
    var isShownOnMap: Boolean = false
    var trackGeoPoints: MutableList<GeoPoint> = mutableListOf()
    private var usedTrackGeoPoints: MutableList<GeoPoint> = mutableListOf()
    var trackPoints: MutableList<TrackPoint> = mutableListOf()
    private var usedTrackPoints: MutableList<TrackPoint> = mutableListOf()
    var gpxTrack: Gpx? = null
    private var minForColorCoding = 0f
    private var maxForColorCoding = 0f

    fun addGpsTrack(mMapView: MapView?, selectedCustomizeTrackItem: TrackColor = TrackColor.None, color: Int = COLOR_POLYLINE_STATIC, rootView: View? = null) {
        try {
            osMapRoute = Polyline(mMapView)
            val textView: TextView? = rootView?.findViewById(R.id.track_value)
            val defaultText = "${trackPoints.size} ${rootView?.resources?.getString(R.string.pts)}"
            textView?.visibility = View.VISIBLE
            textView?.text = defaultText
            osMapRoute?.setOnClickListener { _, _, eventPos ->
                if (textView != null) {
                    textView.visibility = View.VISIBLE
                    val trackPoint = usedTrackPoints.minByOrNull { getDistance(it, TrackPoint(eventPos.latitude, eventPos.longitude)) }
                    if (trackPoint != null && (minForColorCoding != 0f || maxForColorCoding != 0f)) {
                        if (selectedCustomizeTrackItem == TrackColor.None) {
                            textView.visibility = View.GONE
                        } else {
                            setTextForDouble(trackPoint, textView, selectedCustomizeTrackItem, rootView.context)
                        }
                    } else {
                        textView.visibility = View.GONE
                    }
                }
                return@setOnClickListener true
            }
            osMapRoute?.outlinePaint?.color = color
            osMapRoute?.outlinePaint?.strokeWidth = LINE_WIDTH_BIG
            osMapRoute?.outlinePaint?.strokeCap = Paint.Cap.ROUND
            val paintBorder = Paint()
            paintBorder.strokeWidth = 20F
            val summitSlope = SummitSlope(trackPoints)
            when (selectedCustomizeTrackItem) {
                TrackColor.Mileage -> {
                    setUsedPoints()
                    val managers: MutableList<MilestoneManager> = ArrayList()
                    val slicerForPath = MilestoneMeterDistanceSliceLister()
                    managers.add(getAnimatedPathManager(slicerForPath))
                    managers.add(getHalfKilometerManager())
                    managers.add(getKilometerManager())
                    osMapRoute?.setMilestoneManagers(managers)
                }
                TrackColor.Elevation -> {
                    setUsedPoints(selectedCustomizeTrackItem.f)
                    addColorToTrack(paintBorder, selectedCustomizeTrackItem)
                }
                TrackColor.Slope -> {
                    if (trackPoints.none { it.extension?.slope != null }) {
                        summitSlope.calculateMaxSlope(50.0, requiredR2 = 0.0, factor = 100)
                    }
                    setUsedPoints(selectedCustomizeTrackItem.f)
                    addColorToTrack(paintBorder, selectedCustomizeTrackItem)
                }
                TrackColor.VerticalSpeed -> {
                    if (trackPoints.none { it.extension?.verticalVelocity != null }) {
                        summitSlope.calculateMaxVerticalVelocity()
                    }
                    setUsedPoints(selectedCustomizeTrackItem.f)
                    addColorToTrack(paintBorder, selectedCustomizeTrackItem)
                }
                TrackColor.None -> {
                    setUsedPoints()
                    Log.i(TAG, "Nothing to do, selectedCustomizeTrackItem is set to 0")
                }
                else -> {
                    setUsedPoints(selectedCustomizeTrackItem.f)
                    addColorToTrack(paintBorder, selectedCustomizeTrackItem)
                }
            }
            osMapRoute?.setPoints(usedTrackGeoPoints)
            mMapView?.overlayManager?.add(osMapRoute)
            isShownOnMap = true

        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private fun setUsedPoints(f: (TrackPoint) -> Double?) {
        usedTrackPoints = trackPoints.filter { f(it) != 0.0 } as MutableList<TrackPoint>
        usedTrackGeoPoints = usedTrackPoints.map { GeoPoint(it.lat, it.lon) } as MutableList<GeoPoint>
    }


    private fun setUsedPoints() {
        usedTrackPoints = trackPoints
        usedTrackGeoPoints = trackGeoPoints
    }


    private fun setTextForDouble(trackPoint: TrackPoint, textView: TextView, trackColor: TrackColor, context: Context) {
        val value = trackColor.f(trackPoint)
        if (value != null) {
            textView.text = String.format(Locale.US, "%.${trackColor.digits}f %s", value, trackColor.unit)
            val fraction = (value - minForColorCoding) / (maxForColorCoding - minForColorCoding)
            val rectangle = BitmapDrawable(context.resources, drawRectangle(interpolateColor(trackColor.minColor, trackColor.maxColor, fraction.toFloat())))
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(rectangle, null, null, null)
        } else {
            textView.visibility = View.GONE
        }
    }

    private fun drawRectangle(color: Int): Bitmap? {
        val radius = 25f
        val bitmap = Bitmap.createBitmap(
                (radius * 2).toInt(),
                (radius * 2).toInt(),
                Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            strokeWidth = 3f
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        paint.color = color
        canvas.drawRect(0f, 0f, 100f, 100f, paint)
        return bitmap
    }

    private fun addColorToTrack(paintBorder: Paint, trackColor: TrackColor) {
        val values = usedTrackPoints.mapNotNull(trackColor.f)
        minForColorCoding = (values.minOrNull() ?: 0.0).toFloat()
        maxForColorCoding = (values.maxOrNull() ?: 0.0).toFloat()
        val pointsExists = usedTrackPoints.any { trackColor.f(it) != 0.0 }
        if (pointsExists) {
            val attributeColorList = AttitudeColorList(usedTrackPoints, minForColorCoding, maxForColorCoding, trackColor.minColor, trackColor.maxColor, trackColor.f)
            osMapRoute?.outlinePaintLists?.add(PolychromaticPaintList(paintBorder, attributeColorList, false))
        }
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
        val arrowPath = Path()
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

    fun parseTrack(useSimplifiedIfExists: Boolean = true, loadFullTrackAsynchronous: Boolean = false) {
        val fileToUse = if (useSimplifiedIfExists && simplifiedGpsTrackPath != null && simplifiedGpsTrackPath.toFile().exists()) simplifiedGpsTrackPath.toFile() else gpsTrackPath.toFile()
        gpxTrack = getTrack(fileToUse)
        trackPoints = getTrackPoints(gpxTrack)
        trackGeoPoints = getGeoPoints(trackPoints)
        if (loadFullTrackAsynchronous && fileToUse != gpsTrackPath.toFile()) {
            AsyncLoadGpxTrack(gpsTrackPath.toFile(), this).execute()
        }
        if (trackPoints.size > 0 && trackPoints.first().extension?.distance == null) {
            setDistance()
        }
    }


    fun getTrackPositions(): List<TrackPoint?> {
        return trackPoints
    }

    fun setDistance() {
        setDistanceFromPoints(trackPoints)
    }


    fun getTrackGraph(f: (TrackPoint) -> Double?): MutableList<Entry> {
        if (trackPoints.isNotEmpty()) {
            if (trackPoints.first().extension?.distance == null) {
                setDistance()
            }
            val graph = mutableListOf<Entry>()
            for (trackPoint in trackPoints) {
                val value = f(trackPoint)?.toFloat()
                val distance = trackPoint.extension?.distance?.toFloat()
                if (value != null && distance != null) {
                    graph.add(Entry(distance, value, trackPoint))
                }
            }
            return graph
        } else {
            return mutableListOf()
        }
    }

    fun getTrackSlopeGraph(binSizeMeter: Double = 100.0): MutableList<Entry> {
        if (trackPoints.first().extension?.distance == null) {
            setDistance()
        }
        val summitSlope = SummitSlope(trackPoints)
        summitSlope.calculateMaxSlope(binSizeMeter, requiredR2 = 0.0)
        return summitSlope.slopeGraph
    }

    fun getHighestElevation(): TrackPoint? {
        return trackPoints.maxByOrNull { it.ele ?: 0.0 }
    }

    fun hasNoTrackPoints(): Boolean {
        return trackPoints.isNullOrEmpty()
    }

    fun hasOnlyZeroCoordinates(): Boolean {
        return trackPoints.none { it.lat != 0.0 && it.lon != 0.0 }
    }


    companion object {

        private const val COLOR_POLYLINE_STATIC = Color.BLUE
        private const val COLOR_POLYLINE_ANIMATED = Color.GREEN
        private const val COLOR_BACKGROUND = Color.WHITE
        private const val LINE_WIDTH_BIG = 12f
        private const val TEXT_SIZE = 20f
        private const val TAG = "GpsTrack"


        private fun getTrackPoints(gpxTrackLocal: Gpx?): MutableList<TrackPoint> {
            val trackPoints = mutableListOf<TrackPoint>()
            if (gpxTrackLocal != null) {
                val tracks = gpxTrackLocal.tracks
                for (track in tracks.toList().blockingGet()) {
                    for (segment in track.segments.toList().blockingGet()) {
                        val points = segment.points.toList().blockingGet()
                        trackPoints.addAll(points.filter { it.lat != 0.0 && it.lon != 0.0 })
                    }
                }
            }
            return trackPoints
        }

        private fun getGeoPoints(trackPoints: List<TrackPoint>): MutableList<GeoPoint> {
            return trackPoints.map { GeoPoint(it.lat, it.lon) } as MutableList<GeoPoint>
        }

        private fun getTrack(fileToUse: File): Gpx? {
            val mParser = GPXParser()
            try {
                val inputStream: InputStream = FileInputStream(fileToUse)
                return mParser.parse(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            } catch (e: ArrayIndexOutOfBoundsException) {
                e.printStackTrace()
            }
            return null
        }


        internal class AsyncLoadGpxTrack(private val fileToUse: File, private val gpsTrack: GpsTrack) : AsyncTask<Uri, Int?, Void?>() {
            private var trackGeoPoints: MutableList<GeoPoint> = mutableListOf()
            private var trackPoints: MutableList<TrackPoint> = mutableListOf()
            private var track: Gpx? = null

            override fun doInBackground(vararg uri: Uri): Void? {
                track = getTrack(fileToUse)
                trackPoints = getTrackPoints(track)
//              TODO: fix stackOverflow Exception
//                val summitSlope = SummitSlope(trackPoints)
//                summitSlope.calculateMaxSlope(50.0, requiredR2 = 0.0, factor = 100)
//                summitSlope.calculateMaxVerticalVelocity()
                trackGeoPoints = getGeoPoints(trackPoints)
                return null
            }

            override fun onPostExecute(param: Void?) {
                gpsTrack.gpxTrack = track
                gpsTrack.trackGeoPoints = trackGeoPoints
                gpsTrack.trackPoints = trackPoints
                if (gpsTrack.trackPoints.size > 0 && gpsTrack.trackPoints.first().extension?.distance == null) {
                    gpsTrack.setDistance()
                }
                Log.i("AsyncLoadGpxTrack", "Successfully loaded gpx track asynchronous.")
            }
        }

        private fun interpolate(a: Float, b: Float, proportion: Float): Float {
            return a + (b - a) * proportion
        }

        fun interpolateColor(a: Int, b: Int, proportion: Float): Int {
            val hsva = FloatArray(3)
            val hsvb = FloatArray(3)
            Color.colorToHSV(a, hsva)
            Color.colorToHSV(b, hsvb)
            for (i in 0..2) {
                hsvb[i] = interpolate(hsva[i], hsvb[i], proportion)
            }
            return Color.HSVToColor(hsvb)
        }

    }


    internal class AttitudeColorList(private val points: List<TrackPoint>, private val trackMin: Float, private val trackMax: Float, private val startColor: Int, private val endColor: Int, val f: (TrackPoint) -> Double?) : ColorMapping {
        override fun getColorForIndex(pSegmentIndex: Int): Int {
            return if (pSegmentIndex < points.size) {
                val value = f(points[pSegmentIndex])
                val fraction = ((value ?: 0.0) - trackMin) / (trackMax - trackMin)
                interpolateColor(startColor, endColor, fraction.toFloat())
            } else {
                Color.BLACK
            }
        }
    }


}