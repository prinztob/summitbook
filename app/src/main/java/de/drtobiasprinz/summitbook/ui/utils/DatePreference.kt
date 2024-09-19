package de.drtobiasprinz.summitbook.ui.utils


import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference

class DatePreference(context: Context) : DialogPreference(context) {
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