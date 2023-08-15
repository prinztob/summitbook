package de.drtobiasprinz.summitbook.models

import android.content.SharedPreferences
import android.util.Log
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale


class SortFilterValues(
    var startDate: Date? = null,
    var endDate: Date? = null,
    var selectedDateSpinner: Int = 0,
    private var selectedDateSpinnerDefault: Int = 0,

    var sportType: SportType? = null,
    var participants: List<String> = emptyList(),

    var kilometersSlider: RangeSliderValues = RangeSliderValues({ e -> e.kilometers.toFloat() }),
    var topElevationSlider: RangeSliderValues = RangeSliderValues({ e -> e.elevationData.maxElevation.toFloat() }),
    var elevationGainSlider: RangeSliderValues = RangeSliderValues({ e -> e.elevationData.elevationGain.toFloat() }),

    var peakFavoriteButtonGroup: PeakFavoriteButtonGroup = PeakFavoriteButtonGroup.Indifferent,
    var hasPositionButtonGroup: HasPositionButtonGroup = HasPositionButtonGroup.Indifferent,
    var hasImageButtonGroup: HasImageButtonGroup = HasImageButtonGroup.Indifferent,
    var hasGpxTrackButtonGroup: HasGpxTrackButtonGroup = HasGpxTrackButtonGroup.Indifferent,

    var orderByAscDescButtonGroup: OrderByAscDescButtonGroup = OrderByAscDescButtonGroup.Descending,
    var orderByValueSpinner: OrderBySpinnerEntry = OrderBySpinnerEntry.Date,
    var years: List<String> = mutableListOf(),
    var searchString: String = ""
) {
    private var initialized: Boolean = false

    private fun setInitialValues(summits: List<Summit>, sharedPreferences: SharedPreferences) {
        if (!initialized) {
            Log.i("SortFilterValues", "initialized")
            initialized = true
            if (summits.isNotEmpty()) {
                val minDate = getYear(summits.minBy { it.date }.date)
                val maxDate = getYear(summits.maxBy { it.date }.date)
                if (minDate != 1970 && maxDate != 1970) {
                    years = (minDate..maxDate).map { it.toString() }.sortedDescending()
                }
            }
            val showOnlyCurrentYear = sharedPreferences.getBoolean("current_year_switch", false)
            updateCurrentYearSwitch(showOnlyCurrentYear)
        }
    }

    fun updateCurrentYearSwitch(showOnlyCurrentYear: Boolean) {
        if (showOnlyCurrentYear) {
            Log.i("SortFilterValues", "current_year_switch=true")
            setSelectedDateSpinnerAndItsDefault(this, 2, 2)
        } else {
            setSelectedDateSpinnerAndItsDefault(this, 0, 0)
        }
    }

    private fun setDates() {
        val dt = SimpleDateFormat(Summit.DATETIME_FORMAT, Locale.ENGLISH)
        if (wasFullYearSelected() && getSelectedYear() != "") {
            startDate = dt.parse(
                "${getSelectedYear()}-01-01 00:00:00"
            )
            endDate = dt.parse(
                "${getSelectedYear()}-12-31 23:59:59"
            )
        } else {
            startDate = null
            endDate = null
        }
    }

    fun getSelectedYear(): String {
        return if (wasFullYearSelected() && years.isNotEmpty() && years.size > selectedDateSpinner - 2) years[selectedDateSpinner - 2] else ""
    }

    fun wasCurrentYearSelected(): Boolean {
        return wasFullYearSelected() && (Calendar.getInstance())[Calendar.YEAR] == getSelectedYear().toInt()
    }

    fun wasFullYearSelected(): Boolean {
        return selectedDateSpinner >= 2
    }

    fun apply(summits: List<Summit>, sharedPreferences: SharedPreferences): List<Summit> {
        setInitialValues(summits, sharedPreferences)
        val filteredSummits = if (searchString != "" && searchString.length > 1) {
            summits.filter {
                it.name.contains(searchString, ignoreCase = true) ||
                        it.comments.contains(searchString, ignoreCase = true) ||
                        it.places.joinToString(";").contains(searchString, ignoreCase = true)
            }
        } else {
            summits.filter {
                !it.isBookmark &&
                        filterDate(it) &&
                        filterParticipants(it) &&
                        (sportType == null || sportType == it.sportType) &&
                        kilometersSlider.filter(it) &&
                        elevationGainSlider.filter(it) &&
                        topElevationSlider.filter(it) &&
                        peakFavoriteButtonGroup.filter(it) &&
                        hasImageButtonGroup.filter(it) &&
                        hasGpxTrackButtonGroup.filter(it) &&
                        hasPositionButtonGroup.filter(it)
            }
        }
        return sortByAscOrDesc(filteredSummits)
    }

    private fun sortByAscOrDesc(filteredSummits: List<Summit>): List<Summit> {
        return if (orderByAscDescButtonGroup == OrderByAscDescButtonGroup.Ascending) {
            filteredSummits.sortedBy { orderByValueSpinner.f(it) }
        } else {
            filteredSummits.sortedByDescending { orderByValueSpinner.f(it) }
        }
    }

    fun applyForBookmarks(summits: List<Summit>): List<Summit> {
        val filteredSummits = summits.filter {
            it.isBookmark
        }
        return sortByAscOrDesc(filteredSummits)
    }

    fun apply(segments: List<Segment>): List<Segment> {
        return segments
    }

    private fun filterDate(summit: Summit): Boolean {
        val date1 = startDate
        val date2 = endDate
        return if (date1 != null && date2 != null) {
            summit.date.time in date1.time..date2.time
        } else {
            true
        }
    }

    private fun filterParticipants(summit: Summit): Boolean {
        return summit.participants.containsAll(participants)
    }

    fun setToDefault() {
        selectedDateSpinner = selectedDateSpinnerDefault
        setDates()
        sportType = null
        kilometersSlider = RangeSliderValues({ e -> e.kilometers.toFloat() })
        topElevationSlider = RangeSliderValues({ e -> e.elevationData.maxElevation.toFloat() })
        elevationGainSlider = RangeSliderValues({ e -> e.elevationData.elevationGain.toFloat() })

        peakFavoriteButtonGroup = PeakFavoriteButtonGroup.Indifferent
        hasPositionButtonGroup = HasPositionButtonGroup.Indifferent
        hasImageButtonGroup = HasImageButtonGroup.Indifferent
        hasGpxTrackButtonGroup = HasGpxTrackButtonGroup.Indifferent

        orderByAscDescButtonGroup = OrderByAscDescButtonGroup.Descending
        orderByValueSpinner = OrderBySpinnerEntry.Date
    }

    companion object {
        fun getYear(date: Date): Int {
            val calendar: Calendar = GregorianCalendar()
            calendar.time = date
            return calendar[Calendar.YEAR]
        }

        private fun setSelectedDateSpinnerAndItsDefault(
            sortFilterValues: SortFilterValues, selectedDateSpinner: Int, selectedDateSpinnerDefault: Int
        ) {
            sortFilterValues.selectedDateSpinner = selectedDateSpinner
            sortFilterValues.selectedDateSpinnerDefault = selectedDateSpinnerDefault
            sortFilterValues.setDates()
        }
    }
}

