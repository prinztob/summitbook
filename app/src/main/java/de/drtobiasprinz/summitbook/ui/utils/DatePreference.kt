package de.drtobiasprinz.summitbook.ui.utils


import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import de.drtobiasprinz.summitbook.R


/**
 * A dialog preference that shown calendar in the dialog.
 *
 * Saves a string value.
 */
class DatePreference(context: Context?, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    private var mDateValue: String? = null
    override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
        return a.getString(index) ?: ""
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        date = getPersistedString(defaultValue as String?)
    }

    var date: String?
        get() = mDateValue
        set(text) {
            val wasBlocking = shouldDisableDependents()
            mDateValue = text
            persistString(text)
            val isBlocking = shouldDisableDependents()
            if (isBlocking != wasBlocking) {
                notifyDependencyChange(isBlocking)
            }
            notifyChanged()
        }


}