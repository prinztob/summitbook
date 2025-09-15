package de.drtobiasprinz.summitbook.ui.dialog

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.DialogAddEntityEventBinding
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SummitEntitySummary
import de.drtobiasprinz.summitbook.ui.dialog.AddSummitDialog.Companion.showDatePicker
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.ParseException
import java.util.Date

class AddEntityEventDialog : DialogFragment() {
    private val viewModel: DatabaseViewModel by activityViewModels()

    var isUpdate = false
    private var entityEvent: EntityEvent? = null
    private lateinit var summitEntitySummary: SummitEntitySummary
    private lateinit var binding: DialogAddEntityEventBinding


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddEntityEventBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (entityEvent == null) {
            createNewEvent()
        }
        if (isUpdate) {
            binding.addEventSave.setText(R.string.updateButtonText)
            updateDialogFields(entityEvent)
        }
        binding.description.addTextChangedListener(watcher)
        binding.date.setText(entityEvent?.getDateAsString())
        binding.date.addTextChangedListener(watcher)
        binding.date.inputType = InputType.TYPE_NULL
        binding.date.onFocusChangeListener =
            View.OnFocusChangeListener { _: View?, hasFocus: Boolean ->
                if (hasFocus) {
                    showDatePicker(binding.date, view.context)
                }
            }
        binding.addEventSave.setOnClickListener {
            parseEvent()
            val event = entityEvent
            if (event != null) {
                viewModel.saveEntityEvent(isUpdate, event)
                dialog?.cancel()
            }

        }
        binding.addEventCancel.setOnClickListener { v: View ->
            dialog?.cancel()
            val text =
                if (entityEvent != null) getString(R.string.update_event_cancel) else getString(
                    R.string.add_new_event_cancel
                )
            Toast.makeText(v.context, text, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNewEvent() {
        entityEvent = EntityEvent(Date(), "", "")
    }


    private fun parseEvent() {
        try {
            val event = entityEvent
            if (event != null) {
                event.description = binding.description.text.toString()
                event.date = Summit.parseDate(binding.date.text.toString())
                event.equipmentName = summitEntitySummary.name
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    private fun updateDialogFields(entry: EntityEvent?) {
        if (entry != null) {
            setTextIfNotAlreadySet(binding.description, entry.description)
            setTextIfNotAlreadySet(binding.date, entry.getDateAsString())
        }
    }

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            binding.addEventSave.isEnabled =
                !(isEmpty(binding.description) || isEmpty(binding.date))
        }

        private fun isEmpty(editText: EditText): Boolean {
            return TextUtils.isEmpty(editText.text.toString().trim { it <= ' ' })
        }
    }

    companion object {
        fun getInstance(
            event: EntityEvent?,
            entity: SummitEntitySummary
        ): AddEntityEventDialog {
            val add = AddEntityEventDialog()
            add.isUpdate = event != null
            add.entityEvent = event
            add.summitEntitySummary = entity
            return add
        }

        private fun setTextIfNotAlreadySet(editText: EditText, setValue: String) {
            if (editText.text.toString() == "") {
                editText.setText(setValue)
            }
        }
    }

}
