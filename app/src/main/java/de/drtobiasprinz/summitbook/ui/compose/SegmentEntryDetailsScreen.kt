@file:Suppress("AssignedValueIsNeverRead")

package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.core.graphics.createBitmap
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.ExtensionFromYaml
import de.drtobiasprinz.summitbook.models.GpsTrack
import de.drtobiasprinz.summitbook.models.TrackColor
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.advancedpolyline.PolychromaticPaintList
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * Main composable screen for displaying segment entry details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentEntryDetailsScreen(
    segmentDetailsId: Long,
    segmentEntryId: Long = -1L,
    segments: List<Segment>,
    summits: List<Summit>,
    onNavigateBack: () -> Unit,
    onDeleteEntry: (SegmentEntry) -> Unit,
    onEditEntry: (SegmentEntry) -> Unit  // Add this parameter
) {
    var uiState by remember { mutableStateOf(SegmentEntryDetailsUiState()) }
    var trackPoints by remember {
        mutableStateOf<List<Pair<TrackPoint, ExtensionFromYaml>>>(
            emptyList()
        )
    }
    var showDeleteDialog by remember { mutableStateOf<SegmentEntry?>(null) }
    val scope = rememberCoroutineScope()

    // Process data when it changes
    LaunchedEffect(
        segments,
        summits,
        segmentDetailsId,
        segmentEntryId,
        uiState.selectedSortOption
    ) {
        val segmentToUse =
            segments.firstOrNull { it.segmentDetails.segmentDetailsId == segmentDetailsId }

        if (segmentToUse != null) {
            val currentEntryId = if (segmentEntryId == -1L) {
                // Get first entry based on current sorting
                segmentToUse.segmentEntries.let {
                    when (uiState.selectedSortOption) {
                        SegmentSortOptions.AverageVelocity -> it.sortedBy { entry -> entry.kilometers / entry.duration }
                            .reversed()

                        SegmentSortOptions.Date -> it.sortedBy { entry -> entry.getDateAsString() }
                            .reversed()

                        SegmentSortOptions.AverageHeartRate -> it.sortedBy { entry -> entry.averageHeartRate }
                            .reversed()

                        SegmentSortOptions.Power -> it.sortedBy { entry -> entry.averagePower }
                            .reversed()
                    }
                }.firstOrNull()?.entryId ?: -1L
            } else {
                segmentEntryId
            }

            val currentEntry =
                segmentToUse.segmentEntries.firstOrNull { it.entryId == currentEntryId }
            val relevantSummits = segmentToUse.segmentEntries.mapNotNull { entry ->
                summits.firstOrNull { it.activityId == entry.activityId }
            }
            val currentSummit =
                relevantSummits.firstOrNull { it.activityId == currentEntry?.activityId }

            // Load track points for the current summit
            if (currentSummit != null) {
                withContext(Dispatchers.IO) {
                    currentSummit.setGpsTrack(useSimplifiedTrack = false)
                    trackPoints = currentSummit.gpsTrack?.trackPoints ?: emptyList()
                }
            }
            uiState = uiState.copy(
                segment = segmentToUse,
                currentEntry = currentEntry,
                relevantSummits = relevantSummits,
                currentSummit = currentSummit,
                trackPoints = trackPoints,
                isLoading = false
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.segment?.segmentDetails?.getDisplayName() ?: "",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
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
            SegmentEntryDetailsContent(
                uiState = uiState,
                onDeleteEntry = { entry -> showDeleteDialog = entry },
                onEditEntry = onEditEntry,  // Pass the edit function
                onSortOptionSelected = { option ->
                    uiState = uiState.copy(selectedSortOption = option)
                },
                onEntrySelected = { entry ->
                    uiState = uiState.copy(currentEntry = entry, isLoading = true)
                    val currentSummit =
                        uiState.relevantSummits.firstOrNull { it.activityId == entry.activityId }
                    uiState = uiState.copy(currentSummit = currentSummit)

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            currentSummit?.setGpsTrack(
                                useSimplifiedTrack = false,
                                updateTrack = true
                            )
                        }
                        uiState = uiState.copy(
                            trackPoints = currentSummit?.gpsTrack?.trackPoints ?: emptyList(),
                            isLoading = false
                        )
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }

    // Handle delete confirmation dialog
    showDeleteDialog?.let { entry ->
        SegmentEntryDeleteDialog(
            entry = entry,
            segmentName = uiState.segment?.segmentDetails?.getDisplayName() ?: "",
            onConfirm = {
                // Notify the activity to handle the deletion
                onDeleteEntry(entry)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}

/**
 * Main content of the segment entry details screen
 */
@Composable
fun SegmentEntryDetailsContent(
    uiState: SegmentEntryDetailsUiState,
    onDeleteEntry: (SegmentEntry) -> Unit,
    onEditEntry: (SegmentEntry) -> Unit,  // Add this parameter
    onSortOptionSelected: (SegmentSortOptions) -> Unit,
    onEntrySelected: (SegmentEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        val summit = uiState.currentSummit
        val entry = uiState.currentEntry
        val trackPoints = uiState.trackPoints
        // Header with statistics
        if (entry != null) {
            SegmentHeader(
                heightMeterUp = entry.heightMetersUp,
                heightMeterDown = entry.heightMetersDown,
                kilometers = entry.kilometers,
                averageHeartRate = entry.averageHeartRate,
                duration = entry.duration,
                averagePower = entry.averagePower
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Log.i(
            "SegmentEntryDetailsScreen",
            "summit: ${summit?.getDateAsString()}, trackPoints. ${trackPoints.size}"
        )
        // Map section
        if (summit != null && entry != null) {
            SegmentMapSection(
                summit = summit,
                segmentEntry = entry,
                trackPoints = trackPoints,
                segmentDetailsId = uiState.segment?.segmentDetails?.segmentDetailsId ?: -1L
            )
        } else {
            // Show placeholder or loading indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isLoading) "Loading map..." else "Map not available",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart section
        if (entry != null) {
            SegmentChartSection(
                trackPoints = trackPoints,
                segmentEntry = entry,
                selectedTrackColor = uiState.selectedTrackColor
            )
        } else {
            // Show placeholder or loading indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isLoading) "Loading chart..." else "Chart not available",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sorting controls
        SortingControls(
            sortOptions = SegmentSortOptions.entries.toList(),
            selectedOption = uiState.selectedSortOption,
            onSortOptionSelected = onSortOptionSelected
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Segment entries list
        uiState.segment?.let { segment ->
            val sortedEntries = when (uiState.selectedSortOption) {
                SegmentSortOptions.AverageVelocity -> segment.segmentEntries.sortedBy { it.kilometers / it.duration }
                    .reversed()

                SegmentSortOptions.Date -> segment.segmentEntries.sortedBy { it.getDateAsString() }
                    .reversed()

                SegmentSortOptions.AverageHeartRate -> segment.segmentEntries.sortedBy { it.averageHeartRate }
                    .reversed()

                SegmentSortOptions.Power -> segment.segmentEntries.sortedBy { it.averagePower }
                    .reversed()
            }

            SegmentEntriesList(
                segmentEntries = sortedEntries,
                currentEntry = uiState.currentEntry,
                onDeleteEntry = onDeleteEntry,
                onEditEntry = onEditEntry,  // Pass the edit function
                onEntrySelected = onEntrySelected
            )
        }
    }
}

/**
 * Header section showing segment statistics
 */
@Composable
fun SegmentHeader(
    heightMeterUp: Int,
    heightMeterDown: Int,
    kilometers: Double,
    averageHeartRate: Int,
    duration: Double,
    averagePower: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SegmentDetailStatItem(
                    icon = R.drawable.baseline_trending_up_black_24dp,
                    text = "$heightMeterUp/$heightMeterDown ${stringResource(R.string.hm)}"
                )

                SegmentDetailStatItem(
                    icon = R.drawable.ic_baseline_monitor_heart_24,
                    text = "$averageHeartRate ${stringResource(R.string.bpm)}"
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SegmentDetailStatItem(
                    icon = R.drawable.outline_distance_24,
                    text = String.format(
                        Locale.getDefault(),
                        "%.1f %s",
                        kilometers,
                        stringResource(R.string.km)
                    )
                )

                SegmentDetailStatItem(
                    icon = R.drawable.ic_baseline_timer_24,
                    text = String.format(
                        Locale.getDefault(),
                        "%.1f %s",
                        duration,
                        stringResource(R.string.min)
                    )
                )

                SegmentDetailStatItem(
                    icon = R.drawable.ic_baseline_power_24,
                    text = "$averagePower ${stringResource(R.string.watt)}"
                )
            }
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
fun SegmentDetailStatItem(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Map section with custom map view
 */
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
fun SegmentMapSection(
    summit: Summit,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    segmentEntry: SegmentEntry,
    segmentDetailsId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) {
        // Map container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            SummitBookMapView(
                update = { view ->
                    // Update map with track and markers
                    updateMapContent(view, summit, trackPoints, segmentEntry)
                },
                extraControls = { map ->
                    // Map controls
                    MapControls(
                        onUpdateSnapshot = {
                            takeScreenshot(map, segmentDetailsId, context)
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                },
                showMapTypeButton = true,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Controls for the map (map type selector and snapshot button)
 */
@Composable
fun MapControls(
    onUpdateSnapshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Map type toggle is now handled by SummitBookMapView
        // Add spacing to position snapshot button below the map type button
        Spacer(modifier = Modifier.height(48.dp))
        
        IconButton(
            onClick = onUpdateSnapshot,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(50)
                )
                .size(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_refresh_24),
                contentDescription = stringResource(R.string.update)
            )
        }
    }
}

/**
 * Chart section for displaying track profile
 */
@Composable
fun SegmentChartSection(
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    segmentEntry: SegmentEntry,
    selectedTrackColor: TrackColor,
    modifier: Modifier = Modifier
) {
    // Calculate vertical lines for segment start/end
    val verticalLines = remember(trackPoints, segmentEntry) {
        val lines = mutableListOf<Pair<Float, ComposeColor>>()
        if (segmentEntry.startPositionInTrack < trackPoints.size) {
            trackPoints[segmentEntry.startPositionInTrack].second.distance?.toFloat()?.let { distance ->
                lines.add(distance to ComposeColor(0xFF00FF00.toInt())) // Green for start
            }
        }
        if (segmentEntry.endPositionInTrack < trackPoints.size) {
            trackPoints[segmentEntry.endPositionInTrack].second.distance?.toFloat()?.let { distance ->
                lines.add(distance to ComposeColor(0xFFFF0000.toInt())) // Red for end
            }
        }
        lines
    }

    LineChartView(
        trackPoints = trackPoints,
        trackColor = selectedTrackColor,
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        showLegend = true,
        verticalLines = verticalLines
    )
}

/**
 * Sorting controls dropdown
 */
@Composable
fun SortingControls(
    sortOptions: List<SegmentSortOptions>,
    selectedOption: SegmentSortOptions,
    onSortOptionSelected: (SegmentSortOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.sort_by),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box {
            Button(onClick = { expanded = true }) {
                Text(text = stringResource(selectedOption.stringId))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sortOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.stringId)) },
                        onClick = {
                            onSortOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * List of segment entries
 */
@Composable
fun SegmentEntriesList(
    segmentEntries: List<SegmentEntry>,
    currentEntry: SegmentEntry?,
    onDeleteEntry: (SegmentEntry) -> Unit,
    onEditEntry: (SegmentEntry) -> Unit,  // Add this parameter
    onEntrySelected: (SegmentEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        segmentEntries.forEach { entry ->
            SegmentEntryCard(
                entry = entry,
                isSelected = entry.entryId == currentEntry?.entryId,
                onClick = { onEntrySelected(entry) },
                onEdit = { onEditEntry(entry) },  // Pass the edit function
                onDelete = { onDeleteEntry(entry) }
            )
        }
    }
}

/**
 * Individual segment entry card
 */
@Composable
fun SegmentEntryCard(
    entry: SegmentEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,  // Add this parameter
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Entry details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.getDateAsString() ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SegmentEntryStat(
                        value = String.format(
                            Locale.getDefault(),
                            "%d:%02d",
                            entry.duration.toInt(),
                            ((entry.duration - entry.duration.toInt()) * 60).toInt()
                        ),
                        label = stringResource(R.string.min)
                    )

                    SegmentEntryStat(
                        value = String.format(
                            Locale.getDefault(),
                            "%.1f",
                            entry.kilometers / entry.duration * 60
                        ),
                        label = stringResource(R.string.kmh)
                    )

                    SegmentEntryStat(
                        value = entry.averageHeartRate.toString(),
                        label = stringResource(R.string.bpm)
                    )

                    SegmentEntryStat(
                        value = entry.averagePower.toString(),
                        label = stringResource(R.string.watt)
                    )
                }
            }

            // Action buttons
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_edit_24),
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_delete_black_24dp),
                        contentDescription = stringResource(R.string.delete_icon)
                    )
                }
            }
        }
    }
}

/**
 * Individual statistic for a segment entry
 */
@Composable
fun SegmentEntryStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Delete confirmation dialog for segment entry
 */
@Composable
fun SegmentEntryDeleteDialog(
    entry: SegmentEntry,
    segmentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val deleteEntry = stringResource(R.string.delete_entry)
    val deleteCancel = stringResource(R.string.delete_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.delete_entry,
                    "$segmentName on ${entry.getDateAsString()}"
                )
            )
        },
        text = {
            Text(text = stringResource(R.string.delete_entry_text))
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                Toast.makeText(
                    context,
                    String.format(deleteEntry, "$segmentName on ${entry.getDateAsString()}"),
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                Toast.makeText(
                    context,
                    deleteCancel,
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * UI state for the segment entry details screen
 */
data class SegmentEntryDetailsUiState(
    val segment: Segment? = null,
    val currentEntry: SegmentEntry? = null,
    val relevantSummits: List<Summit> = emptyList(),
    val currentSummit: Summit? = null,
    val trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>> = emptyList(),
    val isLoading: Boolean = true,
    val selectedSortOption: SegmentSortOptions = SegmentSortOptions.AverageVelocity,
    val selectedTrackColor: TrackColor = TrackColor.Elevation
)

/**
 * Enum class for segment sorting options
 */
enum class SegmentSortOptions(val stringId: Int) {
    AverageVelocity(R.string.pace_hint),
    Date(R.string.date),
    AverageHeartRate(R.string.bpm),
    Power(R.string.power)
}

// Helper functions (these would need to be implemented based on the original Fragment logic)

/**
 * Update map content with track and markers
 */
private fun updateMapContent(
    mapView: MapView,
    summit: Summit,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    segmentEntry: SegmentEntry
) {
    // Clear existing overlays
    mapView.overlays?.clear()
    mapView.overlayManager?.clear()

    // Add start and end markers
    addMarker(
        mapView,
        GeoPoint(
            segmentEntry.startPositionLatitude,
            segmentEntry.startPositionLongitude
        ),
        R.drawable.ic_filled_location_lightbrown_48,
        mapView.context
    )

    addMarker(
        mapView,
        GeoPoint(
            segmentEntry.endPositionLatitude,
            segmentEntry.endPositionLongitude
        ),
        R.drawable.ic_filled_location_darkbrown_48,
        mapView.context
    )

    // Draw GPX track if available
    if (summit.hasGpsTrack()) {
        drawGpxTrack(mapView, trackPoints, segmentEntry)
    }

    mapView.invalidate()
}

/**
 * Draw GPX track on the map
 */
private fun drawGpxTrack(
    mapView: MapView,
    trackPoints: List<Pair<TrackPoint, ExtensionFromYaml>>,
    segmentEntry: SegmentEntry
) {
    try {
        // Add the full GPX track to the map
        val osMapRoute = Polyline(mapView)
        val paintBorder = Paint()
        paintBorder.strokeWidth = 20F

        val trackPoints = trackPoints.filterIndexed { index, _ ->
            index in segmentEntry.startPositionInTrack..segmentEntry.endPositionInTrack
        }

        if (trackPoints.size > 1) {
            val geoPoints = trackPoints.map { GeoPoint(it.first.latitude, it.first.longitude) }
            addColorToTrack(osMapRoute, trackPoints, paintBorder)
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
        val attributeColorList = GpsTrack.AttitudeColorListContinuous(
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
        val marker = org.osmdroid.views.overlay.Marker(mapView)
        marker.position = point
        marker.setAnchor(
            org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
            org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM
        )
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
 * Take a screenshot of the map view
 */
private fun takeScreenshot(
    view: MapView,
    segmentDetailsId: Long,
    context: Context
) {
    try {
        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val outputStream = FileOutputStream(Segment.getMapScreenshotFile(segmentDetailsId))
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        outputStream.flush()
        outputStream.close()

        Toast.makeText(
            context, context.getString(R.string.screenshot_taken), Toast.LENGTH_SHORT
        ).show()
    } catch (io: FileNotFoundException) {
        io.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}
