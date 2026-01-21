package de.drtobiasprinz.summitbook.models

import android.graphics.*
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils.Companion.getDistance
import io.ticofab.androidgpxparser.parser.GPXParser
import io.ticofab.androidgpxparser.parser.domain.Gpx
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Yaml
import net.mamoe.yamlkt.YamlDecodingException
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
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.system.measureTimeMillis


class GpsTrack(
    private val gpsTrackPath: Path,
    private val simplifiedGpsTrackPath: Path? = null,
    private val yamlExtensionsFile: File? = null
) {
    var osMapRoute: Polyline? = null
    var isShownOnMap: Boolean = false
    private var _trackGeoPoints: MutableList<GeoPoint>? = null
    val trackGeoPoints: MutableList<GeoPoint>
        get() {
            if (_trackGeoPoints == null) {
                _trackGeoPoints = calculateGeoPoints()
            }
            return _trackGeoPoints!!
        }
    private var usedTrackGeoPoints: MutableList<GeoPoint> = mutableListOf()
    var trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>> = mutableListOf()
    private var usedTrackPoints: MutableList<Pair<TrackPoint, ExtensionFromYaml>> = mutableListOf()
    var gpxTrack: Gpx? = null
    private var minForColorCoding = 0f
    private var maxForColorCoding = 0f

    fun addGpsTrack(
        mMapView: MapView?,
        selectedCustomizeTrackItem: TrackColor = TrackColor.None,
        color: Int = COLOR_POLYLINE_STATIC,
        summit: Summit? = null
    ) {
        try {
            if (osMapRoute != null) {
                mMapView?.overlays?.remove(osMapRoute)
            }
            osMapRoute = Polyline(mMapView)

            osMapRoute?.setOnClickListener { _, _, eventPos ->
                if (mMapView != null && summit != null) {
                    Toast.makeText(
                        mMapView.context,
                        "${summit.getDateAsString()} ${summit.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@setOnClickListener true
            }
            osMapRoute?.outlinePaint?.color = color
            osMapRoute?.outlinePaint?.strokeWidth = LINE_WIDTH_BIG
            osMapRoute?.outlinePaint?.strokeCap = Paint.Cap.ROUND
            val paintBorder = Paint()
            paintBorder.strokeWidth = 20f
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

    private fun setUsedPoints(f: (Pair<TrackPoint, ExtensionFromYaml>) -> Double?) {
        usedTrackPoints = trackPoints.filter { f(it) != null }.toMutableList()
        usedTrackGeoPoints = usedTrackPoints.map {
            GeoPoint(
                it.first.latitude, it.first.longitude
            )
        } as MutableList<GeoPoint>
    }


    private fun setUsedPoints() {
        usedTrackPoints = trackPoints.toMutableList()
        usedTrackGeoPoints = trackGeoPoints
    }


    private fun addColorToTrack(paintBorder: Paint, trackColor: TrackColor) {
        val values = usedTrackPoints.mapNotNull(trackColor.f)
        minForColorCoding = (values.minOrNull() ?: 0.0).toFloat()
        maxForColorCoding = (values.maxOrNull() ?: 0.0).toFloat()
        val pointsExists = usedTrackPoints.any { trackColor.f(it) != 0.0 }
        if (pointsExists) {
            val attributeColorList = if (trackColor.discreteInput) {
                AttitudeColorListDiscrete(
                    usedTrackPoints,
                    trackColor
                )
            } else {
                AttitudeColorListContinuos(
                    usedTrackPoints,
                    minForColorCoding,
                    maxForColorCoding,
                    trackColor.minColor,
                    trackColor.maxColor,
                    trackColor.f
                )
            }
            osMapRoute?.outlinePaintLists?.add(
                PolychromaticPaintList(
                    paintBorder, attributeColorList, false
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
            MilestoneMeterDistanceLister(1000.0), object : MilestoneDisplayer(0.0, false) {
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
            })
    }

    private fun getHalfKilometerManager(): MilestoneManager {
        val arrowPath = Path()
        arrowPath.moveTo(-5f, -5f)
        arrowPath.lineTo(5f, 0f)
        arrowPath.lineTo(-5f, 5f)
        arrowPath.close()
        val backgroundPaint = getFillPaint()
        return MilestoneManager(
            MilestoneMeterDistanceLister(500.0),
            object : MilestonePathDisplayer(0.0, true, arrowPath, backgroundPaint) {
                override fun draw(pCanvas: Canvas, pParameter: Any) {
                    val halfKilometers = (pParameter as Double / 500).roundToLong().toInt()
                    if (halfKilometers % 2 == 0) {
                        return
                    }
                    super.draw(pCanvas, pParameter)
                }
            })
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
        var useOriginalTrack = false
        val fileToUse =
            if (useSimplifiedIfExists && simplifiedGpsTrackPath != null && simplifiedGpsTrackPath.toFile()
                    .exists()
            ) {
                simplifiedGpsTrackPath.toFile()
            } else {
                useOriginalTrack = true
                gpsTrackPath.toFile()
            }
        val time = measureTimeMillis {
            gpxTrack = getTrack(fileToUse, gpsTrackPath.toFile())
        }
        trackPoints = calculateTrackPoints(useOriginalTrack)
        Log.i("GpxTrack", "Parsing took $time")

        val isDistancesIncorrect = trackPoints.isEmpty() || run {
            for (i in 1 until trackPoints.size) {
                if ((trackPoints[i - 1].second.distance ?: 0.0) > (trackPoints[i].second.distance
                        ?: 0.0)
                ) {
                    return@run true
                }
            }
            false
        }
        if (trackPoints.isNotEmpty() && (trackPoints.first().second.distance == null || isDistancesIncorrect)) {
            setDistance()
        }
        if (trackPoints.isEmpty() && deleteEmptyTrack) {
            fileToUse.delete()
        }
    }

    private fun calculateTrackPoints(useOriginalTrack: Boolean): List<Pair<TrackPoint, ExtensionFromYaml>> {
        val tracks = gpxTrack?.tracks ?: return emptyList()

        // Pre-calculate size to avoid list resizing
        val estimatedSize = tracks.sumOf { track ->
            track.trackSegments.sumOf { it.trackPoints.size }
        }
        val trackPoints = ArrayList<TrackPoint>(estimatedSize)

        for (track in tracks) {
            for (segment in track.trackSegments) {
                trackPoints.addAll(segment.trackPoints.filter {
                    it.latitude != 0.0 && it.longitude != 0.0
                })
            }
        }

        if (useOriginalTrack) {
            val extensions = getExtensionFromYaml()
            if (extensions.size == trackPoints.size) {
                return List(trackPoints.size) { index ->
                    Pair(trackPoints[index], extensions[index])
                }
            }
        }

        return List(trackPoints.size) { index ->
            Pair(trackPoints[index], ExtensionFromYaml())
        }
    }

    private fun calculateGeoPoints(): MutableList<GeoPoint> {
        return trackPoints.map {
            GeoPoint(
                it.first.latitude, it.first.longitude, it.first.elevation ?: 0.0
            )
        } as MutableList<GeoPoint>
    }

    private fun getExtensionFromYaml(): List<ExtensionFromYaml> {
        val yamlDefault = Yaml.Default
        var pointExtensionFromYaml = ExtensionsFromYaml()
        if (yamlExtensionsFile?.exists() == true) {
            try {
                val timeYaml = measureTimeMillis {
                    yamlExtensionsFile.inputStream().bufferedReader().use { reader ->
                        pointExtensionFromYaml = yamlDefault.decodeFromString(
                            ExtensionsFromYaml.serializer(), reader.readText()
                        )
                    }
                }
                Log.i(
                    "YAML",
                    "Successful loaded in $timeYaml ms with ${pointExtensionFromYaml.extensions.size} points."
                )
            } catch (ex: YamlDecodingException) {
                Log.w(
                    "YAML",
                    "Could not load $yamlExtensionsFile because an error occurred: ${ex.message}"
                )
            }

        }
        return pointExtensionFromYaml.extensions
    }


    fun getTrackPositions(): List<TrackPoint?> {
        return trackPoints.map { it.first }
    }

    fun setDistance() {
        trackPoints.forEachIndexed { i, trackPoint ->
            if (i == 0) {
                trackPoint.second.distance = 0.0
            } else {
                val distance = trackPoints[i - 1].second.distance
                if (distance != null) {
                    trackPoint.second.distance = distance + abs(
                        getDistance(
                            trackPoint.first, trackPoints[i - 1].first
                        )
                    ).toDouble()
                }
            }
        }
    }


    fun getTrackGraph(
        f: (Pair<TrackPoint, ExtensionFromYaml>) -> Double?,
        discreteInput: Boolean = false
    ): MutableList<Entry> {
        if (trackPoints.lastOrNull()?.second?.distance == 0.0) {
            setDistance()
        }
        val filterTrackPoints = trackPoints
        return if (filterTrackPoints.isNotEmpty()) {
            val graph = mutableListOf<Entry>()
            for (trackPoint in filterTrackPoints) {
                val value = if (discreteInput) 1f else f(trackPoint)?.toFloat()
                val distance = trackPoint.second.distance?.toFloat()
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
        if (trackPoints.first().second.distance == null) {
            setDistance()
        }
        //TODO
        return mutableListOf()
    }

    fun getHighestElevation(): GeoPoint? {
        val trackPoint = trackPoints.maxByOrNull { it.first.elevation ?: 0.0 }
        return if (trackPoint != null) {
            GeoPoint(trackPoint.first.latitude, trackPoint.first.longitude)
        } else {
            null
        }
    }

    fun hasNoTrackPoints(): Boolean {
        return trackPoints.isEmpty()
    }

    fun hasOnlyZeroCoordinates(): Boolean {
        return trackPoints.none { it.first.latitude != 0.0 && it.first.longitude != 0.0 }
    }


    companion object {

        private const val COLOR_POLYLINE_STATIC = Color.BLUE
        private const val COLOR_POLYLINE_ANIMATED = Color.GREEN
        private const val COLOR_BACKGROUND = Color.WHITE
        const val LINE_WIDTH_BIG = 16f
        private const val TEXT_SIZE = 20f
        private const val TAG = "GpsTrack"

        // LRU cache for parsed GPX tracks (cache last 10 tracks)
        private val trackCache = LruCache<String, Gpx>(10)

        private fun getTrack(fileToUse: File, fileInCaseOfException: File? = null): Gpx? {
            // Check cache first
            val cacheKey = fileToUse.absolutePath
            trackCache.get(cacheKey)?.let { return it }

            val mParser = GPXParser()
            try {
                BufferedInputStream(FileInputStream(fileToUse)).use { inputStream ->
                    val gpx = mParser.parse(inputStream)
                    gpx?.let { trackCache.put(cacheKey, it) }
                    return gpx
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: org.joda.time.IllegalFieldValueException) {
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
            } catch (e: RuntimeException) {
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


    internal class AttitudeColorListContinuos(
        private val points: List<Pair<TrackPoint, ExtensionFromYaml>>,
        private val trackMin: Float,
        private val trackMax: Float,
        private val startColor: Int,
        private val endColor: Int,
        val f: (Pair<TrackPoint, ExtensionFromYaml>) -> Double?
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

    internal class AttitudeColorListDiscrete(
        private val points: List<Pair<TrackPoint, ExtensionFromYaml>>,
        private val trackColor: TrackColor
    ) : ColorMapping {

        // For checkered pattern: alternate between primary color and a contrasting color
        private val checkeredAlternateColor = Color.BLACK

        override fun getColorForIndex(pSegmentIndex: Int): Int {
            return if (pSegmentIndex < points.size) {
                val baseColor: Int
                val useCheckered: Boolean

                if (trackColor == TrackColor.RoadType) {
                    val roadType = points[pSegmentIndex].second.roadType
                    baseColor = roadType.color
                    useCheckered = roadType.useCheckeredPattern
                } else {
                    baseColor = points[pSegmentIndex].second.surface.color
                    useCheckered = false
                }

                // Apply checkered pattern by alternating colors every few segments
                if (useCheckered) {
                    // Create checkered effect by alternating between base color and black
                    // Use groups of 3 segments to create visible checker pattern
                    val groupIndex = pSegmentIndex
                    if (groupIndex % 2 == 0) baseColor else checkeredAlternateColor
                } else {
                    baseColor
                }
            } else {
                Color.BLACK
            }
        }
    }


}

@Serializable
data class ExtensionsFromYaml(
    val extensions: List<ExtensionFromYaml> = emptyList()
)

@Serializable
data class ExtensionFromYaml(
    var distance: Double? = 0.0,
    val hr: Int? = 0,
    val power: Int? = 0,
    val power60s: Int? = 0,
    val cadence: Int? = 0,
    val slope: Double? = 0.0,
    val speed: Double? = 0.0,
    val verticalVelocity: Double? = 0.0,
    var surface: Surface = Surface.UNKNOWN,
    var roadType: RoadType = RoadType.UNKNOWN
)
