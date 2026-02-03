package de.drtobiasprinz.summitbook.ui.compose

import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.models.AdditionalDataTableEntry
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Jetpack Compose version of AddAdditionalDataFromExternalResourcesDialog
 * Displays additional data extracted from GPX files with options to accept or ignore
 */
@Composable
fun AddAdditionalDataDialogCompose(
    summit: Summit,
    onDismiss: () -> Unit,
    onSaveSummit: (Boolean, Summit) -> Job
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State management
    var isLoading by remember { mutableStateOf(false) }
    val tableEntries = remember { mutableStateListOf<TableEntry>() }
    var summitEntry by remember { mutableStateOf(summit.clone()) }

    // Load data when dialog opens
    LaunchedEffect(summit) {
        summitEntry = summit.clone()
        scope.launch {
            extractDataFromFilesAndPutIntoView(
                context = context,
                summit = summitEntry,
                tableEntries = tableEntries,
                onLoadingChanged = { isLoading = it }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f),
                //.fillMaxHeight(0.90f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.add_additional_data),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Table header
                    TableHeader()

                    // Table entries
                    if (tableEntries.isEmpty() && !isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_additional_data_available),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        ) {
                            itemsIndexed(tableEntries, key = { _, entry -> entry.tableEntry.nameId }) { index, entry ->
                                TableEntryRow(
                                    entry = entry,
                                    summit = summitEntry,
                                    index = index,
                                    onEntryChanged = { updatedEntry ->
                                        tableEntries[index] = updatedEntry
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    ActionButtons(
                        onBack = onDismiss,
                        onSave = {
                            val copyElevationData = summitEntry.elevationData.clone()
                            val copyVelocityData = summitEntry.velocityData.clone()
                            tableEntries.forEach { it.update() }
                            if (copyElevationData != summitEntry.elevationData || copyVelocityData != summitEntry.velocityData) {
                                summitEntry.updated += 1
                                onSaveSummit(true, summitEntry)
                            }
                            onDismiss()
                        },
                        onRecalculate = {
                            scope.launch {
                                recalculateSummit(context, summitEntry, tableEntries) { isLoading = it }
                            }
                        },
                        onDelete = {
                            val gpxPyFile = summitEntry.getGpxPyPath().toFile()
                            if (gpxPyFile.exists()) {
                                gpxPyFile.delete()
                            }
                            val trackFile = summitEntry.getGpsTrackPath(simplified = true).toFile()
                            if (trackFile.exists()) {
                                trackFile.delete()
                            }
                            ignoreAllAdditionalData(summitEntry)
                            onSaveSummit(true, summitEntry)
                            onDismiss()
                        },
                        onIgnore = {
                            ignoreAllAdditionalData(summitEntry)
                            onSaveSummit(true, summitEntry)
                            onDismiss()
                        }
                    )
                }

                // Loading overlay
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
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
private fun TableHeader() {
    val isDarkTheme = isSystemInDarkTheme()
    val headerBackgroundColor = if (isDarkTheme) {
        Color(0xFF424242)
    } else {
        Color(0xFFE0E0E0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBackgroundColor)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.entry),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = stringResource(R.string.new_value),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = stringResource(R.string.old_value),
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TableEntryRow(
    entry: TableEntry,
    summit: Summit,
    index: Int,
    onEntryChanged: (TableEntry) -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val defaultBackground = MaterialTheme.colorScheme.surface
    val greenBackground = Color(0xFF4CAF50)

    val currentValue = entry.tableEntry.getValue(summit)

    val defaultValueAsString = if (abs(currentValue) < 0.05 || abs(entry.value - currentValue) < (if (entry.tableEntry.isInt) 0.51 else 0.05)) {
        "-"
    } else {
        String.format(
            context.resources.configuration.locales[0],
            if (entry.tableEntry.isInt) "%.0f %s" else "%.1f %s",
            currentValue * entry.tableEntry.scaleFactorView,
            context.getString(entry.tableEntry.unitId)
        )
    }

    val newValueAsString = String.format(
        context.resources.configuration.locales[0],
        if (entry.tableEntry.isInt) "%.0f %s" else "%.1f %s",
        entry.value * entry.tableEntry.scaleFactorView,
        context.getString(entry.tableEntry.unitId)
    )

    val backgroundColor = if (index % 2 == 1) {
        if (isDarkTheme) {
            Color(0xFF2C2C2C)
        } else {
            Color(0xFFF5F5F5)
        }
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Entry name
        Text(
            text = context.getString(entry.tableEntry.nameId),
            modifier = Modifier.weight(1f),
            fontSize = 12.sp
        )

        // New value
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    val updated = entry.copy(isChecked = true)
                    onEntryChanged(updated)
                }
                .background(if (entry.isChecked) greenBackground else defaultBackground)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = newValueAsString,
                fontSize = 12.sp,
                color = if (entry.isChecked) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Old value
        Box(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    val updated = entry.copy(isChecked = false)
                    onEntryChanged(updated)
                }
                .background(if (!entry.isChecked) greenBackground else defaultBackground)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = defaultValueAsString,
                fontSize = 12.sp,
                color = if (!entry.isChecked) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onRecalculate: () -> Unit,
    onDelete: () -> Unit,
    onIgnore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(stringResource(R.string.saveButtonText))
            }

            Button(
                onClick = onRecalculate,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_refresh_24),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.recalculate))
            }
        }

        // Secondary action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_delete_black_24dp),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.delete))
            }

            Button(
                onClick = onIgnore,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.ignore))
            }
        }

        // Back button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(stringResource(R.string.back))
        }
    }
}

