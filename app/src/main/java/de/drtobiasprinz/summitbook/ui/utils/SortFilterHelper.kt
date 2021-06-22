package de.drtobiasprinz.summitbook.ui.utils

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.database.sqlite.SQLiteDatabase
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AlertDialog
import co.ceryle.segmentedbutton.SegmentedButtonGroup
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.terminator.ChipTerminatorHandler
import de.drtobiasprinz.summitbook.MainActivity
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.SummitBookDatabaseHelper
import de.drtobiasprinz.summitbook.fragments.StatisticsFragment
import de.drtobiasprinz.summitbook.fragments.SummationFragment
import de.drtobiasprinz.summitbook.models.SportType
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.models.SummitEntry
import io.apptik.widget.MultiSlider
import io.apptik.widget.MultiSlider.SimpleChangeListener
import io.apptik.widget.MultiSlider.Thumb
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToLong


class SortFilterHelper(private val filterAndSortView: View, private val context: Context, var entries: ArrayList<SummitEntry>, val databaseHelper: SummitBookDatabaseHelper, val database: SQLiteDatabase) {
    @JvmField
    var selectedYear = ""
    private lateinit var fragment: SummationFragment
    lateinit var filteredEntries: ArrayList<SummitEntry>
        private set
    private lateinit var uniqueYearsOfSummit: ArrayList<String>
    private var startDate: Date? = null
    private var endDate: Date? = null
    var allEntriesRequested: Boolean = false
    private lateinit var overview: TextView
    private lateinit var startDateText: EditText
    private lateinit var endDateText: EditText
    private var selectedDateItemDefault = 0
    private val segmentedSortAscDesc: SegmentedButtonGroup = filterAndSortView.findViewById(R.id.group_sort_asc_desc)
    private val segmentedSortBy: SegmentedButtonGroup = filterAndSortView.findViewById(R.id.group_sort_by)
    private val segmentedWithPosition: SegmentedButtonGroup = filterAndSortView.findViewById(R.id.group_position)
    private val segmentedWithGpx: SegmentedButtonGroup = filterAndSortView.findViewById(R.id.group_gpx)
    private val segmentedWithImage: SegmentedButtonGroup = filterAndSortView.findViewById(R.id.group_image)
    private var selectedSegmentedSortAscDesc = 1
    private var selectedSegmentedSortBy = 0
    private var selectedSportTypeItem = 0
    private var selectedDateItem = 0
    private var participantsSize = 0
    private var selectedSegmentedWithPosition = 1
    private var selectedSegmentedWithGpx = 1
    private var selectedSegmentedWithImage = 1
    private var extremaValuesAllSummits: ExtremaValuesSummits? = null
    private var extremaValuesFilteredSummits: ExtremaValuesSummits? = null
    private lateinit var multiSliderKilometers: MultiSlider
    private lateinit var multiSliderHeightMeters: MultiSlider
    private lateinit var multiSliderTopElevation: MultiSlider
    private lateinit var multiSliderTopSpeed: MultiSlider
    private lateinit var multiSliderAverageSpeed: MultiSlider
    private lateinit var sportTypeSpinner: Spinner
    private lateinit var dateSpinner: Spinner
    private lateinit var participants: NachoTextView
    var areSharedPrefInitialized: Boolean = false

    private fun setExtremeValues() {
        extremaValuesAllSummits = entries.let { ExtremaValuesSummits(it) }
        extremaValuesFilteredSummits = entries.let { ExtremaValuesSummits(it) }
    }

