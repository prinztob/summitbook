package de.drtobiasprinz.summitbook.ui.compose

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.utils.JsonUtils
import de.drtobiasprinz.summitbook.data.appstate.AppState.pythonInstance
import de.drtobiasprinz.summitbook.data.db.entities.ElevationData
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.db.entities.VelocityData
import de.drtobiasprinz.summitbook.data.model.AdditionalDataTableEntry
import de.drtobiasprinz.summitbook.sync.GpxPyExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "AddAdditionalDataDialog"

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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val recalculateFailedText = stringResource(R.string.recalculate_failed)
    val retryText = stringResource(R.string.retry)

    var isLoading by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var extractionDone by rememberSaveable { mutableStateOf(false) }
    val tableEntries = rememberSaveable(saver = tableEntriesSaver) { mutableStateListOf<TableEntry>() }
    var summitEntry by rememberSaveable(stateSaver = jsonSaver<Summit>()) { mutableStateOf(summit.clone()) }

    suspend fun reloadTableEntries(forceRecalculate: Boolean) {
        var success = loadTableEntries(summitEntry, tableEntries, forceRecalculate) { isLoading = it }
        while (!success) {
            val result = snackbarHostState.showSnackbar(
                message = recalculateFailedText,
                actionLabel = retryText,
                duration = SnackbarDuration.Long
            )
            if (result != SnackbarResult.ActionPerformed) break
            success = loadTableEntries(summitEntry, tableEntries, forceRecalculate) { isLoading = it }
        }
    }

    LaunchedEffect(summit) {
        if (!extractionDone && tableEntries.isEmpty()) {
            reloadTableEntries(forceRecalculate = false)
            extractionDone = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.add_additional_data),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_close_24),
                                contentDescription = stringResource(R.string.close)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    TableHeader()

                    if (tableEntries.isEmpty() && !isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_additional_data_available),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
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

                    ActionButtons(
                        isLoading = isLoading,
                        onSave = {
                            var changed = false
                            tableEntries.forEach { entry ->
                                changed = entry.applyTo(summitEntry) || changed
                            }
                            if (changed) {
                                summitEntry.updated += 1
                                onSaveSummit(true, summitEntry)
                            }
                            onDismiss()
                        },
                        onRecalculate = {
                            scope.launch { reloadTableEntries(forceRecalculate = true) }
                        },
                        onDelete = {
                            showDeleteConfirmation = true
                        },
                        onIgnore = {
                            ignoreAllAdditionalData(summitEntry)
                            onSaveSummit(true, summitEntry)
                            onDismiss()
                        }
                    )
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                if (isLoading) {
                    LoadingPanel(visible = true)
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.delete_gpx_data_title)) },
            text = { Text(stringResource(R.string.delete_gpx_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                summitEntry.getGpxPyPath().toFile().delete()
                                summitEntry.getGpsTrackPath(simplified = true).toFile().delete()
                            }
                            ignoreAllAdditionalData(summitEntry)
                            onSaveSummit(true, summitEntry)
                            onDismiss()
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancelButtonText))
                }
            }
        )
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.entry),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.new_value),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.old_value),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val locale = LocalConfiguration.current.locales[0]
    val unit = stringResource(entry.tableEntry.unitId)
    val currentValue = entry.tableEntry.getValue(summit)

    val oldValueAsString = if (isSameValue(entry.tableEntry, entry.value, currentValue)) {
        "-"
    } else {
        formatValue(locale, entry.tableEntry, currentValue, unit)
    }
    val newValueAsString = formatValue(locale, entry.tableEntry, entry.value, unit)

    val zebraBackground = if (index % 2 == 1) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .background(zebraBackground)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(entry.tableEntry.nameId),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        ValueCell(
            text = newValueAsString,
            selected = entry.isChecked,
            onClick = { onEntryChanged(entry.copy(isChecked = true)) },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        ValueCell(
            text = oldValueAsString,
            selected = !entry.isChecked,
            onClick = { onEntryChanged(entry.copy(isChecked = false)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ValueCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionButtons(
    isLoading: Boolean,
    onSave: () -> Unit,
    onRecalculate: () -> Unit,
    onDelete: () -> Unit,
    onIgnore: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.saveButtonText))
            }
            OutlinedButton(
                onClick = onRecalculate,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_refresh_24),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.recalculate))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onIgnore,
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.ignore))
            }
            TextButton(
                onClick = onDelete,
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_delete_black_24dp),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.delete))
            }
        }
    }
}

