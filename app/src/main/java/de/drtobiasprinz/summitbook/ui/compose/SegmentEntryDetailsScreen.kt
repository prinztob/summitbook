package de.drtobiasprinz.summitbook.ui.compose

import android.graphics.Canvas
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
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
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Main screen for displaying segment entry details in Jetpack Compose
 */
@Composable
fun SegmentEntryDetailsScreen(
    segmentDetailsId: Long,
    viewModel: DatabaseViewModel = hiltViewModel()
) {
    val segments by viewModel.segmentsList.observeAsState()
    val summits by viewModel.summitsList.observeAsState()

    var selectedSegmentEntrySorter by remember { mutableStateOf(SegmentSortOptions.AverageVelocity) }
    var selectedCustomizeTrackItem by remember { mutableStateOf(TrackColor.Elevation) }
    var segmentEntryId by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(segmentDetailsId, segments, summits) {
        segments?.data?.let { segmentsList ->
            val segmentToUse =
                segmentsList.firstOrNull { it.segmentDetails.segmentDetailsId == segmentDetailsId }
            if (segmentToUse != null) {
                if (segmentEntryId == -1L) {
                    val sortedEntries =
                        selectedSegmentEntrySorter.sorter(segmentToUse.segmentEntries)
                    segmentEntryId = sortedEntries.firstOrNull()?.entryId ?: -1L
                }
            }
        }
    }

    segments?.data?.let { segmentsList ->
        val segmentToUse =
            segmentsList.firstOrNull { it.segmentDetails.segmentDetailsId == segmentDetailsId }
        if (segmentToUse != null) {
            summits?.data?.let { summitsList ->
                val relevantSummits = segmentToUse.segmentEntries.mapNotNull { entry ->
                    summitsList.firstOrNull { it.activityId == entry.activityId }
                }

                val segmentEntryToShow = if (segmentEntryId == -1L) {
                    val sortedEntries =
                        selectedSegmentEntrySorter.sorter(segmentToUse.segmentEntries)
                    sortedEntries.firstOrNull()
                } else {
                    segmentToUse.segmentEntries.firstOrNull { it.entryId == segmentEntryId }
                }

                val summitShown =
                    relevantSummits.firstOrNull { it.activityId == segmentEntryToShow?.activityId }

                if (segmentEntryToShow != null && summitShown != null) {
                    // Wrap content in a scrollable container
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        SegmentEntryDetailsContent(
                            segmentToUse = segmentToUse,
                            segmentEntryToShow = segmentEntryToShow,
                            summitShown = summitShown,
                            selectedSegmentEntrySorter = selectedSegmentEntrySorter,
                            onSorterChanged = { sorter ->
                                selectedSegmentEntrySorter = sorter
                            },
                            selectedCustomizeTrackItem = selectedCustomizeTrackItem,
                            onTrackColorChanged = { trackColor ->
                                selectedCustomizeTrackItem = trackColor
                            },
                            onSegmentEntrySelected = { entry ->
                                segmentEntryId = entry.entryId
                            },
                            onDeleteSegmentEntry = { entry ->
                                viewModel.deleteSegmentEntry(entry)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SegmentEntryDetailsContent(
    segmentToUse: Segment,
    segmentEntryToShow: SegmentEntry,
    summitShown: Summit,
    selectedSegmentEntrySorter: SegmentSortOptions,
    onSorterChanged: (SegmentSortOptions) -> Unit,
    selectedCustomizeTrackItem: TrackColor,
    onTrackColorChanged: (TrackColor) -> Unit,
    onSegmentEntrySelected: (SegmentEntry) -> Unit,
    onDeleteSegmentEntry: (SegmentEntry) -> Unit
) {
    var mapScreenshotFile by remember { mutableStateOf(Segment.getMapScreenshotFile(segmentToUse.segmentDetails.segmentDetailsId)) }
    var hasMapScreenshot by remember { mutableStateOf(mapScreenshotFile.exists()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with segment name and basic stats
        SegmentHeader(
            segmentDetails = segmentToUse.segmentDetails,
            segmentEntry = segmentEntryToShow
        )

        // Map view
        Spacer(modifier = Modifier.height(16.dp))
        MapSection(
            segmentToUse = segmentToUse,
            segmentEntryToShow = segmentEntryToShow,
            summitShown = summitShown,
            hasMapScreenshot = hasMapScreenshot,
            mapScreenshotFile = mapScreenshotFile,
            selectedCustomizeTrackItem = selectedCustomizeTrackItem,
            onMapScreenshotTaken = {
                hasMapScreenshot = true
                mapScreenshotFile =
                    Segment.getMapScreenshotFile(segmentToUse.segmentDetails.segmentDetailsId)
            }
        )

        // Chart
        Spacer(modifier = Modifier.height(16.dp))
        ChartSection(
            summitShown = summitShown,
            segmentEntry = segmentEntryToShow,
            selectedCustomizeTrackItem = selectedCustomizeTrackItem,
            onTrackColorChanged = onTrackColorChanged
        )

        // Segment entries list
        Spacer(modifier = Modifier.height(8.dp))
        SegmentEntriesList(
            segmentEntries = selectedSegmentEntrySorter.sorter(segmentToUse.segmentEntries),
            selectedEntry = segmentEntryToShow,
            onSegmentEntrySelected = onSegmentEntrySelected,
            onDeleteSegmentEntry = onDeleteSegmentEntry,
            selectedSorter = selectedSegmentEntrySorter,
            onSorterChanged = onSorterChanged
        )
    }
}

@Composable
fun SegmentHeader(
    segmentDetails: SegmentDetails,
    segmentEntry: SegmentEntry
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = segmentDetails.getDisplayNameWithLineBreak(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats grid
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        iconRes = R.drawable.baseline_trending_up_black_24dp,
                        text = "${segmentEntry.heightMetersUp}/${segmentEntry.heightMetersDown} ${
                            stringResource(
                                R.string.hm
                            )
                        }"
                    )
                    StatItem(
                        iconRes = R.drawable.outline_distance_24,
                        text = "${
                            String.format(
                                Locale.getDefault(),
                                "%.1f",
                                segmentEntry.kilometers
                            )
                        } ${stringResource(R.string.km)}"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        iconRes = R.drawable.ic_baseline_monitor_heart_24,
                        text = "${segmentEntry.averageHeartRate} ${stringResource(R.string.bpm)}"
                    )
                    StatItem(
                        iconRes = R.drawable.ic_baseline_timer_24,
                        text = "${
                            String.format(
                                Locale.getDefault(),
                                "%.1f",
                                segmentEntry.duration
                            )
                        } ${stringResource(R.string.min)}"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        iconRes = R.drawable.ic_baseline_power_24,
                        text = "${segmentEntry.averagePower} ${stringResource(R.string.watt)}"
                    )
                    Spacer(modifier = Modifier.width(0.dp)) // Empty spacer to maintain layout
                }
            }
        }
    }
}

@Composable
fun StatItem(iconRes: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun MapSection(
    segmentToUse: Segment,
    segmentEntryToShow: SegmentEntry,
    summitShown: Summit,
    hasMapScreenshot: Boolean,
    mapScreenshotFile: File,
    selectedCustomizeTrackItem: TrackColor,
    onMapScreenshotTaken: () -> Unit
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var showMap by remember { mutableStateOf(true) } // Always show map instead of screenshot

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Map header with controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.map),
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    // Refresh button
                    IconButton(onClick = {
                        mapView?.let {
                            takeScreenshotWhenTilesAreLoaded(
                                it,
                                segmentToUse.segmentDetails.segmentDetailsId,
                                onMapScreenshotTaken
                            )
                        }
                    }) {
                        Icon(
                            painterResource(id = R.drawable.baseline_refresh_24),
                            contentDescription = stringResource(R.string.update)
                        )
                    }

                    // Change map type button (three dots)
                    IconButton(onClick = {
                        mapView?.let { map ->
                            if (map is CustomMapViewToAllowScrolling) {
                                map.showMapTypeSelectorDialog()
                            }
                        }
                    }) {
                        Icon(
                            painterResource(id = R.drawable.baseline_more_vert_black_24dp),
                            contentDescription = stringResource(R.string.map_type)
                        )
                    }
                }
            }

            // Always show interactive map
            AndroidView(
                factory = { ctx ->
                    val map = CustomMapViewToAllowScrolling(ctx)
                    mapView = map
                    prepareMap(map)
                    drawMarker(map, segmentToUse.segmentEntries)
                    drawGpxTrackAndItsProfile(
                        map,
                        summitShown,
                        segmentEntryToShow,
                        selectedCustomizeTrackItem
                    )
                    map
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .height(250.dp)
            )

            // Take screenshot if needed
            LaunchedEffect(Unit) {
                if (!hasMapScreenshot) {
                    mapView?.let {
                        takeScreenshotWhenTilesAreLoaded(
                            it,
                            segmentToUse.segmentDetails.segmentDetailsId,
                            onMapScreenshotTaken
                        )
                    }
                }
            }
        }
    }
}

