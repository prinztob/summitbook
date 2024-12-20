package de.drtobiasprinz.summitbook

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.databinding.FragmentSegmentEntryDetailsBinding
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.Segment.Companion.getMapScreenshotFile
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.AddSegmentEntryFragment
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.getSportTypeForMapProviders
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.TileStates
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong


@AndroidEntryPoint
class SegmentEntryDetailsFragment : Fragment() {
    private val viewModel: DatabaseViewModel by viewModels()

    var segmentDetailsId: Long = -1L
    private var segmentEntryId: Long = -1L
    private var descending: Boolean = false
    private lateinit var binding: FragmentSegmentEntryDetailsBinding

    private var summitShown: Summit? = null
    private var segmentEntryToShow: SegmentEntry? = null
    private var selectedCustomizeTrackItem = TrackColor.Elevation

    private var setSortLabelFor: Int = 21
    private var sorter: (List<SegmentEntry>) -> List<SegmentEntry> =
        { e -> if (descending) e.sortedByDescending { it.duration } else e.sortedBy { it.duration } }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        binding = FragmentSegmentEntryDetailsBinding.inflate(layoutInflater)

        if (segmentDetailsId == -1L && savedInstanceState != null) {
            segmentDetailsId =
                savedInstanceState.getLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER)
            segmentEntryId =
                savedInstanceState.getLong(SegmentEntry.SEGMENT_ENTRY_ID_EXTRA_IDENTIFIER)
        }

        viewModel.segmentsList.observe(viewLifecycleOwner) { itDataSegments ->
            itDataSegments.data.let { segments ->
                val segmentToUse =
                    segments?.firstOrNull { it.segmentDetails.segmentDetailsId == segmentDetailsId }
                if (segmentEntryId == -1L) {
                    segmentEntryId =
                        segmentToUse?.segmentEntries?.let { sorter(it).firstOrNull() }?.entryId
                            ?: -1
                }
                segmentEntryToShow = if (segmentEntryId == -1L) {
                        segmentToUse?.segmentEntries?.let { sorter(it).firstOrNull() }
                    } else {
                        segmentToUse?.segmentEntries?.firstOrNull { it.entryId == segmentEntryId }
                    }
                viewModel.summitsList.observe(viewLifecycleOwner) { itDataSummits ->
                    itDataSummits.data.let { summits ->
                        val relevantSummits = segmentToUse?.segmentEntries?.mapNotNull { entry ->
                            summits?.firstOrNull { it.activityId == entry.activityId }
                        }
                        summitShown =
                            relevantSummits?.firstOrNull { it.activityId == segmentEntryToShow?.activityId }
                        if (relevantSummits != null && summitShown != null) {
                            binding.osMap.visibility = View.VISIBLE
                            binding.lineChart.visibility = View.VISIBLE
                            binding.tableSegments.visibility = View.VISIBLE
                            binding.gridLayout.visibility = View.VISIBLE
                            binding.changeMapType.visibility = View.VISIBLE
                            prepareMap()
                            update(summitShown, segmentEntryToShow, segmentToUse, relevantSummits)
                        } else {
                            binding.osMap.visibility = View.GONE
                            binding.lineChart.visibility = View.GONE
                            binding.tableSegments.visibility = View.GONE
                            binding.gridLayout.visibility = View.GONE
                            binding.changeMapType.visibility = View.GONE
                        }
                    }
                }
            }
        }
        return binding.root
    }

    private fun takeScreenshot(view: View) {
        try {
            val bitmap = Bitmap.createBitmap(
                view.width, view.height, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            val outputStream = FileOutputStream(getMapScreenshotFile(segmentDetailsId))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            outputStream.flush()
            outputStream.close()
            Toast.makeText(
                requireContext(),
                getString(R.string.screenshot_taken),
                Toast.LENGTH_SHORT
            ).show()
        } catch (io: FileNotFoundException) {
            io.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun update(
        summitToShow: Summit?,
        segmentEntryToShow: SegmentEntry?,
        segmentToUse: Segment?,
        relevantSummits: List<Summit>
    ) {
        if (segmentEntryToShow != null && summitToShow != null && segmentToUse != null) {
            binding.segmentName.text = segmentToUse.segmentDetails.getDisplayName()
            binding.osMap.overlays?.clear()
            binding.osMap.overlayManager?.clear()
            drawMarker(segmentToUse.segmentEntries)
            drawGpxTrackAndItsProfile(summitToShow, segmentEntryToShow)
            drawTable(binding.root, binding.tableSegments, segmentToUse, relevantSummits)
            if (!getMapScreenshotFile(segmentDetailsId).exists()) {
                takeScreenshotWhenTilesAreLoaded()
            }
        }
    }

    private fun takeScreenshotWhenTilesAreLoaded(timeout: Long = TIMEOUT_TILES_LOADED) {
        Handler(Looper.getMainLooper()).postDelayed({
            val tileStates: TileStates =
                binding.osMap.overlayManager.tilesOverlay.tileStates
            if (timeout > 0 && tileStates.total != tileStates.upToDate) {
                Log.d(
                    TAG,
                    "Tiles not loaded, waiting for $STEP_TILES_LOADED ms, time to timeout $timeout ms"
                )
                takeScreenshotWhenTilesAreLoaded(timeout - STEP_TILES_LOADED)
            } else {
                Log.d(TAG, "Will take screenshot of open street map view.")
                takeScreenshot(binding.osMap)
            }
        }, STEP_TILES_LOADED)
    }


    private fun drawGpxTrackAndItsProfile(localSummit: Summit?, segmentEntry: SegmentEntry) {
        if (localSummit != null && localSummit.hasGpsTrack()) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    localSummit.setGpsTrack(useSimplifiedTrack = false)
                }
                val gpsTrack = localSummit.gpsTrack
                if (gpsTrack != null) {
                    val hasPoints = !gpsTrack.hasOnlyZeroCoordinates() || localSummit.latLng != null
                    if (hasPoints) {
                        gpsTrack.addGpsTrack(
                            binding.osMap, rootView = binding.root
                        )
                        putGpxTrackOnMap(gpsTrack, segmentEntry)
                    }

                    drawChart(gpsTrack, segmentEntry)
                    setTextView(gpsTrack, segmentEntry)
                }
            }
        }
    }

    private fun prepareMap() {
        selectedItem = getSportTypeForMapProviders(SportType.Other, requireContext())
        binding.changeMapType.setImageResource(R.drawable.baseline_more_vert_black_24dp)
        binding.changeMapType.setOnClickListener {
            OpenStreetMapUtils.showMapTypeSelectorDialog(
                requireContext(), binding.osMap
            )
        }
        binding.updateSnapshot.setOnClickListener {
            takeScreenshotWhenTilesAreLoaded()
        }
        OpenStreetMapUtils.setTileSource(binding.osMap, requireContext())
        OpenStreetMapUtils.addDefaultSettings(
            requireContext(), binding.osMap, requireActivity()
        )
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    private fun drawMarker(segmentEntries: List<SegmentEntry>) {
        addMarker(
            binding.osMap, GeoPoint(
                segmentEntries.first().startPositionLatitude,
                segmentEntries.first().startPositionLongitude
            ), ResourcesCompat.getDrawable(
                resources, R.drawable.ic_filled_location_lightbrown_48, null
            )
        )
        addMarker(
            binding.osMap, GeoPoint(
                segmentEntries.first().endPositionLatitude,
                segmentEntries.first().endPositionLongitude
            ), ResourcesCompat.getDrawable(
                resources, R.drawable.ic_filled_location_darkbrown_48, null
            )
        )
    }

    private fun drawChart(gpxTrack: GpsTrack?, segmentEntry: SegmentEntry) {
        if (gpxTrack != null) {
            binding.lineChart.clear()
            binding.lineChart.xAxis.removeAllLimitLines()
            setXAxis(binding.lineChart)
            val dataSets: MutableList<ILineDataSet> = ArrayList()
            val trackColor = selectedCustomizeTrackItem
            val lineChartEntries = gpxTrack.getTrackGraph(trackColor.f)
            val label = getString(trackColor.labelId)

            val leftAxis: YAxis = binding.lineChart.axisLeft
            leftAxis.textColor = Color.BLACK
            leftAxis.setDrawGridLines(true)
            leftAxis.isGranularityEnabled = true

            val dataSet = LineDataSet(lineChartEntries, label)
            setGraphView(dataSet)
            setColors(lineChartEntries, dataSet)
            dataSets.add(dataSet)
            binding.lineChart.data = LineData(dataSets)
            setLegend(binding.lineChart)
            if (segmentEntry.startPositionInTrack < gpxTrack.trackPoints.size) {
                gpxTrack.trackPoints[segmentEntry.startPositionInTrack].second.distance?.toFloat()
                    ?.let { drawVerticalLine(it, Color.GREEN) }
            }
            if (segmentEntry.endPositionInTrack < gpxTrack.trackPoints.size) {
                gpxTrack.trackPoints[segmentEntry.endPositionInTrack].second.distance?.toFloat()
                    ?.let { drawVerticalLine(it, Color.RED) }
            }
        } else {
            binding.lineChart.visibility = View.GONE
        }
    }

    private fun drawVerticalLine(distance: Float, color: Int) {
        val ll = LimitLine(distance)
        ll.lineColor = color
        ll.lineWidth = 2f
        binding.lineChart.xAxis.addLimitLine(ll)
    }

    private fun setLegend(lineChart: LineChart) {
        val l: Legend = lineChart.legend
        l.entries
        l.yEntrySpace = 10f
        l.isWordWrapEnabled = true
        val l1 = LegendEntry(
            getString(R.string.min),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            selectedCustomizeTrackItem.minColor
        )
        val l2 = LegendEntry(
            getString(R.string.max),
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            selectedCustomizeTrackItem.maxColor
        )
        l.setCustom(arrayOf(l1, l2))
        l.isEnabled = true
    }

    private fun setColors(lineChartEntries: MutableList<Entry>, dataSet: LineDataSet) {
        val min = lineChartEntries.minByOrNull { it.y }?.y
        val max = lineChartEntries.maxByOrNull { it.y }?.y
        if (min != null && max != null) {
            val colors = lineChartEntries.map {
                val fraction = (it.y - min) / (max - min)
                GpsTrack.interpolateColor(
                    selectedCustomizeTrackItem.minColor,
                    selectedCustomizeTrackItem.maxColor,
                    fraction
                )
            }
            dataSet.colors = colors
        }
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(
                    resources.configuration.locales[0],
                    "%.1f km",
                    (value / 100f).roundToLong() / 10f
                )
            }
        }
    }

    private fun setTextView(gpxTrack: GpsTrack, segmentEntry: SegmentEntry) {
        val allTrackPoints = gpxTrack.trackPoints
        if (allTrackPoints.isNotEmpty()) {

            val startTrackPoint = allTrackPoints[segmentEntry.startPositionInTrack]
            val endTrackPoint = allTrackPoints[segmentEntry.endPositionInTrack]
            val selectedTrackPoints = allTrackPoints.subList(
                segmentEntry.startPositionInTrack, segmentEntry.endPositionInTrack
            )

            val averageHeartRate = selectedTrackPoints.sumOf {
                it.second.hr ?: 0
            } / selectedTrackPoints.size
            val averagePower = selectedTrackPoints.sumOf {
                it.second.power ?: 0
            } / selectedTrackPoints.size
            val pointsOnlyWithMaximalValues = TrackUtils.keepOnlyMaximalValues(selectedTrackPoints)
            val heightMeterResult =
                TrackUtils.removeDeltasSmallerAs(10, pointsOnlyWithMaximalValues)

            val duration =
                (endTrackPoint.first.time.millis - startTrackPoint.first.time.millis).toDouble() / 60000.0
            val distance = ((endTrackPoint.second.distance
                ?: 0.0) - (startTrackPoint.second.distance
                ?: 0.0)) / 1000.0

            binding.duration.text = String.format(
                resources.configuration.locales[0], "%.1f %s", duration, getString(R.string.min)
            )
            binding.kilometers.text = String.format(
                resources.configuration.locales[0], "%.1f %s", distance, getString(R.string.km)
            )
            binding.averageHr.text = String.format(
                resources.configuration.locales[0],
                "%s %s",
                averageHeartRate,
                getString(R.string.bpm)
            )
            binding.averagePower.text = String.format(
                resources.configuration.locales[0], "%s %s", averagePower, getString(R.string.watt)
            )
            binding.heightMeter.text = String.format(
                resources.configuration.locales[0],
                "%s/%s %s",
                heightMeterResult.second.roundToInt(),
                heightMeterResult.third.roundToInt(),
                getString(R.string.hm)
            )
        }
    }


    private fun addMarker(mMapView: MapView, startPoint: GeoPoint?, drawable: Drawable?): Marker? {
        return try {
            val marker = Marker(mMapView)
            marker.position = startPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = drawable
            mMapView.overlays.add(marker)
            marker.setOnMarkerClickListener { _, _ ->
                false
            }
            mMapView.invalidate()
            marker
        } catch (e: NullPointerException) {
            e.printStackTrace()
            null
        }
    }

    private fun putGpxTrackOnMap(gpxTrack: GpsTrack, segmentEntry: SegmentEntry) {
        try {
            val osMapRoute = Polyline(binding.osMap)
            val paintBorder = Paint()
            paintBorder.strokeWidth = 20F
            val geoPoints = gpxTrack.trackGeoPoints.filterIndexed { index, _ ->
                index in segmentEntry.startPositionInTrack..segmentEntry.endPositionInTrack
            }
            val trackPoints = gpxTrack.trackPoints.filterIndexed { index, _ ->
                index in segmentEntry.startPositionInTrack..segmentEntry.endPositionInTrack
            }
            if (geoPoints.size > 1) {
                addColorToTrack(osMapRoute, trackPoints, paintBorder, selectedCustomizeTrackItem)
                osMapRoute.setPoints(geoPoints)
                binding.osMap.overlayManager?.add(osMapRoute)
                val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
                binding.osMap.post {
                    binding.osMap.zoomToBoundingBox(boundingBox, false, 50)
                }
            }

        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    private fun addColorToTrack(
        osMapRoute: Polyline,
        usedTrackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
        paintBorder: Paint,
        trackColor: TrackColor
    ) {
        val values = usedTrackPoints.mapNotNull(trackColor.f)
        val minForColorCoding = (values.minOrNull() ?: 0.0).toFloat()
        val maxForColorCoding = (values.maxOrNull() ?: 0.0).toFloat()
        val pointsExists = usedTrackPoints.any { trackColor.f(it) != 0.0 }
        if (pointsExists) {
            val attributeColorList = GpsTrack.AttitudeColorList(
                usedTrackPoints,
                minForColorCoding,
                maxForColorCoding,
                trackColor.minColor,
                trackColor.maxColor,
                trackColor.f
            )
            osMapRoute.outlinePaintLists?.add(
                PolychromaticPaintList(
                    paintBorder, attributeColorList, false
                )
            )
        }
    }


    private fun setGraphView(set1: LineDataSet?) {
        set1?.setDrawValues(false)
        set1?.setDrawFilled(true)
        set1?.setDrawCircles(false)
        set1?.axisDependency = YAxis.AxisDependency.LEFT
        set1?.color = Color.RED
        set1?.setCircleColor(Color.RED)
        set1?.lineWidth = 5f
        set1?.circleRadius = 3f
        set1?.fillAlpha = 50
        set1?.fillColor = Color.RED
        set1?.setDrawCircleHole(false)
        set1?.highLightColor = Color.rgb(244, 117, 117)
        set1?.setDrawHorizontalHighlightIndicator(true)
    }


    private fun drawTable(
        view: View,
        tableLayout: TableLayout,
        segment: Segment,
        relevantSummits: List<Summit>
    ) {
        tableLayout.removeAllViews()
        addHeader(view, tableLayout, segment, relevantSummits)
        sorter(segment.segmentEntries).forEachIndexed { index, entry ->
            val summit = relevantSummits.firstOrNull {
                it.activityId == entry.activityId
            }
            if (summit != null) {
                addSegmentToTable(
                    summit,
                    entry,
                    view,
                    index,
                    tableLayout,
                    segment,
                    summit == summitShown && segmentEntryToShow == entry,
                    relevantSummits
                )
            }
        }
    }

    private fun addHeader(
        view: View,
        tableLayout: TableLayout,
        segment: Segment,
        relevantSummits: List<Summit>
    ) {
        val tableRowHead = TableRow(view.context)
        10.also { tableRowHead.id = it }
        tableRowHead.setBackgroundColor(Color.WHITE)
        tableRowHead.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT
        )
        addLabel(view,
            tableRowHead,
            20,
            getString(R.string.date),
            tableLayout,
            segment,
            relevantSummits,
            sorter = { e -> if (descending) e.sortedByDescending { it.date } else e.sortedBy { it.date } })
        addLabel(
            view,
            tableRowHead,
            21,
            getString(R.string.minutes),
            tableLayout,
            segment,
            relevantSummits
        )
        addLabel(view,
            tableRowHead,
            22,
            getString(R.string.kmh),
            tableLayout,
            segment,
            relevantSummits,
            sorter = { e -> if (descending) e.sortedByDescending { it.kilometers * 60 / it.duration } else e.sortedBy { it.kilometers * 60 / it.duration } })
        addLabel(view,
            tableRowHead,
            23,
            getString(R.string.bpm),
            tableLayout,
            segment,
            relevantSummits,
            sorter = { e -> if (descending) e.sortedByDescending { it.averageHeartRate } else e.sortedBy { it.averageHeartRate } })
        addLabel(view,
            tableRowHead,
            24,
            getString(R.string.watt),
            tableLayout,
            segment,
            relevantSummits,
            sorter = { e -> if (descending) e.sortedByDescending { it.averagePower } else e.sortedBy { it.averagePower } })
        addLabel(view, tableRowHead, 25, "", tableLayout, segment, relevantSummits)
        addLabel(view, tableRowHead, 26, "", tableLayout, segment, relevantSummits)
        tableLayout.addView(
            tableRowHead, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addLabel(
        view: View,
        tr: TableRow,
        id: Int,
        text: String,
        color: Int = Color.BLACK,
        padding: Int = 5,
        alignment: Int = View.TEXT_ALIGNMENT_CENTER
    ) {
        val label = TextView(view.context)
        label.id = id
        label.text = text
        label.gravity = alignment
        label.setTextColor(color)
        label.setPadding(padding, padding, padding, padding)
        tr.addView(label)
    }

    private fun addLabel(
        view: View,
        tr: TableRow,
        id: Int,
        text: String,
        tableLayout: TableLayout,
        segment: Segment,
        relevantSummits: List<Summit>,
        sorter: (List<SegmentEntry>) -> List<SegmentEntry> = { e -> if (descending) e.sortedByDescending { it.duration } else e.sortedBy { it.duration } }
    ) {
        val label = TextView(view.context)
        label.id = id
        label.text = text
        if (id == setSortLabelFor) {
            label.setCompoundDrawablesWithIntrinsicBounds(
                if (descending) R.drawable.baseline_keyboard_double_arrow_down_black_24dp else R.drawable.baseline_keyboard_double_arrow_up_black_24dp,
                0,
                0,
                0
            )
        }
        label.setOnClickListener {
            descending = !descending
            this.sorter = sorter
            setSortLabelFor = id
            drawTable(view, tableLayout, segment, relevantSummits)
        }
        label.setTextColor(Color.BLACK)
        tr.addView(label)
    }

    private fun addSegmentToTable(
        summit: Summit,
        entry: SegmentEntry,
        view: View,
        i: Int,
        tableLayout: TableLayout,
        segment: Segment,
        mark: Boolean,
        relevantSummits: List<Summit>
    ) {
        val dateParts = (summit.getDateAsString() ?: "").split("-")
        val date = "${dateParts[0]}\n${dateParts[1]}-${dateParts[2]}"
        val tr = TableRow(view.context)
        tr.setOnClickListener {
            segmentEntryToShow = entry
            segmentEntryId = entry.entryId
            summitShown = summit
            update(summitShown, segmentEntryToShow, segment, relevantSummits)
        }
        if (mark) {
            tr.setBackgroundColor(Color.YELLOW)
        } else {
            tr.setBackgroundColor(Color.WHITE)
        }
        tr.id = 100 + i
        tr.layoutParams = TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT
        )
        addLabel(
            view,
            tr,
            200 + i,
            date,
            padding = 2,
            alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        addLabel(
            view, tr, 201 + i, String.format(
                "%s:%s",
                floor(entry.duration).toInt(),
                ((entry.duration - floor(entry.duration)) * 60).roundToInt().toString()
                    .padStart(2, '0')
            ), padding = 2, alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        addLabel(
            view,
            tr,
            202 + i,
            String.format(Locale.getDefault(), "%.1f", entry.kilometers * 60 / entry.duration),
            padding = 2,
            alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        addLabel(
            view,
            tr,
            203 + i,
            String.format("%s", entry.averageHeartRate),
            padding = 2,
            alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        addLabel(
            view,
            tr,
            204 + i,
            String.format("%s", entry.averagePower),
            padding = 2,
            alignment = View.TEXT_ALIGNMENT_TEXT_END
        )
        val updateButton = ImageButton(view.context)
        updateButton.setImageResource(R.drawable.ic_baseline_edit_24)
        updateButton.setOnClickListener {
            val fragment: Fragment = AddSegmentEntryFragment.getInstance(
                segment.segmentDetails.segmentDetailsId, null, entry.entryId
            )
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment).addToBackStack(null).commit()
        }
        tr.addView(updateButton)
        val removeButton = ImageButton(view.context)
        removeButton.setImageResource(R.drawable.ic_baseline_delete_forever_24)
        removeButton.setOnClickListener {
            viewModel.deleteSegmentEntry(entry)
        }
        tr.addView(removeButton)

        tableLayout.addView(
            tr, TableLayout.LayoutParams(
                TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, segmentDetailsId)
        outState.putLong(SegmentEntry.SEGMENT_ENTRY_ID_EXTRA_IDENTIFIER, segmentEntryId)
    }

    companion object {
        const val TIMEOUT_TILES_LOADED = 15000L
        const val STEP_TILES_LOADED = 500L
        const val TAG = "SegmentEntryDetailsFragment"
    }

}