package de.drtobiasprinz.summitbook.ui.compose

import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.GpsTrack.Companion.interpolateColor
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.models.TrackColor.Elevation
import de.drtobiasprinz.summitbook.models.TrackColor.None
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import kotlinx.coroutines.Job
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Jetpack Compose version of AddSegmentEntryFragment
 * Replaces the Fragment-based implementation with a modern Compose Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSegmentEntryDialogCompose(
    summits: List<Summit>,
    segments: List<Segment>,
    segmentId: Long,
    segmentEntryId: Long? = null,
    onDismiss: () -> Unit,
    onSaveSegmentEntry: (Boolean, SegmentEntry) -> Job
) {
    val context = LocalContext.current

    // State management
    var summitsToCompare by remember { mutableStateOf<List<Summit>>(emptyList()) }
    var segmentEntry by remember { mutableStateOf<SegmentEntry?>(null) }
    var segment by remember { mutableStateOf<Segment?>(null) }
    var summitToCompare by remember { mutableStateOf<Summit?>(null) }
    var isUpdate by remember { mutableStateOf(segmentEntryId != null) }

    // Map state
    var mapView by remember { mutableStateOf<CustomMapViewToAllowScrolling?>(null) }
    var startMarker by remember { mutableStateOf<Marker?>(null) }
    var stopMarker by remember { mutableStateOf<Marker?>(null) }
    var pointOverlay by remember { mutableStateOf<SimpleFastPointOverlay?>(null) }
    var firstStartPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var firstEndPoint by remember { mutableStateOf<GeoPoint?>(null) }

    // Chart state
    var lineChart by remember { mutableStateOf<LineChart?>(null) }
    var selectedCustomizeTrackItem by remember { mutableStateOf<TrackColor>(None) }

    // Selection state
    var startSelected by remember { mutableStateOf(true) }
    var startPointId by remember { mutableIntStateOf(0) }
    var endPointId by remember { mutableIntStateOf(0) }

    // UI state
    var showMap by remember { mutableStateOf(true) }
    var showChart by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    // Autocomplete state
    var summitNameToUse by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Statistics state
    var tourDate by remember { mutableStateOf("") }
    var kilometers by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var averageHr by remember { mutableStateOf("") }
    var averagePower by remember { mutableStateOf("") }
    var heightMeter by remember { mutableStateOf("") }


    // Load segment and summit data
    LaunchedEffect(segmentId, segmentEntryId, summits, segments) {
        segment = segments.firstOrNull { it.segmentDetails.segmentDetailsId == segmentId }
        segmentEntry = segment?.segmentEntries?.firstOrNull { it.entryId == segmentEntryId }

        segment?.let { seg ->
            val entries = seg.segmentEntries
            summitsToCompare = if (!entries.isNullOrEmpty()) {
                val startPoint = GeoPoint(
                    entries.first().startPositionLatitude, entries.first().startPositionLongitude
                )
                val endPoint =
                    GeoPoint(
                        entries.first().endPositionLatitude,
                        entries.first().endPositionLongitude
                    )
                summits.filter { it.hasGpsTrack() }
                    .sortedByDescending { it.date }
                    .filter {
                        it.trackBoundingBox?.contains(startPoint) == true && it.trackBoundingBox?.contains(
                            endPoint
                        ) == true
                    }
            } else {
                summits.filter { it.hasGpsTrack() }.sortedByDescending { it.date }
            }

            suggestions = getSummitsSuggestions(summits, seg, segmentEntry)
            summitToCompare = summits.firstOrNull { it.activityId == segmentEntry?.activityId }
            summitNameToUse = summitToCompare?.let { "${it.getDateAsString()} ${it.name}" } ?: ""

            if (summitToCompare != null) {
                setGpsTrack(summitToCompare)
                segmentEntry?.let { entry ->
                    startPointId = entry.startPositionInTrack
                    endPointId = entry.endPositionInTrack
                }
                updateStatistics(summitToCompare, startPointId, endPointId, context) { stats ->
                    tourDate = stats["tourDate"] ?: ""
                    kilometers = stats["kilometers"] ?: ""
                    duration = stats["duration"] ?: ""
                    averageHr = stats["averageHr"] ?: ""
                    averagePower = stats["averagePower"] ?: ""
                    heightMeter = stats["heightMeter"] ?: ""
                }
            }
        }
    }

    // Update statistics when selection changes
    LaunchedEffect(startPointId, endPointId, summitToCompare) {
        summitToCompare?.let { summit ->
            updateStatistics(summit, startPointId, endPointId, context) { stats ->
                tourDate = stats["tourDate"] ?: ""
                kilometers = stats["kilometers"] ?: ""
                duration = stats["duration"] ?: ""
                averageHr = stats["averageHr"] ?: ""
                averagePower = stats["averagePower"] ?: ""
                heightMeter = stats["heightMeter"] ?: ""
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f),
            //.fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            val deleteCancel = stringResource(R.string.delete_cancel)
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Title
                    Text(
                        text = if (isUpdate) stringResource(R.string.update) else stringResource(R.string.add_segment),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Summit selection dropdown
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = summitNameToUse,
                            onValueChange = { summitNameToUse = it },
                            label = { Text(stringResource(R.string.summit_name_hint)) },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            suggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        summitNameToUse = suggestion
                                        expanded = false
                                        val newlySelectedSummit = summitsToCompare.find {
                                            suggestion.startsWith("${it.getDateAsString()} ${it.name}")
                                        }
                                        if (newlySelectedSummit != null && newlySelectedSummit != summitToCompare) {
                                            summitToCompare = newlySelectedSummit
                                            setGpsTrack(newlySelectedSummit)
                                            val (newStartPointId, newEndPointId) = guessStartAndEndPoint(
                                                newlySelectedSummit,
                                                firstStartPoint,
                                                firstEndPoint,
                                                startPointId,
                                                endPointId
                                            )
                                            startPointId = newStartPointId
                                            endPointId = newEndPointId
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.start))
                            Switch(
                                checked = startSelected,
                                onCheckedChange = { startSelected = it },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Text(stringResource(R.string.stop))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.map))
                            Switch(
                                checked = showMap,
                                onCheckedChange = { showMap = it },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.chart))
                            Switch(
                                checked = showChart,
                                onCheckedChange = { showChart = it },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Map view
                    if (showMap) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    CustomMapViewToAllowScrolling(ctx).apply {
                                        mapView = this
                                        CustomMapViewToAllowScrolling.setOsmConfForTiles()
                                        if (PreferencesHelper.loadOnDeviceMaps() &&
                                            FileHelper.getOnDeviceMapFiles(ctx).isNotEmpty()
                                        ) {
                                            CustomMapViewToAllowScrolling.selectedItem =
                                                de.drtobiasprinz.summitbook.ui.MapProvider.HIKING
                                        } else if (FileHelper.getOnDeviceMbtilesFiles(ctx)
                                                .isNotEmpty()
                                        ) {
                                            CustomMapViewToAllowScrolling.selectedItem =
                                                de.drtobiasprinz.summitbook.ui.MapProvider.MBTILES
                                        }
                                        setTileProvider()
                                        addDefaultSettings()
                                        org.osmdroid.config.Configuration.getInstance().userAgentValue =
                                            BuildConfig.APPLICATION_ID
                                    }
                                },
                                update = { map ->
                                    summitToCompare?.let { summit ->
                                        setOpenStreetMap(
                                            map,
                                            segment,
                                            summit,
                                            startPointId,
                                            endPointId,
                                            context
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chart view
                    if (showChart) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    LineChart(ctx).apply {
                                        lineChart = this
                                    }
                                },
                                update = { chart ->
                                    summitToCompare?.let { summit ->
                                        drawChart(chart, summit, startPointId, endPointId, context)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Statistics display
                    StatisticsDisplay(
                        tourDate = tourDate,
                        kilometers = kilometers,
                        duration = duration,
                        averageHr = averageHr,
                        averagePower = averagePower,
                        heightMeter = heightMeter
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                Toast.makeText(
                                    context,
                                    deleteCancel,
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancelButtonText))
                        }

                        Button(
                            onClick = {
                                val localSegmentEntry = segmentEntry
                                val localSummitToCompare = summitToCompare
                                if (localSummitToCompare != null && localSegmentEntry != null) {
                                    onSaveSegmentEntry(
                                        isUpdate,
                                        localSegmentEntry
                                    ).invokeOnCompletion {
                                        onDismiss()
                                    }
                                } else {
                                    onDismiss()
                                }
                            },
                            enabled = summitToCompare != null && segmentEntry != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(
                                    0xFF4CAF50
                                )
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isUpdate) stringResource(R.string.update) else stringResource(R.string.saveButtonText))
                        }
                    }
                }

                // Loading overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsDisplay(
    tourDate: String,
    kilometers: String,
    duration: String,
    averageHr: String,
    averagePower: String,
    heightMeter: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatRow(stringResource(R.string.tour_date), tourDate)
        StatRow(stringResource(R.string.km), kilometers)
        StatRow(stringResource(R.string.min), duration)
        StatRow(stringResource(R.string.bpm), averageHr)
        StatRow(stringResource(R.string.watt), averagePower)
        StatRow(stringResource(R.string.hm), heightMeter)
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

