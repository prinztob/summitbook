package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.graphics.Paint
import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling
import de.drtobiasprinz.summitbook.ui.utils.GpsUtils
import de.drtobiasprinz.summitbook.ui.utils.TrackUtils
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Main composable screen for adding/editing segment entries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSegmentEntryScreen(
    segmentId: Long,
    segmentEntryId: Long?,
    segments: List<Segment>,
    summits: List<Summit>,
    onSaveSegmentEntry: (Boolean, SegmentEntry) -> Job,
    onCancel: () -> Unit
) {
    var uiState by remember { mutableStateOf(AddSegmentEntryUiState()) }
    var trackPoints by remember {
        mutableStateOf<List<Pair<TrackPoint, ExtensionFromYaml>>>(
            emptyList()
        )
    }
    val scope = rememberCoroutineScope()

    // Process data when it changes
    LaunchedEffect(segments, summits, segmentId, segmentEntryId) {
        val segment = segments.firstOrNull { it.segmentDetails.segmentDetailsId == segmentId }

        if (segment != null) {
            val segmentEntry = if (segmentEntryId != null) {
                segment.segmentEntries.firstOrNull { it.entryId == segmentEntryId }
            } else {
                null
            }

            // Get relevant summits for comparison
            val (relevantSummits, showWarning) = if (segment.segmentEntries.isNotEmpty()) {
                val startPoint = GeoPoint(
                    segment.segmentEntries.first().startPositionLatitude,
                    segment.segmentEntries.first().startPositionLongitude
                )
                val endPoint = GeoPoint(
                    segment.segmentEntries.first().endPositionLatitude,
                    segment.segmentEntries.first().endPositionLongitude
                )

                val filteredSummits = summits.filter { summit ->
                    summit.hasGpsTrack() &&
                            summit.trackBoundingBox?.contains(startPoint) == true &&
                            summit.trackBoundingBox?.contains(endPoint) == true
                }

                // Return the filtered summits and a flag indicating if we should show a warning
                if (filteredSummits.isEmpty()) {
                    // Show warning when no summits match the bounding box criteria
                    Pair(summits.filter { it.hasGpsTrack() }.sortedByDescending { it.date }, true)
                } else {
                    Pair(filteredSummits, false)
                }
            } else {
                Pair(summits.filter { it.hasGpsTrack() }.sortedByDescending { it.date }, false)
            }

            val currentSummit = if (segmentEntry != null) {
                relevantSummits.firstOrNull { it.activityId == segmentEntry.activityId }
            } else {
                null
            }
            val startPointId = segmentEntry?.startPositionInTrack ?: 0
            val endPointId = segmentEntry?.endPositionInTrack ?: 0

            // Load track points for the current summit if we're editing
            if (currentSummit != null && trackPoints.isEmpty()) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        currentSummit.setGpsTrack(useSimplifiedTrack = false)
                        trackPoints = currentSummit.gpsTrack?.trackPoints ?: emptyList()
                    }
                }
            }

            uiState = uiState.copy(
                segment = segment,
                segmentEntry = segmentEntry,
                relevantSummits = relevantSummits,
                currentSummit = currentSummit,
                trackPoints = trackPoints,
                isLoading = false,
                isUpdate = segmentEntry != null,
                startPointId = startPointId,
                endPointId = endPointId,
                showFilteredSummitsWarning = showWarning
            )

        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isUpdate) {
                            stringResource(R.string.update_segment_entry)
                        } else {
                            stringResource(R.string.add_segment_entry)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AddSegmentEntryContent(
                uiState = uiState,
                trackPoints = trackPoints,
                onSummitSelected = { summit ->
                    uiState = uiState.copy(currentSummit = summit)
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            summit.setGpsTrack(useSimplifiedTrack = false)
                            trackPoints = summit.gpsTrack?.trackPoints ?: emptyList()
                        }
                        // Update the UI state with the new track points
                        uiState = uiState.copy(trackPoints = trackPoints)
                        
                        val guessedIds =
                            guessStartAndEndPoint(uiState.segment?.segmentEntries, trackPoints)
                        if (guessedIds != null) {
                            uiState = uiState.copy(startPointId = guessedIds.first)
                            uiState = uiState.copy(endPointId = guessedIds.second)
                        }
                    }
                },
                onStartPointSelected = {
                    uiState = uiState.copy(startPointId = it)
                },
                onEndPointSelected = {
                    uiState = uiState.copy(endPointId = it)
                },
                onSave = { entry ->
                    onSaveSegmentEntry(
                        uiState.isUpdate,
                        entry
                    ).invokeOnCompletion { onCancel() }
                },
                onCancel = onCancel,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