/**
 * Load the additional-data table; recalculates the GPX analysis first when it is
 * missing or explicitly requested. Returns false if a forced recalculation failed.
 */
private suspend fun loadTableEntries(
    summit: Summit,
    tableEntries: SnapshotStateList<TableEntry>,
    forceRecalculate: Boolean,
    onLoadingChanged: (Boolean) -> Unit
): Boolean {
    tableEntries.clear()
    onLoadingChanged(true)
    try {
        if (forceRecalculate || !summit.getGpxPyPath().toFile().exists()) {
            if (!recalculateSummit(summit)) return false
        }
        val gpxPyJsonFile = summit.getGpxPyPath().toFile()
        if (gpxPyJsonFile.exists()) {
            withContext(Dispatchers.IO) {
                extractGpxPyJson(gpxPyJsonFile, summit, tableEntries)
            }
        }
        return true
    } finally {
        onLoadingChanged(false)
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
                tableEntries.add(
                    TableEntry(value, it, isSameValue(it, it.getValue(summit), value))
                )
            }
        }
    }
}

/**
 * Recalculate summit data using GpxPyExecutor. Returns false if recalculation failed.
 */
private suspend fun recalculateSummit(summit: Summit): Boolean {
    val python = pythonInstance ?: return false
    return try {
        withContext(Dispatchers.IO) {
            GpxPyExecutor(python).analyzeGpxTrackAndCreateGpxPyDataFile(summit)
        }
        true
    } catch (ex: RuntimeException) {
        Log.e(TAG, "Error analyzing track ${summit.getDateAsString()}_${summit.name}: ${ex.message}")
        false
    }
}

/**
 * True when both values are equivalent in the table's display precision.
 */
private fun isSameValue(tableEntry: AdditionalDataTableEntry, a: Double, b: Double): Boolean =
    if (tableEntry.isInt) {
        a.roundToInt() == b.roundToInt()
    } else {
        abs(a - b) * tableEntry.scaleFactorView < 0.05
    }

private fun formatValue(
    locale: Locale,
    tableEntry: AdditionalDataTableEntry,
    value: Double,
    unit: String
): String = String.format(
    locale,
    if (tableEntry.isInt) "%.0f %s" else "%.1f %s",
    value * tableEntry.scaleFactorView,
    unit
)

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
 * Data class representing a table entry with selection state
 */
data class TableEntry(
    val value: Double,
    val tableEntry: AdditionalDataTableEntry,
    val isChecked: Boolean = false
) {

    /**
     * Writes the selected value into [summit] and returns whether it changed the summit.
     */
    fun applyTo(summit: Summit): Boolean {
        if (value <= 0.0) return false
        val current = tableEntry.getValue(summit)
        val target = if (isChecked) value else current
        tableEntry.setValue(summit, target)
        return !isSameValue(tableEntry, current, target)
    }
}

/**
 * Persists the additional-data table (selection state included) across
 * rotation/process death. Only primitives are saved; [TableEntry] intentionally
 * holds no [Summit] so nothing large lands in the saved-state Bundle and the
 * restored entries operate on the live [Summit] again after restore.
 */
private val tableEntriesSaver = listSaver<SnapshotStateList<TableEntry>, Any>(
    save = { list -> list.flatMap { listOf(it.value, it.tableEntry.name, it.isChecked) } },
    restore = { stored ->
        mutableStateListOf<TableEntry>().also { restored ->
            stored.chunked(3).forEach { chunk ->
                runCatching {
                    TableEntry(
                        value = chunk[0] as Double,
                        tableEntry = AdditionalDataTableEntry.valueOf(chunk[1] as String),
                        isChecked = chunk[2] as Boolean
                    )
                }.getOrNull()?.let(restored::add)
            }
        }
    }
)
