package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.drtobiasprinz.summitbook.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_pref)
    }
}