package de.drtobiasprinz.summitbook.ui.compose

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.core.DataStatus
import de.drtobiasprinz.summitbook.data.db.entities.IgnoredActivity
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.sync.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.activities.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import de.drtobiasprinz.summitbook.data.appstate.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowNewSummitsFromGarminScreen(
    viewModel: DatabaseViewModel,
    summits: List<Summit>,
    modifier: Modifier = Modifier,
    selectedDate: Date? = null,
    onBack: (List<Summit>, Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    // State variables
    var startDate by rememberSaveable(stateSaver = nonNullDateSaver) {
        mutableStateOf(getDefaultStartDate(selectedDate))
    }
    var endDate by rememberSaveable(stateSaver = nonNullDateSaver) {
        mutableStateOf(getDefaultEndDate(selectedDate))
    }
    var showAllButtonEnabled by rememberSaveable { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var entriesWithoutIgnored by remember { mutableStateOf<MutableList<Summit>>(mutableListOf()) }
    val ignoredActivityStatus by viewModel.ignoredActivityList.asFlow()
        .collectAsStateWithLifecycle(initialValue = DataStatus.loading())
    val ignoredActivities = ignoredActivityStatus.data ?: emptyList<IgnoredActivity>()
    var selectedSummits by rememberSaveable(stateSaver = longListSaver) {
        mutableStateOf<List<Long>>(emptyList())
    }
    var canMerge by remember { mutableStateOf(false) }

    // Date format
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Update entries when ignored activities, date range or filters change
    LaunchedEffect(ignoredActivities, startDate, endDate, showAllButtonEnabled) {
        isLoading = true
        val updatedEntries = reloadEntriesWithoutIgnored(
            summits,
            ignoredActivities.map { it.activityId },
            startDate,
            endDate,
            showAllButtonEnabled,
            selectedSummits
        )

        // Update UI on main thread
        entriesWithoutIgnored = updatedEntries
        isLoading = false
        canMerge = canSelectedSummitsBeMerged(updatedEntries.filter { it.isSelected })
    }

    // Main UI
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Date pickers row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start Date Picker
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                readOnly = true,
                value = dateFormat.format(startDate),
                onValueChange = {},
                label = {
                    Text(
                        text = stringResource(R.string.from),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = startDate
                        val year = cal[Calendar.YEAR]
                        val month = cal[Calendar.MONTH]
                        val day = cal[Calendar.DAY_OF_MONTH]

                        val datePicker = DatePickerDialog(
                            context,
                            R.style.CustomDatePickerDialogTheme,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                val newDate = Calendar.getInstance()
                                newDate.set(selectedYear, selectedMonth, selectedDay)
                                startDate = newDate.time
                            },
                            year, month, day
                        )
                        datePicker.show()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_today_black_24dp),
                            contentDescription = stringResource(R.string.from),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )

            // End Date Picker
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                readOnly = true,
                value = dateFormat.format(endDate),
                onValueChange = {},
                label = {
                    Text(
                        text = stringResource(R.string.until),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        cal.time = endDate
                        val year = cal[Calendar.YEAR]
                        val month = cal[Calendar.MONTH]
                        val day = cal[Calendar.DAY_OF_MONTH]

                        val datePicker = DatePickerDialog(
                            context,
                            R.style.CustomDatePickerDialogTheme,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                val newDate = Calendar.getInstance()
                                newDate.set(selectedYear, selectedMonth, selectedDay)
                                endDate = newDate.time
                            },
                            year, month, day
                        )
                        datePicker.show()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_today_black_24dp),
                            contentDescription = stringResource(R.string.until),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }

        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            // Save button
            FloatingActionButton(
                onClick = {
                    val selectedEntries = entriesWithoutIgnored.filter { it.isSelected }
                    if (selectedEntries.isNotEmpty()) {
                        onBack(
                            selectedEntries, false
                        )
                    }
                },
                modifier = Modifier.padding(end = 8.dp),
                containerColor = if (entriesWithoutIgnored.any { it.isSelected }) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_save_black_24dp),
                    contentDescription = stringResource(R.string.saveButtonText)
                )
            }

            // Ignore button
            FloatingActionButton(
                onClick = {
                    val selectedEntries = entriesWithoutIgnored.filter { it.isSelected }
                    if (selectedEntries.isNotEmpty()) {
                        // Update selectedSummits state
                        selectedSummits = selectedSummits.toMutableList().apply {
                            removeAll(selectedEntries.map { it.id })
                            removeAll(selectedEntries.map { it.activityId })
                        }

                        // Remove selected entries from the list
                        entriesWithoutIgnored = entriesWithoutIgnored
                            .minus(selectedEntries)
                            .toMutableList()

                        // Save ignored activities
                        selectedEntries.forEach { summit ->
                            summit.garminData?.activityId?.let { activityId ->
                                viewModel.saveIgnoredActivity(
                                    IgnoredActivity(activityId)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.padding(end = 8.dp),
                containerColor = if (entriesWithoutIgnored.any { it.isSelected }) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_do_not_disturb_on_total_silence_24),
                    contentDescription = stringResource(R.string.ignore)
                )
            }

            // Recalculate button
            FloatingActionButton(
                onClick = {
                    onRefresh()
                },
                modifier = Modifier.padding(end = 8.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_refresh_24),
                    contentDescription = stringResource(R.string.update)
                )
            }
        }

        // Merge and Show All buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    val selectedEntries = entriesWithoutIgnored.filter { it.isSelected }
                    if (canSelectedSummitsBeMerged(selectedEntries)) {
                        onBack(
                            selectedEntries, true
                        )
                    }
                },
                enabled = canMerge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.merge))
            }

            OutlinedButton(
                onClick = {
                    showAllButtonEnabled = !showAllButtonEnabled
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (showAllButtonEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            ) {
                Text(stringResource(R.string.show_all))
            }
        }

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Empty state or list
            if (entriesWithoutIgnored.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_summit),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                val sortedEntries = remember(entriesWithoutIgnored) {
                    entriesWithoutIgnored.sortedByDescending { it.date }
                }
                // Summit list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = sortedEntries,
                        key = { "${it.activityId}-${it.name}-${it.date.time}" }
                    ) { summit ->
                        SummitCardItem(
                            summit = summit,
                            ignoredActivities = ignoredActivities,
                            onSelectionChanged = { isSelected ->
                                summit.isSelected = isSelected
                                // Update selectedSummits state
                                selectedSummits = selectedSummits.toMutableList().apply {
                                    if (isSelected) {
                                        add(summit.id)
                                        add(summit.activityId)
                                    } else {
                                        remove(summit.id)
                                        remove(summit.activityId)
                                    }
                                }
                                // Update canMerge state
                                canMerge = canSelectedSummitsBeMerged(entriesWithoutIgnored.filter { it.isSelected })
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummitCardItem(
    summit: Summit,
    ignoredActivities: List<IgnoredActivity>,
    onSelectionChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isIgnored = summit.garminData?.activityId in ignoredActivities.map { it.activityId }
    var checked by remember(summit) { mutableStateOf(summit.isSelected) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Summit info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Date with link
                Text(
                    text = summit.getDateAsString() ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        summit.garminData?.url?.let { url ->
                            // Open URL in browser
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                url.toUri()
                            )
                            context.startActivity(intent)
                        }
                    }
                )

                // Sport type
                Text(
                    text = if (isIgnored) {
                        "${stringResource(summit.sportType.sportNameStringId)} (${stringResource(R.string.ignored)})"
                    } else {
                        stringResource(summit.sportType.sportNameStringId)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isIgnored) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${
                            String.format(
                                LocalConfiguration.current.locales[0],
                                "%.1f",
                                summit.kilometers
                            )
                        } ${stringResource(R.string.km)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${summit.elevationData.elevationGain} ${stringResource(R.string.hm)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${
                            String.format(
                                LocalConfiguration.current.locales[0],
                                "%.1f",
                                summit.getAverageVelocity()
                            )
                        } ${stringResource(R.string.kmh)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = String.format(
                            LocalConfiguration.current.locales[0],
                            "%.1f",
                            summit.garminData?.vo2max ?: 0f
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Checkbox
            Checkbox(
                checked = checked,
                onCheckedChange = { isChecked ->
                    checked = isChecked
                    onSelectionChanged(isChecked)
                }
            )
        }
    }
}

// Helper functions
private fun getDefaultStartDate(selectedDate: Date?): Date {
    return if (selectedDate != null) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.time
    } else {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1) // One month ago
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.time
    }
}

private fun getDefaultEndDate(selectedDate: Date?): Date {
    return if (selectedDate != null) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        calendar.time
    } else {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        calendar.time
    }
}

private fun getAllActivitiesFromThirdParty(
    activitiesDir: java.io.File?,
    activityIdsInSummitBook: List<String>,
    activitiesIdIgnored: List<String> = emptyList()
): MutableList<Summit> {
    return GarminPythonExecutor.getAllDownloadedSummitsFromGarmin(
        activitiesDir, activityIdsInSummitBook, activitiesIdIgnored
    )
}

private fun updateEntriesWithoutIgnored(
    summits: List<Summit>,
    activitiesIdIgnored: List<String>,
    startDate: Date,
    endDate: Date,
    showAll: Boolean
): MutableList<Summit> {
    // Get activity IDs from garminData.activityIds
    val activityIdsFromGarminData = summits.asSequence()
        .filter { it.garminData?.activityIds?.isNotEmpty() == true }
        .flatMap { it.garminData!!.activityIds.asSequence() }
        .filter { it.isNotBlank() }
        .toList()

    // Get activityId from the summit itself (for backward compatibility)
    val activityIdsFromSummit = summits.map { it.activityId.toString() }
        .filter { it.isNotBlank() && it != "0" }

    // Combine both lists and remove duplicates
    val activityIdsInSummitBook = (activityIdsFromGarminData + activityIdsFromSummit).distinct()

    val allEntries = if (showAll) {
        getAllActivitiesFromThirdParty(
            AppState.activitiesDir,
            activityIdsInSummitBook
        )
    } else {
        getAllActivitiesFromThirdParty(
            AppState.activitiesDir,
            activityIdsInSummitBook,
            activitiesIdIgnored
        )
    }

    // Apply date filtering
    return allEntries.filter { summit ->
        summit.date.time >= startDate.time && summit.date.time <= endDate.time
    }.toMutableList()
}

private suspend fun reloadEntriesWithoutIgnored(
    summits: List<Summit>,
    activitiesIdIgnored: List<String>,
    startDate: Date,
    endDate: Date,
    showAll: Boolean,
    selectedSummits: List<Long>
): MutableList<Summit> {
    val updatedEntries = withContext(Dispatchers.IO) {
        updateEntriesWithoutIgnored(
            summits,
            activitiesIdIgnored,
            startDate,
            endDate,
            showAll
        )
    }

    // Restore selection state
    updatedEntries.forEach { summit ->
        summit.isSelected =
            summit.id in selectedSummits || summit.activityId in selectedSummits
    }

    return updatedEntries
}

private fun canSelectedSummitsBeMerged(summits: List<Summit>): Boolean {
    val selectedSummits = summits.filter { it.isSelected }
    return selectedSummits.map { it.getDateAsString() }.toSet().size == 1 &&
            selectedSummits.size > 1
}
