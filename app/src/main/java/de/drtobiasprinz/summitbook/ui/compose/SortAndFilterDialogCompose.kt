package de.drtobiasprinz.summitbook.ui.compose

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.HasGpxTrackButtonGroup
import de.drtobiasprinz.summitbook.models.HasImageButtonGroup
import de.drtobiasprinz.summitbook.models.HasPositionButtonGroup
import de.drtobiasprinz.summitbook.models.OrderByAscDescButtonGroup
import de.drtobiasprinz.summitbook.models.OrderBySpinnerEntry
import de.drtobiasprinz.summitbook.models.PeakFavoriteButtonGroup
import de.drtobiasprinz.summitbook.models.RangeSliderValues
import de.drtobiasprinz.summitbook.models.SortFilterValues
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Jetpack Compose version of SortAndFilterFragment
 * Replaces the DialogFragment-based implementation with a modern Compose UI
 */
@Composable
fun SortAndFilterDialogCompose(
    sortFilterValues: SortFilterValues,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    summits: List<Summit>
) {

    // State variables for UI components
    var selectedDateSpinner by remember { mutableIntStateOf(sortFilterValues.selectedDateSpinner) }
    var startDate by remember { mutableStateOf(sortFilterValues.startDate) }
    var endDate by remember { mutableStateOf(sortFilterValues.endDate) }
    var sportType by remember { mutableStateOf(sortFilterValues.sportType) }
    var participants by remember { mutableStateOf(sortFilterValues.participants) }

    // Button group states
    var orderByAscDesc by remember { mutableStateOf(sortFilterValues.orderByAscDescButtonGroup) }
    var orderByValue by remember { mutableStateOf(sortFilterValues.orderByValueSpinner) }
    var hasGpxTrack by remember { mutableStateOf(sortFilterValues.hasGpxTrackButtonGroup) }
    var hasPosition by remember { mutableStateOf(sortFilterValues.hasPositionButtonGroup) }
    var hasImage by remember { mutableStateOf(sortFilterValues.hasImageButtonGroup) }
    var peakFavorite by remember { mutableStateOf(sortFilterValues.peakFavoriteButtonGroup) }

    // Range slider values
    var kilometersSlider by remember {
        mutableStateOf(
            sortFilterValues.kilometersSlider.apply {
                stepSize = 5f
            }
        )
    }
    var elevationGainSlider by remember {
        mutableStateOf(
            sortFilterValues.elevationGainSlider.apply {
                stepSize = 250f
            }
        )
    }
    var topElevationSlider by remember {
        mutableStateOf(
            sortFilterValues.topElevationSlider.apply {
                stepSize = 250f
            }
        )
    }

    // Text field for participants input
    var participantInput by remember { mutableStateOf("") }

    // Years for date spinner
    val years = sortFilterValues.years

    // Date format
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", LocalConfiguration.current.locales[0])

    // Get suggestions for participants
    val participantSuggestions = remember(summits) {
        summits.flatMap { it.participants }.distinct()
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
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
                    // Header with close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.sort_entries),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_clear_24),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DateSpinner(
                        selectedDateSpinner = selectedDateSpinner,
                        years = years,
                        onDateSpinnerChange = { selectedDateSpinner = it },
                        startDate = startDate,
                        endDate = endDate,
                        onStartDateChange = { startDate = it },
                        onEndDateChange = { endDate = it },
                        dateFormat = dateFormat
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SortBySection(
                        orderByValue = orderByValue,
                        onOrderByValueChange = { orderByValue = it },
                        orderByAscDesc = orderByAscDesc,
                        onOrderByAscDescChange = { orderByAscDesc = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filter By Section
                    Text(
                        text = stringResource(R.string.filter_by),
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Sport Type Filter
                    SportTypeFilter(
                        selectedSportType = sportType,
                        onSportTypeChange = { sportType = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Participants Filter
                    ParticipantsFilter(
                        participants = participants,
                        participantInput = participantInput,
                        suggestions = participantSuggestions,
                        onParticipantInputChange = { participantInput = it },
                        onAddParticipant = { participant ->
                            if (participant.isNotBlank() && !participants.contains(participant)) {
                                participants = (participants + participant).toMutableList()
                            }
                            participantInput = ""
                        },
                        onRemoveParticipant = { participant ->
                            participants = participants.filter { it != participant }.toMutableList()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle Button Groups in Collapsible Card
                    var toggleGroupsExpanded by remember { mutableStateOf(false) }
                    CollapsibleSection(
                        title = stringResource(R.string.filter_options),
                        expanded = toggleGroupsExpanded,
                        onExpandChange = { toggleGroupsExpanded = it }
                    ) {
                        ToggleButtonGroups(
                            hasGpxTrack = hasGpxTrack,
                            onHasGpxTrackChange = { hasGpxTrack = it },
                            hasPosition = hasPosition,
                            onHasPositionChange = { hasPosition = it },
                            hasImage = hasImage,
                            onHasImageChange = { hasImage = it },
                            peakFavorite = peakFavorite,
                            onPeakFavoriteChange = { peakFavorite = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Range Sliders in Collapsible Card
                    var rangeSlidersExpanded by remember { mutableStateOf(false) }
                    CollapsibleSection(
                        title = stringResource(R.string.range_filters),
                        expanded = rangeSlidersExpanded,
                        onExpandChange = { rangeSlidersExpanded = it }
                    ) {
                        RangeSliders(
                            summits = summits,
                            kilometersSlider = kilometersSlider,
                            onKilometersSliderChange = { kilometersSlider = it },
                            elevationGainSlider = elevationGainSlider,
                            onElevationGainSliderChange = { elevationGainSlider = it },
                            topElevationSlider = topElevationSlider,
                            onTopElevationSliderChange = { topElevationSlider = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    ActionButtons(
                        onApply = {
                            // Update the sortFilterValues with current selections
                            sortFilterValues.selectedDateSpinner = selectedDateSpinner
                            sortFilterValues.startDate = startDate
                            sortFilterValues.endDate = endDate
                            sortFilterValues.sportType = sportType
                            sortFilterValues.participants = participants.toList()
                            sortFilterValues.orderByAscDescButtonGroup = orderByAscDesc
                            sortFilterValues.orderByValueSpinner = orderByValue
                            sortFilterValues.hasGpxTrackButtonGroup = hasGpxTrack
                            sortFilterValues.hasPositionButtonGroup = hasPosition
                            sortFilterValues.hasImageButtonGroup = hasImage
                            sortFilterValues.peakFavoriteButtonGroup = peakFavorite
                            sortFilterValues.kilometersSlider = kilometersSlider
                            sortFilterValues.elevationGainSlider = elevationGainSlider
                            sortFilterValues.topElevationSlider = topElevationSlider

                            // Apply the filters
                            onApply()
                            onDismiss()
                        },
                        onApplyAll = {
                            sortFilterValues.setToDefault(0)
                            startDate = null
                            endDate = null
                            selectedDateSpinner = 0
                            sportType = null
                            participants = emptyList()
                            orderByAscDesc = OrderByAscDescButtonGroup.Descending
                            orderByValue = OrderBySpinnerEntry.Date
                            hasGpxTrack = HasGpxTrackButtonGroup.Indifferent
                            hasPosition = HasPositionButtonGroup.Indifferent
                            hasImage = HasImageButtonGroup.Indifferent
                            peakFavorite = PeakFavoriteButtonGroup.Indifferent

                            // Reset range sliders
                            kilometersSlider =
                                RangeSliderValues({ e -> e.kilometers.toFloat() }).apply {
                                    stepSize = 5f
                                }
                            elevationGainSlider =
                                RangeSliderValues({ e -> e.elevationData.elevationGain.toFloat() }).apply {
                                    stepSize = 250f
                                }
                            topElevationSlider =
                                RangeSliderValues({ e -> e.elevationData.maxElevation.toFloat() }).apply {
                                    stepSize = 250f
                                }

                            onApply()
                            onDismiss()
                        },
                        onReset = {
                            sortFilterValues.setToDefault()
                            onApply()
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateSpinner(
    selectedDateSpinner: Int,
    years: List<String>,
    onDateSpinnerChange: (Int) -> Unit,
    startDate: Date?,
    endDate: Date?,
    onStartDateChange: (Date?) -> Unit,
    onEndDateChange: (Date?) -> Unit,
    dateFormat: SimpleDateFormat
) {
    val context = LocalContext.current

    // Combine default options with years
    val dateOptions = listOf(
        stringResource(R.string.all),
        stringResource(R.string.from) + " - " + stringResource(R.string.until)
    ) + years

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            value = if (selectedDateSpinner < dateOptions.size) dateOptions[selectedDateSpinner] else "",
            onValueChange = {},
            label = { Text(stringResource(R.string.set_date)) },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                    contentDescription = null
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            dateOptions.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onDateSpinnerChange(index)
                        expanded = false

                        // Handle date range visibility
                        if (index == 1) { // Custom date range
                            // Dates will be set via date pickers
                        } else if (index >= 2) { // Year selected
                            // Set start and end dates for the year
                            val year = years[index - 2].toIntOrNull()
                            if (year != null) {
                                val cal = Calendar.getInstance()
                                cal.set(year, 0, 1, 0, 0, 0)
                                cal[Calendar.MILLISECOND] = 0
                                onStartDateChange(cal.time)

                                cal.set(year, 11, 31, 23, 59, 59)
                                cal[Calendar.MILLISECOND] = 999
                                onEndDateChange(cal.time)
                            }
                        } else {
                            onStartDateChange(null)
                            onEndDateChange(null)
                        }
                    }
                )
            }
        }
    }

    // Show date pickers if custom date range is selected
    if (selectedDateSpinner == 1) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start Date Picker
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                readOnly = true,
                value = startDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                label = { Text(stringResource(R.string.from)) },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        startDate?.let { cal.time = it }
                        val year = cal[Calendar.YEAR]
                        val month = cal[Calendar.MONTH]
                        val day = cal[Calendar.DAY_OF_MONTH]

                        val datePicker = DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                val newDate = Calendar.getInstance()
                                newDate.set(selectedYear, selectedMonth, selectedDay)
                                onStartDateChange(newDate.time)
                            },
                            year, month, day
                        )
                        datePicker.show()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_today_black_24dp),
                            contentDescription = stringResource(R.string.from)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // End Date Picker
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                readOnly = true,
                value = endDate?.let { dateFormat.format(it) } ?: "",
                onValueChange = {},
                label = { Text(stringResource(R.string.until)) },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance()
                        endDate?.let { cal.time = it }
                        val year = cal[Calendar.YEAR]
                        val month = cal[Calendar.MONTH]
                        val day = cal[Calendar.DAY_OF_MONTH]

                        val datePicker = DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                val newDate = Calendar.getInstance()
                                newDate.set(selectedYear, selectedMonth, selectedDay)
                                onEndDateChange(newDate.time)
                            },
                            year, month, day
                        )
                        datePicker.show()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_today_black_24dp),
                            contentDescription = stringResource(R.string.until)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun SortBySection(
    orderByValue: OrderBySpinnerEntry,
    onOrderByValueChange: (OrderBySpinnerEntry) -> Unit,
    orderByAscDesc: OrderByAscDescButtonGroup,
    onOrderByAscDescChange: (OrderByAscDescButtonGroup) -> Unit
) {
    // Sort by dropdown
    val sortOptions = OrderBySpinnerEntry.getSpinnerEntriesWithoutAccumulated()
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            value = stringResource(orderByValue.nameId),
            onValueChange = {},
            label = { Text(stringResource(R.string.sort_by)) },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                    contentDescription = null
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            sortOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.nameId)) },
                    onClick = {
                        onOrderByValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Ascending/Descending toggle
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        FilterChip(
            selected = orderByAscDesc == OrderByAscDescButtonGroup.Ascending,
            onClick = { onOrderByAscDescChange(OrderByAscDescButtonGroup.Ascending) },
            label = { Text(stringResource(R.string.ascending)) }
        )

        Spacer(modifier = Modifier.width(8.dp))

        FilterChip(
            selected = orderByAscDesc == OrderByAscDescButtonGroup.Descending,
            onClick = { onOrderByAscDescChange(OrderByAscDescButtonGroup.Descending) },
            label = { Text(stringResource(R.string.descending)) }
        )
    }
}

@Composable
fun SportTypeFilter(
    selectedSportType: SportType?,
    onSportTypeChange: (SportType?) -> Unit
) {
    val sportTypes = listOf(null) + SportType.entries
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            value = selectedSportType?.let { stringResource(it.sportNameStringId) }
                ?: stringResource(R.string.all),
            onValueChange = {},
            label = { Text(stringResource(R.string.activity_hint)) },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                    contentDescription = null
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            sportTypes.forEach { sportType ->
                DropdownMenuItem(
                    text = {
                        Text(
                            sportType?.let { stringResource(it.sportNameStringId) }
                                ?: stringResource(R.string.all)
                        )
                    },
                    onClick = {
                        onSportTypeChange(sportType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ParticipantsFilter(
    participants: List<String>,
    participantInput: String,
    suggestions: List<String>,
    onParticipantInputChange: (String) -> Unit,
    onAddParticipant: (String) -> Unit,
    onRemoveParticipant: (String) -> Unit
) {
    Column {
        // Input field with suggestions
        var showSuggestions by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = participantInput,
            onValueChange = { newValue ->
                onParticipantInputChange(newValue)
                showSuggestions = newValue.isNotBlank()
            },
            label = { Text(stringResource(R.string.participants)) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (participantInput.isNotBlank()) {
                    IconButton(onClick = { onAddParticipant(participantInput) }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_done_24),
                            contentDescription = stringResource(R.string.addGpsTrack)
                        )
                    }
                }
            }
        )

        // Suggestions dropdown
        if (showSuggestions && participantInput.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(4.dp),
                tonalElevation = 4.dp
            ) {
                Column {
                    suggestions
                        .filter {
                            it.contains(participantInput, ignoreCase = true) &&
                                    !participants.contains(it)
                        }
                        .take(5)
                        .forEach { suggestion ->
                            TextButton(
                                onClick = {
                                    onAddParticipant(suggestion)
                                    showSuggestions = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(suggestion)
                            }
                        }
                }
            }
        }

        // Display added participants as chips
        if (participants.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                participants.forEach { participant ->
                    AssistChip(
                        onClick = { onRemoveParticipant(participant) },
                        label = { Text(participant) },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_clear_24),
                                contentDescription = stringResource(R.string.delete_icon),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun ToggleButtonGroups(
    hasGpxTrack: HasGpxTrackButtonGroup,
    onHasGpxTrackChange: (HasGpxTrackButtonGroup) -> Unit,
    hasPosition: HasPositionButtonGroup,
    onHasPositionChange: (HasPositionButtonGroup) -> Unit,
    hasImage: HasImageButtonGroup,
    onHasImageChange: (HasImageButtonGroup) -> Unit,
    peakFavorite: PeakFavoriteButtonGroup,
    onPeakFavoriteChange: (PeakFavoriteButtonGroup) -> Unit
) {
    // GPX Track Filter
    Text(
        text = stringResource(R.string.with_gpx),
        style = MaterialTheme.typography.titleSmall
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FilterChip(
            selected = hasGpxTrack == HasGpxTrackButtonGroup.Yes,
            onClick = { onHasGpxTrackChange(HasGpxTrackButtonGroup.Yes) },
            label = { Text(stringResource(R.string.yes)) }
        )

        FilterChip(
            selected = hasGpxTrack == HasGpxTrackButtonGroup.Indifferent,
            onClick = { onHasGpxTrackChange(HasGpxTrackButtonGroup.Indifferent) },
            label = { Text(stringResource(R.string.all)) }
        )

        FilterChip(
            selected = hasGpxTrack == HasGpxTrackButtonGroup.No,
            onClick = { onHasGpxTrackChange(HasGpxTrackButtonGroup.No) },
            label = { Text(stringResource(R.string.no)) }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Position Filter
    Text(
        text = stringResource(R.string.with_position),
        style = MaterialTheme.typography.titleSmall
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FilterChip(
            selected = hasPosition == HasPositionButtonGroup.Yes,
            onClick = { onHasPositionChange(HasPositionButtonGroup.Yes) },
            label = { Text(stringResource(R.string.yes)) }
        )

        FilterChip(
            selected = hasPosition == HasPositionButtonGroup.Indifferent,
            onClick = { onHasPositionChange(HasPositionButtonGroup.Indifferent) },
            label = { Text(stringResource(R.string.all)) }
        )

        FilterChip(
            selected = hasPosition == HasPositionButtonGroup.No,
            onClick = { onHasPositionChange(HasPositionButtonGroup.No) },
            label = { Text(stringResource(R.string.no)) }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Image Filter
    Text(
        text = stringResource(R.string.with_image),
        style = MaterialTheme.typography.titleSmall
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FilterChip(
            selected = hasImage == HasImageButtonGroup.Yes,
            onClick = { onHasImageChange(HasImageButtonGroup.Yes) },
            label = { Text(stringResource(R.string.yes)) }
        )

        FilterChip(
            selected = hasImage == HasImageButtonGroup.Indifferent,
            onClick = { onHasImageChange(HasImageButtonGroup.Indifferent) },
            label = { Text(stringResource(R.string.all)) }
        )

        FilterChip(
            selected = hasImage == HasImageButtonGroup.No,
            onClick = { onHasImageChange(HasImageButtonGroup.No) },
            label = { Text(stringResource(R.string.no)) }
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Marked Summits Filter
    Text(
        text = stringResource(R.string.marked_summits),
        style = MaterialTheme.typography.titleSmall
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FilterChip(
            selected = peakFavorite == PeakFavoriteButtonGroup.IsFavorite,
            onClick = { onPeakFavoriteChange(PeakFavoriteButtonGroup.IsFavorite) },
            label = {
                Icon(
                    painter = painterResource(R.drawable.baseline_star_black_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = peakFavorite == PeakFavoriteButtonGroup.Indifferent,
            onClick = { onPeakFavoriteChange(PeakFavoriteButtonGroup.Indifferent) },
            label = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_select_all_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        FilterChip(
            selected = peakFavorite == PeakFavoriteButtonGroup.IsPeak,
            onClick = { onPeakFavoriteChange(PeakFavoriteButtonGroup.IsPeak) },
            label = {
                Icon(
                    painter = painterResource(R.drawable.outline_landscape_2_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

@Composable
fun RangeSliders(
    summits: List<Summit>,
    kilometersSlider: RangeSliderValues,
    onKilometersSliderChange: (RangeSliderValues) -> Unit,
    elevationGainSlider: RangeSliderValues,
    onElevationGainSliderChange: (RangeSliderValues) -> Unit,
    topElevationSlider: RangeSliderValues,
    onTopElevationSliderChange: (RangeSliderValues) -> Unit
) {
    if (summits.isNotEmpty()) {
        // Update slider ranges based on summits data
        val updatedKilometersSlider =
            updateRangeSliderValues(summits, kilometersSlider, kilometersSlider.stepSize)
        val updatedElevationGainSlider =
            updateRangeSliderValues(summits, elevationGainSlider, elevationGainSlider.stepSize)
        val updatedTopElevationSlider =
            updateRangeSliderValues(summits, topElevationSlider, topElevationSlider.stepSize)

        // Kilometers Range Slider
        RangeSliderComponent(
            title = stringResource(R.string.filter_by_kilometer),
            minValue = updatedKilometersSlider.selectedMin.roundToInt().toString(),
            maxValue = updatedKilometersSlider.selectedMax.roundToInt().toString(),
            unit = stringResource(R.string.km),
            valueFrom = updatedKilometersSlider.totalMin,
            valueTo = updatedKilometersSlider.totalMax,
            values = listOf(
                updatedKilometersSlider.selectedMin,
                updatedKilometersSlider.selectedMax
            ),
            onValueChange = { values ->
                val updated = updatedKilometersSlider.copy()
                updated.selectedMin = values[0]
                updated.selectedMax = values[1]
                onKilometersSliderChange(updated)
            },
            step = updatedKilometersSlider.stepSize
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Elevation Gain Range Slider
        RangeSliderComponent(
            title = stringResource(R.string.filterByHm),
            minValue = updatedElevationGainSlider.selectedMin.roundToInt().toString(),
            maxValue = updatedElevationGainSlider.selectedMax.roundToInt().toString(),
            unit = stringResource(R.string.hm),
            valueFrom = updatedElevationGainSlider.totalMin,
            valueTo = updatedElevationGainSlider.totalMax,
            values = listOf(
                updatedElevationGainSlider.selectedMin,
                updatedElevationGainSlider.selectedMax
            ),
            onValueChange = { values ->
                val updated = updatedElevationGainSlider.copy()
                updated.selectedMin = values[0]
                updated.selectedMax = values[1]
                onElevationGainSliderChange(updated)
            },
            step = updatedElevationGainSlider.stepSize
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Top Elevation Range Slider
        RangeSliderComponent(
            title = stringResource(R.string.filterByTopElevation),
            minValue = updatedTopElevationSlider.selectedMin.roundToInt().toString(),
            maxValue = updatedTopElevationSlider.selectedMax.roundToInt().toString(),
            unit = stringResource(R.string.masl),
            valueFrom = updatedTopElevationSlider.totalMin,
            valueTo = updatedTopElevationSlider.totalMax,
            values = listOf(
                updatedTopElevationSlider.selectedMin,
                updatedTopElevationSlider.selectedMax
            ),
            onValueChange = { values ->
                val updated = updatedTopElevationSlider.copy()
                updated.selectedMin = values[0]
                updated.selectedMax = values[1]
                onTopElevationSliderChange(updated)
            },
            step = updatedTopElevationSlider.stepSize
        )
    }
}

@Composable
fun RangeSliderComponent(
    title: String,
    minValue: String,
    maxValue: String,
    unit: String,
    valueFrom: Float,
    valueTo: Float,
    values: List<Float>,
    onValueChange: (List<Float>) -> Unit,
    step: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "$title: $minValue - $maxValue $unit")
        }

        Spacer(modifier = Modifier.height(8.dp))

        RangeSlider(
            value = values[0]..values[1],
            onValueChange = { range -> onValueChange(listOf(range.start, range.endInclusive)) },
            valueRange = valueFrom..valueTo,
            steps = if (step > 0f) ((valueTo - valueFrom) / step).toInt() - 1 else 0,
            onValueChangeFinished = { /* Handle value change finished if needed */ }
        )
    }
}

fun updateRangeSliderValues(
    summits: List<Summit>,
    values: RangeSliderValues,
    stepSize: Float
): RangeSliderValues {
    if (summits.isEmpty()) return values

    val updatedValues = values.copy()

    val min = updatedValues.getValue(summits.minByOrNull { updatedValues.getValue(it) }!!)
    updatedValues.totalMin = floor(min / stepSize) * stepSize
    if (updatedValues.totalMin > 0) updatedValues.totalMin = 0f

    val max = updatedValues.getValue(summits.maxByOrNull { updatedValues.getValue(it) }!!)
    updatedValues.totalMax = ceil(max / stepSize) * stepSize

    if (updatedValues.selectedMin !in updatedValues.totalMin..updatedValues.totalMax) {
        updatedValues.selectedMin = updatedValues.totalMin
    }
    if (updatedValues.selectedMax !in updatedValues.totalMin..updatedValues.totalMax || updatedValues.selectedMax == 0f) {
        updatedValues.selectedMax = updatedValues.totalMax
    }

    updatedValues.stepSize = stepSize

    return updatedValues
}

@Composable
fun ActionButtons(
    onApply: () -> Unit,
    onApplyAll: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(
            onClick = onApply,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Green, shape = RoundedCornerShape(24.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_done_24),
                contentDescription = stringResource(R.string.apply),
                tint = Color.White
            )
        }

        IconButton(
            onClick = onApplyAll,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Green, shape = RoundedCornerShape(24.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_filter_list_off_24),
                contentDescription = stringResource(R.string.apply),
                tint = Color.White
            )
        }

        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Red, shape = RoundedCornerShape(24.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_cancel_24),
                contentDescription = stringResource(R.string.set_to_default),
                tint = Color.White
            )
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (expanded) 16.dp else 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onExpandChange(!expanded) }) {
                    Icon(
                        painter = painterResource( if (expanded) R.drawable.baseline_arrow_drop_up_black_24dp else  R.drawable.baseline_arrow_drop_down_24),
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
            
            if (expanded) {
                content()
            }
        }
    }
}

// Helper function to create a copy of RangeSliderValues
fun RangeSliderValues.copy(): RangeSliderValues {
    return RangeSliderValues(
        getValue = this.getValue,
        totalMin = this.totalMin,
        selectedMin = this.selectedMin,
        selectedMax = this.selectedMax,
        totalMax = this.totalMax,
        stepSize = this.stepSize
    )
}