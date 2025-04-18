package de.drtobiasprinz.summitbook.ui.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.ParseException

class AddSegmentDetailsDialog : DialogFragment() {
    private val viewModel: DatabaseViewModel by activityViewModels()

    var isUpdate = false
    private var currentSegmentDetails: SegmentDetails? = null
    private lateinit var usedView: View
    private lateinit var startPointName: EditText
    private lateinit var endPointName: EditText
    private lateinit var saveEntryButton: Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_segment_details, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usedView = view
        saveEntryButton = view.findViewById(R.id.add_segment_save)
        saveEntryButton.isEnabled = false
        startPointName = view.findViewById(R.id.segment_start_point_name)
        startPointName.addTextChangedListener(watcher)
        endPointName = view.findViewById(R.id.segment_end_point_name)
        endPointName.addTextChangedListener(watcher)

        if (currentSegmentDetails == null) {
            createNewSegmentDetails()
        }
        if (isUpdate) {
            saveEntryButton.setText(R.string.updateButtonText)
            updateDialogFields(currentSegmentDetails)
        }

        saveEntryButton.setOnClickListener {
            parseSegmentDetails()
            val segmentDetails = currentSegmentDetails
            if (segmentDetails != null) {
                viewModel.saveSegmentDetails(isUpdate, segmentDetails)
                dialog?.cancel()
            }

        }
        val closeDialogButton = view.findViewById<Button>(R.id.add_segment_cancel)
        closeDialogButton.setOnClickListener { v: View ->
            dialog?.cancel()
            val text =
                if (currentSegmentDetails != null) getString(R.string.update_segment_cancel) else getString(
                    R.string.add_new_segment_cancel
                )
            Toast.makeText(v.context, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNewSegmentDetails() {
        currentSegmentDetails = SegmentDetails(0, "", "")
    }


    private fun parseSegmentDetails() {
        try {
            val segmentDetails = currentSegmentDetails
            if (segmentDetails != null) {
                segmentDetails.startPointName = startPointName.text.toString()
                segmentDetails.endPointName = endPointName.text.toString()
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun updateDialogFields(entry: SegmentDetails?) {
        if (entry != null) {
            setTextIfNotAlreadySet(startPointName, entry.startPointName)
            setTextIfNotAlreadySet(endPointName, entry.endPointName)
        }
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            saveEntryButton.isEnabled = !(isEmpty(startPointName) || isEmpty(endPointName))
        }

        private fun isEmpty(editText: EditText): Boolean {
            return TextUtils.isEmpty(editText.text.toString().trim { it <= ' ' })
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(
            entry: SegmentDetails?
        ): AddSegmentDetailsDialog {
            val add = AddSegmentDetailsDialog()
            add.isUpdate = entry != null
            add.currentSegmentDetails = entry
            return add
        }

        private fun setTextIfNotAlreadySet(editText: EditText, setValue: String) {
            if (editText.text.toString() == "") {
                editText.setText(setValue)
            }
        }
    }

}
