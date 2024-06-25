package de.drtobiasprinz.summitbook.models

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.gpx.GPXParser
import de.drtobiasprinz.gpx.Gpx
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.getDistance
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.setDistanceFromPoints
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.ColorMapping
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList
import org.osmdroid.views.overlay.milestones.MilestoneDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneLineDisplayer
import org.osmdroid.views.overlay.milestones.MilestoneLister
import org.osmdroid.views.overlay.milestones.MilestoneManager
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceLister
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceSliceLister
import org.osmdroid.views.overlay.milestones.MilestonePathDisplayer
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.system.measureTimeMillis


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

    fun addGpsTrack(
        mMapView: MapView?, selectedCustomizeTrackItem: TrackColor = TrackColor.None,
        color: Int = COLOR_POLYLINE_STATIC, rootView: View? = null, summit: Summit? = null
    ) {
        try {
            osMapRoute = Polyline(mMapView)
            val textView: TextView? = rootView?.findViewById(R.id.track_value)
            val defaultText = "${trackPoints.size} ${rootView?.resources?.getString(R.string.pts)}"
            textView?.visibility = View.VISIBLE
            textView?.text = defaultText
            osMapRoute?.setOnClickListener { _, _, eventPos ->
                if (textView != null) {
                    textView.visibility = View.VISIBLE
                    val trackPoint = usedTrackPoints.minByOrNull {
                        getDistance(
                            it,
                            TrackPoint.Builder().setLatitude(eventPos.latitude).setLongitude(eventPos.longitude).build() as TrackPoint
                        )
                    }
                    if (trackPoint != null && (minForColorCoding != 0f || maxForColorCoding != 0f)) {
                        if (selectedCustomizeTrackItem == TrackColor.None) {
                            textView.visibility = View.GONE
                        } else {
                            setTextForDouble(
                                trackPoint,
                                textView,
                                selectedCustomizeTrackItem,
                                rootView.context
                            )
                        }
                    } else {
                        textView.visibility = View.GONE
                    }
                } else if (mMapView != null && summit != null) {
                    Toast.makeText(
                        mMapView.context, "${summit.getDateAsString()} ${summit.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@setOnClickListener true
            }
            osMapRoute?.outlinePaint?.color = color
            osMapRoute?.outlinePaint?.strokeWidth = LINE_WIDTH_BIG
            osMapRoute?.outlinePaint?.strokeCap = Paint.Cap.ROUND
            val paintBorder = Paint()
            paintBorder.strokeWidth = 20F
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
        usedTrackPoints = trackPoints.filter { f(it) != null } as MutableList<TrackPoint>
        usedTrackGeoPoints =
            usedTrackPoints.map { GeoPoint(it.latitude, it.longitude) } as MutableList<GeoPoint>
    }


    private fun setUsedPoints() {
        usedTrackPoints = trackPoints
        usedTrackGeoPoints = trackGeoPoints
    }


    private fun setTextForDouble(
        trackPoint: TrackPoint,
        textView: TextView,
        trackColor: TrackColor,
        context: Context
    ) {
        val value = trackColor.f(trackPoint)
        if (value != null) {
            textView.text =
                String.format(Locale.US, "%.${trackColor.digits}f %s", value, trackColor.unit)
            val fraction = (value - minForColorCoding) / (maxForColorCoding - minForColorCoding)
            val rectangle = BitmapDrawable(
                context.resources,
                drawRectangle(
                    interpolateColor(
                        trackColor.minColor,
                        trackColor.maxColor,
                        fraction.toFloat()
                    )
                )
            )
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
            val attributeColorList = AttitudeColorList(
                usedTrackPoints,
                minForColorCoding,
                maxForColorCoding,
                trackColor.minColor,
                trackColor.maxColor,
                trackColor.f
            )
            osMapRoute?.outlinePaintLists?.add(
                PolychromaticPaintList(
                    paintBorder,
                    attributeColorList,
                    false
                )
            )
        }
    }

    private fun getFillPaint(): Paint {
        val paint = Paint()
        paint.color = COLOR_BACKGROUND
        paint.style = Paint.Style.FILL_AND_STROKE
        return paint
    }

    private fun getTextPaint(): Paint {
        val paint = Paint()
        paint.color = COLOR_POLYLINE_STATIC
        paint.textSize = TEXT_SIZE
        paint.isAntiAlias = true
        return paint
    }

    private fun getKilometerManager(): MilestoneManager {
        val backgroundRadius = 30f
        val backgroundPaint1 = getFillPaint()
        val textPaint1: Paint = getTextPaint()
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
                    pCanvas.drawText(
                        text,
                        -rect.left - rect.width() / 2f,
                        rect.height() / 2f - rect.bottom,
                        textPaint1
                    )
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
        val backgroundPaint = getFillPaint()
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

    fun parseTrack(useSimplifiedIfExists: Boolean = true, deleteEmptyTrack: Boolean = false) {
        val fileToUse =
            if (useSimplifiedIfExists && simplifiedGpsTrackPath != null && simplifiedGpsTrackPath.toFile()
                    .exists()
            ) simplifiedGpsTrackPath.toFile() else gpsTrackPath.toFile()
        val time = measureTimeMillis {
            gpxTrack = getTrack(fileToUse, gpsTrackPath.toFile())
        }
        trackPoints = getTrackPoints(gpxTrack)
        Log.e("GpxTrack", "Took $time")
//        Log.e("GpxTrack", "2nd Took ${
//            measureTimeMillis {
//                val mParser = GPXParser2()
//                val inputStream: InputStream = FileInputStream(fileToUse)
//                mParser.parse(inputStream)
//            }
//        }")
        trackGeoPoints = getGeoPoints(trackPoints)
        if (trackPoints.size > 0 && trackPoints.first().pointExtension?.distance == null) {
            setDistance()
        }
        if (trackPoints.isEmpty() && deleteEmptyTrack) {
            fileToUse.delete()
        }
    }


    fun getTrackPositions(): List<TrackPoint?> {
        return trackPoints
    }

    fun setDistance() {
        setDistanceFromPoints(trackPoints)
    }


    fun getTrackGraph(f: (TrackPoint) -> Double?): MutableList<Entry> {
        if (trackPoints.firstOrNull()?.pointExtension?.distance == null) {
            setDistance()
        }
        val filterTrackPoints = trackPoints
        return if (filterTrackPoints.isNotEmpty()) {
            val graph = mutableListOf<Entry>()
            for (trackPoint in filterTrackPoints) {
                val value = f(trackPoint)?.toFloat()
                val distance = trackPoint.pointExtension?.distance?.toFloat()
                if (value != null && distance != null) {
                    graph.add(Entry(distance, value, trackPoint))
                }
            }
            graph
        } else {
            mutableListOf()
        }
    }

    fun getTrackSlopeGraph(): MutableList<Entry> {
        if (trackPoints.first().pointExtension?.distance == null) {
            setDistance()
        }
        //TODO
        return mutableListOf()
    }

    fun getHighestElevation(): TrackPoint? {
        return trackPoints.maxByOrNull { it.elevation ?: 0.0 }
    }

    fun hasNoTrackPoints(): Boolean {
        return trackPoints.isEmpty()
    }

    fun hasOnlyZeroCoordinates(): Boolean {
        return trackPoints.none { it.latitude != 0.0 && it.longitude != 0.0 }
    }


    companion object {

        private const val COLOR_POLYLINE_STATIC = Color.BLUE
        private const val COLOR_POLYLINE_ANIMATED = Color.GREEN
        private const val COLOR_BACKGROUND = Color.WHITE
        const val LINE_WIDTH_BIG = 12f
        private const val TEXT_SIZE = 20f
        private const val TAG = "GpsTrack"


        private fun getTrackPoints(gpxTrackLocal: Gpx?): MutableList<TrackPoint> {
            val trackPoints = mutableListOf<TrackPoint>()
            if (gpxTrackLocal != null) {
                val tracks = gpxTrackLocal.tracks
                for (track in tracks) {
                    for (segment in track.trackSegments) {
                        val points = segment.trackPoints
                        trackPoints.addAll(points.filter { it.latitude != 0.0 && it.longitude != 0.0 })
                    }
                }
            }
            return trackPoints
        }

        private fun getGeoPoints(trackPoints: List<TrackPoint>): MutableList<GeoPoint> {
            return trackPoints.map {
                GeoPoint(
                    it.latitude,
                    it.longitude,
                    it.elevation ?: 0.0
                )
            } as MutableList<GeoPoint>
        }

        private fun getTrack(fileToUse: File, fileInCaseOfException: File? = null): Gpx? {
            val mParser = GPXParser()
            try {
                val inputStream: InputStream = FileInputStream(fileToUse)
                return mParser.parse(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            } catch (e: XmlPullParserException) {
                if (fileInCaseOfException != null && fileInCaseOfException != fileToUse) {
                    return getTrack(fileInCaseOfException)
                } else {
                    e.printStackTrace()
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                e.printStackTrace()
            }
            return null
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


    internal class AttitudeColorList(
        private val points: List<TrackPoint>,
        private val trackMin: Float,
        private val trackMax: Float,
        private val startColor: Int,
        private val endColor: Int,
        val f: (TrackPoint) -> Double?
    ) : ColorMapping {
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