// Helper functions

private fun getSummitsSuggestions(
    summits: List<Summit>?,
    segment: Segment?,
    segmentEntry: SegmentEntry?
): List<String> {
    val suggestions: MutableList<String> = mutableListOf("None")
    val summitsWithTrack =
        summits?.filter { it.hasGpsTrack() }?.sortedByDescending { it.date } ?: emptyList()
    val entries = segment?.segmentEntries
    val summitsToCompare = if (!entries.isNullOrEmpty()) {
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

private fun setGpsTrack(summit: Summit?) {
    if (summit != null && summit.hasGpsTrack()) {
        summit.setGpsTrack(useSimplifiedTrack = false)
        if (summit.gpsTrack?.trackPoints?.any {
                val value = Elevation.f(it)
                value != null && value != 0.0
            } == true) {
            // selectedCustomizeTrackItem = Elevation
        }
    }
}

private fun guessStartAndEndPoint(
    summit: Summit,
    firstStartPoint: GeoPoint?,
    firstEndPoint: GeoPoint?,
    startPointId: Int,
    endPointId: Int
): Pair<Int, Int> {
    if (firstStartPoint != null && firstEndPoint != null) {
        val startPoint = summit.gpsTrack?.trackGeoPoints?.firstOrNull {
            abs(GpsUtils.getDistance(it, firstStartPoint)) < 10
        }
        val endPoint = summit.gpsTrack?.trackGeoPoints?.lastOrNull {
            abs(GpsUtils.getDistance(it, firstEndPoint)) < 10
        }
        if (startPoint != null && endPoint != null) {
            val newStartPointId =
                summit.gpsTrack?.trackGeoPoints?.indexOf(startPoint) ?: startPointId
            val newEndPointId = summit.gpsTrack?.trackGeoPoints?.indexOf(endPoint) ?: endPointId
            return Pair(newStartPointId, newEndPointId)
        }
    }
    return Pair(startPointId, endPointId)
}

private fun updateStatistics(
    summit: Summit?,
    startPointId: Int,
    endPointId: Int,
    context: android.content.Context,
    onUpdate: (Map<String, String>) -> Unit
) {
    val allTrackPoints = summit?.gpsTrack?.trackPoints
    val stats = mutableMapOf<String, String>()

    if (!allTrackPoints.isNullOrEmpty() && startPointId < allTrackPoints.size && endPointId < allTrackPoints.size) {
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

        stats["tourDate"] = summit.getDateAsString() ?: ""
        stats["kilometers"] = String.format(
            context.resources.configuration.locales[0],
            "%.1f %s",
            distance,
            context.getString(R.string.km)
        )
        stats["duration"] = String.format(
            context.resources.configuration.locales[0],
            "%.1f %s",
            duration,
            context.getString(R.string.min)
        )
        stats["averageHr"] = String.format(
            context.resources.configuration.locales[0],
            "%s %s",
            averageHeartRate,
            context.getString(R.string.bpm)
        )
        stats["averagePower"] = String.format(
            context.resources.configuration.locales[0],
            "%s %s",
            averagePower,
            context.getString(R.string.watt)
        )
        stats["heightMeter"] = String.format(
            context.resources.configuration.locales[0],
            "%s/%s %s",
            heightMeterResult.second.roundToInt(),
            heightMeterResult.third.roundToInt(),
            context.getString(R.string.hm)
        )
    }

    onUpdate(stats)
}

private fun drawChart(
    lineChart: LineChart,
    summit: Summit,
    startPointId: Int,
    endPointId: Int,
    context: android.content.Context
) {
    val localGpsTrack = summit.gpsTrack
    if (localGpsTrack != null) {
        lineChart.clear()
        lineChart.xAxis.removeAllLimitLines()
        setXAxis(lineChart, context)
        val dataSets: MutableList<ILineDataSet> = ArrayList()
        val trackColor = None
        val lineChartEntries = localGpsTrack.getTrackGraph(trackColor.f)
        val label = context.getString(trackColor.labelId)

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.textColor = Color.BLACK
        leftAxis.setDrawGridLines(true)
        leftAxis.isGranularityEnabled = true

        val dataSet = LineDataSet(lineChartEntries, label)
        setGraphView(dataSet)
        setColors(lineChartEntries, dataSet, trackColor)
        dataSets.add(dataSet)
        lineChart.data = LineData(dataSets)
        lineChart.setOnChartValueSelectedListener(object :
            OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight?) {
                if (e.data is Pair<*, *>) {
                    val trackPoint = e.data as Pair<*, *>
                    val index = summit.gpsTrack?.trackPoints?.indexOf(trackPoint)
                    if (index != null) {
                        // Handle chart value selection
                    }
                }
            }

            override fun onNothingSelected() {}
        })
        setLegend(lineChart, label, trackColor)
        drawVerticalLine(lineChart, summit, startPointId, Color.GREEN)
        drawVerticalLine(lineChart, summit, endPointId, Color.RED)
    } else {
        lineChart.visibility = android.view.View.GONE
    }
}