/**
 * Extract data from GPX files and populate table entries
 */
private suspend fun extractDataFromFilesAndPutIntoView(
    context: Context,
    summit: Summit,
    tableEntries: MutableList<TableEntry>,
    onLoadingChanged: (Boolean) -> Unit
) {
    tableEntries.clear()
    val gpxPyJsonFile = summit.getGpxPyPath().toFile()
    if (gpxPyJsonFile.exists()) {
        extractGpxPyJson(gpxPyJsonFile, summit, tableEntries)
    } else {
        onLoadingChanged(true)
        recalculateSummit(context, summit, tableEntries, onLoadingChanged)
        // After recalculation, extract the data again
        val updatedGpxPyJsonFile = summit.getGpxPyPath().toFile()
        if (updatedGpxPyJsonFile.exists()) {
            extractGpxPyJson(updatedGpxPyJsonFile, summit, tableEntries)
        }
    }
}

/**
 * Extract data from GPX JSON file
 */
private fun extractGpxPyJson(
    gpxPyJsonFile: File,
    summit: Summit,
    tableEntries: MutableList<TableEntry>
) {
    val gpxPyJson = JsonParser.parseString(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

    AdditionalDataTableEntry.entries.filter { it.jsonKey != "" }.forEach {
        if (gpxPyJson.has(it.jsonKey)) {
            val value = gpxPyJson.getAsJsonPrimitive(it.jsonKey).asDouble * it.scaleFactorForJson(gpxPyJson)
            if (value > 0) {
                // Calculate initial isChecked state when creating new entries
                val initialIsChecked = if (it.isInt) {
                    it.getValue(summit).roundToInt() == value.roundToInt()
                } else {
                    abs(it.getValue(summit) - value) * it.scaleFactorView < 0.05
                }
                tableEntries.add(TableEntry(value, summit, it, initialIsChecked))
            }
        }
    }
}

/**
 * Recalculate summit data using GpxPyExecutor
 */
private suspend fun recalculateSummit(
    context: Context,
    summit: Summit,
    tableEntries: MutableList<TableEntry>,
    onLoadingChanged: (Boolean) -> Unit
) {
    onLoadingChanged(true)
    withContext(Dispatchers.IO) {
        pythonInstance?.let { python ->
            try {
                Log.i(
                    "AsyncSimplifyGpsTracks",
                    "Simplifying track ${summit.getDateAsString()}_${summit.name}."
                )
                GpxPyExecutor(python).analyzeGpxTrackAndCreateGpxPyDataFile(summit)
            } catch (ex: RuntimeException) {
                Log.e(
                    "AsyncSimplifyGpsTracks",
                    "Error in simplify track for ${summit.getDateAsString()}_${summit.name}: ${ex.message}"
                )
            }
        }
    }
    onLoadingChanged(false)
}

/**
 * Ignore all additional data by resetting to default values
 */
private fun ignoreAllAdditionalData(summit: Summit) {
    summit.velocityData = VelocityData(
        summit.velocityData.maxVelocity
    )
    summit.elevationData = ElevationData(
        summit.elevationData.maxElevation,
        summit.elevationData.elevationGain
    )
}

/**
 * Data class representing a table entry with checkbox state
 */
data class TableEntry(
    var value: Double,
    var summit: Summit,
    var tableEntry: AdditionalDataTableEntry,
    var isChecked: Boolean = false
) {

    fun update() {
        if (value > 0.0) {
            val valueToSet = if (isChecked) value else tableEntry.getValue(summit)
            tableEntry.setValue(summit, valueToSet)
        }
    }
}