package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.models.SummitEntitySummary
import de.drtobiasprinz.summitbook.models.SummitEntityType
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.utils.Constants
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Main screen for displaying summit entities with tabbed interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummitEntitiesScreen(
    filteredSummits: List<Summit>,
    entityEvents: List<EntityEvent>,
    sortFilterValues: SortFilterValues,
    onSaveSummit: (Boolean, Summit) -> Unit,
    onDeleteEntityEvent: (EntityEvent) -> Unit,
    onSaveEntityEvent: (Boolean, EntityEvent) -> Unit,
    onUpdatePeakName: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        R.string.participants,
        R.string.place_hint,
        R.string.country_hint,
        R.string.equipments
    )

    val summitEntityType = when (selectedTabIndex) {
        0 -> SummitEntityType.PARTICIPANTS
        1 -> SummitEntityType.PLACES_VISITED
        2 -> SummitEntityType.COUNTRIES
        3 -> SummitEntityType.EQUIPMENTS
        else -> SummitEntityType.COUNTRIES
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            tabTitles.forEachIndexed { index, titleRes ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = stringResource(titleRes),
                            color = if (selectedTabIndex == index) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            }
                        )
                    }
                )
            }
        }

        // Content for selected tab
        SummitEntitiesList(
            entityType = summitEntityType,
            filteredSummits,
            entityEvents,
            sortFilterValues,
            modifier = Modifier.weight(1f),
            onSaveSummit = onSaveSummit,
            onDeleteEntityEvent = onDeleteEntityEvent,
            onSaveEntityEvent = onSaveEntityEvent,
            onUpdatePeakName = onUpdatePeakName
        )
    }
}

/**
 * List of summit entities for a specific type
 */
@Suppress("AssignedValueIsNeverRead")
@Composable
fun SummitEntitiesList(
    entityType: SummitEntityType,
    filteredSummits: List<Summit>,
    entityEvents: List<EntityEvent>,
    sortFilterValues: SortFilterValues,
    modifier: Modifier = Modifier,
    onSaveSummit: (Boolean, Summit) -> Unit,
    onDeleteEntityEvent: (EntityEvent) -> Unit,
    onSaveEntityEvent: (Boolean, EntityEvent) -> Unit,
    onUpdatePeakName: (String, String) -> Unit
) {
    val context = LocalContext.current

    // State for entity event dialog
    var showEntityEventDialog by remember { mutableStateOf(false) }
    var currentEntityEvent by remember { mutableStateOf<EntityEvent?>(null) }
    var currentEntity by remember { mutableStateOf<SummitEntitySummary?>(null) }

    // Create a key that changes when any summit is updated
    // Use id and hashCode of mutable properties to detect changes
    val summitsKey = filteredSummits.fold(0L) { acc, summit ->
        acc + summit.id + summit.participants.hashCode() + summit.equipments.hashCode() + summit.places.hashCode() + summit.countries.hashCode()
    }

    val entitySummaries = remember(summitsKey, entityEvents, entityType) {
        Log.d("SummitEntitiesScreen", "Recalculating entitySummaries with key: $summitsKey, filteredSummits size: ${filteredSummits.size}")
        calculateEntitySummaries(
            filteredSummits,
            entityType,
        )
    }
    val entitySummariesSorted = sortFilterValues.applyOnSummitEntities(entitySummaries)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (entitySummariesSorted.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No data available")
                }
            }
        } else {
            items(entitySummariesSorted, key = { "${it.type.name}-${it.name}-${it.count}-${it.distance}-${it.heightMeters}" }) { entity ->
                SummitEntityCard(
                    entity = entity,
                    entityType = entityType,
                    entityEvents = entityEvents,
                    filteredSummits = filteredSummits,
                    onUpdateEntity = { oldName, newName ->
                        updateEntityName(
                            context,
                            oldName,
                            newName,
                            entityType,
                            summits = filteredSummits,
                            onSaveSummit = onSaveSummit,
                            onUpdatePeakName = onUpdatePeakName
                        )
                    },
                    onDeleteEntityEvent = onDeleteEntityEvent,
                    onAddEntityEvent = {
                        currentEntityEvent = null
                        currentEntity = entity
                        showEntityEventDialog = true
                    },
                    onEditEntityEvent = { event ->
                        currentEntityEvent = event
                        currentEntity = entity
                        showEntityEventDialog = true
                    }
                )
            }
        }
    }

    // Entity Event Dialog
    if (showEntityEventDialog && currentEntity != null) {
        AddEntityEventDialogCompose(
            entityEvent = currentEntityEvent,
            entity = currentEntity!!,
            onDismiss = { showEntityEventDialog = false },
            onSaveEntityEvent = onSaveEntityEvent
        )
    }
}

/**
 * Calculate entity summaries from summits
 */
private fun calculateEntitySummaries(
    filteredSummits: List<Summit>,
    entityType: SummitEntityType,
): List<SummitEntitySummary> {
    Log.d(
        "SummitEntitiesScreen",
        "calculateEntitySummaries called with ${filteredSummits.size} summits"
    )

    val entityNames = filteredSummits.flatMap { entityType.getRelevantValueFromSummit(it) }
        .filter { it.isNotBlank() && !it.startsWith(Constants.CONNECTED_ACTIVITY_PREFIX) }
        .toSet()
        .toList()

    return entityNames.map { name ->
        val relevantSummits = filteredSummits.filter {
            name in entityType.getRelevantValueFromSummit(it)
        }
        SummitEntitySummary(
            type = entityType,
            name = name,
            count = relevantSummits.size,
            distance = relevantSummits.sumOf { it.kilometers },
            heightMeters = relevantSummits.sumOf { it.elevationData.elevationGain }
        )
    }
}