private fun drawVerticalLine(lineChart: LineChart, summit: Summit, index: Int, color: Int) {
    val distance = summit.gpsTrack?.trackPoints?.get(index)?.second?.distance?.toFloat()
    if (distance != null) {
        val ll = LimitLine(distance)
        ll.lineColor = color
        ll.lineWidth = 2f
        lineChart.xAxis.addLimitLine(ll)
    }
}

private fun setLegend(lineChart: LineChart, label: String?, trackColor: TrackColor) {
    val l: Legend = lineChart.legend
    l.yEntrySpace = 10f
    l.isWordWrapEnabled = true
    val l1 = LegendEntry(
        "$label ${lineChart.context.getString(R.string.min)}",
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        trackColor.minColor
    )
    val l2 = LegendEntry(
        "$label ${lineChart.context.getString(R.string.max)}",
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        trackColor.maxColor
    )
    l.setCustom(arrayOf(l1, l2))
    l.isEnabled = true
}

private fun setColors(
    lineChartEntries: MutableList<Entry>,
    dataSet: LineDataSet,
    trackColor: TrackColor
) {
    val min = lineChartEntries.minByOrNull { it.y }?.y
    val max = lineChartEntries.maxByOrNull { it.y }?.y
    if (min != null && max != null) {
        val colors = lineChartEntries.map {
            val fraction = (it.y - min) / (max - min)
            interpolateColor(
                trackColor.minColor,
                trackColor.maxColor,
                fraction
            )
        }
        dataSet.colors = colors
    }
}

private fun setXAxis(lineChart: LineChart, context: android.content.Context) {
    val xAxis = lineChart.xAxis
    xAxis?.position = XAxis.XAxisPosition.BOTTOM
    xAxis?.valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return String.format(
                context.resources.configuration.locales[0],
                "%.1f km",
                (value / 100f).roundToLong() / 10f
            )
        }
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

private fun setOpenStreetMap(
    mapView: MapView,
    segment: Segment?,
    summitToCompare: Summit?,
    startPointId: Int,
    endPointId: Int,
    context: android.content.Context
) {
    val summit = summitToCompare
    if (summit != null) {
        mapView.overlays?.clear()
        mapView.overlayManager?.clear()

        val hasPoints =
            summit.gpsTrack?.hasOnlyZeroCoordinates() == false || summit.latLng != null
        if (hasPoints) {
            if (summit.gpsTrack?.osMapRoute == null) {
                summit.gpsTrack?.addGpsTrack(
                    mapView, None
                )
            }
            mapView.post {
                summit.gpsTrack?.let {
                    (mapView as? CustomMapViewToAllowScrolling)?.calculateBoundingBox(
                        it.trackGeoPoints
                    )
                }
            }
        }
    }
}