package de.drtobiasprinz.summitbook.ui.compose

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.CustomMapViewToAllowScrolling.Companion.selectedItem
import de.drtobiasprinz.summitbook.ui.MainActivityCompose
import de.drtobiasprinz.summitbook.ui.MapProvider
import de.drtobiasprinz.summitbook.ui.dialog.FileRowType
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Jetpack Compose version of SettingsFragment
 * Replaces the PreferenceFragmentCompat-based implementation with a modern Compose UI
 */
@Composable
fun SettingsScreen(
    summits: List<Summit>,
    onSharedPreferenceChanged: (String) -> Unit = {},
    onSaveSummit: (Boolean, Summit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    // General Settings State
    var currentYearSwitch by remember { mutableStateOf(false) }
    var annualTargetActivities by remember { mutableStateOf("52") }
    var annualTargetSummits by remember { mutableStateOf("10") }
    var annualTargetHeightMeter by remember { mutableStateOf("50000") }
    var annualTargetKilometer by remember { mutableStateOf("1200") }
    var indoorHeightMeterPerCent by remember { mutableIntStateOf(0) }
    var forecastAverageOfLastXYears by remember { mutableIntStateOf(3) }

    // Map Settings State
    var mapProvider by remember { mutableStateOf(false) }
    var accessAllFiles by remember { mutableStateOf(false) }
    var onDeviceMapsFolder by remember { mutableStateOf("") }
    var useSimplifiedTracks by remember { mutableStateOf(true) }
    var maxPointsOnTrack by remember { mutableStateOf("10000") }

    // Third Party Settings State
    var garminUserName by remember { mutableStateOf("") }
    var garminPassword by remember { mutableStateOf("") }
    var garminMFASwitch by remember { mutableStateOf(false) }
    var downloadTCXSwitch by remember { mutableStateOf(false) }
    var garminSyncStartDate by remember { mutableStateOf<Date?>(null) }

    // Export Settings State
    var exportThirdPartyData by remember { mutableStateOf(true) }
    var exportCalculatedData by remember { mutableStateOf(true) }
    var disableStartUpTasks by remember { mutableStateOf(false) }

    // On-device maps state
    var onDeviceMapFiles by remember { mutableStateOf(emptyList<DocumentFile>()) }
    var onDeviceMbTilesFiles by remember { mutableStateOf(emptyList<DocumentFile>()) }
    var onDeviceMapsFolderName by remember { mutableStateOf("") }

    // Dialog states
    var showProgressDialog by remember { mutableStateOf(false) }
    var progressDialogMessage by remember { mutableStateOf("") }
    var showEmptyFolderError by remember { mutableStateOf(false) }

    // Date format
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Load preferences on first composition
    LaunchedEffect(Unit) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        currentYearSwitch = prefs.getBoolean(Keys.PREF_CURRENT_YEAR_SWITCH, false)
        annualTargetActivities = prefs.getString(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, "52") ?: "52"
        annualTargetSummits = prefs.getString(Keys.PREF_ANNUAL_TARGET_SUMMITS, "10") ?: "10"
        annualTargetHeightMeter = prefs.getString(Keys.PREF_ANNUAL_TARGET, "50000") ?: "50000"
        annualTargetKilometer = prefs.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200") ?: "1200"
        indoorHeightMeterPerCent = prefs.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
        forecastAverageOfLastXYears = prefs.getInt(Keys.PREF_FORECAST_AVERAGE, 3)

        mapProvider = prefs.getBoolean(Keys.PREF_ON_DEVICE_MAPS, false)
        accessAllFiles = prefs.getBoolean(Keys.PREF_ACCESS_ALL_FILES, false)
        onDeviceMapsFolder = prefs.getString(Keys.PREF_ON_DEVICE_MAPS_FOLDER, "") ?: ""
        useSimplifiedTracks = prefs.getBoolean(Keys.PREF_USE_SIMPLIFIED_TRACKS, true)
        maxPointsOnTrack = prefs.getString(Keys.PREF_MAX_NUMBER_POINT, "10000") ?: "10000"

        garminUserName = prefs.getString(Keys.PREF_GARMIN_USERNAME, "") ?: ""
        garminPassword = prefs.getString(Keys.PREF_GARMIN_PASSWORD, "") ?: ""
        garminMFASwitch = prefs.getBoolean(Keys.PREF_GARMIN_MFA, false)
        downloadTCXSwitch = prefs.getBoolean(Keys.PREF_DOWNLOAD_TCX, false)

        val startDateStr = prefs.getString(Keys.PREF_THIRD_PARTY_START_DATE, "2024-01-01")
        garminSyncStartDate = try {
            dateFormat.parse(startDateStr ?: "2024-01-01")
        } catch (_: Exception) {
            null
        }

        exportThirdPartyData = prefs.getBoolean(Keys.PREF_EXPORT_THIRD_PARTY_DATA, true)
        exportCalculatedData = prefs.getBoolean(Keys.PREF_EXPORT_CALCULATED_DATA, true)
        disableStartUpTasks = prefs.getBoolean(Keys.PREF_DEBUG, false)

        // Update on-device maps state
        onDeviceMapFiles = FileHelper.getOnDeviceMapFiles(context)
        onDeviceMbTilesFiles = FileHelper.getOnDeviceMbtilesFiles(context)
        onDeviceMapsFolderName = FileHelper.getOnDeviceMapsFolderName(context)
    }

    // Launcher for selecting on-device maps folder
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                FileHelper.makeUriPersistent(context, uri)
                PreferencesHelper.saveOnDeviceMapsFolder(uri.toString())
                onDeviceMapFiles = FileHelper.getOnDeviceMapFiles(context)
                onDeviceMbTilesFiles = FileHelper.getOnDeviceMbtilesFiles(context)
                onDeviceMapsFolderName = FileHelper.getOnDeviceMapsFolderName(context)
                if (onDeviceMapFiles.isEmpty()) {
                    showEmptyFolderError = true
                }
            }
        }
    }

    // Function to save preference
    fun savePreference(key: String, value: Any) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                is Int -> putInt(key, value)
            }
            apply()
        }
        onSharedPreferenceChanged(key)
    }

    // Function to open folder picker
    fun openOnDeviceMapsFolderDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            try {
                folderPickerLauncher.launch(intent)
            } catch (exception: Exception) {
                Log.e("SettingsScreen", "Unable to select a on-device maps folder.\n$exception")
                Toast.makeText(
                    context,
                    R.string.toast_message_install_file_helper,
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Log.i("SettingsScreen", "This is not supported in your Android Version")
        }
    }

    // Function to handle map provider switch
    fun onMapProviderChanged(checked: Boolean) {
        mapProvider = checked
        savePreference(Keys.PREF_ON_DEVICE_MAPS, checked)

        if (checked && onDeviceMapFiles.isEmpty() && onDeviceMbTilesFiles.isEmpty()) {
            openOnDeviceMapsFolderDialog()
        } else {
            selectedItem = if (checked) {
                MapProvider.HIKING
            } else if (onDeviceMbTilesFiles.isNotEmpty()) {
                MapProvider.MBTILES
            } else {
                MapProvider.OPENTOPO
            }
        }
    }

    // Function to handle access all files switch
    fun onAccessAllFilesChanged(checked: Boolean) {
        accessAllFiles = checked
        savePreference(Keys.PREF_ACCESS_ALL_FILES, checked)

        if (checked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    ("package:" + BuildConfig.APPLICATION_ID).toUri()
                )
                context.startActivity(intent)
            } else {
                Log.i("SettingsScreen", "This is not supported in your Android Version")
            }
        }
    }

    // Function to handle Garmin MFA switch
    fun onGarminMFAChanged(checked: Boolean) {
        garminMFASwitch = checked
        savePreference(Keys.PREF_GARMIN_MFA, checked)

        val garminMFA = File(MainActivityCompose.storage?.absolutePath, ".garminconnect")
        if (!checked && garminMFA.exists()) {
            garminMFA.deleteRecursively()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // General Settings Category
        SettingsCategory(title = stringResource(R.string.pref_general_title)) {
            SwitchSetting(
                title = stringResource(R.string.current_year_title),
                summary = stringResource(R.string.current_year_summary),
                icon = R.drawable.baseline_calendar_today_24,
                checked = currentYearSwitch,
                onCheckedChange = {
                    currentYearSwitch = it
                    savePreference(Keys.PREF_CURRENT_YEAR_SWITCH, it)
                }
            )

            EditTextSetting(
                title = stringResource(R.string.annual_target_activities_title),
                summary = stringResource(R.string.annual_target_activities_summary),
                icon = R.drawable.baseline_directions_walk_black_24dp,
                value = annualTargetActivities,
                onValueChange = {
                    annualTargetActivities = it
                    savePreference(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, it)
                }
            )

            EditTextSetting(
                title = stringResource(R.string.annual_target_summits_title),
                summary = stringResource(R.string.annual_target_summits_summary),
                icon = R.drawable.outline_landscape_2_24,
                value = annualTargetSummits,
                onValueChange = {
                    annualTargetSummits = it
                    savePreference(Keys.PREF_ANNUAL_TARGET_SUMMITS, it)
                }
            )

            EditTextSetting(
                title = stringResource(R.string.annual_target_height_meter_title),
                summary = stringResource(R.string.annual_target_height_meter_summary),
                icon = R.drawable.baseline_trending_up_black_24dp,
                value = annualTargetHeightMeter,
                onValueChange = {
                    annualTargetHeightMeter = it
                    savePreference(Keys.PREF_ANNUAL_TARGET, it)
                }
            )

            EditTextSetting(
                title = stringResource(R.string.annual_target_kilometer_title),
                summary = stringResource(R.string.annual_target_kilometer_summary),
                icon = R.drawable.outline_distance_24,
                value = annualTargetKilometer,
                onValueChange = {
                    annualTargetKilometer = it
                    savePreference(Keys.PREF_ANNUAL_TARGET_KM, it)
                }
            )

            SliderSetting(
                title = stringResource(R.string.indoor_hm_percent),
                icon = R.drawable.baseline_other_houses_24,
                value = indoorHeightMeterPerCent.toFloat(),
                valueRange = 0f..100f,
                onValueChange = {
                    indoorHeightMeterPerCent = it.toInt()
                    savePreference(Keys.PREF_INDOOR_HEIGHT_METER, it.toInt())
                }
            )

            SliderSetting(
                title = stringResource(R.string.forecast_average_title),
                summary = stringResource(R.string.forecast_average_summary),
                icon = R.drawable.baseline_trending_flat_24,
                value = forecastAverageOfLastXYears.toFloat(),
                valueRange = 0f..20f,
                onValueChange = {
                    forecastAverageOfLastXYears = it.toInt()
                    savePreference(Keys.PREF_FORECAST_AVERAGE, it.toInt())
                }
            )
        }

        // Map Settings Category
        SettingsCategory(title = stringResource(R.string.pref_map_title)) {
            SwitchSetting(
                title = stringResource(R.string.map_provider),
                summary = if (mapProvider) stringResource(R.string.on_device_maps) else stringResource(
                    R.string.online_maps
                ),
                icon = R.drawable.baseline_map_black_24dp,
                checked = mapProvider,
                onCheckedChange = { onMapProviderChanged(it) }
            )

            SwitchSetting(
                title = stringResource(R.string.access_all_files),
                summary = stringResource(R.string.access_all_files_summary),
                icon = R.drawable.baseline_map_black_24dp,
                checked = accessAllFiles,
                onCheckedChange = { onAccessAllFilesChanged(it) }
            )

            if (mapProvider && onDeviceMapFiles.isNotEmpty()) {
                EditTextSetting(
                    title = stringResource(R.string.map_folder),
                    summary = String.format(
                        stringResource(R.string.map_folder_summary),
                        onDeviceMapsFolderName
                    ),
                    icon = R.drawable.baseline_map_black_24dp,
                    value = onDeviceMapsFolder,
                    readOnly = true,
                    onClick = { openOnDeviceMapsFolderDialog() }
                )
            }

            SwitchSetting(
                title = stringResource(R.string.use_simplified_tracks_title),
                summary = stringResource(R.string.use_simplified_tracks),
                icon = R.drawable.baseline_map_black_24dp,
                checked = useSimplifiedTracks,
                onCheckedChange = {
                    useSimplifiedTracks = it
                    savePreference(Keys.PREF_USE_SIMPLIFIED_TRACKS, it)
                }
            )

            EditTextSetting(
                title = stringResource(R.string.max_number_points_title),
                summary = stringResource(R.string.max_number_points),
                icon = R.drawable.baseline_map_black_24dp,
                value = maxPointsOnTrack,
                onValueChange = {
                    maxPointsOnTrack = it
                    savePreference(Keys.PREF_MAX_NUMBER_POINT, it)
                }
            )
        }

        // Third Party Settings Category
        SettingsCategory(title = stringResource(R.string.pref_third_party_title)) {
            EditTextSetting(
                title = stringResource(R.string.garmin_user_title),
                summary = stringResource(R.string.garmin_user_summary),
                icon = R.drawable.baseline_account_circle_24,
                value = garminUserName,
                onValueChange = {
                    garminUserName = it
                    savePreference(Keys.PREF_GARMIN_USERNAME, it)
                }
            )

            PasswordSetting(
                title = stringResource(R.string.garmin_pwd_title),
                summary = stringResource(R.string.garmin_pwd_summary),
                icon = R.drawable.baseline_password_24,
                value = garminPassword,
                onValueChange = {
                    garminPassword = it
                    savePreference(Keys.PREF_GARMIN_PASSWORD, it)
                }
            )

            SwitchSetting(
                title = stringResource(R.string.garmin_mfa_switch_title),
                summary = stringResource(R.string.garmin_mfa_switch),
                icon = R.drawable.baseline_account_circle_24,
                checked = garminMFASwitch,
                onCheckedChange = { onGarminMFAChanged(it) }
            )

            SwitchSetting(
                title = stringResource(R.string.tcx_switch_title),
                summary = stringResource(R.string.tcx_switch),
                icon = R.drawable.baseline_download_black_24dp,
                checked = downloadTCXSwitch,
                onCheckedChange = {
                    downloadTCXSwitch = it
                    savePreference(Keys.PREF_DOWNLOAD_TCX, it)
                }
            )

            DateSetting(
                title = stringResource(R.string.start_date_sync_garmin),
                icon = R.drawable.baseline_calendar_today_24,
                date = garminSyncStartDate,
                onDateChange = {
                    garminSyncStartDate = it
                    savePreference(Keys.PREF_THIRD_PARTY_START_DATE, dateFormat.format(it))
                }
            )
        }

        // Export Settings Category
        SettingsCategory(title = stringResource(R.string.pref_export_title)) {
            SwitchSetting(
                title = stringResource(R.string.export_third_party_data_title),
                summary = stringResource(R.string.export_third_party_data),
                icon = R.drawable.baseline_import_export_24,
                checked = exportThirdPartyData,
                onCheckedChange = {
                    exportThirdPartyData = it
                    savePreference(Keys.PREF_EXPORT_THIRD_PARTY_DATA, it)
                }
            )

            SwitchSetting(
                title = stringResource(R.string.export_calculated_data_title),
                summary = stringResource(R.string.export_calculated_data),
                icon = R.drawable.baseline_import_export_24,
                checked = exportCalculatedData,
                onCheckedChange = {
                    exportCalculatedData = it
                    savePreference(Keys.PREF_EXPORT_CALCULATED_DATA, it)
                }
            )

            SwitchSetting(
                title = stringResource(R.string.debug_switch_title),
                summary = stringResource(R.string.debug_switch),
                icon = R.drawable.baseline_do_not_disturb_on_total_silence_24,
                checked = disableStartUpTasks,
                onCheckedChange = {
                    disableStartUpTasks = it
                    savePreference(Keys.PREF_DEBUG, it)
                }
            )
        }

        // Bulk File Management Category
        SettingsCategory(title = stringResource(R.string.pref_bulk_file_management_title)) {
            val bulkUpdateComplete = stringResource(R.string.bulk_update_complete)
            FileRowType.entries.forEach { fileRowType ->
                val fileCount = countFilesToUpdate(summits, fileRowType)

                if (fileCount > 0) {
                    BulkUpdateSetting(
                        title = when (fileRowType) {
                            FileRowType.GPX_TRACK -> stringResource(
                                R.string.bulk_update_gpx_tracks
                            )

                            FileRowType.GPX_SIMPLIFIED -> stringResource(
                                R.string.bulk_update_gpx_simplified
                            )

                            FileRowType.YAML_EXTENSIONS -> stringResource(
                                R.string.bulk_update_yaml_extensions
                            )

                            FileRowType.GPXPY_JSON -> stringResource(
                                R.string.bulk_update_gpxpy_json
                            )
                        },
                        summary = stringResource(R.string.bulk_update_file_count, fileCount),
                        icon = R.drawable.baseline_refresh_24,
                        onClick = {
                            performBulkUpdate(
                                summits = summits,
                                fileRowType = fileRowType,
                                context = context,
                                onProgressUpdate = { message ->
                                    progressDialogMessage = message
                                    showProgressDialog = true
                                }, onComplete = { successCount, failCount ->
                                    showProgressDialog = false
                                    Toast.makeText(
                                        context,
                                        String.format(
                                            bulkUpdateComplete,
                                            successCount,
                                            failCount
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }, onSaveSummit = onSaveSummit,
                                coroutineScope = coroutineScope
                            )
                        }
                    )
                }
            }
        }
    }

    // Empty folder error dialog
    if (showEmptyFolderError) {
        AlertDialog(
            onDismissRequest = { showEmptyFolderError = false },
            title = { Text(stringResource(R.string.error)) },
            text = { Text("The folder used for on-device maps must contain .map files.") },
            confirmButton = {
                TextButton(onClick = { showEmptyFolderError = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // Progress dialog
    if (showProgressDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(stringResource(R.string.bulk_update_progress_title)) },
            text = { Text(progressDialogMessage) },
            confirmButton = { }
        )
    }
}

@Composable
fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                content()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SwitchSetting(
    title: String,
    summary: String? = null,
    icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
fun EditTextSetting(
    title: String,
    summary: String? = null,
    icon: Int,
    value: String,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit = {},
    onClick: () -> Unit = {}
) {
    var text by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        text = value
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (readOnly) {
        OutlinedTextField(
            value = text,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 8.dp, bottom = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            singleLine = true
        )
    } else {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 8.dp, bottom = 8.dp),
            singleLine = true
        )
    }

    if (onClick != {}) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 8.dp, bottom = 8.dp)
        ) {
            TextButton(onClick = onClick) {
                Text("Change")
            }
        }
    }

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
fun PasswordSetting(
    title: String,
    summary: String? = null,
    icon: Int,
    value: String,
    onValueChange: (String) -> Unit
) {
    var text by remember { mutableStateOf(value) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        text = value
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onValueChange(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 8.dp, bottom = 8.dp),
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = {
                passwordVisible = !passwordVisible
            }) {
                Icon(
                    painter = painterResource(
                        if (passwordVisible) R.drawable.baseline_visibility_24 else R.drawable.baseline_visibility_off_24
                    ),
                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                )
            }
        }
    )

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
fun SliderSetting(
    title: String,
    summary: String? = null,
    icon: Int,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(value) }

    LaunchedEffect(value) {
        sliderValue = value
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = sliderValue.toInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    Slider(
        value = sliderValue,
        onValueChange = {
            sliderValue = it
            onValueChange(it)
        },
        valueRange = valueRange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 8.dp, bottom = 8.dp)
    )

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
fun DateSetting(
    title: String,
    icon: Int,
    date: Date?,
    onDateChange: (Date) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    OutlinedTextField(
        value = date?.let { dateFormat.format(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 8.dp, bottom = 8.dp),
        trailingIcon = {
            IconButton(onClick = {
                val cal = Calendar.getInstance()
                date?.let { cal.time = it }
                val year = cal[Calendar.YEAR]
                val month = cal[Calendar.MONTH]
                val day = cal[Calendar.DAY_OF_MONTH]

                val datePicker = DatePickerDialog(
                    context,
                    { _, selectedYear, selectedMonth, selectedDay ->
                        val newDate = Calendar.getInstance()
                        newDate.set(selectedYear, selectedMonth, selectedDay)
                        onDateChange(newDate.time)
                    },
                    year, month, day
                )
                datePicker.show()
            }) {
                Icon(
                    painter = painterResource(R.drawable.baseline_today_black_24dp),
                    contentDescription = "Select date"
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        singleLine = true
    )

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
fun BulkUpdateSetting(
    title: String,
    summary: String,
    icon: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(R.drawable.baseline_refresh_24),
                contentDescription = "Update",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

private fun countFilesToUpdate(
    summits: List<Summit>,
    fileRowType: FileRowType
): Int {
    val cacheDir = File(MainActivityCompose.cache, "file_backups")
    return summits.count { summit ->
        val file = fileRowType.getFile(summit)
        val backupFile = File(cacheDir, file.name)
        file.exists() && !backupFile.exists()
    }
}

private fun performBulkUpdate(
    summits: List<Summit>,
    fileRowType: FileRowType,
    context: Context,
    onProgressUpdate: (String) -> Unit,
    onComplete: (Int, Int) -> Unit,
    onSaveSummit: (Boolean, Summit) -> Unit,
    coroutineScope: CoroutineScope
) {
    val cacheDir = File(MainActivityCompose.cache, "file_backups")
    val summitsToUpdate = summits.filter { summit ->
        val file = fileRowType.getFile(summit)
        val backupFile = File(cacheDir, file.name)
        file.exists() && !backupFile.exists()
    }

    if (summitsToUpdate.isEmpty()) {
        Toast.makeText(
            context,
            context.getString(R.string.no_files_to_update),
            Toast.LENGTH_SHORT
        ).show()
        return
    }

    AlertDialog.Builder(context)
        .setTitle(context.getString(R.string.bulk_update_confirm_title))
        .setMessage(
            context.getString(
                R.string.bulk_update_confirm_message,
                summitsToUpdate.size,
                fileRowType.name
            )
        )
        .setPositiveButton(R.string.yes) { _, _ ->
            executeBulkUpdate(
                summits = summitsToUpdate,
                fileRowType = fileRowType,
                context = context,
                onProgressUpdate = onProgressUpdate,
                onComplete = onComplete,
                onSaveSummit = onSaveSummit,
                coroutineScope = coroutineScope
            )
        }
        .setNegativeButton(R.string.no, null)
        .show()
}

private fun executeBulkUpdate(
    summits: List<Summit>,
    fileRowType: FileRowType,
    context: Context,
    onProgressUpdate: (String) -> Unit,
    onComplete: (Int, Int) -> Unit,
    onSaveSummit: (Boolean, Summit) -> Unit,
    coroutineScope: CoroutineScope
) {
    val cacheDir = File(MainActivityCompose.cache, "file_backups")

    coroutineScope.launch(Dispatchers.Main) {
        var successCount = 0
        var failCount = 0

        summits.forEachIndexed { index, summit ->
            try {
                if (fileRowType.checkAction(summit)) {
                    withContext(Dispatchers.IO) {
                        val file = fileRowType.getFile(summit)
                        if (!cacheDir.exists()) {
                            cacheDir.mkdirs()
                        }
                        val backupFile = File(cacheDir, file.name)

                        if (file.exists()) {
                            Files.move(
                                file.toPath(),
                                backupFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }

                        fileRowType.updateAction.invoke(summit, backupFile)

                        if (fileRowType.shouldUpdateRoadInfos) {
                            val analyzer = OfflineMapAnalyzer.from(context)
                            if (OfflineMapAnalyzer.isDistancePerSurfacesAndRoadTypePossible(
                                    analyzer,
                                    summit
                                )
                            ) {
                                val updated = OfflineMapAnalyzer.setDistancePerSurfacesAndRoadType(
                                    context, summit
                                )
                                if (updated) {
                                    onSaveSummit(true, summit)
                                }
                            }
                        }
                    }
                    Log.d(
                        "SettingsScreen",
                        "Update for ${summit.getDateAsString()}_${summit.name} done."
                    )
                } else {
                    Log.d(
                        "SettingsScreen",
                        "Skip update for ${summit.getDateAsString()}_${summit.name} as the precondition is not fulfilled."
                    )
                }
                successCount++
            } catch (e: Exception) {
                Log.e(
                    "SettingsScreen",
                    "Failed to update file for summit ${summit.name}: ${e.message}"
                )
                failCount++
            }

            withContext(Dispatchers.Main) {
                onProgressUpdate(
                    context.getString(
                        R.string.bulk_update_progress_message,
                        index + 1,
                        summits.size
                    )
                )
            }
        }

        onComplete(successCount, failCount)
    }
}