package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.utils.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import kotlinx.coroutines.Job
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Composable function that displays a list of summits
 * This replaces the RecyclerView-based SummitsAdapter
 */
@Composable
fun SummitsListScreen(
    filteredSummits: List<Summit>,
    summitsFromDatabase: List<Summit>,
    peaks: List<de.drtobiasprinz.summitbook.db.entities.Peak>,
    modifier: Modifier = Modifier,
    isBookmark: Boolean = false,
    onSaveSummit: (Boolean, Summit) -> Job,
    onDelete: (Summit) -> Unit = {},
    onPeakToggle: ((String, Boolean) -> Unit)? = null
) {
    if (filteredSummits.isEmpty()) {
        // Show app icon and "no summit" message when list is empty
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_summit),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(
                items = filteredSummits,
                key = { summit -> summit.id }
            ) { summit ->
                SummitCard(
                    summitsFromDatabase = summitsFromDatabase,
                    peaks = peaks,
                    summit = summit,
                    isBookmark = isBookmark,
                    onDelete = onDelete,
                    onSaveSummit = onSaveSummit,
                    onPeakToggle = onPeakToggle
                )
            }
        }
    }
}

/**
 * Individual summit card composable
 */
@Suppress("AssignedValueIsNeverRead")
@Composable
fun SummitCard(
    summitsFromDatabase: List<Summit>,
    peaks: List<de.drtobiasprinz.summitbook.db.entities.Peak>,
    summit: Summit,
    isBookmark: Boolean,
    onDelete: (Summit) -> Unit,
    onSaveSummit: (Boolean, Summit) -> Job,
    onPeakToggle: ((String, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddImagesDialog by remember { mutableStateOf(false) }
    var showSelectOnMapDialog by remember { mutableStateOf(false) }
    var showAddAdditionalDataDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val isDarkTheme = isSystemInDarkTheme()
    val deleteCancelMessage = stringResource(R.string.delete_cancel)

    // Force recomposition when refreshTrigger changes
    val currentSummit = remember(refreshTrigger) { summit }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable {
                navigateToSummitDetails(context, summit.id)
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
                    .height(if (currentSummit.hasImagePath()) 200.dp else 60.dp)
            ) {
                // Background image if available
                if (currentSummit.hasImagePath()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("file://" + currentSummit.getImagePath(currentSummit.imageIds.first()))
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.summit_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay with sport type icon, date and summit name
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            if (currentSummit.hasImagePath()) {
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
                        // Sport type icon
                        val sportTypeIcon = if (currentSummit.hasImagePath()) {
                            currentSummit.sportType.imageIdWhite
                        } else {
                            if (isDarkTheme) currentSummit.sportType.imageIdWhite else currentSummit.sportType.imageIdBlack
                        }

                        Image(
                            painter = painterResource(id = sportTypeIcon),
                            contentDescription = stringResource(R.string.sport_type_image),
                            modifier = Modifier.size(40.dp),
                            colorFilter = if (!summit.hasImagePath() && !isDarkTheme) {
                                ColorFilter.tint(androidx.compose.ui.graphics.Color.Black)
                            } else {
                                ColorFilter.tint(androidx.compose.ui.graphics.Color.White)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Date and Summit name column
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            // Date (only for non-bookmarks)
                            if (!currentSummit.isBookmark) {
                                Text(
                                    text = currentSummit.getDateAsString() ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (summit.hasImagePath()) {
                                        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                    } else {
                                        if (isDarkTheme) androidx.compose.ui.graphics.Color.White.copy(
                                            alpha = 0.7f
                                        )
                                        else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                                    }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            // Summit name
                            Text(
                                text = currentSummit.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (summit.hasImagePath()) {
                                    androidx.compose.ui.graphics.Color.White
                                } else {
                                    if (isDarkTheme) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Record badges aligned to the right
                        RecordBadges(summit = currentSummit)
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
                // Height meters
                StatItem(
                    icon = R.drawable.baseline_trending_up_black_24dp,
                    text = String.format(
                        Locale.getDefault(),
                        "%s %s",
                        currentSummit.elevationData.elevationGain,
                        stringResource(R.string.hm)
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Distance
                StatItem(
                    icon = R.drawable.baseline_trending_up_black_24dp,
                    text = String.format(
                        Locale.getDefault(),
                        "%.1f %s",
                        currentSummit.kilometers,
                        stringResource(R.string.km)
                    ),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = getThirdEntryValues(currentSummit).third,
                    text = String.format(
                        Locale.getDefault(),
                        if (getThirdEntryValues(currentSummit).first is Int) "%s %s" else "%.1f %s",
                        getThirdEntryValues(currentSummit).first,
                        stringResource(getThirdEntryValues(currentSummit).second)
                    ),
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
                // Add image button
                IconButton(
                    onClick = {
                        if (currentSummit.isBookmark) {
                            // Bookmark icon - no action
                        } else {
                            showAddImagesDialog = true
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (currentSummit.isBookmark) {
                                R.drawable.ic_baseline_bookmarks_24
                            } else {
                                R.drawable.baseline_add_a_photo_black_24dp
                            }
                        ),
                        contentDescription = stringResource(R.string.add_image),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Add velocity data button (only if has GPS track)
                if (currentSummit.hasTrack && !currentSummit.isBookmark) {
                    IconButton(
                        onClick = {
                            showAddAdditionalDataDialog = true
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (currentSummit.velocityData.hasAdditionalData() || currentSummit.elevationData.hasAdditionalData()) {
                                    R.drawable.baseline_speed_black_24dp
                                } else {
                                    R.drawable.baseline_more_time_black_24dp
                                }
                            ),
                            contentDescription = stringResource(R.string.add_image),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Add coordinate button
                IconButton(
                    onClick = {
                        showSelectOnMapDialog = true
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = when {
                                currentSummit.latLng == null -> R.drawable.baseline_add_location_black_24dp
                                currentSummit.hasTrack -> R.drawable.baseline_edit_location_alt_24
                                else -> R.drawable.baseline_edit_location_black_24dp
                            }
                        ),
                        contentDescription = stringResource(R.string.add_coordinate),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Edit button
                IconButton(
                    onClick = {
                        showEditDialog = true
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

                // Favorite button (only for non-bookmarks)
                if (!currentSummit.isBookmark) {
                    IconButton(
                        onClick = {
                            summit.isFavorite = !summit.isFavorite
                            onSaveSummit(true, summit).invokeOnCompletion {
                                refreshTrigger++
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (currentSummit.isFavorite) {
                                    R.drawable.baseline_star_black_24dp
                                } else {
                                    R.drawable.baseline_star_border_black_24dp
                                }
                            ),
                            contentDescription = stringResource(R.string.is_favorite),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Peak button
                    IconButton(
                        onClick = {
                            summit.isPeak = !summit.isPeak
                            onSaveSummit(true, summit).invokeOnCompletion {
                                refreshTrigger++
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (currentSummit.isPeak) {
                                    R.drawable.outline_landscape_2_24
                                } else {
                                    R.drawable.outline_landscape_2_off_24
                                }
                            ),
                            contentDescription = stringResource(R.string.is_favorite),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            summitName = summit.name,
            onConfirm = {
                deleteEntry(summit, onDelete)
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

    // Edit summit dialog
    if (showEditDialog) {
        AddSummitDialogCompose(
            summitsFromDatabase = summitsFromDatabase,
            peaks = peaks,
            summitId = currentSummit.id,
            isBookmark = isBookmark,
            onDismiss = { showEditDialog = false },
            onSaveSummit = { isEdit, updatedSummit ->
                val job = onSaveSummit(isEdit, updatedSummit)
                job.invokeOnCompletion {
                    refreshTrigger++
                }
                job
            },
            onPeakToggle = onPeakToggle
        )
    }

    // Add images dialog
    if (showAddImagesDialog) {
        AddImagesDialogCompose(
            summit = currentSummit,
            onDismiss = { showAddImagesDialog = false },
            onSaveSummit = { isEdit, updatedSummit ->
                val job = onSaveSummit(isEdit, updatedSummit)
                refreshTrigger++
                job
            }
        )
    }

    // Select on map dialog
    if (showSelectOnMapDialog) {
        SelectOnMapDialogCompose(
            summit = currentSummit,
            onDismiss = { showSelectOnMapDialog = false },
            onSaveSummit = { isEdit, updatedSummit ->
                val job = onSaveSummit(isEdit, updatedSummit)
                refreshTrigger++
                job
            }
        )
    }

    // Add additional data dialog
    if (showAddAdditionalDataDialog) {
        AddAdditionalDataDialogCompose(
            summit = currentSummit,
            onDismiss = { showAddAdditionalDataDialog = false },
            onSaveSummit = { isEdit, updatedSummit ->
                val job = onSaveSummit(isEdit, updatedSummit)
                refreshTrigger++
                job
            }
        )
    }
}

private fun getThirdEntryValues(summit: Summit): Triple<Number, Int, Int> {
    val power = summit.garminData?.power?.avgPower ?: 0f

    var thirdEntryValue: Number
    var thirdEntryUnit: Int
    var thirdEntryIcon: Int

    if (summit.isPeak) {
        thirdEntryValue = summit.elevationData.maxElevation
        thirdEntryUnit = R.string.masl
        thirdEntryIcon = R.drawable.outline_landscape_2_24
    } else if (power > 0) {
        thirdEntryValue = power.roundToInt()
        thirdEntryUnit = R.string.watt
        thirdEntryIcon = R.drawable.ic_baseline_power_24
    } else {
        thirdEntryValue = summit.getAverageVelocity().toFloat()
        thirdEntryUnit = R.string.kmh
        thirdEntryIcon = R.drawable.baseline_speed_black_24dp
    }
    return Triple(thirdEntryValue, thirdEntryUnit, thirdEntryIcon)
}

@Composable
fun StatItem(icon: Int, text: String, modifier: Modifier = Modifier) {
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
fun RecordBadges(summit: Summit) {
    // Power record badge
    val powerRecordColor = when (summit.activityId) {
        in MainActivityCompose.activitiesWithPowerRecordsAll -> Color.rgb(255, 215, 0) // Gold
        in MainActivityCompose.activitiesWithPowerRecordsLast5Years -> Color.rgb(
            192,
            192,
            192
        ) // Silver
        in MainActivityCompose.activitiesWithPowerRecordsFiltered -> Color.rgb(
            168,
            112,
            0
        ) // Bronze
        else -> null
    }

    if (powerRecordColor != null) {
        IconButton(
            onClick = {},
            enabled = false
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_power_24),
                contentDescription = stringResource(R.string.new_power_record),
                tint = androidx.compose.ui.graphics.Color(powerRecordColor)
            )
        }
    }

    // Segment record badge
    val bestPositionInSegment =
        MainActivityCompose.activitiesWithSegmentsRecord.firstOrNull { it.first == summit.activityId }

    if (bestPositionInSegment != null) {
        val segmentColor = when (bestPositionInSegment.second) {
            1 -> Color.rgb(255, 215, 0) // Gold
            2 -> Color.rgb(192, 192, 192) // Silver
            3 -> Color.rgb(168, 112, 0) // Bronze
            else -> null
        }

        if (segmentColor != null) {
            IconButton(
                onClick = {},
                enabled = false
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_route_24),
                    contentDescription = stringResource(R.string.new_segment_record),
                    tint = androidx.compose.ui.graphics.Color(segmentColor)
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    summitName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = String.format(
                    stringResource(R.string.delete_entry),
                    summitName
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
private fun navigateToSummitDetails(context: Context, summitId: Long) {
    val intent = Intent(context, SummitEntryDetailsComposeActivity::class.java)
    intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, summitId)
    context.startActivity(intent)
}


private fun deleteEntry(summit: Summit, onDelete: (Summit) -> Unit) {
    onDelete(summit)
    if (summit.hasGpsTrack()) {
        summit.getGpsTrackPath().toFile()?.delete()
    }
    if (summit.hasImagePath()) {
        summit.imageIds.forEach {
            summit.getImagePath(it).toFile().delete()
        }
    }
}
