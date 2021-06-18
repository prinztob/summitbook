package de.drtobiasprinz.summitbook.fragments

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.utils.DatePreference




class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings_pref)
    }
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is DatePreference) {
            val f: DialogFragment
            f = DatePreferenceDialogFragment.newInstance(preference.getKey())
            f.setTargetFragment(this, 0)
            f.show(parentFragmentManager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}