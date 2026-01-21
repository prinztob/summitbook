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
import de.drtobiasprinz.summitbook.AddImagesActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.dialog.AddAdditionalDataFromExternalResourcesDialog
import de.drtobiasprinz.summitbook.utils.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import de.drtobiasprinz.summitbook.utils.findActivity
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Composable function that displays a list of summits
 * This replaces the RecyclerView-based SummitsAdapter
 */
@Composable
fun SummitsListScreen(
    summits: List<Summit>,
    modifier: Modifier = Modifier,
    isBookmark: Boolean = false,
    onUpdateIsFavorite: (Summit) -> Unit = {},
    onUpdateIsPeak: (Summit) -> Unit = {},
    onDelete: (Summit) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        items(
            items = summits,
            key = { summit -> summit.id }
        ) { summit ->
            SummitCard(
                summit = summit,
                isBookmark = isBookmark,
                onUpdateIsFavorite = onUpdateIsFavorite,
                onUpdateIsPeak = onUpdateIsPeak,
                onDelete = onDelete
            )
        }
    }
}

/**
 * Individual summit card composable
 */
@Composable
fun SummitCard(
    summit: Summit,
    isBookmark: Boolean,
    onUpdateIsFavorite: (Summit) -> Unit,
    onUpdateIsPeak: (Summit) -> Unit,
    onDelete: (Summit) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val deleteCancelMessage = stringResource(R.string.delete_cancel)

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
                    .height(if (summit.hasImagePath()) 200.dp else 60.dp)
            ) {
                // Background image if available
                if (summit.hasImagePath()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("file://" + summit.getImagePath(summit.imageIds.first()))
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
                            if (summit.hasImagePath()) {
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
                        val sportTypeIcon = if (summit.hasImagePath()) {
                            summit.sportType.imageIdWhite
                        } else {
                            if (isDarkTheme) summit.sportType.imageIdWhite else summit.sportType.imageIdBlack
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
                            if (!summit.isBookmark) {
                                Text(
                                    text = summit.getDateAsString() ?: "",
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
                                text = summit.name,
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
                        RecordBadges(summit = summit)
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
                        summit.elevationData.elevationGain,
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
                        summit.kilometers,
                        stringResource(R.string.km)
                    ),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    icon = getThirdEntryValues(summit).third,
                    text = String.format(
                        Locale.getDefault(),
                        if (getThirdEntryValues(summit).first is Int) "%s %s" else "%.1f %s",
                        getThirdEntryValues(summit).first,
                        stringResource(getThirdEntryValues(summit).second)
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
                        if (summit.isBookmark) {
                            // Bookmark icon - no action
                        } else {
                            navigateToAddImages(context, summit.id)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (summit.isBookmark) {
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
                if (summit.hasGpsTrack() && !summit.isBookmark) {
                    IconButton(
                        onClick = {
                            showAddVelocityDataDialog(context, summit)
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (summit.velocityData.hasAdditionalData() || summit.elevationData.hasAdditionalData()) {
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
                        navigateToSelectOnMap(context, summit.id)
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = when {
                                summit.latLng == null -> R.drawable.baseline_add_location_black_24dp
                                summit.hasGpsTrack(true) -> R.drawable.baseline_edit_location_alt_24
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
                if (!summit.isBookmark) {
                    IconButton(
                        onClick = {
                            onUpdateIsFavorite(summit)
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (summit.isFavorite) {
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
                            onUpdateIsPeak(summit)
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (summit.isPeak) {
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
            summitId = summit.id,
            isBookmark = isBookmark,
            onDismiss = { showEditDialog = false }
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
        in MainActivity.activitiesWithPowerRecordsAll -> Color.rgb(255, 215, 0) // Gold
        in MainActivity.activitiesWithPowerRecordsLast5Years -> Color.rgb(192, 192, 192) // Silver
        in MainActivity.activitiesWithPowerRecordsFiltered -> Color.rgb(168, 112, 0) // Bronze
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
        MainActivity.activitiesWithSegmentsRecord.firstOrNull { it.first == summit.activityId }

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

private fun navigateToAddImages(context: Context, summitId: Long) {
    val intent = Intent(context, AddImagesActivity::class.java)
    intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, summitId)
    context.startActivity(intent)
}

private fun navigateToSelectOnMap(context: Context, summitId: Long) {
    val intent = Intent(context, SelectOnOsMapActivity::class.java)
    intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, summitId)
    context.startActivity(intent)
}

private fun showAddVelocityDataDialog(context: Context, summit: Summit) {
    context.findActivity()?.supportFragmentManager?.let { fragmentManager ->
        AddAdditionalDataFromExternalResourcesDialog.getInstance(summit)
            .show(fragmentManager, "Show addition data")
    }
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