/**
 * Main content of the add segment entry screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSegmentEntryContent(
    uiState: AddSegmentEntryUiState,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    onSummitSelected: (Summit) -> Unit,
    onSave: (SegmentEntry) -> Unit,
    onCancel: () -> Unit,
    onStartPointSelected: (Int) -> Unit,
    onEndPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSummitName by remember { mutableStateOf("") }
    // Initialize selected summit name when editing an existing entry
    LaunchedEffect(uiState.currentSummit, uiState.isUpdate) {
        if (uiState.isUpdate && uiState.currentSummit != null && selectedSummitName.isEmpty()) {
            selectedSummitName = "${uiState.currentSummit.getDateAsString()} ${uiState.currentSummit.name}"
        }
    }
    var expanded by remember { mutableStateOf(false) }
    var mapVisible by remember { mutableStateOf(true) }
    var chartVisible by remember { mutableStateOf(true) }
    var startSelected by remember { mutableStateOf(true) }

    val summitSuggestions = uiState.relevantSummits.map { summit ->
        "${summit.getDateAsString()} ${summit.name}"
    }.toMutableList().apply { add(0, stringResource(R.string.none)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val none = stringResource(R.string.none)
        // Summit selection dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                readOnly = true,
                value = selectedSummitName,
                onValueChange = {},
                label = { Text(stringResource(R.string.select_summit)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                summitSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            selectedSummitName = suggestion
                            expanded = false

                            if (suggestion != none) {
                                val selectedSummit = uiState.relevantSummits.find {
                                    "${it.getDateAsString()} ${it.name}" == suggestion
                                }
                                selectedSummit?.let { onSummitSelected(it) }
                            }
                        }
                    )
                }
            }
        }

        // Warning message when filtered summits list was empty
        if (uiState.showFilteredSummitsWarning) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_info_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.no_summits_in_bounding_box),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.cancelButtonText))
            }

            Button(
                onClick = {
                    // Create segment entry and save
                    uiState.currentSummit?.let { summit ->
                        val trackPoints = summit.gpsTrack?.trackPoints
                        if (trackPoints != null && trackPoints.isNotEmpty() &&
                            uiState.startPointId < trackPoints.size && uiState.endPointId < trackPoints.size
                        ) {

                            val startTrackPoint = trackPoints[uiState.startPointId]
                            val endTrackPoint = trackPoints[uiState.endPointId]
                            val selectedTrackPoints = trackPoints.subList(
                                uiState.startPointId.coerceAtMost(uiState.endPointId),
                                (uiState.endPointId.coerceAtLeast(uiState.startPointId)) + 1
                            )

                            val averageHeartRate = if (selectedTrackPoints.isNotEmpty()) {
                                selectedTrackPoints.sumOf {
                                    it.second.hr ?: 0
                                } / selectedTrackPoints.size
                            } else 0

                            val averagePower = if (selectedTrackPoints.isNotEmpty()) {
                                selectedTrackPoints.sumOf {
                                    it.second.power ?: 0
                                } / selectedTrackPoints.size
                            } else 0

                            val pointsOnlyWithMaximalValues =
                                TrackUtils.keepOnlyMaximalValues(selectedTrackPoints)
                            val heightMeterResult =
                                TrackUtils.removeDeltasSmallerAs(10, pointsOnlyWithMaximalValues)

                            val duration =
                                ((endTrackPoint.first.time.millis - startTrackPoint.first.time.millis).toDouble() / 60000.0).coerceAtLeast(
                                    0.0
                                )
                            val distance = (((endTrackPoint.second.distance
                                ?: 0.0) - (startTrackPoint.second.distance
                                ?: 0.0)) / 1000.0).coerceAtLeast(0.0)

                            val entry = SegmentEntry(
                                entryId = uiState.segmentEntry?.entryId ?: 0,
                                segmentId = uiState.segment?.segmentDetails?.segmentDetailsId ?: 0,
                                date = summit.date,
                                activityId = summit.activityId,
                                startPositionInTrack = uiState.startPointId,
                                startPositionLatitude = startTrackPoint.first.latitude,
                                startPositionLongitude = startTrackPoint.first.longitude,
                                endPositionInTrack = uiState.endPointId,
                                endPositionLatitude = endTrackPoint.first.latitude,
                                endPositionLongitude = endTrackPoint.first.longitude,
                                duration = duration,
                                kilometers = distance,
                                heightMetersUp = heightMeterResult.second.toInt(),
                                heightMetersDown = heightMeterResult.third.toInt(),
                                averageHeartRate = averageHeartRate,
                                averagePower = averagePower
                            )

                            onSave(entry)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = uiState.currentSummit != null
            ) {
                Text(if (uiState.isUpdate) stringResource(R.string.update) else stringResource(R.string.saveButtonText))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistics display (when summit is selected)
        uiState.currentSummit?.let { summit ->
            val trackPoints = summit.gpsTrack?.trackPoints
            if (trackPoints != null && trackPoints.isNotEmpty() &&
                uiState.startPointId < trackPoints.size && uiState.endPointId < trackPoints.size
            ) {

                val startTrackPoint = trackPoints[uiState.startPointId]
                val endTrackPoint = trackPoints[uiState.endPointId]
                val selectedTrackPoints = trackPoints.subList(
                    uiState.startPointId.coerceAtMost(uiState.endPointId),
                    (uiState.endPointId.coerceAtLeast(uiState.startPointId)) + 1
                )

                val averageHeartRate = if (selectedTrackPoints.isNotEmpty()) {
                    selectedTrackPoints.sumOf { it.second.hr ?: 0 } / selectedTrackPoints.size
                } else 0

                val averagePower = if (selectedTrackPoints.isNotEmpty()) {
                    selectedTrackPoints.sumOf { it.second.power ?: 0 } / selectedTrackPoints.size
                } else 0

                val pointsOnlyWithMaximalValues =
                    TrackUtils.keepOnlyMaximalValues(selectedTrackPoints)
                val heightMeterResult =
                    TrackUtils.removeDeltasSmallerAs(10, pointsOnlyWithMaximalValues)

                val duration =
                    ((endTrackPoint.first.time.millis - startTrackPoint.first.time.millis).toDouble() / 60000.0).coerceAtLeast(
                        0.0
                    )
                val distance =
                    (((endTrackPoint.second.distance ?: 0.0) - (startTrackPoint.second.distance
                        ?: 0.0)) / 1000.0).coerceAtLeast(0.0)

                AddSegmentStatsCard(
                    date = summit.getDateAsString() ?: "",
                    heightMeterUp = heightMeterResult.second.toInt(),
                    heightMeterDown = heightMeterResult.third.toInt(),
                    kilometers = distance,
                    averageHeartRate = averageHeartRate,
                    duration = duration,
                    averagePower = averagePower
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle switches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = mapVisible,
                    onCheckedChange = { mapVisible = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.map))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = chartVisible,
                    onCheckedChange = { chartVisible = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.chart))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start/Stop selection toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.start),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (startSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (startSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = startSelected,
                onCheckedChange = { startSelected = it }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.stop),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (!startSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (!startSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Map section
        if (mapVisible && uiState.currentSummit != null) {
            AddSegmentMapSection(
                trackPoints = trackPoints,
                startPointId = uiState.startPointId,
                endPointId = uiState.endPointId,
                startSelected = startSelected,
                onStartPointSelected = onStartPointSelected,
                onEndPointSelected = onEndPointSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart section
        if (chartVisible && uiState.currentSummit != null) {
            AddSegmentChartSection(
                trackPoints = trackPoints,
                startPointId = uiState.startPointId,
                endPointId = uiState.endPointId,
                startSelected = startSelected,
                onStartPointSelected = onStartPointSelected,
                onEndPointSelected = onEndPointSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Card displaying segment statistics for add segment entry
 */
