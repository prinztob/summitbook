package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreference.OnBindEditTextListener

internal class PasswordPreference : EditTextPreference, OnBindEditTextListener {

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setOnBindEditTextListener(this)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setOnBindEditTextListener(this)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setOnBindEditTextListener(this)
    }

    constructor(context: Context?) : super(context) {
        setOnBindEditTextListener(this)
    }

    override fun getSummary(): CharSequence {
        if (summaryProvider != null) {
            val text = super.getText()
            if (!TextUtils.isEmpty(text)) {
                return getMaskedText(text)
            }
        }
        return super.getSummary()
    }

    /**
     * Called when the dialog view for this preference has been bound, allowing you to
     * customize the [EditText] displayed in the dialog.
     *
     * @param editText The [EditText] displayed in the dialog
     */
    override fun onBindEditText(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    companion object {
        /**
         * Get string with each letter substituted by asterisk
         * @param text Input string
         * @return Masked string
         */
        private fun getMaskedText(text: String): String {
            return String(CharArray(text.length)).replace("\u0000", "*")
        }
    }
}