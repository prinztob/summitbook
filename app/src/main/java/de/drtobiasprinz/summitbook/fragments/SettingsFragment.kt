package de.drtobiasprinz.summitbook.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.contains
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.BuildConfig
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.storage
import de.drtobiasprinz.summitbook.ui.dialog.FileRowType
import de.drtobiasprinz.summitbook.ui.utils.DatePreference
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.PasswordPreference
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.OfflineMapAnalyzer
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files


@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: DatabaseViewModel by viewModels()

    private lateinit var preferenceCurrentYearSwitch: SwitchPreferenceCompat
    private lateinit var preferenceAnnualTargetActivities: EditTextPreference
    private lateinit var preferenceAnnualTargetSummits: EditTextPreference
    private lateinit var preferenceAnnualTargetHeightMeter: EditTextPreference
    private lateinit var preferenceAnnualTargetKilometer: EditTextPreference
    private lateinit var preferenceIndoorHeightMeterPerCent: SeekBarPreference
    private lateinit var preferenceForecastAverageOfLastXYears: SeekBarPreference

    private lateinit var preferenceMapProvider: SwitchPreferenceCompat
    private lateinit var preferenceAccessAllFiles: SwitchPreferenceCompat
    private lateinit var preferenceOnDeviceMapsFolder: EditTextPreference
    private lateinit var preferenceUseSimplifiedTracks: SwitchPreferenceCompat
    private lateinit var preferenceMaxPointsOnTrack: EditTextPreference

    private lateinit var preferenceGarminUserName: EditTextPreference
    private lateinit var preferenceGarminPassword: PasswordPreference
    private lateinit var preferenceGarminMFASwitch: SwitchPreferenceCompat
    private lateinit var preferenceDownloadTCXSwitch: SwitchPreferenceCompat
    private lateinit var preferenceGarminSyncStartDate: DatePreference

    private lateinit var preferenceExportThirdPartyData: SwitchPreferenceCompat
    private lateinit var preferenceExportCalculatedData: SwitchPreferenceCompat
    private lateinit var preferenceDisableStartUpTasks: SwitchPreferenceCompat

    private lateinit var onDeviceMapFiles: List<DocumentFile>
    private lateinit var onDeviceMbTilesFiles: List<DocumentFile>
    private lateinit var onDeviceMapsFolderName: String


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val screen = preferenceManager.createPreferenceScreen(preferenceManager.context)
        onDeviceMapFiles = FileHelper.getOnDeviceMapFiles(requireContext())
        onDeviceMapsFolderName = FileHelper.getOnDeviceMapsFolderName(requireContext())


        preferenceAnnualTargetActivities = EditTextPreference(requireContext())
        preferenceAnnualTargetActivities.title = getString(R.string.annual_target_activities_title)
        preferenceAnnualTargetActivities.key = Keys.PREF_ANNUAL_TARGET_ACTIVITIES
        preferenceAnnualTargetActivities.setIcon(R.drawable.baseline_directions_walk_black_24dp)
        preferenceAnnualTargetActivities.summary =
            getString(R.string.annual_target_activities_summary)
        preferenceAnnualTargetActivities.setDefaultValue("52")

        preferenceAnnualTargetSummits = EditTextPreference(requireContext())
        preferenceAnnualTargetSummits.title = getString(R.string.annual_target_summits_title)
        preferenceAnnualTargetSummits.key = Keys.PREF_ANNUAL_TARGET_SUMMITS
        preferenceAnnualTargetSummits.setIcon(R.drawable.outline_landscape_2_24)
        preferenceAnnualTargetSummits.summary =
            getString(R.string.annual_target_summits_summary)
        preferenceAnnualTargetSummits.setDefaultValue("10")

        preferenceAnnualTargetHeightMeter = EditTextPreference(requireContext())
        preferenceAnnualTargetHeightMeter.title =
            getString(R.string.annual_target_height_meter_title)
        preferenceAnnualTargetHeightMeter.key = Keys.PREF_ANNUAL_TARGET
        preferenceAnnualTargetHeightMeter.setIcon(R.drawable.baseline_trending_up_black_24dp)
        preferenceAnnualTargetHeightMeter.summary =
            getString(R.string.annual_target_height_meter_summary)
        preferenceAnnualTargetHeightMeter.setDefaultValue("50000")

        preferenceAnnualTargetKilometer = EditTextPreference(requireContext())
        preferenceAnnualTargetKilometer.title = getString(R.string.annual_target_kilometer_title)
        preferenceAnnualTargetKilometer.key = Keys.PREF_ANNUAL_TARGET_KM
        preferenceAnnualTargetKilometer.setIcon(R.drawable.outline_distance_24)
        preferenceAnnualTargetKilometer.summary =
            getString(R.string.annual_target_kilometer_summary)
        preferenceAnnualTargetKilometer.setDefaultValue("1200")

        preferenceGarminUserName = EditTextPreference(requireContext())
        preferenceGarminUserName.title = getString(R.string.garmin_user_title)
        preferenceGarminUserName.key = Keys.PREF_GARMIN_USERNAME
        preferenceGarminUserName.setIcon(R.drawable.baseline_account_circle_24)
        preferenceGarminUserName.summary = getString(R.string.garmin_user_summary)

        preferenceGarminPassword = PasswordPreference(requireContext())
        preferenceGarminPassword.title = getString(R.string.garmin_pwd_title)
        preferenceGarminPassword.key = Keys.PREF_GARMIN_PASSWORD
        preferenceGarminPassword.setIcon(R.drawable.baseline_password_24)
        preferenceGarminPassword.summary = getString(R.string.garmin_pwd_summary)

        preferenceDownloadTCXSwitch = SwitchPreferenceCompat(requireContext())
        preferenceDownloadTCXSwitch.title = getString(R.string.tcx_switch_title)
        preferenceDownloadTCXSwitch.key = Keys.PREF_DOWNLOAD_TCX
        preferenceDownloadTCXSwitch.setIcon(R.drawable.baseline_download_black_24dp)
        preferenceDownloadTCXSwitch.summary = getString(R.string.tcx_switch)
        preferenceDownloadTCXSwitch.setDefaultValue(false)

        preferenceGarminMFASwitch = SwitchPreferenceCompat(requireContext())
        preferenceGarminMFASwitch.title = getString(R.string.garmin_mfa_switch_title)
        preferenceGarminMFASwitch.key = Keys.PREF_GARMIN_MFA
        preferenceGarminMFASwitch.setIcon(R.drawable.baseline_account_circle_24)
        preferenceGarminMFASwitch.summary = getString(R.string.garmin_mfa_switch)
        preferenceGarminMFASwitch.setDefaultValue(false)
        preferenceGarminMFASwitch.setOnPreferenceClickListener {
            val garminMFA = File(
                storage?.absolutePath,
                ".garminconnect"
            )
            if (!preferenceGarminMFASwitch.isChecked && garminMFA.exists()) {
                garminMFA.deleteRecursively()
            }
            return@setOnPreferenceClickListener true
        }

        preferenceDisableStartUpTasks = SwitchPreferenceCompat(requireContext())
        preferenceDisableStartUpTasks.title = getString(R.string.debug_switch_title)
        preferenceDisableStartUpTasks.key = Keys.PREF_DEBUG
        preferenceDisableStartUpTasks.setIcon(R.drawable.baseline_do_not_disturb_on_total_silence_24)
        preferenceDisableStartUpTasks.summary = getString(R.string.debug_switch)
        preferenceDisableStartUpTasks.setDefaultValue(false)

        preferenceCurrentYearSwitch = SwitchPreferenceCompat(requireContext())
        preferenceCurrentYearSwitch.title = getString(R.string.current_year_title)
        preferenceCurrentYearSwitch.key = Keys.PREF_CURRENT_YEAR_SWITCH
        preferenceCurrentYearSwitch.setIcon(R.drawable.baseline_calendar_today_24)
        preferenceCurrentYearSwitch.summary = getString(R.string.current_year_summary)
        preferenceCurrentYearSwitch.setDefaultValue(false)

        preferenceGarminSyncStartDate = DatePreference(requireContext())
        preferenceGarminSyncStartDate.title = getString(R.string.start_date_sync_garmin)
        preferenceGarminSyncStartDate.key = Keys.PREF_THIRD_PARTY_START_DATE
        preferenceGarminSyncStartDate.setIcon(R.drawable.baseline_calendar_today_24)
        preferenceGarminSyncStartDate.setDefaultValue("2024-01-01")

        preferenceUseSimplifiedTracks = SwitchPreferenceCompat(requireContext())
        preferenceUseSimplifiedTracks.title = getString(R.string.use_simplified_tracks_title)
        preferenceUseSimplifiedTracks.key = Keys.PREF_USE_SIMPLIFIED_TRACKS
        preferenceUseSimplifiedTracks.setIcon(R.drawable.baseline_map_black_24dp)
        preferenceUseSimplifiedTracks.summary = getString(R.string.use_simplified_tracks)
        preferenceUseSimplifiedTracks.setDefaultValue(true)

        preferenceExportThirdPartyData = SwitchPreferenceCompat(requireContext())
        preferenceExportThirdPartyData.title = getString(R.string.export_third_party_data_title)
        preferenceExportThirdPartyData.key = Keys.PREF_EXPORT_THIRD_PARTY_DATA
        preferenceExportThirdPartyData.setIcon(R.drawable.baseline_import_export_24)
        preferenceExportThirdPartyData.summary = getString(R.string.export_third_party_data)
        preferenceExportThirdPartyData.setDefaultValue(true)

        preferenceExportCalculatedData = SwitchPreferenceCompat(requireContext())
        preferenceExportCalculatedData.title = getString(R.string.export_calculated_data_title)
        preferenceExportCalculatedData.key = Keys.PREF_EXPORT_CALCULATED_DATA
        preferenceExportCalculatedData.setIcon(R.drawable.baseline_import_export_24)
        preferenceExportCalculatedData.summary = getString(R.string.export_calculated_data)
        preferenceExportCalculatedData.setDefaultValue(true)

        preferenceMaxPointsOnTrack = EditTextPreference(requireContext())
        preferenceMaxPointsOnTrack.title = getString(R.string.max_number_points_title)
        preferenceMaxPointsOnTrack.key = Keys.PREF_MAX_NUMBER_POINT
        preferenceMaxPointsOnTrack.setIcon(R.drawable.baseline_map_black_24dp)
        preferenceMaxPointsOnTrack.summary = getString(R.string.max_number_points)
        preferenceMaxPointsOnTrack.setDefaultValue("10000")

        preferenceIndoorHeightMeterPerCent = SeekBarPreference(requireContext())
        preferenceIndoorHeightMeterPerCent.title = getString(R.string.indoor_hm_percent)
        preferenceIndoorHeightMeterPerCent.key = Keys.PREF_INDOOR_HEIGHT_METER
        preferenceIndoorHeightMeterPerCent.setIcon(R.drawable.baseline_other_houses_24)
        preferenceIndoorHeightMeterPerCent.showSeekBarValue = true
        preferenceIndoorHeightMeterPerCent.setDefaultValue(0)

        preferenceForecastAverageOfLastXYears = SeekBarPreference(requireContext())
        preferenceForecastAverageOfLastXYears.title = getString(R.string.forecast_average_title)
        preferenceForecastAverageOfLastXYears.key = Keys.PREF_FORECAST_AVERAGE
        preferenceForecastAverageOfLastXYears.setIcon(R.drawable.baseline_trending_flat_24)
        preferenceForecastAverageOfLastXYears.summary = getString(R.string.forecast_average_summary)
        preferenceForecastAverageOfLastXYears.setDefaultValue(3)
        preferenceForecastAverageOfLastXYears.min = 0
        preferenceForecastAverageOfLastXYears.max = 20


        preferenceAccessAllFiles = SwitchPreferenceCompat(requireContext())
        preferenceAccessAllFiles.title = getString(R.string.access_all_files)
        preferenceAccessAllFiles.setIcon(R.drawable.baseline_map_black_24dp)
        preferenceAccessAllFiles.key = Keys.PREF_ACCESS_ALL_FILES
        preferenceAccessAllFiles.summary = getString(R.string.access_all_files_summary)
        preferenceAccessAllFiles.setDefaultValue(false)
        preferenceAccessAllFiles.setOnPreferenceClickListener {
            // open the folder chooser, if on-device maps is selected and folder does not contain any .map files
            if (preferenceAccessAllFiles.isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        ("package:" + BuildConfig.APPLICATION_ID).toUri()
                    ).apply {}
                    startActivity(intent)
                } else {
                    Log.i(TAG, "This is not supported in your Android Version")
                }
            }
            return@setOnPreferenceClickListener true
        }
        preferenceMapProvider = SwitchPreferenceCompat(requireContext())
        preferenceMapProvider.title = getString(R.string.map_provider)
        preferenceMapProvider.setIcon(R.drawable.baseline_map_black_24dp)
        preferenceMapProvider.key = Keys.PREF_ON_DEVICE_MAPS
        preferenceMapProvider.summaryOn = getString(R.string.on_device_maps)
        preferenceMapProvider.summaryOff = getString(R.string.online_maps)
        preferenceMapProvider.setDefaultValue(false)
        preferenceMapProvider.setOnPreferenceClickListener {
            if (preferenceMapProvider.isChecked && onDeviceMapFiles.isEmpty() && onDeviceMbTilesFiles.isEmpty()) {
                openOnDeviceMapsFolderDialog()
            } else {
                preferenceOnDeviceMapsFolder.isVisible = preferenceMapProvider.isChecked
                selectedItem = if (preferenceMapProvider.isChecked) {
                    MapProvider.HIKING
                } else if (onDeviceMbTilesFiles.isNotEmpty()) {
                    MapProvider.MBTILES
                } else {
                    MapProvider.OPENTOPO
                }
            }
            return@setOnPreferenceClickListener true
        }

        preferenceOnDeviceMapsFolder = EditTextPreference(requireContext())
        preferenceOnDeviceMapsFolder.title = getString(R.string.map_folder)
        preferenceOnDeviceMapsFolder.key = Keys.PREF_ON_DEVICE_MAPS_FOLDER
        preferenceOnDeviceMapsFolder.setIcon(R.drawable.baseline_map_black_24dp)
        preferenceOnDeviceMapsFolder.summary =
            String.format(getString(R.string.map_folder_summary), onDeviceMapsFolderName)
        preferenceOnDeviceMapsFolder.setOnPreferenceClickListener {
            openOnDeviceMapsFolderDialog()
            return@setOnPreferenceClickListener true
        }

        val preferenceCategoryGeneral = PreferenceCategory(requireContext())
        preferenceCategoryGeneral.title = getString(R.string.pref_general_title)
        preferenceCategoryGeneral.contains(preferenceCurrentYearSwitch)
        preferenceCategoryGeneral.contains(preferenceAnnualTargetActivities)
        preferenceCategoryGeneral.contains(preferenceAnnualTargetHeightMeter)
        preferenceCategoryGeneral.contains(preferenceAnnualTargetKilometer)
        preferenceCategoryGeneral.contains(preferenceIndoorHeightMeterPerCent)
        preferenceCategoryGeneral.contains(preferenceForecastAverageOfLastXYears)

        val preferenceCategoryMap = PreferenceCategory(requireContext())
        preferenceCategoryMap.title = getString(R.string.pref_map_title)
        preferenceCategoryMap.contains(preferenceMapProvider)
        preferenceCategoryMap.contains(preferenceAccessAllFiles)
        preferenceCategoryMap.contains(preferenceOnDeviceMapsFolder)
        preferenceCategoryMap.contains(preferenceUseSimplifiedTracks)
        preferenceCategoryMap.contains(preferenceMaxPointsOnTrack)

        val preferenceCategoryThirdParty = PreferenceCategory(requireContext())
        preferenceCategoryThirdParty.title = getString(R.string.pref_third_party_title)
        preferenceCategoryThirdParty.contains(preferenceGarminUserName)
        preferenceCategoryThirdParty.contains(preferenceGarminPassword)
        preferenceCategoryThirdParty.contains(preferenceGarminMFASwitch)
        preferenceCategoryThirdParty.contains(preferenceDownloadTCXSwitch)
        preferenceCategoryThirdParty.contains(preferenceGarminSyncStartDate)

        val preferenceCategoryExport = PreferenceCategory(requireContext())
        preferenceCategoryExport.title = getString(R.string.pref_export_title)
        preferenceCategoryExport.contains(preferenceExportThirdPartyData)
        preferenceCategoryExport.contains(preferenceExportCalculatedData)
        preferenceCategoryExport.contains(preferenceDisableStartUpTasks)

        val preferenceCategoryBulkFileManagement = PreferenceCategory(requireContext())
        preferenceCategoryBulkFileManagement.title = getString(R.string.pref_bulk_file_management_title)

        screen.addPreference(preferenceCategoryGeneral)
        screen.addPreference(preferenceCurrentYearSwitch)
        screen.addPreference(preferenceAnnualTargetActivities)
        screen.addPreference(preferenceAnnualTargetHeightMeter)
        screen.addPreference(preferenceAnnualTargetKilometer)
        screen.addPreference(preferenceIndoorHeightMeterPerCent)
        screen.addPreference(preferenceForecastAverageOfLastXYears)

        screen.addPreference(preferenceCategoryMap)
        screen.addPreference(preferenceMapProvider)
        screen.addPreference(preferenceAccessAllFiles)
        screen.addPreference(preferenceOnDeviceMapsFolder)
        screen.addPreference(preferenceUseSimplifiedTracks)
        screen.addPreference(preferenceMaxPointsOnTrack)

        screen.addPreference(preferenceCategoryThirdParty)
        screen.addPreference(preferenceGarminUserName)
        screen.addPreference(preferenceGarminPassword)
        screen.addPreference(preferenceGarminMFASwitch)
        screen.addPreference(preferenceDownloadTCXSwitch)
        screen.addPreference(preferenceGarminSyncStartDate)

        screen.addPreference(preferenceCategoryExport)
        screen.addPreference(preferenceExportThirdPartyData)
        screen.addPreference(preferenceExportCalculatedData)
        screen.addPreference(preferenceDisableStartUpTasks)

        screen.addPreference(preferenceCategoryBulkFileManagement)
        addBulkFileManagementPreferences(screen)

        updateOnDeviceMapsPreferencesState()

        preferenceScreen = screen
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is DatePreference) {
            val f: DialogFragment = DatePreferenceDialogFragment.newInstance(preference.getKey())
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun updateOnDeviceMapsPreferencesState() {
        onDeviceMapFiles = FileHelper.getOnDeviceMapFiles(preferenceManager.context)
        onDeviceMbTilesFiles = FileHelper.getOnDeviceMbtilesFiles(preferenceManager.context)
        onDeviceMapsFolderName = FileHelper.getOnDeviceMapsFolderName(preferenceManager.context)
        preferenceOnDeviceMapsFolder.isVisible =
            preferenceMapProvider.isChecked && onDeviceMapFiles.isNotEmpty()
        preferenceMapProvider.isChecked = preferenceOnDeviceMapsFolder.isVisible
        if (preferenceMapProvider.isChecked) {
            preferenceOnDeviceMapsFolder.summary =
                String.format(getString(R.string.map_folder_summary), onDeviceMapsFolderName)
        }
    }


    private val requestOnDeviceMapsFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::requestOnDeviceMapsFolderResult
    )

    private fun openOnDeviceMapsFolderDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {}
            try {
                requestOnDeviceMapsFolderLauncher.launch(intent)
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to select a on-device maps folder.\n$exception")
                Toast.makeText(
                    requireContext(),
                    R.string.toast_message_install_file_helper,
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Log.i(TAG, "This is not supported in your Android Version")
        }
    }

    private fun requestOnDeviceMapsFolderResult(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                FileHelper.makeUriPersistent(preferenceManager.context, uri)
                PreferencesHelper.saveOnDeviceMapsFolder(uri.toString())
                updateOnDeviceMapsPreferencesState()
                if (onDeviceMapFiles.isEmpty()) {
                    showEmptyFolderError()
                }
            }
        }
    }


    private fun showEmptyFolderError() {
        val anchorView: View? = activity?.findViewById(R.id.content_frame)
        val contextView: View? = this.view?.rootView
        if (contextView != null && anchorView != null) {
            Snackbar.make(
                contextView,
                "The folder used for on-device maps must contain .map files.",
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction("Dismiss") { }
                .setAnchorView(anchorView)
                .show()
        }
    }

    private fun addBulkFileManagementPreferences(screen: androidx.preference.PreferenceScreen) {
        viewModel.summitsList.observe(this) { summitsDataStatus ->
            summitsDataStatus.data?.let { summits ->
                FileRowType.entries.forEach { fileRowType ->
                    val preference = Preference(requireContext())
                    val fileCount = countFilesToUpdate(summits, fileRowType)
                    
                    preference.title = when (fileRowType) {
                        FileRowType.GPX_TRACK -> getString(R.string.bulk_update_gpx_tracks)
                        FileRowType.GPX_SIMPLIFIED -> getString(R.string.bulk_update_gpx_simplified)
                        FileRowType.YAML_EXTENSIONS -> getString(R.string.bulk_update_yaml_extensions)
                        FileRowType.GPXPY_JSON -> getString(R.string.bulk_update_gpxpy_json)
                    }
                    
                    preference.summary = getString(R.string.bulk_update_file_count, fileCount)
                    preference.setIcon(R.drawable.baseline_refresh_24)
                    preference.isEnabled = fileCount > 0
                    
                    preference.setOnPreferenceClickListener {
                        performBulkUpdate(summits, fileRowType)
                        true
                    }
                    
                    screen.addPreference(preference)
                }
            }
        }
    }

    private fun countFilesToUpdate(summits: List<Summit>, fileRowType: FileRowType): Int {
        val cacheDir = File(MainActivity.cache, "file_backups")
        return summits.count { summit ->
            val file = fileRowType.getFile(summit)
            val backupFile = File(cacheDir, file.name)
            // Only count files that exist and don't already have a backup
            file.exists() && !backupFile.exists()
        }
    }

    private fun performBulkUpdate(summits: List<Summit>, fileRowType: FileRowType) {
        val cacheDir = File(MainActivity.cache, "file_backups")
        val summitsToUpdate = summits.filter { summit ->
            val file = fileRowType.getFile(summit)
            val backupFile = File(cacheDir, file.name)
            // Only include files that exist and don't already have a backup
            file.exists() && !backupFile.exists()
        }

        if (summitsToUpdate.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.no_files_to_update),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.bulk_update_confirm_title))
            .setMessage(getString(R.string.bulk_update_confirm_message, summitsToUpdate.size, fileRowType.name))
            .setPositiveButton(R.string.yes) { _, _ ->
                executeBulkUpdate(summitsToUpdate, fileRowType)
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun executeBulkUpdate(summits: List<Summit>, fileRowType: FileRowType) {
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.bulk_update_progress_title))
            .setMessage(getString(R.string.bulk_update_progress_message, 0, summits.size))
            .setCancelable(false)
            .create()
        
        progressDialog.show()

        lifecycleScope.launch {
            var successCount = 0
            var failCount = 0

            summits.forEachIndexed { index, summit ->
                try {
                    if (fileRowType.checkAction(summit)) {
                        withContext(Dispatchers.IO) {
                            val file = fileRowType.getFile(summit)
                            val cacheDir = File(MainActivity.cache, "file_backups")
                            if (!cacheDir.exists()) {
                                cacheDir.mkdirs()
                            }
                            val backupFile = File(cacheDir, file.name)

                            if (file.exists()) {
                                Files.move(
                                    file.toPath(),
                                    backupFile.toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                )
                            }

                            fileRowType.updateAction.invoke(summit, backupFile)

                            if (fileRowType.shouldUpdateRoadInfos) {
                                val analyzer = OfflineMapAnalyzer.from(requireContext())
                                if (OfflineMapAnalyzer.isDistancePerSurfacesAndRoadTypePossible(analyzer, summit)) {
                                    val updated = OfflineMapAnalyzer.setDistancePerSurfacesAndRoadType(
                                        requireContext(), summit
                                    )
                                    if (updated) {
                                        viewModel.saveSummit(true, summit)
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Update for ${summit.getDateAsString()}_${summit.name} done.")
                    } else {
                        Log.d(TAG, "Skip update for ${summit.getDateAsString()}_${summit.name} as the precondition is not fulfilled.")
                    }
                    successCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update file for summit ${summit.name}: ${e.message}")
                    failCount++
                }

                withContext(Dispatchers.Main) {
                    progressDialog.setMessage(
                        getString(R.string.bulk_update_progress_message, index + 1, summits.size)
                    )
                }
            }

            progressDialog.dismiss()
            
            Toast.makeText(
                requireContext(),
                getString(R.string.bulk_update_complete, successCount, failCount),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private val TAG: String = SettingsFragment::class.java.simpleName
    }

}