@Composable
fun AddSegmentStatsCard(
    date: String,
    heightMeterUp: Int,
    heightMeterDown: Int,
    kilometers: Double,
    averageHeartRate: Int,
    duration: Double,
    averagePower: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_today_black_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AddSegmentStatItem(
                    icon = R.drawable.baseline_trending_up_black_24dp,
                    text = "$heightMeterUp/$heightMeterDown ${stringResource(R.string.hm)}"
                )

                AddSegmentStatItem(
                    icon = R.drawable.outline_distance_24,
                    text = String.format(
                        Locale.getDefault(),
                        "%.1f %s",
                        kilometers,
                        stringResource(R.string.km)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AddSegmentStatItem(
                    icon = R.drawable.ic_baseline_monitor_heart_24,
                    text = "$averageHeartRate ${stringResource(R.string.bpm)}"
                )

                AddSegmentStatItem(
                    icon = R.drawable.ic_baseline_timer_24,
                    text = String.format(
                        Locale.getDefault(),
                        "%.1f %s",
                        duration,
                        stringResource(R.string.min)
                    )
                )

                AddSegmentStatItem(
                    icon = R.drawable.ic_baseline_power_24,
                    text = "$averagePower ${stringResource(R.string.watt)}"
                )
            }
        }
    }
}