    private fun setMultiSliders() {
        multiSliderKilometers.step = 5
        extremaValuesAllSummits?.minKilometers?.let { multiSliderKilometers.setMin(it.toInt(), true, true) }
        extremaValuesAllSummits?.maxKilometersCeil?.let { multiSliderKilometers.setMax(it, true, true) }
        multiSliderHeightMeters.step = 250
        extremaValuesAllSummits?.minHeightMeters?.let { multiSliderHeightMeters.setMin(it, true, true) }
        extremaValuesAllSummits?.maxHeightMeters?.let { multiSliderHeightMeters.setMax(it, true, true) }
        multiSliderTopElevation.step = 250
        extremaValuesAllSummits?.minTopElevation?.let { multiSliderTopElevation.setMin(it, true, true) }
        extremaValuesAllSummits?.maxTopElevation?.let { multiSliderTopElevation.setMax(it, true, true) }
        multiSliderTopSpeed.step = 1
        extremaValuesAllSummits?.minTopSpeed?.let { multiSliderTopSpeed.setMin(it.toInt(), true, true) }
        extremaValuesAllSummits?.maxTopSpeedCeil?.let { multiSliderTopSpeed.setMax(it, true, true) }
        multiSliderAverageSpeed.step = 1
        extremaValuesAllSummits?.minAverageSpeed?.let { multiSliderAverageSpeed.setMin(it.toInt(), true, true) }
        extremaValuesAllSummits?.maxAverageSpeedCeil?.let { multiSliderAverageSpeed.setMax(it, true, true) }
    }