/**
 * Update entity name across all relevant summits
 */
private fun updateEntityName(
    context: Context,
    oldName: String,
    newName: String,
    entityType: SummitEntityType,
    summits: List<Summit>,
    onSaveSummit: (Boolean, Summit) -> Unit,
    onUpdatePeakName: (String, String) -> Unit
) {
    Log.d("SummitEntitiesScreen", "updateEntityName called: $oldName -> $newName")
    summits.forEach { summit ->
        if (oldName in entityType.getRelevantValueFromSummit(summit)) {
            Log.d("SummitEntitiesScreen", "Updating summit: ${summit.name}")
            entityType.setRelevantValueFromSummit(summit, oldName, newName)
            summit.updated++
            onSaveSummit(true, summit)
        }
    }
    
    // Update peak database if entity type is PLACES_VISITED
    if (entityType == SummitEntityType.PLACES_VISITED) {
        Log.d("SummitEntitiesScreen", "Updating peak name: $oldName -> $newName")
        onUpdatePeakName(oldName, newName)
    }
    
    Toast.makeText(context, R.string.update_done, Toast.LENGTH_SHORT).show()
}

/**
 * Card for displaying a single summit entity
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummitEntityCard(
    entity: SummitEntitySummary,
    entityType: SummitEntityType,
    entityEvents: List<EntityEvent>,
    filteredSummits: List<Summit>,
    onUpdateEntity: (String, String) -> Unit,
    onDeleteEntityEvent: (EntityEvent) -> Unit,
    onAddEntityEvent: () -> Unit,
    onEditEntityEvent: (EntityEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("SummitEntitiesScreen", "SummitEntityCard recomposed: ${entity.name}")
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(entity.name) { mutableStateOf(entity.name) }
    var expanded by remember { mutableStateOf(false) }
    val relevantEvents = entityEvents.filter { it.equipmentName == entity.name }
        .sortedByDescending { it.date }

    val isActive = entity.name in MainActivityCompose.peaks.map { it.name }
    val imageResourceId = if (isActive && entityType.drawableIdActive != null) {
        entityType.drawableIdActive!!
    } else {
        entityType.drawableIdDefault
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row with image and name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = imageResourceId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(end = 8.dp)
                )

                if (isEditing) {
                    TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    IconButton(onClick = {
                        onUpdateEntity(entity.name, editedName)
                        isEditing = false
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_done_24),
                            contentDescription = stringResource(R.string.saveButtonText),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        editedName = entity.name
                        isEditing = false
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_cancel_24),
                            contentDescription = stringResource(R.string.cancelButtonText),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = entity.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isEditing = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_edit_24),
                            contentDescription = stringResource(R.string.edit_icon)
                        )
                    }
                    IconButton(onClick = {
                        onAddEntityEvent()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_add_black_24dp),
                            contentDescription = stringResource(R.string.add_event)
                        )
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.number_activities, entity.count),
                    fontSize = 14.sp
                )
                Text(
                    text = "${entity.heightMeters} ${stringResource(R.string.hm)}",
                    fontSize = 14.sp
                )
                Text(
                    text = "${round(entity.distance).toInt()} ${stringResource(R.string.km)}",
                    fontSize = 14.sp
                )
            }

            // Expandable events section
            if (relevantEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = if (expanded) painterResource(R.drawable.baseline_arrow_drop_up_black_24dp) else painterResource(
                            R.drawable.baseline_arrow_drop_down_24
                        ),
                        contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(
                            R.string.expand
                        )
                    )
                }

                if (expanded) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(relevantEvents) { event ->
                            EntityEventItem(
                                event = event,
                                summitEntitySummary = entity,
                                summits = filteredSummits,
                                onEditEvent = { onEditEntityEvent(it) },
                                onDeleteEvent = { onDeleteEntityEvent(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item for displaying an entity event
 */
@Composable
fun EntityEventItem(
    event: EntityEvent,
    summitEntitySummary: SummitEntitySummary,
    summits: List<Summit>,
    onEditEvent: (EntityEvent) -> Unit,
    onDeleteEvent: (EntityEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val relevantSummits = summits.filter {
        it.date > event.date && event.equipmentName in summitEntitySummary.type.getRelevantValueFromSummit(
            it
        )
    }
    val numberFormat = NumberFormat.getInstance(Locale.getDefault())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Date and description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.getDateAsString(),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.description,
                        fontSize = 12.sp
                    )
                }

                // Edit button
                IconButton(onClick = { onEditEvent(event) }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_mode_edit_black_24dp),
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete button
                IconButton(onClick = { onDeleteEvent(event) }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_delete_black_24dp),
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format(
                        stringResource(R.string.value_with_km),
                        numberFormat.format(relevantSummits.sumOf { it.kilometers }.roundToInt())
                    ),
                    fontSize = 12.sp
                )
                Text(
                    text = String.format(
                        stringResource(R.string.value_with_hm),
                        numberFormat.format(relevantSummits.sumOf { it.elevationData.elevationGain })
                    ),
                    fontSize = 12.sp
                )
                Text(
                    text = String.format(
                        stringResource(R.string.value_with_h),
                        numberFormat.format(
                            TimeUnit.SECONDS.toHours(
                                relevantSummits.sumOf { it.duration }.toLong()
                            )
                        )
                    ),
                    fontSize = 12.sp
                )
            }
        }
    }
}