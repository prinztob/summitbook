package de.drtobiasprinz.summitbook.fragments

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.chivorn.smartmaterialspinner.SmartMaterialSpinner
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import de.drtobiasprinz.gpx.TrackPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SegmentsViewAdapter
import de.drtobiasprinz.summitbook.models.*
import de.drtobiasprinz.summitbook.models.GpsTrack.Companion.interpolateColor
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.setTileSource
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.showMapTypeSelectorDialog
import de.drtobiasprinz.summitbook.ui.utils.SummitSlope
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class AddSegmentEntryFragment : Fragment() {

    private lateinit var root: View
    private lateinit var osMap: CustomMapViewToAllowScrolling
    private lateinit var lineChart: LineChart
    private var gpsTrack: GpsTrack? = null
    private var startMarker: Marker? = null
    private var stopMarker: Marker? = null
    private var pointOverlay: SimpleFastPointOverlay? = null
    private var summitToCompare: Summit? = null
    private var summitsToCompare: List<Summit> = emptyList()
    private lateinit var resultReceiver: FragmentResultReceiver
    private var selectedCustomizeTrackItem = TrackColor.None
    private var startSelected = true
    private var startPointId: Int = 0
    private var endPointId: Int = 0
    private var segmentId: Long = 0
    private var segmentEntryId: Long? = null
    private var segmentEntry: SegmentEntry? = null
    private var segment: Segment? = null
    private var segmentsViewAdapter: SegmentsViewAdapter? = null
    private var isUpdate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultReceiver = context as FragmentResultReceiver
        setHasOptionsMenu(true)
        segmentEntry = segmentEntryId?.let { resultReceiver.getSortFilterHelper().database.segmentsDao()?.getSegmentEntry(it) }
        segment = segmentsViewAdapter?.segments?.first { it.segmentDetails.segmentDetailsId == segmentId }
        summitToCompare = segmentEntry?.activityId?.let { resultReceiver.getSortFilterHelper().database.summitDao()?.getSummitFromActivityId(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        root = inflater.inflate(R.layout.fragment_add_segment_entry, container, false)
        lineChart = root.findViewById(R.id.lineChart)
        osMap = root.findViewById(R.id.osmap)
        setTileSource(selectedItem, osMap)
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel).setOnClickListener {
            activity?.supportFragmentManager?.popBackStack();
        }
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.save).setOnClickListener {
            val summit = summitToCompare
            val segmentEntryLocal = segmentEntry
            if (summit != null && segmentEntryLocal != null) {
                if (isUpdate) {
                    resultReceiver.getSortFilterHelper().database.segmentsDao()?.updateSegmentEntry(segmentEntryLocal)
                    segmentsViewAdapter?.segments?.first { it.segmentDetails.segmentDetailsId == segmentId }?.segmentEntries?.remove(segmentEntryLocal)
                } else {
                    resultReceiver.getSortFilterHelper().database.segmentsDao()?.addSegmentEntry(segmentEntryLocal)
                }
                segmentsViewAdapter?.segments?.first { it.segmentDetails.segmentDetailsId == segmentId }?.segmentEntries?.add(segmentEntryLocal)
                segmentsViewAdapter?.notifyDataSetChanged()
            }
            activity?.supportFragmentManager?.popBackStack();
        }

        root.findViewById<SwitchCompat>(R.id.startOrStop).setOnCheckedChangeListener { _, isChecked ->
            startSelected = !isChecked
        }
        root.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.mapOnOff).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                osMap.visibility = View.VISIBLE
                val scale = root.resources.displayMetrics.density
                val pixels = (150 * scale + 0.5f).toInt()
                lineChart.layoutParams.height = pixels
            } else {
                osMap.visibility = View.GONE
                lineChart.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        root.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.chartOnOff).setOnCheckedChangeListener { _, isChecked ->
            lineChart.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        setHasOptionsMenu(true)
        if (summitToCompare != null) {
            setGpsTrack(summitToCompare)
            val segmentEntryLocal = segmentEntry
            if (segmentEntryLocal != null) {
                startPointId = segmentEntryLocal.startPositionInTrack
                endPointId = segmentEntryLocal.endPositionInTrack
            }
            setOpenStreetMap()
            setStartMarker(startPointId, false)
            setEndMarker(endPointId)
        }
        prepareCompareAutoComplete()
        return root
    }

    private fun prepareCompareAutoComplete() {
        val summitToCompareSpinner: SmartMaterialSpinner<String> = root.findViewById(R.id.summit_name_to_use)
        val items = getSummitsSuggestions()
        summitToCompareSpinner.item = items
        val summit = summitToCompare
        if (summit != null) {
            val position = items.indexOf("${summit.getDateAsString()} ${summit.name}")
            if (position >= 0) {
                summitToCompareSpinner.setSelection(position)
            }
        }
        summitToCompareSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (view != null) {
                    val text = items[position]
                    if (text != "" && text != "None") {
                        val newlySelectedSummit = summitsToCompare.find { text.startsWith("${it.getDateAsString()} ${it.name}") }
                        if (newlySelectedSummit != summitToCompare) {
                            summitToCompare = newlySelectedSummit
                            setGpsTrack(summitToCompare)
                            setOpenStreetMap()
                            drawChart()
                            setTextView()
                        }
                    }
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }
    }

    private fun setTextView() {
        root.findViewById<TextView>(R.id.tour_date).text = summitToCompare?.getDateAsString()
        val allTrackPoints = gpsTrack?.trackPoints
        val summit = summitToCompare
        if (summit != null && allTrackPoints != null && allTrackPoints.isNotEmpty()) {

            val startTrackPoint = allTrackPoints[startPointId]
            val endTrackPoint = allTrackPoints[endPointId]
            val selectedTrackPoints = allTrackPoints.subList(startPointId, endPointId)

            val averageHeartRate = selectedTrackPoints.sumBy {
                it.extension?.heartRate ?: 0
            } / selectedTrackPoints.size
            val averagePower = selectedTrackPoints.sumBy {
                it.extension?.power ?: 0
            } / selectedTrackPoints.size
            val pointsOnlyWithMaximalValues = SummitSlope.keepOnlyMaximalValues(selectedTrackPoints)
            val heightMeterResult = SummitSlope.removeDeltasSmallerAs(10, pointsOnlyWithMaximalValues)

            val duration = ((endTrackPoint.time ?: 0L) - (startTrackPoint.time
                    ?: 0L)).toDouble() / 60000.0
            val distance = ((endTrackPoint.extension?.distance
                    ?: 0.0) - (startTrackPoint.extension?.distance ?: 0.0)) / 1000.0

            root.findViewById<TextView>(R.id.duration).text = String.format(requireContext().resources.configuration.locales[0], "%.1f %s", duration, getString(R.string.min))
            root.findViewById<TextView>(R.id.kilometers).text = String.format(requireContext().resources.configuration.locales[0], "%.1f %s", distance, getString(R.string.km))
            root.findViewById<TextView>(R.id.averageHr).text = String.format(requireContext().resources.configuration.locales[0], "%s %s", averageHeartRate, getString(R.string.bpm))
            root.findViewById<TextView>(R.id.averagePower).text = String.format(requireContext().resources.configuration.locales[0], "%s %s", averagePower, getString(R.string.watt))
            root.findViewById<TextView>(R.id.height_meter).text = String.format(requireContext().resources.configuration.locales[0], "%s/%s %s",
                    heightMeterResult.second.roundToInt(), heightMeterResult.third.roundToInt(), getString(R.string.hm))
            val saveButton = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.save)
            saveButton.isEnabled = true
            if (isUpdate) {
                saveButton.text = getString(R.string.update)
            }
            segmentEntry = SegmentEntry(segmentEntryId
                    ?: 0, segmentId, summit.date, summit.activityId,
                    startPointId, startTrackPoint.lat, startTrackPoint.lon,
                    endPointId, endTrackPoint.lat, endTrackPoint.lon,
                    duration, distance, heightMeterResult.second.roundToInt(), heightMeterResult.third.roundToInt(), averageHeartRate, averagePower
            )
        }
    }

    private fun getSummitsSuggestions(): ArrayList<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsToCompareFromActivity = resultReceiver.getSortFilterHelper().entries
        val summitsWithTrack = summitsToCompareFromActivity.filter { it.hasGpsTrack() }.sortedByDescending { it.date }
        val entries = segment?.segmentEntries
        summitsToCompare = if (entries != null && entries.isNotEmpty()) {
            val startPoint = GeoPoint(entries.first().startPositionLatitude, entries.first().startPositionLongitude)
            val endPoint = GeoPoint(entries.first().endPositionLatitude, entries.first().endPositionLongitude)
            summitsWithTrack.filter {
                it.trackBoundingBox?.contains(startPoint) == true && it.trackBoundingBox?.contains(endPoint) == true
            }
        } else {
            summitsWithTrack
        }
        summitsToCompare.forEach { summit: Summit ->
            val occurrence = segment?.segmentEntries?.filter { it.activityId == summit.activityId }?.size
                    ?: 0
            suggestions.add("${summit.getDateAsString()} ${summit.name} ${if (occurrence > 0) "($occurrence)" else ""}")
        }
        return suggestions as ArrayList
    }

    private fun setGpsTrack(localSummit: Summit?) {
        if (localSummit != null && localSummit.hasGpsTrack()) {
            localSummit.setGpsTrack(useSimplifiedTrack = false, loadFullTrackAsynchronous = true)
            gpsTrack = localSummit.gpsTrack
            startPointId = 0
            endPointId = (gpsTrack?.trackGeoPoints?.size ?: 1) - 1
            if (gpsTrack?.trackPoints?.any {
                        val value = TrackColor.Elevation.f(it)
                        value != null && value != 0.0
                    } == true) {
                selectedCustomizeTrackItem = TrackColor.Elevation
            }
        }
    }

    private fun drawChart() {
        val localGpsTrack = gpsTrack
        if (localGpsTrack != null) {
            lineChart.clear()
            lineChart.xAxis.removeAllLimitLines()
            setXAxis(lineChart)
            val dataSets: MutableList<ILineDataSet> = ArrayList()
            val trackColor = selectedCustomizeTrackItem
            val lineChartEntries = localGpsTrack.getTrackGraph(trackColor.f)
            val label = getString(trackColor.labelId)

            val leftAxis: YAxis = lineChart.axisLeft
            leftAxis.textColor = Color.BLACK
            leftAxis.setDrawGridLines(true)
            leftAxis.isGranularityEnabled = true

            val dataSet = LineDataSet(lineChartEntries, label)
            setGraphView(dataSet)
            setColors(lineChartEntries, dataSet)
            dataSets.add(dataSet)
            lineChart.data = LineData(dataSets)
            lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, h: Highlight?) {
                    if (e.data is TrackPoint) {
                        val trackPoint = e.data as TrackPoint
                        val index = gpsTrack?.trackPoints?.indexOf(trackPoint)
                        if (index != null) {
                            if (startSelected) {
                                setStartMarker(index)
                            } else {
                                setEndMarker(index)
                            }
                            osMap.controller.setCenter(gpsTrack?.trackGeoPoints?.get(index))
                        }
                    }
                }

                override fun onNothingSelected() {}
            })
            setLegend(lineChart, label)
            drawVerticalLine(startPointId, Color.GREEN)
            drawVerticalLine(endPointId, Color.RED)
        } else {
            lineChart.visibility = View.GONE
        }
    }

    private fun drawVerticalLine(index: Int, color: Int) {
        val distance = gpsTrack?.trackPoints?.get(index)?.extension?.distance?.toFloat()
        if (distance != null) {
            val ll = LimitLine(distance)
            ll.lineColor = color
            ll.lineWidth = 2f
            lineChart.xAxis.addLimitLine(ll)
        }
    }

    private fun setLegend(lineChart: LineChart, label: String?) {
        val l: Legend = lineChart.legend
        l.entries
        l.yEntrySpace = 10f
        l.isWordWrapEnabled = true
        val l1 = LegendEntry("$label ${getString(R.string.min)}", Legend.LegendForm.CIRCLE, 9f, 5f, null, selectedCustomizeTrackItem.minColor)
        val l2 = LegendEntry("$label ${getString(R.string.max)}", Legend.LegendForm.CIRCLE, 9f, 5f, null, selectedCustomizeTrackItem.maxColor)
        l.setCustom(arrayOf(l1, l2))
        l.isEnabled = true
    }

    private fun setColors(lineChartEntries: MutableList<Entry>, dataSet: LineDataSet) {
        val min = lineChartEntries.minByOrNull { it.y }?.y
        val max = lineChartEntries.maxByOrNull { it.y }?.y
        if (min != null && max != null) {
            val colors = lineChartEntries.map {
                val fraction = (it.y - min) / (max - min)
                interpolateColor(selectedCustomizeTrackItem.minColor, selectedCustomizeTrackItem.maxColor, fraction)
            }
            dataSet.colors = colors
        }
    }

    private fun setXAxis(lineChart: LineChart) {
        val xAxis = lineChart.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format(requireContext().resources.configuration.locales[0], "%.1f km", (value / 100f).roundToLong() / 10f)
            }
        }
    }

    private fun setOpenStreetMap() {
        osMap.overlays?.clear()
        osMap.overlayManager?.clear()
        val changeMapTypeFab: ImageButton = root.findViewById(R.id.change_map_type)
        changeMapTypeFab.setImageResource(R.drawable.ic_more_vert_black_24dp)
        changeMapTypeFab.setOnClickListener { showMapTypeSelectorDialog(requireContext(), osMap) }
        addDefaultSettings(requireContext(), osMap, requireActivity())
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        val summit = summitToCompare
        if (summit != null) {
            val hasPoints = gpsTrack?.hasOnlyZeroCoordinates() == false || summit.latLng != null
            if (summitToCompare != null && hasPoints) {
                if (gpsTrack?.osMapRoute == null) {
                    gpsTrack?.addGpsTrack(osMap, selectedCustomizeTrackItem, rootView = root)
                }
                showSinglePoints()
                osMap.post { gpsTrack?.let { OpenStreetMapUtils.calculateBoundingBox(osMap, it.trackGeoPoints) } }
            }
        }
        val entries = segment?.segmentEntries
        if (entries != null && entries.isNotEmpty()) {
            val firstStartPoint = GeoPoint(entries.first().startPositionLatitude, entries.first().startPositionLongitude)
            val firstEndPoint = GeoPoint(entries.first().endPositionLatitude, entries.first().endPositionLongitude)
            addMarker(osMap, firstStartPoint, ResourcesCompat.getDrawable(requireContext().resources, R.drawable.ic_filled_location_lightbrown_48, null))
            addMarker(osMap, firstEndPoint, ResourcesCompat.getDrawable(requireContext().resources, R.drawable.ic_filled_location_darkbrown_48, null))
        }
    }

    private fun showSinglePoints() {
        val trackGeoPoints = gpsTrack?.trackGeoPoints
        if (trackGeoPoints != null && trackGeoPoints.isNotEmpty()) {
            osMap.overlays.remove(pointOverlay)
            val filteredPoints = ArrayList<IGeoPoint>();
            for ((i, point) in trackGeoPoints.withIndex()) {
                if (i > startPointId - 30 && i < endPointId + 30) {
                    filteredPoints.add(LabelledGeoPoint(point.latitude, point.longitude, i.toString()));
                }
            }
            val pt = SimplePointTheme(filteredPoints, true);

            val textStyle = Paint();
            textStyle.style = Paint.Style.FILL
            textStyle.color = Color.BLACK
            textStyle.textAlign = Paint.Align.CENTER
            textStyle.textSize = 55f
            textStyle.isAntiAlias = true
            textStyle.isDither = true
            textStyle.style = Paint.Style.STROKE
            textStyle.strokeJoin = Paint.Join.ROUND
            textStyle.strokeCap = Paint.Cap.ROUND
            textStyle.strokeWidth = 5f

            val opt = SimpleFastPointOverlayOptions.getDefaultStyle()
                    .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.NO_OPTIMIZATION)
                    .setRadius(10f)
                    .setIsClickable(true)
                    .setCellSize(20)
                    .setMinZoomShowLabels(19)
                    .setTextStyle(textStyle)
            pointOverlay = SimpleFastPointOverlay(pt, opt);
            pointOverlay?.setOnClickListener { _: SimpleFastPointOverlay.PointAdapter, i: Int ->
                val indexOfTrackPoint = (filteredPoints[i] as LabelledGeoPoint).label.toInt()
                if (startSelected) {
                    setStartMarker(indexOfTrackPoint)
                } else {
                    setEndMarker(indexOfTrackPoint)
                }
            }
            osMap.overlays?.add(pointOverlay);
        }
    }

    private fun setEndMarker(i: Int) {
        if (i >= startPointId) {
            endPointId = i
            updateMapAndChart()
        } else {
            Toast.makeText(root.context, getString(R.string.stop_before_start), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setStartMarker(i: Int, updateView: Boolean = true) {
        if (i <= endPointId) {
            startPointId = i
            updateMapAndChart(updateView)
        } else {
            Toast.makeText(root.context, getString(R.string.start_after_stop), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateMapAndChart(updateView: Boolean = true) {
        if (updateView) {
            drawChart()
            showSinglePoints()
            setTextView()
        }
        osMap.overlays.remove(startMarker)
        osMap.overlays.remove(stopMarker)
        startMarker = addMarker(osMap, gpsTrack?.trackGeoPoints?.get(startPointId), ResourcesCompat.getDrawable(requireContext().resources, R.drawable.ic_filled_location_green_48, null))
        stopMarker = addMarker(osMap, gpsTrack?.trackGeoPoints?.get(endPointId), ResourcesCompat.getDrawable(requireContext().resources, R.drawable.ic_filled_location_red_48, null))
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

    private fun addMarker(mMapView: MapView, startPoint: GeoPoint?, drawable: Drawable?): Marker? {
        try {
            val marker = Marker(mMapView)
            marker.position = startPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = drawable
            mMapView.overlays.add(marker)
            marker.setOnMarkerClickListener { _, _ ->
                false
            }
            mMapView.invalidate()
            return marker
        } catch (e: NullPointerException) {
            e.printStackTrace()
            return null
        }
    }

    companion object {
        fun getInstance(segmentId: Long, segmentsViewAdapter: SegmentsViewAdapter, segmentEntryId: Long?): AddSegmentEntryFragment {
            val fragment = AddSegmentEntryFragment()
            fragment.segmentId = segmentId
            if (segmentEntryId != null) {
                fragment.segmentEntryId = segmentEntryId
                fragment.isUpdate = true
            }
            fragment.segmentsViewAdapter = segmentsViewAdapter
            return fragment
        }

        const val TAG = "osmBaseFrag"
    }

}