    private fun updateDateSpinner() {
        val customDateLayout = filterAndSortView.findViewById<LinearLayout>(R.id.date_start_end_selector)
        startDateText = filterAndSortView.findViewById(R.id.date_start)
        startDateText.inputType = InputType.TYPE_NULL
        startDateText.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {
                showDatePicker(startDateText, context)
            }
        }
        startDateText.setOnClickListener {
            showDatePicker(startDateText, context)
        }
        endDateText = filterAndSortView.findViewById(R.id.date_end)
        endDateText.inputType = InputType.TYPE_NULL
        endDateText.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
            if (hasFocus) {
                showDatePicker(endDateText, context)
            }
        }
        endDateText.setOnClickListener {
            showDatePicker(startDateText, context)
        }
        val dates = ArrayList(listOf(*context.resources.getStringArray(R.array.date_array)))
        uniqueYearsOfSummit = StatisticsFragment.getAllYears(entries)
        dates.addAll(uniqueYearsOfSummit)
        val dateAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, dates)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        addParticipantsFilter()

        dateSpinner = filterAndSortView.findViewById(R.id.spinner_date)
        dateSpinner.adapter = dateAdapter
        dateSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                selectedDateItem = position
                if (position == 1) {
                    customDateLayout.visibility = View.VISIBLE
                } else {
                    customDateLayout.visibility = View.GONE
                }
                sortAndFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //Another interface callback
            }
        }
    }

    private fun addParticipantsFilter() {
        participants = filterAndSortView.findViewById(R.id.participants)
        val suggestions: MutableList<String> = mutableListOf()
        entries.forEach {
            suggestions.addAll(it.participants)
        }
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, suggestions.distinct())
        participants.setAdapter(adapter)
        participants.addChipTerminator(',', ChipTerminatorHandler.BEHAVIOR_CHIPIFY_TO_TERMINATOR)
        participants.enableEditChipOnTouch(false, true)
        participantsSize = participants.chipValues.size
    }

    private fun showDatePicker(eText: EditText, context: Context) {
        val cldr = Calendar.getInstance()
        var day = cldr[Calendar.DAY_OF_MONTH]
        var month = cldr[Calendar.MONTH]
        var year = cldr[Calendar.YEAR]
        if (eText.text.toString().trim() != "") {
            val dateSplitted = eText.text.toString().trim().split("-".toRegex()).toTypedArray()
            if (dateSplitted.size == 3) {
                day =  dateSplitted[2].toInt()
                month = dateSplitted[1].toInt() - 1
                year = dateSplitted[0].toInt()
            }
        }
        val picker = DatePickerDialog(context, R.style.CustomDatePickerDialogTheme,
                { view: DatePicker, yearSelected: Int, monthSelected: Int, daySelected: Int -> eText.setText(view.context.getString(R.string.date_format, String.format(Locale.ENGLISH, "%02d", daySelected), String.format(Locale.ENGLISH, "%02d", monthSelected + 1), String.format(Locale.ENGLISH, "%02d", yearSelected))) }, year, month, day)
        picker.show()
    }

    fun setDataSpinnerToDefault() {
        selectedDateItem = selectedDateItemDefault
        dateSpinner.setSelection(selectedDateItem)
    }

    fun showDialog() {
        val alert = AlertDialog.Builder(context)
        if (filterAndSortView.parent != null) {
            (filterAndSortView.parent as ViewGroup).removeView(filterAndSortView) // <- fix
        }
        setMinMaxValuesKm()
        setMinMaxValuesHm()
        setMinMaxValuesTopSpeed()
        setMinMaxValuesAverageSpeed()
        setMinMaxValuesTopElevation()
        alert.setTitle("Filter and sorting")
        alert.setView(filterAndSortView)
        alert.setPositiveButton("Apply") { _: DialogInterface?, _: Int ->
            sortAndFilter()
        }
        alert.setNegativeButton("Set to default") { _: DialogInterface?, _: Int ->
            setAllToDefault()
        }
        alert.show()
    }

    fun setAllToDefault() {
        selectedSegmentedSortAscDesc = 1
        segmentedSortAscDesc.position = selectedSegmentedSortAscDesc
        selectedSegmentedSortBy = 0
        segmentedSortBy.position = selectedSegmentedSortBy
        selectedSegmentedWithGpx = 1
        segmentedWithGpx.position = selectedSegmentedWithGpx
        selectedSegmentedWithPosition = 1
        segmentedWithPosition.position = selectedSegmentedWithPosition
        selectedSegmentedWithImage = 1
        segmentedWithImage.position = selectedSegmentedWithImage
        selectedSportTypeItem = 0
        sportTypeSpinner.setSelection(selectedSportTypeItem)
        setDataSpinnerToDefault()
        extremaValuesFilteredSummits = extremaValuesAllSummits
        multiSliderKilometers.getThumb(0).value = extremaValuesAllSummits?.minKilometers?.toInt()
                ?: multiSliderKilometers.getThumb(0).value
        multiSliderKilometers.getThumb(1).value = extremaValuesAllSummits?.maxKilometersCeil
                ?: multiSliderKilometers.getThumb(1).value
        multiSliderKilometers.repositionThumbs()
        multiSliderHeightMeters.getThumb(0).value = extremaValuesAllSummits?.minHeightMeters
                ?: multiSliderHeightMeters.getThumb(0).value
        multiSliderHeightMeters.getThumb(1).value = extremaValuesAllSummits?.maxHeightMeters
                ?: multiSliderHeightMeters.getThumb(1).value
        multiSliderHeightMeters.repositionThumbs()
        multiSliderTopElevation.getThumb(0).value = extremaValuesAllSummits?.minTopElevation
                ?: multiSliderTopElevation.getThumb(0).value
        multiSliderTopElevation.getThumb(1).value = extremaValuesAllSummits?.maxTopElevation
                ?: multiSliderTopElevation.getThumb(1).value
        multiSliderTopElevation.repositionThumbs()
        multiSliderTopSpeed.getThumb(0).value = extremaValuesAllSummits?.minTopSpeed?.toInt()
                ?: multiSliderTopSpeed.getThumb(0).value
        multiSliderTopSpeed.getThumb(1).value = extremaValuesAllSummits?.maxTopSpeedCeil
                ?: multiSliderTopSpeed.getThumb(1).value
        multiSliderTopSpeed.repositionThumbs()
        multiSliderAverageSpeed.getThumb(0).value = extremaValuesAllSummits?.minAverageSpeed?.toInt()
                ?: multiSliderAverageSpeed.getThumb(0).value
        multiSliderAverageSpeed.getThumb(1).value = extremaValuesAllSummits?.maxAverageSpeedCeil
                ?: multiSliderAverageSpeed.getThumb(1).value
        multiSliderAverageSpeed.repositionThumbs()
        startDateText.setText("")
        endDateText.setText("")
        participants.setText("")
        sortAndFilter()
    }

    private fun setMinMaxValuesKm() {
        val min1 = filterAndSortView.findViewById<TextView>(R.id.minValueKm)
        min1.text = String.format("%s km", (extremaValuesFilteredSummits?.minKilometers ?: 0.0).toInt())
        val max1 = filterAndSortView.findViewById<TextView>(R.id.maxValueKm)
        max1.text = String.format("%s km", extremaValuesFilteredSummits?.maxKilometersCeil)
        multiSliderKilometers.setOnThumbValueChangeListener(object : SimpleChangeListener() {
            override fun onValueChanged(multiSlider: MultiSlider, thumb: Thumb, thumbIndex: Int, value: Int) {
                if (thumbIndex == 0) {
                    min1.text = String.format("%s km", value.toString())
                    extremaValuesFilteredSummits?.minKilometers = value.toDouble()
                } else {
                    max1.text = String.format("%s km", value.toString())
                    extremaValuesFilteredSummits?.maxKilometers = value.toDouble()
                }
            }
        })
    }

    private fun setMinMaxValuesHm() {
        val min1 = filterAndSortView.findViewById<TextView>(R.id.minValueHm)
        min1.text = String.format("%s hm", extremaValuesFilteredSummits?.minHeightMeters)
        val max1 = filterAndSortView.findViewById<TextView>(R.id.maxValueHm)
        max1.text = String.format("%s hm", extremaValuesFilteredSummits?.maxHeightMeters)
        multiSliderHeightMeters.setOnThumbValueChangeListener(object : SimpleChangeListener() {
            override fun onValueChanged(multiSlider: MultiSlider, thumb: Thumb, thumbIndex: Int, value: Int) {
                if (thumbIndex == 0) {
                    min1.text = String.format("%s hm", value.toString())
                    extremaValuesFilteredSummits?.minHeightMeters = value
                } else {
                    max1.text = String.format("%s hm", value.toString())
                    extremaValuesFilteredSummits?.maxHeightMeters = value
                }
            }
        })
    }

    private fun setMinMaxValuesTopSpeed() {
        val min1 = filterAndSortView.findViewById<TextView>(R.id.minValueTopSpeed)
        min1.text = String.format("%s km/s", (extremaValuesFilteredSummits?.minTopSpeed ?: 0.0).toInt())
        val max1 = filterAndSortView.findViewById<TextView>(R.id.maxValueTopSpeed)
        max1.text = String.format("%s km/s", extremaValuesFilteredSummits?.maxTopSpeedCeil)
        multiSliderTopSpeed.setOnThumbValueChangeListener(object : SimpleChangeListener() {
            override fun onValueChanged(multiSlider: MultiSlider, thumb: Thumb, thumbIndex: Int, value: Int) {
                if (thumbIndex == 0) {
                    min1.text = String.format("%s km/s", value.toString())
                    extremaValuesFilteredSummits?.minTopSpeed = value.toDouble()
                } else {
                    max1.text = String.format("%s km/s", value.toString())
                    extremaValuesFilteredSummits?.maxTopSpeed = value.toDouble()
                }
            }
        })
    }

    private fun setMinMaxValuesAverageSpeed() {
        val min1 = filterAndSortView.findViewById<TextView>(R.id.minValueAverageSpeed)
        min1.text = String.format("%s km/s", (extremaValuesFilteredSummits?.minAverageSpeed ?: 0.0).toInt())
        val max1 = filterAndSortView.findViewById<TextView>(R.id.maxValueAverageSpeed)
        max1.text = String.format("%s km/s", extremaValuesFilteredSummits?.maxAverageSpeedCeil)
        multiSliderAverageSpeed.setOnThumbValueChangeListener(object : SimpleChangeListener() {
            override fun onValueChanged(multiSlider: MultiSlider, thumb: Thumb, thumbIndex: Int, value: Int) {
                if (thumbIndex == 0) {
                    min1.text = String.format("%s km/s", value.toString())
                    extremaValuesFilteredSummits?.minAverageSpeed = value.toDouble()
                } else {
                    max1.text = String.format("%s km/s", value.toString())
                    extremaValuesFilteredSummits?.maxAverageSpeed = value.toDouble()
                }
            }
        })
    }

    private fun setMinMaxValuesTopElevation() {
        val min1 = filterAndSortView.findViewById<TextView>(R.id.minValueTopElevation)
        min1.text = String.format("%s hm", extremaValuesFilteredSummits?.minTopElevation)
        val max1 = filterAndSortView.findViewById<TextView>(R.id.maxValueTopElevation)
        max1.text = String.format("%s hm", extremaValuesFilteredSummits?.maxTopElevation)
        multiSliderTopElevation.setOnThumbValueChangeListener(object : SimpleChangeListener() {
            override fun onValueChanged(multiSlider: MultiSlider, thumb: Thumb, thumbIndex: Int, value: Int) {
                if (thumbIndex == 0) {
                    min1.text = String.format("%s hm", value.toString())
                    extremaValuesFilteredSummits?.minTopElevation = value
                } else {
                    max1.text = String.format("%s hm", value.toString())
                    extremaValuesFilteredSummits?.maxTopElevation = value
                }
            }
        })
    }

    fun apply() {
        segmentedSortAscDesc.position = selectedSegmentedSortAscDesc
        segmentedSortBy.position = selectedSegmentedSortBy
        segmentedWithGpx.position = selectedSegmentedWithGpx
        segmentedWithPosition.position = selectedSegmentedWithPosition
        segmentedWithImage.position = selectedSegmentedWithImage
        segmentedSortAscDesc.setOnClickedButtonListener { position: Int ->
            selectedSegmentedSortAscDesc = position
        }
        segmentedSortBy.setOnClickedButtonListener { position: Int ->
            selectedSegmentedSortBy = position
        }
        segmentedWithGpx.setOnClickedButtonListener { position: Int ->
            selectedSegmentedWithGpx = position
        }
        segmentedWithPosition.setOnClickedButtonListener { position: Int ->
            selectedSegmentedWithPosition = position
        }
        segmentedWithImage.setOnClickedButtonListener { position: Int ->
            selectedSegmentedWithImage = position
        }
        sortAndFilter()
    }

    private fun sortAndFilter() {
        val entriesLocal = entries
        filteredEntries = entriesLocal
        sort()
        filter()
        fragment.update(filteredEntries)
        setOverviewText()
    }

    private fun setOverviewText() {
        val statisticEntry = StatisticEntry(filteredEntries)
        statisticEntry.calculate()
        overview.text = context.getString(R.string.base_info, filteredEntries.size.toString(), statisticEntry.totalKm.roundToLong().toInt().toString(), statisticEntry.totalHm.toFloat().roundToLong().toString())
    }

    private fun sort() {
        when (selectedSegmentedSortBy) {
            1 -> filteredEntries.sortWith(compareBy { it.name })
            2 -> filteredEntries.sortWith(compareBy { it.heightMeter })
            3 -> filteredEntries.sortWith(compareBy { it.kilometers })
            4 -> filteredEntries.sortWith(compareBy { it.topElevation })
            else -> filteredEntries.sortWith(compareBy { it.date })
        }
        when (selectedSegmentedSortAscDesc) {
            1 -> filteredEntries.reverse()
        }
    }

    private fun filter() {
        filterByPosition()
        filterByGpx()
        filterByImage()
        filterByKm()
        filterByHm()
        filterByTopSpeed()
        filterByAverageSpeed()
        filterByTopElevation()
        filterByDate()
        filterBySportType()
        filterByParticipants()
    }

    private fun filterByPosition() {
        val entries = ArrayList<SummitEntry>()
        for (entry in filteredEntries) {
            when (selectedSegmentedWithPosition) {
                0 -> if (entry.latLng != null) {
                    entries.add(entry)
                }
                2 -> if (entry.latLng == null) {
                    entries.add(entry)
                }
                else -> entries.add(entry)
            }
        }
        filteredEntries = entries
    }

    private fun filterByGpx() {
        val entries = ArrayList<SummitEntry>()
        for (entry in filteredEntries) {
            when (selectedSegmentedWithGpx) {
                0 -> if (entry.hasGpsTrack()) {
                    entries.add(entry)
                }
                2 -> if (!entry.hasGpsTrack()) {
                    entries.add(entry)
                }
                else -> entries.add(entry)
            }
        }
        filteredEntries = entries
    }

    private fun filterByImage() {
        val entries = ArrayList<SummitEntry>()
        for (entry in filteredEntries) {
            when (selectedSegmentedWithImage) {
                0 -> if (entry.hasImagePath()) {
                    entries.add(entry)
                }
                2 -> if (!entry.hasImagePath()) {
                    entries.add(entry)
                }
                else -> entries.add(entry)
            }
        }
        filteredEntries = entries
    }

    private fun filterByDate() {
        selectedDateSpinner(selectedDateItem)
        val entries = ArrayList<SummitEntry>()
        for (entry in filteredEntries) {
            if (entry.date.after(startDate) && entry.date.before(endDate)) {
                entries.add(entry)
            }
        }
        filteredEntries = entries
    }

    private fun filterBySportType() {
        val selectedSportType = selectedSportTypeSpinner(selectedSportTypeItem)
        val entries = ArrayList<SummitEntry>()
        if (selectedSportType != null) {
            for (entry in filteredEntries) {
                if (entry.sportType == selectedSportType) {
                    entries.add(entry)
                }
            }
            filteredEntries = entries
        }
    }

    private fun filterByParticipants() {
        val selectedParticipants = participants.chipValues
        val entries = ArrayList<SummitEntry>()
        if (selectedParticipants.size != 0) {
            for (entry in filteredEntries) {
                for (participant in selectedParticipants) {
                    if (participant in entry.participants) {
                        entries.add(entry)
                    }
                }
            }
            filteredEntries = entries
        }
    }

    private fun selectedSportTypeSpinner(selectedItemId: Int): SportType? {
        return when (selectedItemId) {
            1 -> SportType.Bicycle
            2 -> SportType.Racer
            3 -> SportType.Mountainbike
            4 -> SportType.BikeAndHike
            5 -> SportType.Climb
            6 -> SportType.Hike
            7 -> SportType.Running
            8 -> SportType.Skitour
            9 -> SportType.Other
            else -> null
        }
    }

    private fun filterByKm() {
        val entries = ArrayList<SummitEntry>()
        val extremaValues = extremaValuesFilteredSummits
        if (extremaValues != null) {
            for (entry in filteredEntries) {
                if (entry.kilometers >= extremaValues.minKilometers && entry.kilometers <= extremaValues.maxKilometersCeil || extremaValues.minKilometers == 0.0 && entry.kilometers == -1.0) {
                    entries.add(entry)
                }
            }
            filteredEntries = entries
        }
    }

    private fun filterByHm() {
        val entries = ArrayList<SummitEntry>()
        val extremaValues = extremaValuesFilteredSummits
        if (extremaValues != null) {
            for (entry in filteredEntries) {
                if (entry.heightMeter >= extremaValues.minHeightMeters && entry.heightMeter <= extremaValues.maxHeightMeters || extremaValues.minHeightMeters == 0 && entry.heightMeter == -1) {
                    entries.add(entry)
                }
            }
            filteredEntries = entries
        }
    }

    private fun filterByTopSpeed() {
        val entries = ArrayList<SummitEntry>()
        val extremaValues = extremaValuesFilteredSummits
        if (extremaValues != null) {
            for (entry in filteredEntries) {
                if (entry.topSpeed >= extremaValues.minTopSpeed && entry.topSpeed <= extremaValues.maxTopSpeedCeil || extremaValues.minTopSpeed == 0.0 && entry.topSpeed == -1.0) {
                    entries.add(entry)
                }
            }
            filteredEntries = entries
        }
    }

    private fun filterByAverageSpeed() {
        val entries = ArrayList<SummitEntry>()
        val extremaValues = extremaValuesFilteredSummits
        if (extremaValues != null) {
            for (entry in filteredEntries) {
                if (entry.pace >= extremaValues.minAverageSpeed && entry.pace <= extremaValues.maxAverageSpeedCeil || extremaValues.minAverageSpeed == 0.0 && entry.pace == -1.0) {
                    entries.add(entry)
                }
            }
            filteredEntries = entries
        }
    }

    private fun filterByTopElevation() {
        val entries = ArrayList<SummitEntry>()
        val extremaValues = extremaValuesFilteredSummits
        if (extremaValues != null) {
            for (entry in filteredEntries) {
                if (entry.topElevation >= extremaValues.minTopElevation && entry.topElevation <= extremaValues.maxTopElevation || extremaValues.minTopElevation == 0 && entry.topElevation == -1) {
                    entries.add(entry)
                }
            }
            filteredEntries = entries
        }
    }

    fun setFragment(fragment: SummationFragment) {
        this.fragment = fragment
    }

