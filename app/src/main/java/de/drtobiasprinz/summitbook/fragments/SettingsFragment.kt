package de.drtobiasprinz.summitbook.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.contains
import com.google.android.material.snackbar.Snackbar
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.storage
import de.drtobiasprinz.summitbook.ui.utils.DatePreference
import de.drtobiasprinz.summitbook.ui.utils.MapProvider
import de.drtobiasprinz.summitbook.ui.utils.OpenStreetMapUtils.selectedItem
import de.drtobiasprinz.summitbook.ui.utils.PasswordPreference
import de.drtobiasprinz.summitbook.utils.FileHelper
import de.drtobiasprinz.summitbook.utils.PreferencesHelper
import java.io.File


class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var preferenceCurrentYearSwitch: SwitchPreferenceCompat
    private lateinit var preferenceAnnualTargetActivities: EditTextPreference
    private lateinit var preferenceAnnualTargetHeightMeter: EditTextPreference
    private lateinit var preferenceAnnualTargetKilometer: EditTextPreference
    private lateinit var preferenceIndoorHeightMeterPerCent: SeekBarPreference
    private lateinit var preferenceForecastAverageOfLastXYears: SeekBarPreference

    private lateinit var preferenceMapProvider: SwitchPreferenceCompat
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

    private lateinit var onDeviceMapFiles: List<DocumentFile>
    private lateinit var onDeviceMapsFolderName: String


    /* Overrides onCreatePreferences from PreferenceFragmentCompat */
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
        preferenceAnnualTargetKilometer.setIcon(R.drawable.baseline_multiple_stop_24)
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
                garminMFA.delete()
            }
            return@setOnPreferenceClickListener true
        }

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

        // set up "Map Provider" preference
        preferenceMapProvider = SwitchPreferenceCompat(requireContext())
        preferenceMapProvider.title = getString(R.string.map_provider)
        preferenceMapProvider.setIcon(R.drawable.baseline_map_black_24dp)
        preferenceMapProvider.key = Keys.PREF_ON_DEVICE_MAPS
        preferenceMapProvider.summaryOn = getString(R.string.on_device_maps)
        preferenceMapProvider.summaryOff = getString(R.string.online_maps)
        preferenceMapProvider.setDefaultValue(false)
        preferenceMapProvider.setOnPreferenceClickListener {
            // open the folder chooser, if on-device maps is selected and folder does not contain any .map files
            if (preferenceMapProvider.isChecked && onDeviceMapFiles.isEmpty()) {
                openOnDeviceMapsFolderDialog()
            } else {
                preferenceOnDeviceMapsFolder.isVisible = preferenceMapProvider.isChecked
                if (preferenceMapProvider.isChecked) {
                    selectedItem = MapProvider.HIKING
                } else {
                    selectedItem = MapProvider.OPENTOPO
                }
            }
            return@setOnPreferenceClickListener true
        }

        // set up "On-device Maps" preference
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

        // set preference categories
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


        // setup preference screen
        screen.addPreference(preferenceCategoryGeneral)
        screen.addPreference(preferenceCurrentYearSwitch)
        screen.addPreference(preferenceAnnualTargetActivities)
        screen.addPreference(preferenceAnnualTargetHeightMeter)
        screen.addPreference(preferenceAnnualTargetKilometer)
        screen.addPreference(preferenceIndoorHeightMeterPerCent)
        screen.addPreference(preferenceForecastAverageOfLastXYears)

        screen.addPreference(preferenceCategoryMap)
        screen.addPreference(preferenceMapProvider)
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

        updateOnDeviceMapsPreferencesState()

        preferenceScreen = screen
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is DatePreference) {
            val f: DialogFragment = DatePreferenceDialogFragment.newInstance(preference.getKey())
            f.setTargetFragment(this, 0)
            f.show(fragmentManager!!, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    /* Toggle the visibility of the "On-device Maps Folder" preference and reset the "Map Provider" switch */
    private fun updateOnDeviceMapsPreferencesState() {
        onDeviceMapFiles = FileHelper.getOnDeviceMapFiles(preferenceManager.context)
        onDeviceMapsFolderName = FileHelper.getOnDeviceMapsFolderName(preferenceManager.context)
        preferenceOnDeviceMapsFolder.isVisible =
            preferenceMapProvider.isChecked && onDeviceMapFiles.isNotEmpty()
        preferenceMapProvider.isChecked = preferenceOnDeviceMapsFolder.isVisible
        if (preferenceMapProvider.isChecked) {
            preferenceOnDeviceMapsFolder.summary =
                String.format(getString(R.string.map_folder_summary), onDeviceMapsFolderName)
        }
    }


    /* Register the ActivityResultLauncher for the Select On Device Folder dialog */
    private val requestOnDeviceMapsFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        this::requestOnDeviceMapsFolderResult
    )


    /* Opens up a file picker to select the folder containing the on-device map files */
    private fun openOnDeviceMapsFolderDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        }
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
    }


    /* Get the activity result from the on-device folder dialog */
    private fun requestOnDeviceMapsFolderResult(result: ActivityResult) {
        // save location of the on-device maps folder
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


    /* Notify user that the selected folder does not contain any .mpa files */
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

    companion object {
        private val TAG: String = SettingsFragment::class.java.simpleName
    }

}
