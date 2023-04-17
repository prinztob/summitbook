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
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.android.material.chip.Chip
import com.google.android.material.slider.RangeSlider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.*
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.CustomAutoCompleteChips
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.utils.Constants
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt


@AndroidEntryPoint
class SortAndFilterFragment : DialogFragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues
    lateinit var database: AppDatabase
    private val viewModel: DatabaseViewModel by activityViewModels()

    var apply: () -> Unit = { }

    private lateinit var binding: FragmentSortAndFilterBinding

    var fragment: SummationFragment? = null
    private var extremaValuesAllSummits: ExtremaValuesSummits? = null
    private var extremaValuesFilteredSummits: ExtremaValuesSummits? = null

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
        binding.imgClose.setOnClickListener {
            dismiss()
        }
        binding.apply.setOnClickListener {
            sortFilterValues.participants =
                binding.chipGroupParticipants.children.toList().map { (it as Chip).text.toString() }
            apply()
            dismiss()
        }
        binding.setToDefault.setOnClickListener {
            sortFilterValues.setToDefault()
            apply()
            dismiss()
        }
        viewModel.summitsList.observe(viewLifecycleOwner) {
            it.data.let { summits ->
                if (summits != null) {
                    setExtremeValues(summits)
                    updateDateSpinner()
                    updateSportTypeSpinner()
                    addParticipantsFilter(summits)
                    updateButtonGroups()
                    setRangeSliders()
                }
            }
        }
    }

    private fun updateButtonGroups() {
        binding.groupSortBy.check(
            sortFilterValues.orderByValueButtonGroup.bindingId(
                binding
            )
        )
        binding.groupSortBy.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sortFilterValues.orderByValueButtonGroup =
                    OrderByValueButtonGroup.values().firstOrNull {
                        it.bindingId(binding) == checkedId
                    } ?: OrderByValueButtonGroup.Date
            }
        }
        binding.groupSortAscDesc.check(
            sortFilterValues.orderByAscDescButtonGroup.bindingId(
                binding
            )
        )
        binding.groupSortAscDesc.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sortFilterValues.orderByAscDescButtonGroup =
                    OrderByAscDescButtonGroup.values().firstOrNull {
                        it.bindingId(binding) == checkedId
                    } ?: OrderByAscDescButtonGroup.Descending
            }
        }
        binding.groupGpx.check(sortFilterValues.hasGpxTrackButtonGroup.bindingId(binding))
        binding.groupGpx.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sortFilterValues.hasGpxTrackButtonGroup =
                    HasGpxTrackButtonGroup.values().firstOrNull {
                        it.bindingId(binding) == checkedId
                    } ?: HasGpxTrackButtonGroup.Indifferent
            }
        }
        binding.groupPosition.check(
            sortFilterValues.hasPositionButtonGroup.bindingId(
                binding
            )
        )
        binding.groupPosition.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sortFilterValues.hasPositionButtonGroup =
                    HasPositionButtonGroup.values().firstOrNull {
                        it.bindingId(binding) == checkedId
                    } ?: HasPositionButtonGroup.Indifferent
            }
        }
        binding.groupImage.check(sortFilterValues.hasImageButtonGroup.bindingId(binding))
        binding.groupImage.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sortFilterValues.hasImageButtonGroup =
                    HasImageButtonGroup.values().firstOrNull {
                        it.bindingId(binding) == checkedId
                    } ?: HasImageButtonGroup.Indifferent
            }
        }
        binding.groupMarkedSummits.check(
            sortFilterValues.peakFavoriteButtonGroup.bindingId(
                binding
            )
        )
        binding.groupMarkedSummits.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sortFilterValues.peakFavoriteButtonGroup =
                    PeakFavoriteButtonGroup.values().firstOrNull {
                        it.bindingId(binding) == checkedId
                    } ?: PeakFavoriteButtonGroup.Indifferent
            }
        }
    }

    private fun setRangeSliders() {
        val sortFilterValues = sortFilterValues
        setRangeSlider(
            binding.minValueKm,
            binding.maxValueKm,
            getString(R.string.km),
            binding.rangeSliderKilometers,
            binding.layoutKilometers,
            sortFilterValues.kilometersSlider
        )
        setRangeSlider(
            binding.minValueHm,
            binding.maxValueHm,
            getString(R.string.hm),
            binding.rangeSliderHeightMeter,
            binding.layoutHeightMeter,
            sortFilterValues.elevationGainSlider
        )
        setRangeSlider(
            binding.minValueTopElevation,
            binding.maxValueTopElevation,
            getString(R.string.masl),
            binding.rangeSliderTopElevation,
            binding.layoutTopElevation,
            sortFilterValues.topElevationSlider
        )
    }

    private fun setRangeSlider(
        textMin: TextView,
        textMax: TextView,
        unit: String,
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
            if (values.selectedMin !in values.totalMin..values.totalMax) {
                values.selectedMin = values.totalMin
            }
            if (values.selectedMax !in values.totalMin..values.totalMax || values.selectedMax == 0f) {
                values.selectedMax = values.totalMax
            }
            slider.values = listOf(values.selectedMin, values.selectedMax)
            slider.addOnChangeListener { slider1, _, _ ->
                values.selectedMin = slider1.values[0]
                values.selectedMax = slider1.values[1]
                textMin.text = "${values.selectedMin.roundToInt()}"
                textMax.text = " ${values.selectedMax.roundToInt()} $unit"
            }
        } else {
            slider.visibility = View.GONE
            layout.visibility = View.GONE
        }
        textMin.text = "${values.selectedMin.roundToInt()}"
        textMax.text = " ${values.selectedMax.roundToInt()} $unit"
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
        val position = if (sortFilterValues.sportType == null) {
            0
        } else {
            SportType.values().indexOfFirst { it == sortFilterValues.sportType } + 1
        }
        binding.spinnerSportsType.setSelection(position)
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

    private fun addParticipantsFilter(summits: List<Summit>) {
        val suggestions = summits.flatMap { it.participants }.distinct()
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions)
        CustomAutoCompleteChips(requireView()).addChips(
            adapter,
            sortFilterValues.participants,
            binding.autoCompleteTextViewParticipants,
            binding.chipGroupParticipants
        )
        binding.imageParticipants.imageTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.black)
    }

    private fun setExtremeValues(summits: List<Summit>) {
        extremaValuesAllSummits = ExtremaValuesSummits(summits)
        extremaValuesFilteredSummits = ExtremaValuesSummits(summits)
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
        dates.addAll(sortFilterValues.years)
        val dateAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dates)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val dt = SimpleDateFormat(Summit.DATETIME_FORMAT, Locale.ENGLISH)
        binding.spinnerDate.adapter = dateAdapter
        binding.spinnerDate.setSelection(sortFilterValues.selectedDateSpinner)
        binding.spinnerDate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
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
                            sortFilterValues.startDate = dt.parse(
                                "${sortFilterValues.getSelectedYear()}-01-01 00:00:00"
                            )
                            sortFilterValues.endDate = dt.parse(
                                "${sortFilterValues.getSelectedYear()}-12-31 23:59:59"
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

}