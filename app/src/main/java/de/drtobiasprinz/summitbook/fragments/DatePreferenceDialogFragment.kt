package de.drtobiasprinz.summitbook.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import androidx.preference.PreferenceDialogFragmentCompat
import de.drtobiasprinz.summitbook.ui.utils.DatePreference
import java.text.SimpleDateFormat
import java.util.*


class DatePreferenceDialogFragment : PreferenceDialogFragmentCompat() {
    private var year = 0
    private var month = 0
    private var day = 0
    private lateinit var mDatePicker: DatePicker
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var dateValue: String? = datePreference.date
        if (dateValue.isNullOrEmpty()) {
            val calendar = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            dateValue = df.format(calendar.time)
        }
        year = getYear(dateValue)
        month = getMonth(dateValue)
        day = getDay(dateValue)
    }

    override fun onCreateDialogView(context: Context): View {
        mDatePicker = DatePicker(getContext())
        return mDatePicker
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        mDatePicker.updateDate(year, month - 1, day)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            year = mDatePicker.year
            month = mDatePicker.month + 1
            day = mDatePicker.dayOfMonth
            val dateVal = (String.format("%04d", year) + "-"
                    + String.format("%02d", month) + "-"
                    + String.format("%02d", day))
            val preference: DatePreference = datePreference
            if (preference.callChangeListener(dateVal)) {
                preference.date = dateVal
            }
        }
    }

    private val datePreference: DatePreference
        get() = preference as DatePreference

    private fun getYear(dateString: String?): Int {
        val datePieces = dateString!!.split("-").toTypedArray()
        return datePieces[0].toInt()
    }

    private fun getMonth(dateString: String?): Int {
        val datePieces = dateString!!.split("-").toTypedArray()
        return datePieces[1].toInt()
    }

    private fun getDay(dateString: String?): Int {
        val datePieces = dateString!!.split("-").toTypedArray()
        return datePieces[2].toInt()
    }

    companion object {
        fun newInstance(key: String?): DatePreferenceDialogFragment {
            val fragment = DatePreferenceDialogFragment()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}