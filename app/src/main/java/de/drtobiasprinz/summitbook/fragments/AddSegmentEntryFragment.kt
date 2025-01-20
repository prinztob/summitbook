package de.drtobiasprinz.summitbook.fragments

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
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
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.SegmentsViewAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentAddSegmentEntryBinding
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.GpsTrack.Companion.interpolateColor
import de.drtobiasprinz.summitbook.models.TrackColor.Elevation
import de.drtobiasprinz.summitbook.models.TrackColor.None
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.addDefaultSettings
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.setTileProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.showMapTypeSelectorDialog
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@AndroidEntryPoint
class AddSegmentEntryFragment : Fragment() {

    private var summitsToCompare: List<Summit> = emptyList()
    private var segmentEntry: SegmentEntry? = null

    private lateinit var binding: FragmentAddSegmentEntryBinding
    private val viewModel: DatabaseViewModel by activityViewModels()

    private var startMarker: Marker? = null
    private var stopMarker: Marker? = null
    private var pointOverlay: SimpleFastPointOverlay? = null
    private var selectedCustomizeTrackItem = None
    private var startSelected = true
    private var startPointId: Int = 0
    private var firstStartPoint: GeoPoint? = null
    private var firstEndPoint: GeoPoint? = null
    private var endPointId: Int = 0
    private var segmentId: Long = 0
    private var segmentEntryId: Long? = null
    private var segmentsViewAdapter: SegmentsViewAdapter? = null
    private var isUpdate = false
    private var summitToCompare: Summit? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddSegmentEntryBinding.inflate(layoutInflater, container, false)
        OpenStreetMapUtils.setOsmConfForTiles()
        if (PreferencesHelper.loadOnDeviceMaps() &&
            FileHelper.getOnDeviceMapFiles(requireContext()).isNotEmpty()
        ) {
            selectedItem = MapProvider.HIKING
        } else if (FileHelper.getOnDeviceMbtilesFiles(requireContext()).isNotEmpty()) {
            selectedItem = MapProvider.MBTILES
        }
        setTileProvider(binding.osmap, requireContext())
        viewModel.segmentsList.observe(viewLifecycleOwner, object :
            Observer<DataStatus<List<Segment>>> {
            override fun onChanged(value: DataStatus<List<Segment>>) {
                apply(value)
            }
        })
        return binding.root
    }

    private fun Observer<DataStatus<List<Segment>>>.apply(
        summitListDataStatus: DataStatus<List<Segment>>
    ) {
        summitListDataStatus.data.let { segments ->
            val segment =
                segments?.firstOrNull { entry -> entry.segmentDetails.segmentDetailsId == segmentId }
            segmentEntry =
                segment?.segmentEntries?.firstOrNull { entry -> entry.entryId == segmentEntryId }
            viewModel.summitsList.observe(viewLifecycleOwner) { itSummitData ->
                itSummitData.data.let { summits ->
                    summitToCompare =
                        summits?.firstOrNull { entry -> entry.activityId == segmentEntry?.activityId }
                    binding.save.findViewById<MaterialButton>(R.id.save).setOnClickListener {
                        viewModel.segmentsList.removeObserver(this)
                        val segmentEntryLocal = segmentEntry
                        if (summitToCompare != null && segmentEntryLocal != null) {
                            viewModel.saveSegmentEntry(isUpdate, segmentEntryLocal)
                                .invokeOnCompletion { activity?.supportFragmentManager?.popBackStack() }
                        } else {
                            activity?.supportFragmentManager?.popBackStack()
                        }
                    }
                    prepareCompareAutoComplete(summits, segment)
                    val localSummitToCompare = summitToCompare
                    if (localSummitToCompare != null) {
                        setGpsTrack()
                        val localSegmentEntry = segmentEntry
                        if (localSegmentEntry != null) {
                            startPointId = localSegmentEntry.startPositionInTrack
                            endPointId = localSegmentEntry.endPositionInTrack
                            setStartMarker(startPointId, false)
                            setEndMarker(endPointId)
                        }
                        if (segment != null) {
                            setOpenStreetMap(segment, localSummitToCompare)
                        }
                    }
                }
            }
        }
        binding.cancel.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
        binding.startOrStop.setOnCheckedChangeListener { _, isChecked ->
            startSelected = !isChecked
        }
        binding.mapOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.osmap.visibility = View.VISIBLE
                val scale = binding.root.resources.displayMetrics.density
                val pixels = (150 * scale + 0.5f).toInt()
                binding.lineChart.layoutParams.height = pixels
            } else {
                binding.osmap.visibility = View.GONE
                binding.lineChart.layoutParams.height =
                    ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        binding.chartOnOff.setOnCheckedChangeListener { _, isChecked ->
            binding.lineChart.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun prepareCompareAutoComplete(
        summits: List<Summit>?,
        segment: Segment?
    ) {
        val items = getSummitsSuggestions(summits, segment)
        binding.summitNameToUse.item = items
        val position =
            items.indexOf("${summitToCompare?.getDateAsString()} ${summitToCompare?.name}")
        if (position >= 0) {
            binding.summitNameToUse.setSelection(position)
        }
        binding.summitNameToUse.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    if (view != null) {
                        val text = items[position]
                        if (text != "" && text != "None") {
                            val newlySelectedSummit =
                                summitsToCompare.find { text.startsWith("${it.getDateAsString()} ${it.name}") }
                            if (segment != null && newlySelectedSummit != null && newlySelectedSummit != summitToCompare) {
                                summitToCompare = newlySelectedSummit
                                setGpsTrack()
                                setOpenStreetMap(segment, newlySelectedSummit)
                                drawChart(newlySelectedSummit)
                                setTextView(newlySelectedSummit)
                                guessStartAndEndPoint(newlySelectedSummit)
                            }
                        }
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
    }

    private fun setTextView(summitToCompare: Summit) {
        binding.tourDate.text = summitToCompare.getDateAsString()
        val allTrackPoints = summitToCompare.gpsTrack?.trackPoints
        if (!allTrackPoints.isNullOrEmpty()) {
            val startTrackPoint = allTrackPoints[startPointId]
            val endTrackPoint = allTrackPoints[endPointId]
            val selectedTrackPoints = allTrackPoints.subList(startPointId, endPointId)

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
                requireContext().resources.configuration.locales[0],
                "%.1f %s",
                duration,
                getString(R.string.min)
            )
            binding.kilometers.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%.1f %s",
                distance,
                getString(R.string.km)
            )
            binding.averageHr.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%s %s",
                averageHeartRate,
                getString(R.string.bpm)
            )
            binding.averagePower.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%s %s",
                averagePower,
                getString(R.string.watt)
            )
            binding.heightMeter.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%s/%s %s",
                heightMeterResult.second.roundToInt(),
                heightMeterResult.third.roundToInt(),
                getString(R.string.hm)
            )
            binding.save.isEnabled = true
            if (isUpdate) {
                binding.save.text = getString(R.string.update)
            }
            segmentEntry = SegmentEntry(
                segmentEntryId ?: 0,
                segmentId,
                summitToCompare.date,
                summitToCompare.activityId,
                startPointId,
                startTrackPoint.first.latitude,
                startTrackPoint.first.longitude,
                endPointId,
                endTrackPoint.first.latitude,
                endTrackPoint.first.longitude,
                duration,
                distance,
                heightMeterResult.second.roundToInt(),
                heightMeterResult.third.roundToInt(),
                averageHeartRate,
                averagePower
            )
        }
    }

    private fun getSummitsSuggestions(summits: List<Summit>?, segment: Segment?): List<String> {
        val suggestions: MutableList<String> = mutableListOf(getString(R.string.none))
        val summitsWithTrack =
            summits?.filter { it.hasGpsTrack() }?.sortedByDescending { it.date } ?: emptyList()
        val entries = segment?.segmentEntries
        summitsToCompare = if (!entries.isNullOrEmpty()) {
            val startPoint = GeoPoint(
                entries.first().startPositionLatitude, entries.first().startPositionLongitude
            )
            val endPoint =
                GeoPoint(entries.first().endPositionLatitude, entries.first().endPositionLongitude)
            summitsWithTrack.filter {
                it.trackBoundingBox?.contains(startPoint) == true && it.trackBoundingBox?.contains(
                    endPoint
                ) == true
            }
        } else {
            summitsWithTrack
        }
        summitsToCompare.forEach { summit: Summit ->
            val occurrence =
                segment?.segmentEntries?.filter { it.activityId == summit.activityId }?.size ?: 0
            suggestions.add("${summit.getDateAsString()} ${summit.name} ${if (occurrence > 0) "($occurrence)" else ""}")
        }
        return suggestions
    }

    private fun setGpsTrack() {
        val summit = summitToCompare
        if (summit != null) {
            if (summit.hasGpsTrack()) {
                summit.setGpsTrack(useSimplifiedTrack = false)
                startPointId = 0
                endPointId = (summit.gpsTrack?.trackGeoPoints?.size ?: 1) - 1
                if (summit.gpsTrack?.trackPoints?.any {
                        val value = Elevation.f(it)
                        value != null && value != 0.0
                    } == true) {
                    selectedCustomizeTrackItem = Elevation
                }
            }
        }
    }

    private fun guessStartAndEndPoint(summit: Summit) {
        val firstStartPointLocal = firstStartPoint
        val firstEndPointLocal = firstEndPoint
        if (firstStartPointLocal != null && firstEndPointLocal != null) {
            val startPoint = summit.gpsTrack?.trackGeoPoints?.firstOrNull {
                abs(GpsUtils.getDistance(it, firstStartPointLocal)) < 10
            }
            val endPoint = summit.gpsTrack?.trackGeoPoints?.lastOrNull {
                abs(GpsUtils.getDistance(it, firstEndPointLocal)) < 10
            }
            if (startPoint != null && endPoint != null) {
                startPointId = summit.gpsTrack?.trackGeoPoints?.indexOf(startPoint) ?: startPointId
                endPointId = summit.gpsTrack?.trackGeoPoints?.indexOf(endPoint) ?: endPointId
                updateMapAndChart(summit)
            }
        }
    }

    private fun drawChart(summit: Summit) {
        val localGpsTrack = summit.gpsTrack
        if (localGpsTrack != null) {
            binding.lineChart.clear()
            binding.lineChart.xAxis.removeAllLimitLines()
            setXAxis(binding.lineChart)
            val dataSets: MutableList<ILineDataSet> = ArrayList()
            val trackColor = selectedCustomizeTrackItem
            val lineChartEntries = localGpsTrack.getTrackGraph(trackColor.f)
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
            binding.lineChart.setOnChartValueSelectedListener(object :
                OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry, h: Highlight?) {
                    if (e.data is Pair<*, *>) {
                        val trackPoint = e.data as Pair<*, *>
                        val index = summit.gpsTrack?.trackPoints?.indexOf(trackPoint)
                        if (index != null) {
                            if (startSelected) {
                                setStartMarker(index)
                            } else {
                                setEndMarker(index)
                            }
                            binding.osmap.controller.setCenter(
                                summit.gpsTrack?.trackGeoPoints?.get(
                                    index
                                )
                            )
                        }
                    }
                }

                override fun onNothingSelected() {}
            })
            setLegend(binding.lineChart, label)
            drawVerticalLine(summit, startPointId, Color.GREEN)
            drawVerticalLine(summit, endPointId, Color.RED)
        } else {
            binding.lineChart.visibility = View.GONE
        }
    }

    private fun drawVerticalLine(summit: Summit, index: Int, color: Int) {
        val distance = summit.gpsTrack?.trackPoints?.get(index)?.second?.distance?.toFloat()
        if (distance != null) {
            val ll = LimitLine(distance)
            ll.lineColor = color
            ll.lineWidth = 2f
            binding.lineChart.xAxis.addLimitLine(ll)
        }
    }

    private fun setLegend(lineChart: LineChart, label: String?) {
        val l: Legend = lineChart.legend
        l.entries
        l.yEntrySpace = 10f
        l.isWordWrapEnabled = true
        val l1 = LegendEntry(
            "$label ${getString(R.string.min)}",
            Legend.LegendForm.CIRCLE,
            9f,
            5f,
            null,
            selectedCustomizeTrackItem.minColor
        )
        val l2 = LegendEntry(
            "$label ${getString(R.string.max)}",
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
                interpolateColor(
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
                    requireContext().resources.configuration.locales[0],
                    "%.1f km",
                    (value / 100f).roundToLong() / 10f
                )
            }
        }
    }

    private fun setOpenStreetMap(segment: Segment, summitToCompare: Summit?) {
        val summit = summitToCompare
        if (summit != null) {

            binding.osmap.overlays?.clear()
            binding.osmap.overlayManager?.clear()
            binding.changeMapType.setImageResource(R.drawable.baseline_more_vert_black_24dp)
            binding.changeMapType.setOnClickListener {
                showMapTypeSelectorDialog(
                    requireContext(), binding.osmap
                )
            }
            addDefaultSettings(requireContext(), binding.osmap, requireActivity())
            Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

            val hasPoints =
                summit.gpsTrack?.hasOnlyZeroCoordinates() == false || summit.latLng != null
            if (hasPoints) {
                if (summit.gpsTrack?.osMapRoute == null) {
                    summit.gpsTrack?.addGpsTrack(
                        binding.osmap, selectedCustomizeTrackItem, rootView = binding.root
                    )
                }
                showSinglePoints(summit)
                binding.osmap.post {
                    summit.gpsTrack?.let {
                        OpenStreetMapUtils.calculateBoundingBox(
                            binding.osmap, it.trackGeoPoints
                        )
                    }
                }
            }
            val entries = segment.segmentEntries
            if (entries.isNotEmpty()) {
                firstStartPoint = GeoPoint(
                    entries.first().startPositionLatitude, entries.first().startPositionLongitude
                )
                firstEndPoint =
                    GeoPoint(
                        entries.first().endPositionLatitude,
                        entries.first().endPositionLongitude
                    )
                addMarker(
                    binding.osmap, firstStartPoint, ResourcesCompat.getDrawable(
                        requireContext().resources,
                        R.drawable.ic_filled_location_lightbrown_48,
                        null
                    )
                )
                addMarker(
                    binding.osmap, firstEndPoint, ResourcesCompat.getDrawable(
                        requireContext().resources, R.drawable.ic_filled_location_darkbrown_48, null
                    )
                )
            }
        }
    }

    private fun showSinglePoints(summit: Summit) {
        val trackGeoPoints = summit.gpsTrack?.trackGeoPoints
        if (!trackGeoPoints.isNullOrEmpty()) {
            binding.osmap.overlays.remove(pointOverlay)
            val filteredPoints = ArrayList<IGeoPoint>()
            for ((i, point) in trackGeoPoints.withIndex()) {
                if (i > startPointId - 30 && i < endPointId + 30) {
                    filteredPoints.add(
                        LabelledGeoPoint(
                            point.latitude, point.longitude, i.toString()
                        )
                    )
                }
            }
            val pt = SimplePointTheme(filteredPoints, true)

            val textStyle = Paint()
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
                .setRadius(10f).setIsClickable(true).setCellSize(20).setMinZoomShowLabels(19)
                .setTextStyle(textStyle)
            pointOverlay = SimpleFastPointOverlay(pt, opt)
            pointOverlay?.setOnClickListener { _: SimpleFastPointOverlay.PointAdapter, i: Int ->
                val indexOfTrackPoint = (filteredPoints[i] as LabelledGeoPoint).label.toInt()
                if (startSelected) {
                    setStartMarker(indexOfTrackPoint)
                } else {
                    setEndMarker(indexOfTrackPoint)
                }
            }
            binding.osmap.overlays?.add(pointOverlay)
        }
    }

    private fun setEndMarker(i: Int) {
        if (i >= startPointId) {
            endPointId = i
            summitToCompare?.let { updateMapAndChart(it) }
        } else {
            Toast.makeText(
                requireContext(), getString(R.string.stop_before_start), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setStartMarker(i: Int, updateView: Boolean = true) {
        if (i <= endPointId) {
            startPointId = i
            summitToCompare?.let { updateMapAndChart(it, updateView) }
        } else {
            Toast.makeText(
                requireContext(), getString(R.string.start_after_stop), Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateMapAndChart(summit: Summit, updateView: Boolean = true) {
        if (updateView) {
            drawChart(summit)
            showSinglePoints(summit)
            setTextView(summit)
        }
        binding.osmap.overlays.remove(startMarker)
        binding.osmap.overlays.remove(stopMarker)
        startMarker = addMarker(
            binding.osmap,
            summit.gpsTrack?.trackGeoPoints?.get(startPointId),
            ResourcesCompat.getDrawable(
                requireContext().resources, R.drawable.ic_filled_location_green_48, null
            )
        )
        stopMarker = addMarker(
            binding.osmap,
            summit.gpsTrack?.trackGeoPoints?.get(endPointId),
            ResourcesCompat.getDrawable(
                requireContext().resources, R.drawable.ic_filled_location_red_48, null
            )
        )
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

    companion object {
        fun getInstance(
            segmentId: Long, segmentsViewAdapter: SegmentsViewAdapter?, segmentEntryId: Long?
        ): AddSegmentEntryFragment {
            val fragment = AddSegmentEntryFragment()
            fragment.segmentId = segmentId
            if (segmentEntryId != null) {
                fragment.segmentEntryId = segmentEntryId
                fragment.isUpdate = true
            }
            fragment.segmentsViewAdapter = segmentsViewAdapter
            return fragment
        }

    }

}