package de.drtobiasprinz.summitbook.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.drtobiasprinz.summitbook.R


class CustomAutoCompleteChips(private val mView: View, private val chipIconId: Int? = null) {
    private val newline = "\n"

    fun addChips(suggestionsAdapter: ArrayAdapter<String>, entries: List<String>?, autoCompleteTextView: AutoCompleteTextView, chipGroup: ChipGroup) {
        autoCompleteTextView.setAdapter(suggestionsAdapter)
        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            autoCompleteTextView.text = null
            val chipNames = chipGroup.children.toList().map { (it as Chip).text.toString() }
            val selected = parent.getItemAtPosition(position) as String
            if (selected != "" && !chipNames.contains(selected)) {
                addChipToGroup(selected, chipGroup)
            }
        }

        entries?.forEach {
            if (it != "") {
                addChipToGroup(it, chipGroup)
            }
        }

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val chipNames = chipGroup.children.toList().map { (it as Chip).text.toString() }
                var name = s.toString()
                if (name.endsWith(newline)) {
                    name = name.replace(newline, "")
                    if (name != "" && !chipNames.contains(name)) {
                        addChipToGroup(name, chipGroup)
                    }
                    s.clear()
                }
            }
        })
    }


    private fun addChipToGroup(name: String, chipGroup: ChipGroup) {
        val chip = Chip(mView.context)
        chip.text = name
        if (chipIconId != null) {
            chip.chipIcon = ContextCompat.getDrawable(mView.context, chipIconId)
        }
        chip.setChipIconTintResource(R.color.ForestGreen)
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chipGroup.addView(chip as View)
        chip.setOnCloseIconClickListener { chipGroup.removeView(chip as View) }
    }

}