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
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.slider.RangeSlider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.RangeSliderValues
import de.drtobiasprinz.summitbook.db.entities.SortFilterValues
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.CustomAutoCompleteChips
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.utils.Constants
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SortAndFilterFragment : DialogFragment() {

    @Inject
    lateinit var contactsAdapter: ContactsAdapter

    @Inject
    lateinit var sortFilterValues: SortFilterValues
    private var summitViewFragmentViewModel: DatabaseViewModel? = null
    lateinit var database: AppDatabase
    lateinit var entries: List<Summit>

    private lateinit var binding: FragmentSortAndFilterBinding

    var fragment: SummationFragment? = null
    private var extremaValuesAllSummits: ExtremaValuesSummits? = null
    private var extremaValuesFilteredSummits: ExtremaValuesSummits? = null
    private val dateFormat: SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSortAndFilterBinding.inflate(layoutInflater, container, false)
        database = DatabaseModule.provideDatabase(requireContext())
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entries = contactsAdapter.differ.currentList
        binding.imgClose.setOnClickListener {
            dismiss()
        }
        binding.apply.setOnClickListener {
            summitViewFragmentViewModel?.getSortedAndFilteredSummits(sortFilterValues)
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
        setRangeSliders()
    }

    private fun updateButtonGroups() {
        binding.groupSortAscDesc.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (checkedId == binding.buttonDescending.id) {
                    sortFilterValues.orderByDescAsc = "DESC"
                } else {
                    sortFilterValues.orderByDescAsc = "ASC"
                }
            }
        }
        binding.groupSortBy.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonByName.id -> {
                        sortFilterValues.orderBy = "name"
                    }
                    binding.buttonByElevation.id -> {
                        sortFilterValues.orderBy = "maxElevation"
                    }
                    binding.buttonByHeightMeter.id -> {
                        sortFilterValues.orderBy = "elevationGain"
                    }
                    binding.buttonByKilometers.id -> {
                        sortFilterValues.orderBy = "kilometers"
                    }
                    else -> {
                        sortFilterValues.orderBy = "date"
                    }
                }
            }
        }
        binding.groupGpx.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonGpxYes.id -> {
                        sortFilterValues.filterByHasGpxTrack = 1
                    }
                    binding.buttonGpxNo.id -> {
                        sortFilterValues.filterByHasGpxTrack = 0
                    }
                    else -> {
                        sortFilterValues.filterByHasGpxTrack = -1
                    }
                }
            }
        }
        binding.groupImage.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonImageYes.id -> {
                        sortFilterValues.filterByHasImage = 1
                    }
                    binding.buttonImageNo.id -> {
                        sortFilterValues.filterByHasImage = 0
                    }
                    else -> {
                        sortFilterValues.filterByHasImage = -1
                    }
                }
            }
        }
        binding.groupPosition.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonPositionYes.id -> {
                        sortFilterValues.filterByHasPosition = 1
                    }
                    binding.buttonPositionNo.id -> {
                        sortFilterValues.filterByHasPosition = 0
                    }
                    else -> {
                        sortFilterValues.filterByHasPosition = -1
                    }
                }
            }
        }
        binding.groupMarkedSummits.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonMarkedSummit.id -> {
                        sortFilterValues.filterByIsSummit = 1
                        sortFilterValues.filterByIsFavorite = 0
                    }
                    binding.buttonPositionNo.id -> {
                        sortFilterValues.filterByIsSummit = 0
                        sortFilterValues.filterByIsFavorite = 1
                    }
                    else -> {
                        sortFilterValues.filterByIsSummit = 0
                        sortFilterValues.filterByIsFavorite = 0
                    }
                }
            }
        }
    }

    private fun setRangeSliders() {
        setRangeSlider(
            5f,
            binding.rangeSliderKilometers,
            binding.layoutKilometers,
            sortFilterValues.kilometersSlider
        )
        setRangeSlider(
            250f,
            binding.rangeSliderHeightMeter,
            binding.layoutHeightMeter,
            sortFilterValues.elevationGainSlider
        )
        setRangeSlider(
            250f,
            binding.rangeSliderTopElevation,
            binding.layoutTopElevation,
            sortFilterValues.topElevationSlider
        )
    }

    private fun setRangeSlider(
        stepSize: Float,
        slider: RangeSlider,
        layout: LinearLayout,
        values: RangeSliderValues
    ) {
        values.totalMin = database.summitsDao()
            .get(SimpleSQLiteQuery("SELECT MIN(${values.dbColumnName}) FROM ${Constants.SUMMITS_TABLE} where isBookmark = 0"))
            .toFloat()
        values.totalMax = database.summitsDao()
            .get(SimpleSQLiteQuery("SELECT MAX(${values.dbColumnName}) FROM ${Constants.SUMMITS_TABLE} where isBookmark = 0"))
            .toFloat()

        if (values.totalMax > 0) {
            slider.visibility = View.VISIBLE
            layout.visibility = View.VISIBLE
            slider.valueFrom = values.totalMin
            slider.valueTo = values.totalMax
            if (!(values.selectedMin in values.totalMin..values.totalMax)) {
                values.selectedMin = values.totalMin
            }
            if (!(values.selectedMax in values.totalMin..values.totalMax) || values.selectedMax == 0f) {
                values.selectedMax = values.totalMax
            }
            slider.values = listOf(values.selectedMin, values.selectedMax)
            slider.addOnChangeListener { slider1, _, _ ->
                values.selectedMin = slider1.values[0]
                values.selectedMax = slider1.values[1]
            }
        } else {
            slider.visibility = View.GONE
            layout.visibility = View.GONE
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
                    }
                    0 -> {
                        binding.dateStartEndSelector.visibility = View.GONE
                        sortFilterValues.startDate = null
                        sortFilterValues.endDate = null
                    }
                    else -> {
                        binding.dateStartEndSelector.visibility = View.GONE
                        sortFilterValues.startDate = dateFormat.parse(
                            String.format(
                                "%s-01-01 00:00:00",
                                uniqueYearsOfSummit[position - 2]
                            )
                        )
                        sortFilterValues.endDate = dateFormat.parse(
                            String.format(
                                "%s-31-12 23:59:59",
                                uniqueYearsOfSummit[position - 2]
                            )
                        )
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

    fun setViewModel(viewModel: DatabaseViewModel) {
        summitViewFragmentViewModel = viewModel
    }


}