package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SegmentEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.util.Locale

/**
 * Composable function that displays a list of segments
 * This replaces the RecyclerView-based SegmentsViewAdapter
 */
@Composable
fun SegmentsListScreen(
    segments: List<Segment>,
    summits: List<Summit>,
    modifier: Modifier = Modifier,
    onDeleteSegment: (Segment) -> Unit = {},
    onDeleteSegmentEntry: (SegmentEntry) -> Unit = {}
) {
    val viewModel: DatabaseViewModel = viewModel()
    var showAddSegmentEntryDialog by remember { mutableStateOf(false) }
    var selectedSegmentId by remember { mutableStateOf(0L) }
    var showAddSegmentDetailsDialog by remember { mutableStateOf(false) }
    var showEditSegmentDetailsDialog by remember { mutableStateOf(false) }
    var selectedSegmentDetails by remember { mutableStateOf<SegmentDetails?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(
            items = segments,
            key = { segment -> segment.segmentDetails.segmentDetailsId }
        ) { segment ->
            SegmentCard(
                segment = segment,
                onDelete = onDeleteSegment,
                onAddSegmentEntry = { segmentId ->
                    selectedSegmentId = segmentId
                    showAddSegmentEntryDialog = true
                },
                onEditSegmentDetails = { segmentDetails ->
                    selectedSegmentDetails = segmentDetails
                    showEditSegmentDetailsDialog = true
                }
            )
        }
        
        // Add "Add Segment Details" button at the end
        item {
            AddSegmentDetailsButton(
                onClick = { showAddSegmentDetailsDialog = true }
            )
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
            }
        )
    }

    // Show Edit Segment Details Dialog
    if (showEditSegmentDetailsDialog) {
        AddSegmentDetailsDialogCompose(
            segmentDetails = selectedSegmentDetails,
            onDismiss = { showEditSegmentDetailsDialog = false },
            onSaveSegmentDetails = { isUpdate, segmentDetails ->
                viewModel.saveSegmentDetails(isUpdate, segmentDetails)
            }
        )
    }
}

/**
 * Individual segment card composable
 */
@Composable
fun SegmentCard(
    segment: Segment,
    onDelete: (Segment) -> Unit,
    onAddSegmentEntry: (Long) -> Unit = {},
    onEditSegmentDetails: (SegmentDetails) -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val deleteCancelMessage = stringResource(R.string.delete_cancel)
    
    // Calculate average values
    val averageDistance = if (segment.segmentEntries.isNotEmpty()) {
        segment.segmentEntries.sumOf { it.kilometers } / segment.segmentEntries.size
    } else 0.0
    
    val averageElevationGainUp = if (segment.segmentEntries.isNotEmpty()) {
        segment.segmentEntries.sumOf { it.heightMetersUp } / segment.segmentEntries.size
    } else 0
    
    val averageElevationGainDown = if (segment.segmentEntries.isNotEmpty()) {
        segment.segmentEntries.sumOf { it.heightMetersDown } / segment.segmentEntries.size
    } else 0
    
    val mapScreenshotFile = Segment.getMapScreenshotFile(segment.segmentDetails.segmentDetailsId)
    val hasMapScreenshot = mapScreenshotFile.exists()
    
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
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Overlay with segment name and entry count
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            if (hasMapScreenshot) {
                                androidx.compose.ui.graphics.Color(0x55000000)
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
                            colorFilter = ColorFilter.tint(androidx.compose.ui.graphics.Color.White)
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
                            Locale.getDefault(),
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
                            Locale.getDefault(),
                            "%s/%s %s",
                            averageElevationGainUp,
                            averageElevationGainDown,
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
                Toast.makeText(
                    context,
                    deleteCancelMessage,
                    Toast.LENGTH_SHORT
                ).show()
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