private fun prepareMap(mapView: CustomMapViewToAllowScrolling) {
    mapView.setTileProvider()
    mapView.addDefaultSettings()
    Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
}

private fun drawMarker(mMapView: MapView, segmentEntries: List<SegmentEntry>) {
    try {
        val startPoint = GeoPoint(
            segmentEntries.first().startPositionLatitude,
            segmentEntries.first().startPositionLongitude
        )
        val startMarker = Marker(mMapView)
        startMarker.position = startPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.icon = AppCompatResources.getDrawable(mMapView.context, R.drawable.ic_filled_location_lightbrown_48)
        mMapView.overlays.add(startMarker)

        val endPoint = GeoPoint(
            segmentEntries.first().endPositionLatitude,
            segmentEntries.first().endPositionLongitude
        )
        val endMarker = Marker(mMapView)
        endMarker.position = endPoint
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        endMarker.icon = AppCompatResources.getDrawable(mMapView.context, R.drawable.ic_filled_location_darkbrown_48)
        mMapView.overlays.add(endMarker)

        mMapView.invalidate()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun drawGpxTrackAndItsProfile(
    mMapView: MapView,
    localSummit: Summit,
    segmentEntry: SegmentEntry,
    trackColor: TrackColor
) {
    if (localSummit.hasGpsTrack()) {
        localSummit.setGpsTrack(useSimplifiedTrack = false)
        val gpsTrack = localSummit.gpsTrack
        if (gpsTrack != null) {
            val hasPoints = !gpsTrack.hasOnlyZeroCoordinates() || localSummit.latLng != null
            if (hasPoints) {
                gpsTrack.addGpsTrack(mMapView)
                putGpxTrackOnMap(mMapView, gpsTrack, segmentEntry, trackColor)
            }
        }
    }
}

private fun putGpxTrackOnMap(
    mMapView: MapView,
    gpxTrack: GpsTrack,
    segmentEntry: SegmentEntry,
    trackColor: TrackColor
) {
    try {
        val osMapRoute = Polyline(mMapView)
        val paintBorder = android.graphics.Paint()
        paintBorder.strokeWidth = 20F
        val geoPoints = gpxTrack.trackGeoPoints.filterIndexed { index, _ ->
            index in segmentEntry.startPositionInTrack..segmentEntry.endPositionInTrack
        }
        val trackPoints = gpxTrack.trackPoints.filterIndexed { index, _ ->
            index in segmentEntry.startPositionInTrack..segmentEntry.endPositionInTrack
        }
        if (geoPoints.size > 1) {
            addColorToTrack(osMapRoute, trackPoints, paintBorder, trackColor)
            osMapRoute.setPoints(geoPoints)
            mMapView.overlayManager?.add(osMapRoute)
            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
            mMapView.post {
                mMapView.zoomToBoundingBox(boundingBox, false, 50)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun addColorToTrack(
    osMapRoute: Polyline,
    usedTrackPoints: List<Pair<io.ticofab.androidgpxparser.parser.domain.TrackPoint, ExtensionFromYaml>>,
    paintBorder: android.graphics.Paint,
    trackColor: TrackColor
) {
    val values = usedTrackPoints.mapNotNull(trackColor.f)
    val minForColorCoding = (values.minOrNull() ?: 0.0).toFloat()
    val maxForColorCoding = (values.maxOrNull() ?: 0.0).toFloat()
    val pointsExists = usedTrackPoints.any { trackColor.f(it) != 0.0 }
    if (pointsExists) {
        val attributeColorList = GpsTrack.AttitudeColorListContinuos(
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

private fun takeScreenshotWhenTilesAreLoaded(
    mapView: MapView,
    segmentDetailsId: Long,
    onScreenshotTaken: () -> Unit,
    timeout: Long = TIMEOUT_TILES_LOADED
) {
    // In a real implementation, we would check if tiles are loaded
    // For now, we'll just take the screenshot immediately
    takeScreenshot(mapView, segmentDetailsId)
    onScreenshotTaken()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartSection(
    summitShown: Summit,
    segmentEntry: SegmentEntry,
    selectedCustomizeTrackItem: TrackColor,
    onTrackColorChanged: (TrackColor) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.chart),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Track color selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.customize_track),
                    style = MaterialTheme.typography.bodyMedium
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = stringResource(selectedCustomizeTrackItem.nameId),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.track_color)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TrackColor.entries.forEach { trackColor ->
                            DropdownMenuItem(
                                text = { Text(stringResource(trackColor.nameId)) },
                                onClick = {
                                    onTrackColorChanged(trackColor)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart view
            AndroidView(
                factory = { context ->
                    val chart = LineChart(context)
                    // Set up chart when it's created
                    chart
                },
                update = { chart ->
                    // Load GPS track if not already loaded
                    if (summitShown.hasGpsTrack() && summitShown.gpsTrack == null) {
                        summitShown.setGpsTrack(useSimplifiedTrack = false)
                    }
                    // Update chart when data changes
                    drawChart(
                        chart.context,
                        chart,
                        summitShown.gpsTrack,
                        segmentEntry,
                        selectedCustomizeTrackItem
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

private fun drawChart(
    context: android.content.Context,
    lineChart: LineChart,
    gpxTrack: GpsTrack?,
    segmentEntry: SegmentEntry,
    trackColor: TrackColor
) {
    if (gpxTrack != null) {
        lineChart.clear()
        lineChart.xAxis.removeAllLimitLines()
        setXAxis(lineChart)
        val dataSets: MutableList<ILineDataSet> = ArrayList()
        val lineChartEntries = gpxTrack.getTrackGraph(trackColor.f)

        val label = context.getString(trackColor.labelId)

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.textColor = android.graphics.Color.BLACK
        leftAxis.setDrawGridLines(true)
        leftAxis.isGranularityEnabled = true

        val dataSet = LineDataSet(lineChartEntries, label)
        setGraphView(dataSet)
        setColors(lineChartEntries, dataSet, trackColor)
        dataSets.add(dataSet)
        lineChart.data = LineData(dataSets)
        setLegend(context, lineChart, trackColor)
        if (segmentEntry.startPositionInTrack < gpxTrack.trackPoints.size) {
            gpxTrack.trackPoints[segmentEntry.startPositionInTrack].second.distance?.toFloat()
                ?.let { drawVerticalLine(lineChart, it, android.graphics.Color.GREEN) }
        }
        if (segmentEntry.endPositionInTrack < gpxTrack.trackPoints.size) {
            gpxTrack.trackPoints[segmentEntry.endPositionInTrack].second.distance?.toFloat()
                ?.let { drawVerticalLine(lineChart, it, android.graphics.Color.RED) }
        }

        // Refresh the chart
        lineChart.invalidate()
    }
}

private fun drawVerticalLine(lineChart: LineChart, distance: Float, color: Int) {
    val ll = LimitLine(distance)
    ll.lineColor = color
    ll.lineWidth = 2f
    lineChart.xAxis.addLimitLine(ll)
}

private fun setLegend(
    context: android.content.Context,
    lineChart: LineChart,
    trackColor: TrackColor
) {
    val l: Legend = lineChart.legend
    l.yEntrySpace = 10f
    l.isWordWrapEnabled = true
    val l1 = LegendEntry(
        context.getString(R.string.min),
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        trackColor.minColor
    )
    val l2 = LegendEntry(
        context.getString(R.string.max),
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
    lineChartEntries: List<Entry>,
    dataSet: LineDataSet,
    trackColor: TrackColor
) {
    val min = lineChartEntries.minByOrNull { it.y }?.y
    val max = lineChartEntries.maxByOrNull { it.y }?.y
    if (min != null && max != null) {
        val colors = lineChartEntries.map {
            val fraction = (it.y - min) / (max - min)
            GpsTrack.interpolateColor(
                trackColor.minColor,
                trackColor.maxColor,
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
                Locale.getDefault(),
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
    set1?.color = android.graphics.Color.RED
    set1?.setCircleColor(android.graphics.Color.RED)
    set1?.lineWidth = 5f
    set1?.circleRadius = 3f
    set1?.fillAlpha = 50
    set1?.fillColor = android.graphics.Color.RED
    set1?.setDrawCircleHole(false)
    set1?.highLightColor = android.graphics.Color.rgb(244, 117, 117)
    set1?.setDrawHorizontalHighlightIndicator(true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentEntriesList(
    segmentEntries: List<SegmentEntry>,
    selectedEntry: SegmentEntry,
    onSegmentEntrySelected: (SegmentEntry) -> Unit,
    onDeleteSegmentEntry: (SegmentEntry) -> Unit,
    selectedSorter: SegmentSortOptions,
    onSorterChanged: (SegmentSortOptions) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.segment_entries),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = stringResource(selectedSorter.stringId),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.sort_by)) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    SegmentSortOptions.entries.forEach { sorter ->
                        DropdownMenuItem(
                            text = { Text(stringResource(sorter.stringId)) },
                            onClick = {
                                onSorterChanged(sorter)
                                expanded = false
                            }
                        )
                    }
                }
            }
            // Display segment entries in a scrollable container with max height
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Fixed height with scrolling
                    .verticalScroll(rememberScrollState())
            ) {
                segmentEntries.forEach { entry ->
                    SegmentEntryItem(
                        entry = entry,
                        isSelected = entry.entryId == selectedEntry.entryId,
                        onClick = { onSegmentEntrySelected(entry) },
                        onDelete = { onDeleteSegmentEntry(entry) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun SegmentEntryItem(
    entry: SegmentEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 8.dp) else CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.getDateAsString() ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${
                            String.format(
                                Locale.getDefault(),
                                "%.1f",
                                entry.duration
                            )
                        } ${stringResource(R.string.min)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${
                            String.format(
                                Locale.getDefault(),
                                "%.1f",
                                entry.kilometers / entry.duration * 60
                            )
                        } ${stringResource(R.string.kmh)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${entry.averageHeartRate} ${stringResource(R.string.bpm)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${entry.averagePower} ${stringResource(R.string.watt)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_delete_black_24dp),
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    }
}

// Helper functions
private fun takeScreenshot(view: View, segmentDetailsId: Long) {
    try {
        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        val outputStream = FileOutputStream(Segment.getMapScreenshotFile(segmentDetailsId))
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, outputStream)
        outputStream.flush()
        outputStream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Constants
const val TIMEOUT_TILES_LOADED = 15000L
const val STEP_TILES_LOADED = 500L

// Segment sort options enum
enum class SegmentSortOptions(
    val stringId: Int, val sorter: (List<SegmentEntry>) -> List<SegmentEntry>
) {
    AverageVelocity(
        R.string.pace_hint,
        { it.sortedBy { entry -> entry.kilometers / entry.duration }.reversed() }),
    Date(
        R.string.date,
        { it.sortedBy { entry -> entry.getDateAsString() }.reversed() }),
    AverageHeartRate(
        R.string.bpm,
        { it.sortedBy { entry -> entry.averageHeartRate } }),
    Power(R.string.power, { it.sortedBy { entry -> entry.averagePower }.reversed() }),
}