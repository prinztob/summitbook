package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.SegmentEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.data.db.entities.Segment
import de.drtobiasprinz.summitbook.data.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.data.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.core.DataStatus
import de.drtobiasprinz.summitbook.ui.viewmodel.DatabaseViewModel
import kotlin.math.roundToInt
import de.drtobiasprinz.summitbook.ui.theme.Scrim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composable function that displays a list of segments
 * This replaces the RecyclerView-based SegmentsViewAdapter
 *
 * The [viewModel] is passed in from the activity instead of being created via
 * `viewModel()` here: inside a NavHost destination the LocalViewModelStoreOwner
 * is the NavBackStackEntry, whose default factory cannot build the Hilt-injected
 * DatabaseViewModel.
 */
@Composable
fun SegmentsListScreen(
    viewModel: DatabaseViewModel,
    segments: List<Segment>,
    summits: List<Summit>,
    modifier: Modifier = Modifier,
    onDeleteSegment: (Segment) -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val mountainPassesState by viewModel.mountainPasses.asFlow()
        .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
    val mountainPasses = mountainPassesState.data ?: emptyList()

    var showMountainPasses by rememberSaveable { mutableStateOf(false) }
    var showAddSegmentEntryDialog by rememberSaveable { mutableStateOf(false) }
    var selectedSegmentId by rememberSaveable { mutableLongStateOf(0L) }
    var showAddSegmentDetailsDialog by rememberSaveable { mutableStateOf(false) }
    var showEditSegmentDetailsDialog by rememberSaveable { mutableStateOf(false) }
    var selectedSegmentDetails by remember { mutableStateOf<SegmentDetails?>(null) }

    var segmentsWithScreenshots by remember { mutableStateOf<Set<Long>>(emptySet()) }
    LaunchedEffect(segments) {
        withContext(Dispatchers.IO) {
            segmentsWithScreenshots = segments.mapNotNull { segment ->
                if (Segment.getMapScreenshotFile(segment.segmentDetails.segmentDetailsId).exists()) {
                    segment.segmentDetails.segmentDetailsId
                } else {
                    null
                }
            }.toSet()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Switch between Segments and Mountain Passes
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.segments),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (!showMountainPasses) FontWeight.Bold else FontWeight.Normal,
                    color = if (!showMountainPasses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = showMountainPasses,
                    onCheckedChange = { showMountainPasses = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.mountain_passes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (showMountainPasses) FontWeight.Bold else FontWeight.Normal,
                    color = if (showMountainPasses) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showMountainPasses) {
            if (mountainPasses.isEmpty()) {
                item {
                    EmptyListHint(text = stringResource(R.string.no_mountain_passes))
                }
            }
            items(
                items = mountainPasses,
                key = { pass -> pass.entryId }
            ) { pass ->
                MountainPassCard(
                    pass = pass,
                    onDelete = { viewModel.deleteMountainPass(it) },
                    onShowSnackbar = onShowSnackbar
                )
            }
        } else {
            if (segments.isEmpty()) {
                item {
                    EmptyListHint(text = stringResource(R.string.no_segments))
                }
            }
            items(
                items = segments,
                key = { segment -> segment.segmentDetails.segmentDetailsId }
            ) { segment ->
                SegmentCard(
                    segment = segment,
                    hasScreenshot = segment.segmentDetails.segmentDetailsId in segmentsWithScreenshots,
                    onDelete = onDeleteSegment,
                    onAddSegmentEntry = { segmentId ->
                        selectedSegmentId = segmentId
                        showAddSegmentEntryDialog = true
                    },
                    onEditSegmentDetails = { segmentDetails ->
                        selectedSegmentDetails = segmentDetails
                        showEditSegmentDetailsDialog = true
                    },
                    onShowSnackbar = onShowSnackbar
                )
            }

            // Add "Add Segment Details" button at the end
            item {
                AddSegmentDetailsButton(
                    onClick = { showAddSegmentDetailsDialog = true }
                )
            }
        }
    }

    // Show Add Segment Entry Dialog
    if (showAddSegmentEntryDialog) {
        AddSegmentEntryScreen(
            summits = summits,
            segments = segments,
            segmentId = selectedSegmentId,
            segmentEntryId = null,
            onCancel = { showAddSegmentEntryDialog = false },
            onSaveSegmentEntry = { isUpdate, segmentEntry ->
                viewModel.saveSegmentEntry(isUpdate, segmentEntry)
            }
        )
    }

    // Show Add Segment Details Dialog
    if (showAddSegmentDetailsDialog) {
        AddSegmentDetailsDialogCompose(
            segmentDetails = null,
            onDismiss = { showAddSegmentDetailsDialog = false },
            onSaveSegmentDetails = { isUpdate, segmentDetails ->
                viewModel.saveSegmentDetails(isUpdate, segmentDetails)
            },
            onShowSnackbar = onShowSnackbar
        )
    }

    // Show Edit Segment Details Dialog
    if (showEditSegmentDetailsDialog) {
        AddSegmentDetailsDialogCompose(
            segmentDetails = selectedSegmentDetails,
            onDismiss = { showEditSegmentDetailsDialog = false },
            onSaveSegmentDetails = { isUpdate, segmentDetails ->
                viewModel.saveSegmentDetails(isUpdate, segmentDetails)
            },
            onShowSnackbar = onShowSnackbar
        )
    }
}

/**
 * Individual segment card composable
 */
@Composable
fun SegmentCard(
    segment: Segment,
    hasScreenshot: Boolean,
    onDelete: (Segment) -> Unit,
    onAddSegmentEntry: (Long) -> Unit = {},
    onEditSegmentDetails: (SegmentDetails) -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val deleteCancelMessage = stringResource(R.string.delete_cancel)
    
    // Calculate average values
    val averageDistance = if (segment.segmentEntries.isNotEmpty()) {
        segment.segmentEntries.sumOf { it.kilometers } / segment.segmentEntries.size
    } else 0.0
    
    val averageElevationGainUp = if (segment.segmentEntries.isNotEmpty()) {
        segment.segmentEntries.sumOf { it.heightMetersUp } / segment.segmentEntries.size
    } else 0.0
    
    val averageElevationGainDown = if (segment.segmentEntries.isNotEmpty()) {
        segment.segmentEntries.sumOf { it.heightMetersDown } / segment.segmentEntries.size
    } else 0.0
    
    val mapScreenshotFile = Segment.getMapScreenshotFile(segment.segmentDetails.segmentDetailsId)
    val hasMapScreenshot = hasScreenshot
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                navigateToSegmentDetails(context, segment.segmentDetails.segmentDetailsId)
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Image and title section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hasMapScreenshot) 200.dp else 60.dp)
            ) {
                // Background image if available
                if (hasMapScreenshot) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("file://" + mapScreenshotFile.absolutePath)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.map),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
                
                // Overlay with segment name and entry count
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            if (hasMapScreenshot) {
                                Scrim
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            }
                        )
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Route icon
                        Image(
                            painter = painterResource(id = R.drawable.ic_baseline_route_24),
                            contentDescription = stringResource(R.string.segments),
                            modifier = Modifier.size(40.dp),
                            colorFilter = ColorFilter.tint(
                                if (hasMapScreenshot) {
                                    androidx.compose.ui.graphics.Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Route name and count column
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Route name
                            Text(
                                text = segment.segmentDetails.getDisplayName(),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (hasMapScreenshot) {
                                    androidx.compose.ui.graphics.Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Entry count
                            Text(
                                text = "# ${segment.segmentEntries.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasMapScreenshot) {
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }
            
            // Stats section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Distance
                SegmentStatItem(
                    icon = R.drawable.baseline_trending_up_black_24dp,
                    text = if (averageDistance > 0.0) {
                        String.format(
                            LocalConfiguration.current.locales[0],
                            "%.1f %s",
                            averageDistance,
                            stringResource(R.string.km)
                        )
                    } else "",
                    modifier = Modifier.weight(1f)
                )
                
                // Elevation gain
                SegmentStatItem(
                    icon = R.drawable.baseline_trending_up_black_24dp,
                    text = if (averageElevationGainUp > 0 || averageElevationGainDown > 0) {
                        String.format(
                            LocalConfiguration.current.locales[0],
                            "%s/%s %s",
                            averageElevationGainUp.roundToInt(),
                            averageElevationGainDown.roundToInt(),
                            stringResource(R.string.hm)
                        )
                    } else "",
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Action buttons section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 2.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                // Add segment entry button
                IconButton(
                    onClick = {
                        onAddSegmentEntry(segment.segmentDetails.segmentDetailsId)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_add_black_24dp),
                        contentDescription = stringResource(R.string.add_segment),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Edit button
                IconButton(
                    onClick = {
                        onEditSegmentDetails(segment.segmentDetails)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_mode_edit_black_24dp),
                        contentDescription = stringResource(R.string.edit_icon),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Delete button
                IconButton(
                    onClick = {
                        showDeleteDialog = true
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_delete_black_24dp),
                        contentDescription = stringResource(R.string.delete_icon),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        SegmentDeleteConfirmationDialog(
            segmentName = segment.segmentDetails.getDisplayName(),
            onConfirm = {
                onDelete(segment)
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
                onShowSnackbar(deleteCancelMessage)
            }
        )
    }
}

/**
 * Mountain pass card composable
 */
@Composable
fun MountainPassCard(
    pass: SegmentEntry,
    onDelete: (SegmentEntry) -> Unit,
    onShowSnackbar: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val deleteCancelMessage = stringResource(R.string.delete_cancel)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Title section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_add_mountain_pass_24),
                        contentDescription = stringResource(R.string.mountain_passes),
                        modifier = Modifier.size(40.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pass.getDisplayName(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pass.getDateAsString() ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Stats section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Distance
                SegmentStatItem(
                    icon = R.drawable.outline_distance_24,
                    text = String.format(LocalConfiguration.current.locales[0], "%.1f %s", pass.kilometers, stringResource(R.string.km)),
                    modifier = Modifier.weight(1f)
                )

                // Elevation gain/loss
                SegmentStatItem(
                    icon = R.drawable.baseline_trending_up_black_24dp,
                    text = String.format(LocalConfiguration.current.locales[0], "%.0f/%.0f %s", pass.heightMetersUp, pass.heightMetersDown, stringResource(R.string.hm)),
                    modifier = Modifier.weight(1f)
                )

                // Avg gradient
                SegmentStatItem(
                    icon = R.drawable.baseline_trending_flat_24,
                    text = String.format(LocalConfiguration.current.locales[0], "%.1f%%", pass.avgGradient),
                    modifier = Modifier.weight(1f)
                )
            }

            // Action buttons section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 2.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                // Delete button
                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_delete_black_24dp),
                        contentDescription = stringResource(R.string.delete_icon),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        SegmentDeleteConfirmationDialog(
            segmentName = pass.getDisplayName(),
            onConfirm = {
                onDelete(pass)
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
                onShowSnackbar(deleteCancelMessage)
            }
        )
    }
}

/**
 * Add Segment Details button composable
 */
@Composable
fun AddSegmentDetailsButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = stringResource(R.string.add_new_segment_details),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun SegmentStatItem(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SegmentDeleteConfirmationDialog(
    segmentName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = String.format(
                    stringResource(R.string.delete_entry),
                    segmentName
                )
            )
        },
        text = {
            Text(text = stringResource(R.string.delete_entry_text))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

// Navigation helper functions
private fun navigateToSegmentDetails(context: Context, segmentDetailsId: Long) {
    val intent = Intent(context, SegmentEntryDetailsComposeActivity::class.java)
    intent.putExtra(SegmentDetails.SEGMENT_DETAILS_ID_EXTRA_IDENTIFIER, segmentDetailsId)
    context.startActivity(intent)
}

@Composable
private fun EmptyListHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    )
}
