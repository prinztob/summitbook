package de.drtobiasprinz.summitbook.fragments

import android.annotation.SuppressLint
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
import com.google.android.material.chip.Chip
import com.google.android.material.slider.RangeSlider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.*
import de.drtobiasprinz.summitbook.ui.CustomAutoCompleteChips
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.hasRecordsBeenAdded
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt


@AndroidEntryPoint
class SortAndFilterFragment : DialogFragment() {

    @Inject
    lateinit var sortFilterValues: SortFilterValues
    private val viewModel: DatabaseViewModel by activityViewModels()
    private val df: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)

    var apply: () -> Unit = { }

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
        binding.imgClose.setOnClickListener {
            dismiss()
        }
        binding.apply.setOnClickListener {
            if (sortFilterValues.selectedDateSpinner == 1 &&
                binding.dateStart.text.toString().trim { it <= ' ' } != ""
            ) {
                sortFilterValues.startDate = Summit.parseDate(binding.dateStart.text.toString())
            }
            if (sortFilterValues.selectedDateSpinner == 1 &&
                binding.dateEnd.text.toString().trim { it <= ' ' } != ""
            ) {
                sortFilterValues.endDate = Summit.parseDate(binding.dateEnd.text.toString())
            }
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
                    updateSpinner()
                    addParticipantsFilter(summits)
                    updateButtonGroups()
                    setRangeSliders(summits)
                }
            }
        }
    }

    private fun updateButtonGroups() {
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

    private fun setRangeSliders(summits: List<Summit>) {
        val sortFilterValues = sortFilterValues
        setRangeSlider(
            summits,
            binding.minValueKm,
            binding.maxValueKm,
            getString(R.string.km),
            binding.rangeSliderKilometers,
            binding.layoutKilometers,
            sortFilterValues.kilometersSlider,
            5f
        )
        setRangeSlider(
            summits,
            binding.minValueHm,
            binding.maxValueHm,
            getString(R.string.hm),
            binding.rangeSliderHeightMeter,
            binding.layoutHeightMeter,
            sortFilterValues.elevationGainSlider,
            250f
        )
        setRangeSlider(
            summits,
            binding.minValueTopElevation,
            binding.maxValueTopElevation,
            getString(R.string.masl),
            binding.rangeSliderTopElevation,
            binding.layoutTopElevation,
            sortFilterValues.topElevationSlider,
            250f
        )
    }

    @SuppressLint("SetTextI18n")
    private fun setRangeSlider(
        summits: List<Summit>,
        textMin: TextView,
        textMax: TextView,
        unit: String,
        slider: RangeSlider,
        layout: LinearLayout,
        values: RangeSliderValues,
        stepSize: Float
    ) {

        if (summits.isNotEmpty()) {

            val min = values.getValue(summits.minBy { values.getValue(it) })
            values.totalMin = floor(min / stepSize) * stepSize
            if (values.totalMin > 0) values.totalMin = 0f
            val max = values.getValue(summits.maxBy { values.getValue(it) })
            values.totalMax = ceil(max / stepSize) * stepSize
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
                slider.stepSize = stepSize
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
        } else {
            slider.visibility = View.GONE
            layout.visibility = View.GONE
        }
    }

    private fun updateSpinner() {
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
                    view: View?,
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

        val dateAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            OrderBySpinnerEntry.getSpinnerEntriesWithoutAccumulated()
                .map { resources.getString(it.nameId) }.toTypedArray()
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSortBy.adapter = dateAdapter
        binding.spinnerSortBy.setSelection(
            OrderBySpinnerEntry.getSpinnerEntriesWithoutAccumulated()
                .indexOfFirst { it == sortFilterValues.orderByValueSpinner })
        binding.spinnerSortBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                i: Int,
                l: Long
            ) {
                sortFilterValues.orderByValueSpinner =
                    OrderBySpinnerEntry.getSpinnerEntriesWithoutAccumulated()[i]
            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
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
                    adapterView: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (view != null) {
                        if (sortFilterValues.selectedDateSpinner != position) {
                            hasRecordsBeenAdded = false
                        }
                        sortFilterValues.selectedDateSpinner = position
                        when (position) {
                            1 -> {
                                sortFilterValues.startDate?.let {
                                    binding.dateStart.setText(df.format(it))
                                }
                                sortFilterValues.endDate?.let {
                                    binding.dateEnd.setText(df.format(it))
                                }
                                binding.dateStart.visibility = View.VISIBLE
                                binding.dateEnd.visibility = View.VISIBLE
                            }

                            0 -> {
                                binding.dateStart.visibility = View.GONE
                                binding.dateEnd.visibility = View.GONE
                                sortFilterValues.startDate = null
                                sortFilterValues.endDate = null
                            }

                            else -> {
                                binding.dateStart.visibility = View.GONE
                                binding.dateEnd.visibility = View.GONE
                                sortFilterValues.startDate = dt.parse(
                                    "${sortFilterValues.getSelectedYear()}-01-01 00:00:00"
                                )
                                sortFilterValues.endDate = dt.parse(
                                    "${sortFilterValues.getSelectedYear()}-12-31 23:59:59"
                                )
                            }
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
            val dateSplit = eText.text.toString().trim().split("-".toRegex()).toTypedArray()
            if (dateSplit.size == 3) {
                day = dateSplit[2].toInt()
                month = dateSplit[1].toInt() - 1
                year = dateSplit[0].toInt()
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