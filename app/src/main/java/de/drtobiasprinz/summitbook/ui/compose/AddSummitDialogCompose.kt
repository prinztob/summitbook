@file:Suppress("AssignedValueIsNeverRead")

package de.drtobiasprinz.summitbook.ui.compose

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.ui.GpxPyExecutor
import de.drtobiasprinz.summitbook.ui.MainActivityCompose.Companion.pythonInstance
import de.drtobiasprinz.summitbook.ui.utils.GarminTrackAndDataDownloader
import de.drtobiasprinz.summitbook.ui.utils.JsonUtils
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import org.osmdroid.util.GeoPoint
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import de.drtobiasprinz.summitbook.utils.Constants.CONNECTED_ACTIVITY_PREFIX
import java.util.concurrent.TimeUnit
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Jetpack Compose version of AddSummitDialog
 * Replaces the DialogFragment-based implementation with a modern Compose UI
 */
@Composable
fun AddSummitDialogCompose(
    summitsFromDatabase: List<Summit>,
    peaks: List<de.drtobiasprinz.summitbook.db.entities.Peak>,
    summitId: Long = 0L,
    isBookmark: Boolean = false,
    uri: Uri? = null,
    onDismiss: () -> Unit,
    onSaveSummit: (Boolean, Summit) -> Job,
    onPeakToggle: ((String, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEdit = summitId > 0

    // State management
    var entity by remember { mutableStateOf(createEmptySummit(isBookmark, context)) }
    var isLoading by remember { mutableStateOf(false) }
    var temporaryGpxFile by remember { mutableStateOf<File?>(null) }
    var latLngHighestPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var garminDataFromGarminConnect by remember { mutableStateOf<GarminData?>(null) }

    // UI State - Basic fields
    var summitName by remember { mutableStateOf("") }
    var tourDate by remember { mutableStateOf("") }
    var selectedSportType by remember { mutableStateOf(SportType.Bicycle) }
    var kilometers by remember { mutableStateOf("") }
    var heightMeter by remember { mutableStateOf("") }
    var topElevation by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var topSpeed by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }

    // Chip groups
    var participants by remember { mutableStateOf<List<String>>(emptyList()) }
    var places by remember { mutableStateOf<List<String>>(emptyList()) }
    var countries by remember { mutableStateOf<List<String>>(emptyList()) }
    var equipments by remember { mutableStateOf<List<String>>(emptyList()) }

    // Connected summits (summits within 0-1 days of the current entity's date)
    var connectedSummits by remember { mutableStateOf<List<Summit>>(emptyList()) }

    // Performance data state
    val performanceState = remember { PerformanceDataState() }

    // Expandable sections
    var elevationAndSpeedExpanded by remember { mutableStateOf(false) }
    var locationDetailsExpanded by remember { mutableStateOf(false) }
    var commentsExpanded by remember { mutableStateOf(false) }
    var generalMetricsExpanded by remember { mutableStateOf(false) }
    var powerMetricsExpanded by remember { mutableStateOf(false) }


    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uriNotNull ->
            scope.launch {
                handleGpxTrackUpload(
                    context, uriNotNull, entity, isLoading = { isLoading = it },
                    onUpdate = { name, km, hm, elev, dur ->
                        summitName = name
                        kilometers = km
                        heightMeter = hm
                        topElevation = elev
                        duration = dur
                    },
                    onFileUpdate = { temporaryGpxFile = it },
                    onPointUpdate = { latLngHighestPoint = it }
                )
            }
        }
    }

    // Load existing summit data if editing
    LaunchedEffect(summitId, summitsFromDatabase) {
        summitsFromDatabase.firstOrNull { it.id == summitId }?.let { summit ->
            entity = summit
            summitName = entity.name
            tourDate = entity.getDateAsString() ?: ""
            selectedSportType = entity.sportType
            kilometers = if (entity.kilometers > 0.0) entity.kilometers.toString() else ""
            heightMeter =
                if (entity.elevationData.elevationGain > 0) entity.elevationData.elevationGain.toString() else ""
            topElevation =
                if (entity.elevationData.maxElevation > 0) entity.elevationData.maxElevation.toString() else ""
            duration = if (entity.duration > 0) entity.duration.toString() else ""
            topSpeed =
                if (entity.velocityData.maxVelocity > 0.0) entity.velocityData.maxVelocity.toString() else ""
            comments = entity.comments
            participants = entity.participants
            // Convert ac_id:XXXXX entries to display strings for the UI
            places = entity.getPlacesWithConnectedEntryString(context, summitsFromDatabase)
            countries = entity.countries
            equipments = entity.equipments
            garminDataFromGarminConnect = entity.garminData
            performanceState.loadFromGarminData(entity.garminData)
        }
        // Compute connected summits based on entity date when editing
        connectedSummits = computeConnectedSummits(entity, summitsFromDatabase)
    }

    // Recompute connected summits when tourDate changes (for new summits)
    LaunchedEffect(tourDate, summitsFromDatabase) {
        if (tourDate.isNotBlank()) {
            try {
                val parsedDate = Summit.parseDate(tourDate)
                val tempEntity = entity.clone()
                tempEntity.date = parsedDate
                connectedSummits = computeConnectedSummits(tempEntity, summitsFromDatabase)
            } catch (_: Exception) {
                // If date parsing fails, keep existing connected summits
            }
        } else if (!isEdit) {
            // For new summits with no date, clear connected summits
            connectedSummits = emptyList()
        }
    }

    LaunchedEffect(temporaryGpxFile) {
        if (uri != null) {
            scope.launch {
                handleGpxTrackUpload(
                    context, uri, entity, isLoading = { isLoading = it },
                    onUpdate = { name, km, hm, elev, dur ->
                        summitName = name
                        kilometers = km
                        heightMeter = hm
                        topElevation = elev
                        duration = dur
                    },
                    onFileUpdate = { temporaryGpxFile = it },
                    onPointUpdate = { latLngHighestPoint = it }
                )
            }
        }

    }

    // Validation
    val isSaveEnabled = summitName.isNotBlank() &&
            heightMeter.isNotBlank() &&
            kilometers.isNotBlank() &&
            (isBookmark || tourDate.isNotBlank())

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
                        .padding(20.dp)
                ) {
                    // Title
                    Text(
                        text = when {
                            isEdit -> stringResource(R.string.update)
                            isBookmark -> stringResource(R.string.add_new_bookmark)
                            else -> stringResource(R.string.add_new_summit)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Date field (not for bookmarks)
                    if (!isBookmark) {
                        DatePickerField(
                            value = tourDate,
                            onValueChange = { tourDate = it },
                            context = context,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Summit name
                    AutoCompleteCompose(
                        value = summitName,
                        onItemSelected = { summitName = it },
                        label = stringResource(if (isBookmark) R.string.add_new_bookmark else R.string.summit_name_hint),
                        icon = R.drawable.baseline_landscape_black_24dp,
                        options = summitsFromDatabase.flatMap { it.places + it.name }.distinct(),
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { summitName = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sport type dropdown
                    SportTypeDropdown(
                        selectedSportType = selectedSportType,
                        onSportTypeSelected = { selectedSportType = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // GPS Track buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(
                                painterResource(R.drawable.baseline_attach_file_24),
                                stringResource(R.string.addGpsTrack)
                            )
                        }

                        if (FileHelper.getOnDeviceMapFiles(context).isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        withContext(Dispatchers.IO) {
                                            (latLngHighestPoint ?: entity.latLng)?.let { point ->
                                                try {
                                                    updateLocationInfo(
                                                        context,
                                                        point,
                                                        entity
                                                    ) { name, elev ->
                                                        summitName = name
                                                        topElevation = elev
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                        isLoading = false
                                    }
                                }
                            ) {
                                Icon(painterResource(R.drawable.baseline_refresh_24), "Update name")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Kilometers
                    OutlinedTextField(
                        value = kilometers,
                        onValueChange = { input ->
                            // Allow only digits and at most one decimal point
                            val filtered = input.filter { char -> char.isDigit() || char == '.' }
                            val decimalCount = filtered.count { it == '.' }
                            kilometers =
                                if (decimalCount <= 1) filtered else filtered.substringBeforeLast(".")
                        },
                        label = { Text(stringResource(R.string.kilometers_hint)) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.outline_distance_24),
                                null
                            )
                        },
                        trailingIcon = { Text(stringResource(R.string.km)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Height meters
                    OutlinedTextField(
                        value = heightMeter,
                        onValueChange = {
                            // Allow only digits (no decimal point)
                            heightMeter = it.filter { char -> char.isDigit() }
                        },
                        label = { Text(stringResource(R.string.height_meter_hint)) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.baseline_trending_up_black_24dp),
                                null
                            )
                        },
                        trailingIcon = { Text(stringResource(R.string.hm)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Additional data section (expandable)
                    if (!isBookmark) {
                        // Build places suggestions including connected summit entries
                        val placesSuggestions = summitsFromDatabase.flatMap {
                            it.places + it.name
                        }.filter {
                            it.isNotEmpty() && !it.startsWith(CONNECTED_ACTIVITY_PREFIX)
                        }.distinct().toMutableList()
                        for (entry in connectedSummits) {
                            placesSuggestions.add(entry.getConnectedEntryString(context))
                        }

                        AdditionalDataFields(
                            topElevation, { topElevation = it },
                            duration, { duration = it },
                            topSpeed, { topSpeed = it },
                            participants, { participants = it },
                            places, { places = it },
                            countries, { countries = it },
                            equipments, { equipments = it },
                            comments, { comments = it },
                            summitsFromDatabase,
                            peaks,
                            onPeakToggle,
                            elevationAndSpeedExpanded, { elevationAndSpeedExpanded = it },
                            locationDetailsExpanded, { locationDetailsExpanded = it },
                            commentsExpanded, { commentsExpanded = it },
                            placesSuggestions = placesSuggestions
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Performance data section
                        PerformanceDataFields(
                            performanceState,
                            generalMetricsExpanded, { generalMetricsExpanded = it },
                            powerMetricsExpanded, { powerMetricsExpanded = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cancelMessage = stringResource(
                            if (isEdit) R.string.update_summit_cancel
                            else R.string.add_new_summit_cancel
                        )

                        Button(
                            onClick = {
                                onDismiss()
                                Toast.makeText(
                                    context,
                                    cancelMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancelButtonText))
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    saveSummit(
                                        entity, summitName, tourDate, selectedSportType,
                                        kilometers, heightMeter, topElevation, duration, topSpeed,
                                        comments, participants, places, countries, equipments,
                                        performanceState, isBookmark, latLngHighestPoint,
                                        garminDataFromGarminConnect, connectedSummits, context
                                    )

                                    onSaveSummit(isEdit, entity).invokeOnCompletion {
                                        temporaryGpxFile?.let { tempFile ->
                                            if (tempFile.exists() && entity.sportType != SportType.IndoorTrainer) {
                                                tempFile.copyTo(
                                                    entity.getGpsTrackPath().toFile(),
                                                    overwrite = true
                                                )
                                            }
                                        }
                                        isLoading = false
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = isSaveEnabled && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isEdit) stringResource(R.string.update) else stringResource(R.string.saveButtonText))
                        }
                    }
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
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    val showDatePicker = {
        val calendar = Calendar.getInstance()
        var day = calendar[Calendar.DAY_OF_MONTH]
        var month = calendar[Calendar.MONTH]
        var year = calendar[Calendar.YEAR]

        if (value.trim().isNotEmpty()) {
            val dateSplit = value.trim().split("-")
            if (dateSplit.size == 3) {
                day = dateSplit[2].toIntOrNull() ?: day
                month = (dateSplit[1].toIntOrNull() ?: (month + 1)) - 1
                year = dateSplit[0].toIntOrNull() ?: year
            }
        }

        DatePickerDialog(
            context,
            R.style.CustomDatePickerDialogTheme,
            { _, y, m, d ->
                onValueChange(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d))
            },
            year, month, day
        ).show()
    }

    Box(modifier = modifier.clickable { showDatePicker() }) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            label = { Text(stringResource(R.string.tour_date)) },
            leadingIcon = {
                Icon(painterResource(R.drawable.baseline_today_black_24dp), null)
            },
            readOnly = true,
            enabled = false,
            colors = TextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Icon(
                    painter = painterResource(if (expanded) R.drawable.ic_baseline_expand_less_24 else R.drawable.ic_baseline_expand_more_24),
                    contentDescription = null
                )
            }
            if (expanded) content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportTypeDropdown(
    selectedSportType: SportType,
    onSportTypeSelected: (SportType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            if (it) keyboardController?.hide()
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = stringResource(selectedSportType.sportNameStringId),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.activity_hint)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SportType.entries.forEach { sportType ->
                DropdownMenuItem(
                    text = { Text(stringResource(sportType.sportNameStringId)) },
                    onClick = {
                        onSportTypeSelected(sportType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AdditionalDataFields(
    topElevation: String, onTopElevationChange: (String) -> Unit,
    duration: String, onDurationChange: (String) -> Unit,
    topSpeed: String, onTopSpeedChange: (String) -> Unit,
    participants: List<String>, onParticipantsChange: (List<String>) -> Unit,
    places: List<String>, onPlacesChange: (List<String>) -> Unit,
    countries: List<String>, onCountriesChange: (List<String>) -> Unit,
    equipments: List<String>, onEquipmentsChange: (List<String>) -> Unit,
    comments: String, onCommentsChange: (String) -> Unit,
    summitsFromDatabase: List<Summit>,
    peaks: List<de.drtobiasprinz.summitbook.db.entities.Peak>,
    onPeakToggle: ((String, Boolean) -> Unit)? = null,
    elevationAndSpeedExpanded: Boolean,
    onElevationAndSpeedExpandedChange: (Boolean) -> Unit,
    locationDetailsExpanded: Boolean,
    onLocationDetailsExpandedChange: (Boolean) -> Unit,
    commentsExpanded: Boolean,
    onCommentsExpandedChange: (Boolean) -> Unit,
    placesSuggestions: List<String> = emptyList()
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Elevation & Speed Section
        ExpandableSection(
            title = stringResource(R.string.elevation_and_speed),
            expanded = elevationAndSpeedExpanded,
            onExpandChange = onElevationAndSpeedExpandedChange
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = topElevation, onValueChange = onTopElevationChange,
                        label = { Text(stringResource(R.string.top_elevation_hint)) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.outline_landscape_2_24),
                                null
                            )
                        },
                        trailingIcon = { Text(stringResource(R.string.hm)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = duration, onValueChange = onDurationChange,
                        label = { Text(stringResource(R.string.duration)) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.baseline_av_timer_24_black),
                                null
                            )
                        },
                        trailingIcon = { Text(stringResource(R.string.sec)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = topSpeed, onValueChange = onTopSpeedChange,
                        label = { Text(stringResource(R.string.top_speed_hint)) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.baseline_speed_black_24dp),
                                null
                            )
                        },
                        trailingIcon = { Text(stringResource(R.string.kmh)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Location Details Section
        ExpandableSection(
            title = stringResource(R.string.additional_summit_data),
            expanded = locationDetailsExpanded,
            onExpandChange = onLocationDetailsExpandedChange
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AutoCompleteComposeChipField(
                        stringResource(R.string.participants),
                        R.drawable.ic_baseline_people_24,
                        participants,
                        onParticipantsChange,
                        summitsFromDatabase.flatMap { it.participants }.distinct()
                    )
                    AutoCompleteComposeChipField(
                        stringResource(R.string.place_hint),
                        R.drawable.outline_distance_24,
                        places,
                        onPlacesChange,
                        placesSuggestions.ifEmpty { summitsFromDatabase.flatMap { it.places + it.name }.distinct() },
                        peakIcon = R.drawable.outline_landscape_2_24,
                        nonPeakIcon = R.drawable.outline_landscape_2_off_24,
                        peaksList = peaks.map { it.name },
                        onPeakToggle = onPeakToggle
                    )
                    AutoCompleteComposeChipField(
                        stringResource(R.string.country_hint),
                        R.drawable.ic_baseline_flag_24,
                        countries,
                        onCountriesChange,
                        Locale.getAvailableLocales().map { it.displayCountry }.distinct()
                            .filter { it.isNotEmpty() })
                    AutoCompleteComposeChipField(
                        stringResource(R.string.equipments),
                        R.drawable.ic_baseline_handyman_24,
                        equipments,
                        onEquipmentsChange,
                        summitsFromDatabase.flatMap { it.equipments }.distinct()
                    )
                }
            }
        }

        // Comments Section
        ExpandableSection(
            title = stringResource(R.string.comments),
            expanded = commentsExpanded,
            onExpandChange = onCommentsExpandedChange
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = comments, onValueChange = onCommentsChange,
                        label = { Text(stringResource(R.string.comment_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            }
        }
    }
}


class PerformanceDataState {
    var calories by mutableStateOf("")
    var averageHr by mutableStateOf("")
    var maxHr by mutableStateOf("")
    var ftp by mutableStateOf("")
    var vo2Max by mutableStateOf("")
    var normPower by mutableStateOf("")
    var avgPower by mutableStateOf("")
    var power1s by mutableStateOf("")
    var power2s by mutableStateOf("")
    var power5s by mutableStateOf("")
    var power10s by mutableStateOf("")
    var power20s by mutableStateOf("")
    var power30s by mutableStateOf("")
    var power1min by mutableStateOf("")
    var power2min by mutableStateOf("")
    var power5min by mutableStateOf("")
    var power10min by mutableStateOf("")
    var power20min by mutableStateOf("")
    var power30min by mutableStateOf("")
    var power1h by mutableStateOf("")
    var power2h by mutableStateOf("")
    var power5h by mutableStateOf("")

    fun loadFromGarminData(data: GarminData?) {
        data?.let {
            calories = it.calories.toInt().toString()
            averageHr = it.averageHR.toInt().toString()
            maxHr = it.maxHR.toInt().toString()
            ftp = it.ftp.toString()
            vo2Max = it.vo2max.toString()
            normPower = it.power.normPower.toInt().toString()
            avgPower = it.power.avgPower.toInt().toString()
            power1s = it.power.oneSec.toString()
            power2s = it.power.twoSec.toString()
            power5s = it.power.fiveSec.toString()
            power10s = it.power.tenSec.toString()
            power20s = it.power.twentySec.toString()
            power30s = it.power.thirtySec.toString()
            power1min = it.power.oneMin.toString()
            power2min = it.power.twoMin.toString()
            power5min = it.power.fiveMin.toString()
            power10min = it.power.tenMin.toString()
            power20min = it.power.twentyMin.toString()
            power30min = it.power.thirtyMin.toString()
            power1h = it.power.oneHour.toString()
            power2h = it.power.twoHours.toString()
            power5h = it.power.fiveHours.toString()
        }
    }
}

@Composable
fun PerformanceDataFields(
    state: PerformanceDataState,
    generalMetricsExpanded: Boolean,
    onGeneralMetricsExpandedChange: (Boolean) -> Unit,
    powerMetricsExpanded: Boolean,
    onPowerMetricsExpandedChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // General Metrics Section
        ExpandableSection(
            title = stringResource(R.string.general_metrics),
            expanded = generalMetricsExpanded,
            onExpandChange = onGeneralMetricsExpandedChange
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerformanceField(
                        stringResource(R.string.calories),
                        state.calories,
                        { state.calories = it },
                        ""
                    )
                    PerformanceField(
                        stringResource(R.string.average_hr),
                        state.averageHr,
                        { state.averageHr = it },
                        stringResource(R.string.bpm)
                    )
                    PerformanceField(
                        stringResource(R.string.max_hr),
                        state.maxHr,
                        { state.maxHr = it },
                        stringResource(R.string.bpm)
                    )
                    PerformanceField(
                        stringResource(R.string.ftp),
                        state.ftp,
                        { state.ftp = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.vo2Max),
                        state.vo2Max,
                        { state.vo2Max = it },
                        ""
                    )
                }
            }
        }

        // Power Metrics Section
        ExpandableSection(
            title = stringResource(R.string.power_metrics),
            expanded = powerMetricsExpanded,
            onExpandChange = onPowerMetricsExpandedChange
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PerformanceField(
                        stringResource(R.string.normalized_power),
                        state.normPower,
                        { state.normPower = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.average_power),
                        state.avgPower,
                        { state.avgPower = it },
                        stringResource(R.string.watt)
                    )
                    Text(
                        text = stringResource(R.string.short_intervals),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    PerformanceField(
                        stringResource(R.string.power_1sec),
                        state.power1s,
                        { state.power1s = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_2sec),
                        state.power2s,
                        { state.power2s = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_5sec),
                        state.power5s,
                        { state.power5s = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_10sec),
                        state.power10s,
                        { state.power10s = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_20sec),
                        state.power20s,
                        { state.power20s = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_30sec),
                        state.power30s,
                        { state.power30s = it },
                        stringResource(R.string.watt)
                    )

                    // Medium intervals (1min-10min)
                    Text(
                        text = stringResource(R.string.medium_intervals),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    PerformanceField(
                        stringResource(R.string.power_1min),
                        state.power1min,
                        { state.power1min = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_2min),
                        state.power2min,
                        { state.power2min = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_5min),
                        state.power5min,
                        { state.power5min = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_10min),
                        state.power10min,
                        { state.power10min = it },
                        stringResource(R.string.watt)
                    )

                    // Long intervals (20min-5h)
                    Text(
                        text = stringResource(R.string.long_intervals),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    PerformanceField(
                        stringResource(R.string.power_20min),
                        state.power20min,
                        { state.power20min = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_30min),
                        state.power30min,
                        { state.power30min = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_1h),
                        state.power1h,
                        { state.power1h = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_2h),
                        state.power2h,
                        { state.power2h = it },
                        stringResource(R.string.watt)
                    )
                    PerformanceField(
                        stringResource(R.string.power_5h),
                        state.power5h,
                        { state.power5h = it },
                        stringResource(R.string.watt)
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceField(label: String, value: String, onValueChange: (String) -> Unit, unit: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        if (unit.isNotEmpty()) {
            Text(
                unit,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Helper functions

private fun createEmptySummit(isBookmark: Boolean, context: Context): Summit {
    return Summit(
        Date(),
        if (isBookmark) context.getString(R.string.bookmarks) else context.getString(R.string.new_summits),
        SportType.Bicycle,
        emptyList(),
        emptyList(),
        "",
        ElevationData.parse(0, 0),
        0.0,
        VelocityData(0.0),
        0.0,
        0.0,
        emptyList(),
        emptyList(),
        isFavorite = false,
        isPeak = false,
        imageIds = mutableListOf(),
        garminData = null,
        trackBoundingBox = null,
        isBookmark = isBookmark
    )
}

private suspend fun handleGpxTrackUpload(
    context: Context,
    uri: Uri,
    entity: Summit,
    isLoading: (Boolean) -> Unit,
    onUpdate: (String, String, String, String, String) -> Unit,
    onFileUpdate: (File?) -> Unit,
    onPointUpdate: (GeoPoint?) -> Unit
) {
    isLoading(true)

    withContext(Dispatchers.IO) {
        val temporaryGpxFile = GarminTrackAndDataDownloader.getTempGpsFilePath(
            SimpleDateFormat("yyyy_MM_dd_HHmmss", Locale.US).format(Date())
        ).toFile()

        onFileUpdate(temporaryGpxFile)

        try {
            context.contentResolver?.openInputStream(uri)?.use { inputStream ->
                Files.copy(
                    inputStream,
                    entity.getGpsTrackPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

            var python = pythonInstance
            if (python == null) {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(context))
                }
                python = Python.getInstance()
            }

            if (entity.hasGpsTrack()) {
                GpxPyExecutor(python).createSimplifiedGpxTrack(entity.getGpsTrackPath())
                GpxPyExecutor(python).analyzeGpxTrackAndCreateGpxPyDataFile(entity)
                entity.setGpsTrack()
                val highestElevation = entity.gpsTrack?.getHighestElevation()
                entity.gpsTrack?.setDistance()

                onPointUpdate(highestElevation)

                entity.lat = highestElevation?.latitude
                entity.lng = highestElevation?.longitude
                entity.latLng = highestElevation

                val gpsTrack = entity.gpsTrack
                if (gpsTrack != null) {
                    val nameFromTrack = gpsTrack.gpxTrack?.metadata?.name
                        ?: gpsTrack.gpxTrack?.tracks?.first()?.trackName

                    if (gpsTrack.hasNoTrackPoints()) {
                        gpsTrack.parseTrack(useSimplifiedIfExists = false)
                    }

                    val gpxPyJsonFile = entity.getGpxPyPath().toFile()
                    if (gpxPyJsonFile.exists()) {
                        val gpxPyJson =
                            JsonParser.parseString(JsonUtils.getJsonData(gpxPyJsonFile)) as JsonObject

                        val elevationGain = try {
                            gpxPyJson.getAsJsonPrimitive("elevation_gain").asDouble.roundToInt()
                        } catch (_: ClassCastException) {
                            0
                        }

                        val maxElevation = try {
                            gpxPyJson.getAsJsonPrimitive("max_elevation").asDouble.roundToInt()
                        } catch (_: ClassCastException) {
                            0
                        }

                        val distance = try {
                            gpxPyJson.getAsJsonPrimitive("moving_distance").asDouble / 1000
                        } catch (_: ClassCastException) {
                            0.0
                        }

                        val movingDuration = try {
                            gpxPyJson.getAsJsonPrimitive("moving_time").asDouble
                        } catch (_: ClassCastException) {
                            0.0
                        }

                        entity.elevationData.elevationGain = elevationGain
                        if (maxElevation > 0) entity.elevationData.maxElevation = maxElevation
                        entity.kilometers = distance
                        if (movingDuration > 0) entity.duration = movingDuration.toInt()

                        onUpdate(
                            nameFromTrack ?: "",
                            String.format(Locale.ENGLISH, "%.1f", distance),
                            elevationGain.toString(),
                            if (maxElevation > 0) maxElevation.toString() else "",
                            if (movingDuration > 0) movingDuration.toString() else ""
                        )
                    }

                    highestElevation?.let { point ->
                        updateLocationInfo(context, point, entity) { name, elev ->
                            onUpdate(name, "", "", elev, "")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    isLoading(false)
}

private fun updateLocationInfo(
    context: Context,
    latLngHighestPoint: GeoPoint,
    summit: Summit,
    onUpdate: (String, String) -> Unit
) {
    OfflineMapAnalyzer.from(context).use { analyzer ->
        val info = analyzer.getClosestLocationInfo(
            LatLong(latLngHighestPoint.latitude, latLngHighestPoint.longitude)
        )

        if (info != null) {
            val name = info.name
            var elevation = ""

            if (info.placeType == "peak") {
                summit.isPeak = true
                try {
                    val elevationValue = info.additionalTags?.get("ele")
                    if (elevationValue != null) {
                        val elevationAsInt = Integer.valueOf(elevationValue)
                        summit.elevationData.maxElevation = elevationAsInt
                        elevation = elevationAsInt.toString()
                    }
                } catch (_: Exception) {
                }
            }

            onUpdate(name, elevation)
        }
    }
}

private fun saveSummit(
    entity: Summit,
    summitName: String,
    tourDate: String,
    selectedSportType: SportType,
    kilometers: String,
    heightMeter: String,
    topElevation: String,
    duration: String,
    topSpeed: String,
    comments: String,
    participants: List<String>,
    places: List<String>,
    countries: List<String>,
    equipments: List<String>,
    performanceState: PerformanceDataState,
    isBookmark: Boolean,
    latLngHighestPoint: GeoPoint?,
    garminDataFromGarminConnect: GarminData?,
    connectedSummits: List<Summit> = emptyList(),
    context: Context? = null
) {
    try {
        entity.date = if (isBookmark) Date() else Summit.parseDate(tourDate)
        entity.name = summitName
        entity.sportType = selectedSportType
        // Convert connected entry display strings (e.g. "End of SummitName") back to ac_id:XXXXX format
        val resolvedPlaces = places.map { place ->
            var resolved: String? = null
            for (connectedSummit in connectedSummits) {
                val connectedEntryString = context?.let { connectedSummit.getConnectedEntryString(it) }
                if (place == connectedEntryString) {
                    resolved = "$CONNECTED_ACTIVITY_PREFIX${connectedSummit.activityId}"
                    break
                }
            }
            resolved ?: place
        }
        // Filter out empty strings from lists before saving
        entity.places = resolvedPlaces.filter { it.isNotEmpty() }.toMutableList()
        entity.countries = countries.filter { it.isNotEmpty() }.toMutableList()
        entity.comments = comments
        entity.elevationData.elevationGain = heightMeter.toIntOrNull() ?: 0
        entity.kilometers = kilometers.toDoubleOrNull() ?: 0.0
        entity.duration = duration.toIntOrNull() ?: 0
        entity.velocityData.maxVelocity = topSpeed.toDoubleOrNull() ?: 0.0
        entity.elevationData.maxElevation = topElevation.toIntOrNull() ?: 0
        // Filter out empty strings from lists before saving
        entity.participants = participants.filter { it.isNotEmpty() }.toMutableList()
        entity.equipments = equipments.filter { it.isNotEmpty() }.toMutableList()

        if (entity.latLng == null && latLngHighestPoint != null) {
            entity.latLng = latLngHighestPoint
        }
        entity.hasTrack = true
        if (isBookmark) entity.isBookmark = true

        // Parse Garmin data
        if (garminDataFromGarminConnect != null) {
            garminDataFromGarminConnect.calories =
                performanceState.calories.toDoubleOrNull()?.toFloat() ?: 0f
            garminDataFromGarminConnect.averageHR =
                performanceState.averageHr.toDoubleOrNull()?.toFloat() ?: 0f
            garminDataFromGarminConnect.maxHR =
                performanceState.maxHr.toDoubleOrNull()?.toFloat() ?: 0f
            garminDataFromGarminConnect.ftp = performanceState.ftp.toIntOrNull() ?: 0
            garminDataFromGarminConnect.vo2max =
                performanceState.vo2Max.toDoubleOrNull()?.toFloat() ?: 0f
            garminDataFromGarminConnect.power.avgPower =
                performanceState.avgPower.toDoubleOrNull()?.toFloat() ?: 0f
            garminDataFromGarminConnect.power.normPower =
                performanceState.normPower.toDoubleOrNull()?.toFloat() ?: 0f
            garminDataFromGarminConnect.power.oneSec = performanceState.power1s.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.twoSec = performanceState.power2s.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.fiveSec = performanceState.power5s.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.tenSec = performanceState.power10s.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.twentySec =
                performanceState.power20s.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.thirtySec =
                performanceState.power30s.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.oneMin =
                performanceState.power1min.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.twoMin =
                performanceState.power2min.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.fiveMin =
                performanceState.power5min.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.tenMin =
                performanceState.power10min.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.twentyMin =
                performanceState.power20min.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.thirtyMin =
                performanceState.power30min.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.oneHour = performanceState.power1h.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.twoHours =
                performanceState.power2h.toIntOrNull() ?: 0
            garminDataFromGarminConnect.power.fiveHours =
                performanceState.power5h.toIntOrNull() ?: 0
            entity.garminData = garminDataFromGarminConnect
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun computeConnectedSummits(
    entity: Summit,
    summitsFromDatabase: List<Summit>
): List<Summit> {
    return summitsFromDatabase.filter { entry ->
        entry.activityId != entity.activityId && run {
            val differenceInMilliSec = entity.date.time - entry.date.time
            val differenceInDays = round(TimeUnit.MILLISECONDS.toDays(differenceInMilliSec).toDouble())
            0.0 < differenceInDays && differenceInDays <= 1.0
        }
    }
}