/**
 * Individual statistic item for add segment entry
 */
@Composable
fun AddSegmentStatItem(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Map section for selecting start/end points
 */
@Composable
fun AddSegmentMapSection(
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    startPointId: Int,
    endPointId: Int,
    startSelected: Boolean,
    onStartPointSelected: (Int) -> Unit,
    onEndPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView: CustomMapViewToAllowScrolling? by remember { mutableStateOf(null) }
    var isMapReady by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(300.dp)
            .clipToBounds()
    ) {
        AndroidView(
            factory = { ctx ->
                CustomMapViewToAllowScrolling(ctx).apply {
                    mapView = this
                    setTileProvider()
                    addDefaultSettings()
                    isMapReady = true
                }
            },
            update = { view ->
                if (isMapReady) {
                    updateAddSegmentMapContent(
                        view,
                        trackPoints,
                        startPointId,
                        endPointId,
                        startSelected,
                        onStartPointSelected,
                        onEndPointSelected,
                        context
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Map type selector button
        IconButton(
            onClick = {
                mapView?.showMapTypeSelectorDialog()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(50)
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_more_vert_black_24dp),
                contentDescription = stringResource(R.string.map_type)
            )
        }
    }
}

/**
 * Update map content with track and markers for add segment entry
 */
private fun updateAddSegmentMapContent(
    mapView: MapView,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    startPointId: Int,
    endPointId: Int,
    startSelected: Boolean,
    onStartPointSelected: (Int) -> Unit,
    onEndPointSelected: (Int) -> Unit,
    context: Context
) {
    // Clear existing overlays
    mapView.overlays?.clear()
    mapView.overlayManager?.clear()
    if (trackPoints.isNotEmpty()) {
        // Add start marker
        if (startPointId < trackPoints.size) {
            val startPoint = trackPoints[startPointId].first
            addMarker(
                mapView,
                GeoPoint(startPoint.latitude, startPoint.longitude),
                R.drawable.ic_filled_location_green_48,
                context
            )
        }

        // Add end marker
        if (endPointId < trackPoints.size) {
            val endPoint = trackPoints[endPointId].first
            addMarker(
                mapView,
                GeoPoint(endPoint.latitude, endPoint.longitude),
                R.drawable.ic_filled_location_red_48,
                context
            )
        }

        // Add special marker for currently selected point based on toggle switch
        val selectedPointId = if (startSelected) startPointId else endPointId
        if (selectedPointId < trackPoints.size) {
            val selectedPoint = trackPoints[selectedPointId].first
            val selectedMarkerIcon =
                if (startSelected) R.drawable.ic_outline_location_green_48 else R.drawable.ic_outline_location_red_48
            addMarker(
                mapView,
                GeoPoint(selectedPoint.latitude, selectedPoint.longitude),
                selectedMarkerIcon,
                context
            )
        }

        // Draw GPX track
        drawGpxTrack(mapView, trackPoints, startPointId, endPointId)

        // Add point overlay for selection
        addPointOverlay(
            mapView,
            trackPoints,
            startPointId,
            endPointId,
            startSelected,
            onStartPointSelected,
            onEndPointSelected
        )
    }

    mapView.invalidate()
}

/**
 * Draw GPX track on the map
 */
private fun drawGpxTrack(
    mapView: MapView,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    startPointId: Int,
    endPointId: Int
) {
    try {
        val osMapRoute = Polyline(mapView)
        val paintBorder = Paint()
        paintBorder.strokeWidth = 20F

        val usedTrackPoints = trackPoints.filterIndexed { index, _ ->
            index in startPointId..endPointId
        }

        if (usedTrackPoints.size > 1) {
            val geoPoints = usedTrackPoints.map { GeoPoint(it.first.latitude, it.first.longitude) }
            addColorToTrack(osMapRoute, usedTrackPoints, paintBorder)
            osMapRoute.setPoints(geoPoints)
            mapView.overlays.add(osMapRoute)

            // Zoom to the bounding box of the track segment
            val boundingBox = BoundingBox.fromGeoPoints(geoPoints)
            mapView.post {
                mapView.zoomToBoundingBox(boundingBox, false, 50)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Add colored track to polyline
 */
private fun addColorToTrack(
    osMapRoute: Polyline,
    usedTrackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    paintBorder: Paint,
    trackColor: TrackColor = TrackColor.Elevation
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

private fun guessStartAndEndPoint(
    entries: List<SegmentEntry>?,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
): Pair<Int, Int>? {
    if (entries != null && entries.isNotEmpty()) {
        val firstStartPoint = GeoPoint(
            entries.first().startPositionLatitude,
            entries.first().startPositionLongitude
        )
        val firstEndPoint =
            GeoPoint(
                entries.first().endPositionLatitude,
                entries.first().endPositionLongitude
            )
        val startPoint = trackPoints.firstOrNull {
            abs(
                GpsUtils.getDistance(
                    GeoPoint(it.first.latitude, it.first.longitude),
                    firstStartPoint
                )
            ) < 10
        }
        val endPoint = trackPoints.lastOrNull {
            abs(
                GpsUtils.getDistance(
                    GeoPoint(it.first.latitude, it.first.longitude),
                    firstEndPoint
                )
            ) < 10
        }
        if (startPoint != null && endPoint != null) {
            val startPointId =
                trackPoints.indexOf(startPoint)
            val endPointId = trackPoints.indexOf(endPoint)
            Log.i(
                "AddSegmentEntryScreen",
                "Update startPointId: $startPointId and endPointId: $endPointId"
            )
            return Pair(startPointId, endPointId)
        }

    }
    return null
}

/**
 * Add point overlay for interactive point selection
 */
private fun addPointOverlay(
    mapView: MapView,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    startPointId: Int,
    endPointId: Int,
    startSelected: Boolean,
    onStartPointSelected: (Int) -> Unit,
    onEndPointSelected: (Int) -> Unit
) {
    val filteredPoints = ArrayList<org.osmdroid.api.IGeoPoint>()
    val startIndex = (startPointId - 30).coerceAtLeast(0)
    val endIndex = (endPointId + 30).coerceAtMost(trackPoints.size - 1)

    for (i in startIndex..endIndex) {
        val point = trackPoints[i].first
        filteredPoints.add(LabelledGeoPoint(point.latitude, point.longitude, i.toString()))
    }

    val pt = SimplePointTheme(filteredPoints, true)

    val textStyle = Paint().apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 55f
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 5f
    }

    val opt = SimpleFastPointOverlayOptions.getDefaultStyle()
        .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.NO_OPTIMIZATION)
        .setRadius(10f)
        .setIsClickable(true)
        .setCellSize(20)
        .setMinZoomShowLabels(19)
        .setTextStyle(textStyle)

    val pointOverlay = SimpleFastPointOverlay(pt, opt)
    pointOverlay.setOnClickListener { _: SimpleFastPointOverlay.PointAdapter, i: Int ->
        val indexOfTrackPoint = (filteredPoints[i] as LabelledGeoPoint).label.toInt()
        val selectedPoint = trackPoints[indexOfTrackPoint].first
        val geoPoint = GeoPoint(selectedPoint.latitude, selectedPoint.longitude)
        if (startSelected) {
            onStartPointSelected(indexOfTrackPoint)
        } else {
            onEndPointSelected(indexOfTrackPoint)
        }
        addMarker(
            mapView,
            geoPoint,
            if (startSelected) R.drawable.ic_filled_location_lightbrown_48 else R.drawable.ic_filled_location_darkbrown_48,
            mapView.context
        )
        mapView.invalidate()
    }

    mapView.overlays?.add(pointOverlay)
}

/**
 * Add a marker to the map
 */
private fun addMarker(
    mapView: MapView,
    point: GeoPoint,
    iconResId: Int,
    context: Context
) {
    try {
        val marker = Marker(mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = androidx.core.content.res.ResourcesCompat.getDrawable(
            context.resources, iconResId, null
        )
        mapView.overlays.add(marker)
        marker.setOnMarkerClickListener { _, _ ->
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Chart section for displaying track profile
 */
@Composable
fun AddSegmentChartSection(
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    startPointId: Int,
    endPointId: Int,
    startSelected: Boolean,
    onStartPointSelected: (Int) -> Unit,
    onEndPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                // Configure chart
                setupAddSegmentChart(
                    this,
                    trackPoints,
                    startPointId,
                    endPointId,
                    startSelected,
                    onStartPointSelected,
                    onEndPointSelected
                )
            }
        },
        update = { chart ->
            // Update chart when data changes
            setupAddSegmentChart(
                chart,
                trackPoints,
                startPointId,
                endPointId,
                startSelected,
                onStartPointSelected,
                onEndPointSelected
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    )
}

/**
 * Setup the line chart with data for add segment entry
 */
private fun setupAddSegmentChart(
    lineChart: LineChart,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    startPointId: Int,
    endPointId: Int,
    startSelected: Boolean,
    onStartPointSelected: (Int) -> Unit,
    onEndPointSelected: (Int) -> Unit
) {
    if (trackPoints.isNotEmpty()) {
        lineChart.clear()
        lineChart.xAxis.removeAllLimitLines()
        setXAxis(lineChart)

        val dataSets: MutableList<ILineDataSet> = ArrayList()
        val lineChartEntries = GpsTrack.getTrackGraph(trackPoints, TrackColor.Elevation.f)
        val label = "Elevation" // TODO: Get proper label from resources

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.textColor = android.graphics.Color.BLACK
        leftAxis.setDrawGridLines(true)
        leftAxis.isGranularityEnabled = true

        val dataSet = LineDataSet(lineChartEntries, label)
        setGraphView(dataSet)
        setColors(lineChartEntries, dataSet)
        dataSets.add(dataSet)
        lineChart.data = LineData(dataSets)
        setLegendForSegment(lineChart)

        // Add chart value selection listener
        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight?) {
                // Get the track point from the entry data
                @Suppress("UNCHECKED_CAST")
                if (e.data is Pair<*, *>) {
                    val trackPoint = e.data as Pair<TrackPoint, ExtensionFromYaml>
                    val index = trackPoints.indexOf(trackPoint)
                    if (index != -1) {
                        // Update the start or end point
                        if (startSelected) {
                            onStartPointSelected(index)
                        } else {
                            onEndPointSelected(index)
                        }
                    }
                }
            }

            override fun onNothingSelected() {}
        })

        // Add vertical lines for segment start/end
        if (startPointId < trackPoints.size) {
            trackPoints[startPointId].second.distance?.toFloat()
                ?.let { drawVerticalLine(lineChart, it, android.graphics.Color.GREEN) }
        }
        if (endPointId < trackPoints.size) {
            trackPoints[endPointId].second.distance?.toFloat()
                ?.let { drawVerticalLine(lineChart, it, android.graphics.Color.RED) }
        }

        lineChart.invalidate()
    }
}

/**
 * Draw a vertical line on the chart
 */
private fun drawVerticalLine(lineChart: LineChart, distance: Float, color: Int) {
    val ll = LimitLine(distance)
    ll.lineColor = color
    ll.lineWidth = 2f
    lineChart.xAxis.addLimitLine(ll)
}

/**
 * Set up the legend for the chart
 */
private fun setLegendForSegment(
    lineChart: LineChart,
    trackColor: TrackColor = TrackColor.Elevation
) {
    val l: Legend = lineChart.legend
    l.yEntrySpace = 10f
    l.isWordWrapEnabled = true
    val l1 = LegendEntry(
        "Min",
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        trackColor.minColor
    )
    val l2 = LegendEntry(
        "Max",
        Legend.LegendForm.CIRCLE,
        9f,
        5f,
        null,
        trackColor.maxColor
    )
    l.setCustom(arrayOf(l1, l2))
    l.isEnabled = true
}

/**
 * Set colors for the chart data set
 */
private fun setColors(
    lineChartEntries: MutableList<Entry>,
    dataSet: LineDataSet,
    trackColor: TrackColor = TrackColor.Elevation
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

/**
 * Interpolate between two colors
 */
private fun interpolateColor(colorA: Int, colorB: Int, fraction: Float): Int {
    val a = (android.graphics.Color.alpha(colorA) +
            (android.graphics.Color.alpha(colorB) - android.graphics.Color.alpha(colorA)) * fraction).toInt()
    val r = (android.graphics.Color.red(colorA) +
            (android.graphics.Color.red(colorB) - android.graphics.Color.red(colorA)) * fraction).toInt()
    val g = (android.graphics.Color.green(colorA) +
            (android.graphics.Color.green(colorB) - android.graphics.Color.green(colorA)) * fraction).toInt()
    val b = (android.graphics.Color.blue(colorA) +
            (android.graphics.Color.blue(colorB) - android.graphics.Color.blue(colorA)) * fraction).toInt()
    return android.graphics.Color.argb(a, r, g, b)
}

/**
 * Set up the graph view properties
 */
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

/**
 * Set up the X axis formatting
 */
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

/**
 * UI state for the add segment entry screen
 */
data class AddSegmentEntryUiState(
    val segment: Segment? = null,
    val segmentEntry: SegmentEntry? = null,
    val relevantSummits: List<Summit> = emptyList(),
    val currentSummit: Summit? = null,
    val trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>> = emptyList(),
    val isLoading: Boolean = true,
    val isUpdate: Boolean = false,
    val startPointId: Int = 0,
    val endPointId: Int = 0,
    val showFilteredSummitsWarning: Boolean = false
)