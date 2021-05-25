package de.drtobiasprinz.summitbook.ui.utils

import android.text.InputFilter
import android.text.Spanned

class InputFilterMinMax(private val min: Int, private val max: Int) : InputFilter {
    override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        try {
            // Remove the string out of destination that is to be replaced
            var newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend)
            // Add the new string in
            newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart)
            val input = newVal.toDouble().toInt()
            if (isInRange(min, max, input)) {
                return null
            }
        } catch (nfe: NumberFormatException) {
            if (source == ".") {
                return null
            }
            // just in case an invalid character was added
        }
        return ""
    }

    private fun isInRange(a: Int, b: Int, c: Int): Boolean {
        return if (b > a) c in a..b else c in b..a
    }

}