fun setAllEntries(entries: ArrayList<SummitEntry>) {
    this.entries = entries
    this.filteredEntries = entries
}

fun update(entries: ArrayList<SummitEntry>) {
    MainActivity.extremaValuesAllSummits = ExtremaValuesSummits(entries)
    setAllEntries(entries)
    setExtremeValues()
    sortAndFilter()
}

    private fun selectedDateSpinner(position: Int) {
        try {
            when (position) {
                0 -> {
                    startDate = SummitEntry.parseDate(String.format("%s-01-01", if (uniqueYearsOfSummit.size == 0) "1900" else Collections.min(uniqueYearsOfSummit)))
                    endDate = SummitEntry.parseDate(String.format("%s-12-31", if (uniqueYearsOfSummit.size == 0) "2200" else Collections.max(uniqueYearsOfSummit)))
                    selectedYear = ""
                }
                1 -> {
                    startDate = if (startDateText.text.toString().trim { it <= ' ' } != "") {
                        SummitEntry.parseDate(startDateText.text.toString())
                    } else {
                        SummitEntry.parseDate(String.format("%s-01-01", Collections.min(uniqueYearsOfSummit)))
                    }
                    endDate = if (endDateText.text.toString().trim { it <= ' ' } != "") {
                        SummitEntry.parseDate(endDateText.text.toString())
                    } else {
                        SummitEntry.parseDate(String.format("%s-12-31", uniqueYearsOfSummit))
                    }
                    selectedYear = ""
                }
                else -> setDateFromPosition(position)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    @Throws(ParseException::class)
    private fun setDateFromPosition(position: Int) {
        selectedYear = uniqueYearsOfSummit[position - 2]
        val df: DateFormat = SimpleDateFormat(SummitEntry.DATETIME_FORMAT, Locale.ENGLISH)
        df.isLenient = false
        startDate = df.parse(String.format("%s-01-01 00:00:00", selectedYear))
        endDate = df.parse(String.format("%s-12-31 23:59:59", selectedYear))
    }

    fun setSelectedDateItemDefault(selectedDateItemDefault: Int) {
        this.selectedDateItemDefault = selectedDateItemDefault
    }

    init {
        prepare()
    }

    fun prepare() {
        filteredEntries = entries
        setExtremeValues()
        multiSliderKilometers = filterAndSortView.findViewById(R.id.range_slider_kilometers)
        multiSliderHeightMeters = filterAndSortView.findViewById(R.id.range_slider_height_meter)
        multiSliderTopElevation = filterAndSortView.findViewById(R.id.range_slider_topElevation)
        multiSliderTopSpeed = filterAndSortView.findViewById(R.id.range_slider_topSpeed)
        multiSliderAverageSpeed = filterAndSortView.findViewById(R.id.range_slider_averageSpeed)
        setMultiSliders()
        val sportTypes: ArrayList<String> = arrayListOf("ALL")
        for (value in SportType.values()) {
            sportTypes.add(context.getString(value.sportNameStringId))
        }
        sportTypeSpinner = filterAndSortView.findViewById(R.id.spinner_sports_type)
        val sportTypeAdapter: ArrayAdapter<*> = ArrayAdapter(context,
                android.R.layout.simple_spinner_item, sportTypes)
        sportTypeSpinner.adapter = sportTypeAdapter
        sportTypeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                selectedSportTypeItem = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //Another interface callback
            }
        }
        updateDateSpinner()
        overview = MainActivity.mainActivity!!.findViewById(R.id.overview)
        setOverviewText()
    }
}