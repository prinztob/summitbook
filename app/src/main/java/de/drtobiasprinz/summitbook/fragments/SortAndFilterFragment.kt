package de.drtobiasprinz.summitbook.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.slider.RangeSlider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.SortFilterValues
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.CustomAutoCompleteChips
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SortAndFilterFragment : DialogFragment() {

    @Inject
    lateinit var contactsAdapter: ContactsAdapter

    @Inject
    lateinit var sortFilterValues: SortFilterValues

    lateinit var database: AppDatabase
    lateinit var entries: List<Summit>

    private lateinit var binding: FragmentSortAndFilterBinding

    var fragment: SummationFragment? = null
    private var extremaValuesAllSummits: ExtremaValuesSummits? = null
    private var extremaValuesFilteredSummits: ExtremaValuesSummits? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSortAndFilterBinding.inflate(layoutInflater, container, false)
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entries = contactsAdapter.differ.currentList
        val uniqueYearsOfSummit = StatisticsFragment.getAllYears(entries)
        binding.imgClose.setOnClickListener {
            dismiss()
        }
        binding.apply.setOnClickListener {
            dismiss()
        }
        binding.setToDefault.setOnClickListener {
            sortFilterValues = SortFilterValues()
            dismiss()
        }
        setExtremeValues()
        updateDateSpinner()
        updateSportTypeSpinner()
        addParticipantsFilter()
        updateButtonGroups()
        setMultiSliders()
    }

    private fun updateButtonGroups() {
        binding.groupSortAscDesc.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == binding.buttonDescending.id) {
                    sortFilterValues.sortDescending = true
                } else {
                    sortFilterValues.sortDescending = true
                }
            }
        }
        binding.groupSortBy.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonByName.id -> {
                        sortFilterValues.sortBy = { e -> e.name }
                    }
                    binding.buttonByElevation.id -> {
                        sortFilterValues.sortBy = { e -> e.elevationData.maxElevation }
                    }
                    binding.buttonByHeightMeter.id -> {
                        sortFilterValues.sortBy = { e -> e.elevationData.elevationGain }
                    }
                    binding.buttonByKilometers.id -> {
                        sortFilterValues.sortBy = { e -> e.kilometers }
                    }
                    else -> {
                        sortFilterValues.sortBy = { e -> e.date }
                    }
                }
            }
        }
        binding.groupGpx.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonGpxYes.id -> {
                        sortFilterValues.filterByHasGpxTrack = { e -> e.hasGpsTrack() }
                    }
                    binding.buttonGpxNo.id -> {
                        sortFilterValues.filterByHasGpxTrack = { e -> !e.hasGpsTrack() }
                    }
                    else -> {
                        sortFilterValues.filterByHasGpxTrack = { e -> true }
                    }
                }
            }
        }
        binding.groupImage.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonImageYes.id -> {
                        sortFilterValues.filterByHasImage = { e -> e.hasImagePath() }
                    }
                    binding.buttonImageNo.id -> {
                        sortFilterValues.filterByHasImage = { e -> !e.hasImagePath() }
                    }
                    else -> {
                        sortFilterValues.filterByHasImage = { e -> true }
                    }
                }
            }
        }
        binding.groupPosition.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonPositionYes.id -> {
                        sortFilterValues.filterByHasPosition = { e -> e.latLng != null }
                    }
                    binding.buttonPositionNo.id -> {
                        sortFilterValues.filterByHasPosition = { e -> e.latLng == null }
                    }
                    else -> {
                        sortFilterValues.filterByHasPosition = { e -> true }
                    }
                }
            }
        }
        binding.groupMarkedSummits.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonMarkedSummit.id -> {
                        sortFilterValues.filterByIsSummit = true
                        sortFilterValues.filterByIsFavorite = false
                    }
                    binding.buttonPositionNo.id -> {
                        sortFilterValues.filterByIsSummit = false
                        sortFilterValues.filterByIsFavorite = true
                    }
                    else -> {
                        sortFilterValues.filterByIsSummit = false
                        sortFilterValues.filterByIsFavorite = false
                    }
                }
            }
        }
    }

    private fun setMultiSliders() {
        setRangeSlider(
            5f,
            binding.rangeSliderKilometers,
            sortFilterValues.valuesRangeSliderKilometers
        )
        setRangeSlider(
            250f,
            binding.rangeSliderHeightMeter,
            sortFilterValues.valuesRangeSliderHeightMeters
        )
        setRangeSlider(
            250f,
            binding.rangeSliderTopElevation,
            sortFilterValues.valuesRangeSliderTopElevation
        )
    }

    private fun setRangeSlider(separation: Float, slider: RangeSlider, values: MutableList<Float>) {
        if (values[3] > 0) {
            slider.visibility = View.VISIBLE
            slider.minSeparation = separation
            slider.valueFrom = values[0]
            slider.valueTo = values[3]
            if (values[1] >= values[0] && values[3] <= values[2]) {
                slider.values = listOf(values[1], values[2])
            }
            slider.addOnChangeListener { slider, value, fromUser ->
                values[1] = slider.values[0]
                values[2] = slider.values[1]
            }
        } else {
            slider.visibility = View.GONE
        }
    }

    private fun updateSportTypeSpinner() {
        val sportTypes: ArrayList<String> = arrayListOf(requireContext().getString(R.string.all))
        for (value in SportType.values()) {
            sportTypes.add(getString(value.sportNameStringId))
        }
        val sportTypeAdapter: ArrayAdapter<*> = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item, sportTypes
        )
        binding.spinnerSportsType.adapter = sportTypeAdapter
        binding.spinnerSportsType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    sortFilterValues.sportType = if (position == 0) {
                        null
                    } else {
                        SportType.values()[position - 1]
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun addParticipantsFilter() {
        val suggestions = entries.flatMap { it.participants }.distinct()
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions)
        CustomAutoCompleteChips(requireView()).addChips(
            adapter,
            emptyList(),
            binding.autoCompleteTextViewParticipants,
            binding.chipGroupParticipants
        )
    }

    private fun setExtremeValues() {
        extremaValuesAllSummits = ExtremaValuesSummits(entries)
        extremaValuesFilteredSummits = ExtremaValuesSummits(entries)
        setInitialValues(
            sortFilterValues.valuesRangeSliderKilometers,
            extremaValuesAllSummits?.minKilometers?.toFloat(),
            extremaValuesAllSummits?.maxKilometersCeil?.toFloat()
        )
        setInitialValues(
            sortFilterValues.valuesRangeSliderHeightMeters,
            extremaValuesAllSummits?.minHeightMeters?.toFloat(),
            extremaValuesAllSummits?.maxHeightMeters?.toFloat()
        )
        setInitialValues(
            sortFilterValues.valuesRangeSliderTopElevation,
            extremaValuesAllSummits?.minTopElevation?.toFloat(),
            extremaValuesAllSummits?.maxTopElevation?.toFloat()
        )

    }

    private fun setInitialValues(values: MutableList<Float>, min: Float?, max: Float?) {
        values[0] = min ?: 0f
        values[1] = min ?: 0f
        values[2] = max ?: 0f
        values[3] = max ?: 0f
    }

    private fun updateDateSpinner() {
        binding.dateStart.inputType = InputType.TYPE_NULL
        binding.dateStart.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {
                showDatePicker(binding.dateStart, requireContext())
            }
        }
        binding.dateStart.setOnClickListener {
            showDatePicker(binding.dateStart, requireContext())
        }

        binding.dateEnd.inputType = InputType.TYPE_NULL
        binding.dateEnd.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {
                showDatePicker(binding.dateEnd, requireContext())
            }
        }
        binding.dateEnd.setOnClickListener {
            showDatePicker(binding.dateEnd, requireContext())
        }
        val dates =
            ArrayList(listOf(*requireContext().resources.getStringArray(R.array.date_array)))
        val uniqueYearsOfSummit = StatisticsFragment.getAllYears(entries)
        dates.addAll(uniqueYearsOfSummit)
        val dateAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dates)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDate.adapter = dateAdapter
        binding.spinnerDate.setSelection(sortFilterValues.selectedDateSpinner)
        binding.spinnerDate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                sortFilterValues.selectedDateSpinner = position
                when (position) {
                    1 -> {
                        binding.dateStartEndSelector.visibility = View.VISIBLE
                        sortFilterValues.selectedYear = ""
                    }
                    0 -> {
                        binding.dateStartEndSelector.visibility = View.GONE
                        sortFilterValues.selectedYear = ""
                    }
                    else -> {
                        binding.dateStartEndSelector.visibility = View.GONE
                        sortFilterValues.selectedYear =uniqueYearsOfSummit[position-2]
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //Another interface callback
            }
        }
    }

    private fun showDatePicker(eText: EditText, context: Context) {
        val cldr = Calendar.getInstance()
        var day = cldr[Calendar.DAY_OF_MONTH]
        var month = cldr[Calendar.MONTH]
        var year = cldr[Calendar.YEAR]
        if (eText.text.toString().trim() != "") {
            val dateSplitted = eText.text.toString().trim().split("-".toRegex()).toTypedArray()
            if (dateSplitted.size == 3) {
                day = dateSplitted[2].toInt()
                month = dateSplitted[1].toInt() - 1
                year = dateSplitted[0].toInt()
            }
        }
        val picker = DatePickerDialog(
            context, R.style.CustomDatePickerDialogTheme,
            { view: DatePicker, yearSelected: Int, monthSelected: Int, daySelected: Int ->
                eText.setText(
                    view.context.getString(
                        R.string.date_format,
                        String.format(
                            context.resources.configuration.locales[0],
                            "%02d",
                            daySelected
                        ),
                        String.format(
                            context.resources.configuration.locales[0],
                            "%02d",
                            monthSelected + 1
                        ),
                        String.format(
                            context.resources.configuration.locales[0],
                            "%02d",
                            yearSelected
                        )
                    )
                )
            }, year, month, day
        )
        picker.show()
    }


}