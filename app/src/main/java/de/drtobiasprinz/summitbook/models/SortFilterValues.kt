package de.drtobiasprinz.summitbook.models

import android.content.SharedPreferences
import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import java.text.SimpleDateFormat
import java.util.*


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
    var orderByValueButtonGroup: OrderByValueButtonGroup = OrderByValueButtonGroup.Date,
    var years: List<String> = mutableListOf()
) {
    fun setInitialValues(summits: List<Summit>, sharedPreferences: SharedPreferences) {
        if (summits.isNotEmpty()) {
            val minDate = getYear(summits.minBy { it.date }.date)
            val maxDate = getYear(summits.maxBy { it.date }.date)
            if (minDate != 1970 && maxDate != 1970) {
                years = (minDate..maxDate).map { it.toString() }.sortedDescending()
            }
        }
        if (sharedPreferences.getBoolean("current_year_switch", false)) {
            setSelectedDateSpinnerAndItsDefault(2, 2)
        } else {
            setSelectedDateSpinnerAndItsDefault(0, 0)
        }
    }

    private fun setSelectedDateSpinnerAndItsDefault(
        selectedDateSpinner: Int, selectedDateSpinnerDefault: Int
    ) {
        this.selectedDateSpinner = selectedDateSpinner
        this.selectedDateSpinnerDefault = selectedDateSpinnerDefault
        setDates()
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

    fun apply(summits: List<Summit>): List<Summit> {
        val filteredSummits = summits.filter {
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
        return orderByValueButtonGroup.sort(filteredSummits, orderByAscDescButtonGroup)
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
        orderByValueButtonGroup = OrderByValueButtonGroup.Date
    }

    companion object {
        fun getYear(date: Date): Int {
            val calendar: Calendar = GregorianCalendar()
            calendar.time = date
            return calendar[Calendar.YEAR]
        }
    }
}

class RangeSliderValues(
    var getValue: (Summit) -> Float,
    var totalMin: Float = 0f,
    var selectedMin: Float = 0f,
    var selectedMax: Float = 0f,
    var totalMax: Float = 0f
) {
    fun filter(summit: Summit): Boolean {
        return if (selectedMin in (totalMin + 0.001f)..(totalMax - 0.001f) || selectedMax in (totalMin + 0.001f)..(totalMax - 0.001f)) {
            getValue(summit) in selectedMin..selectedMax
        } else {
            true
        }
    }
}

enum class OrderByAscDescButtonGroup(
    val query: String, val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    Ascending("ASC", { e -> e.buttonAscending.id }),
    Descending("DESC", { e -> e.buttonDescending.id }),
}

enum class OrderByValueButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val sort: (List<Summit>, OrderByAscDescButtonGroup) -> List<Summit>
) {
    Date("date", { e -> e.buttonByDate.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.date.time }
        } else {
            e.sortedBy { it.date.time }
        }
    }),
    Name("name", { e -> e.buttonByName.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.name }
        } else {
            e.sortedBy { it.name }
        }
    }),
    HeightMeter("elevationGain", { e -> e.buttonByHeightMeter.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.elevationData.elevationGain }
        } else {
            e.sortedBy { it.elevationData.elevationGain }
        }
    }),
    Kilometer("kilometer", { e -> e.buttonByKilometers.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.kilometers }
        } else {
            e.sortedBy { it.kilometers }
        }
    }),
    TopElevation("maxElevation", { e -> e.buttonByElevation.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.elevationData.maxElevation }
        } else {
            e.sortedBy { it.elevationData.maxElevation }
        }
    })
}

enum class HasGpxTrackButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    Yes(" AND (hasTrack = 1)", { e -> e.buttonGpxYes.id }, { e -> e.hasTrack }),
    No(" AND (hasTrack = 0)", { e -> e.buttonGpxNo.id }, { e -> !e.hasTrack }),
    Indifferent("", { e -> e.buttonGpxAll.id }, { true })
}

enum class HasPositionButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    Yes(
        " AND (lat IS NOT NULL) AND (lng IS NOT NULL)",
        { e -> e.buttonPositionYes.id },
        { e -> e.lat != null && e.lng != null }),
    No(
        " AND (lat IS NULL) AND (lng IS NULL)",
        { e -> e.buttonPositionNo.id },
        { e -> e.lat == null && e.lng == null }),
    Indifferent("", { e -> e.buttonPositionAll.id }, { true })
}

enum class HasImageButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    Yes(" AND (imageIds != '')", { e -> e.buttonImageYes.id }, { e -> e.imageIds.isNotEmpty() }),
    No(" AND (imageIds = '')", { e -> e.buttonImageNo.id }, { e -> e.imageIds.isEmpty() }),
    Indifferent("", { e -> e.buttonImageAll.id }, { true })
}

enum class PeakFavoriteButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    IsFavorite(" AND (isFavorite = 1)", { e -> e.buttonMarkedFavorite.id }, { e -> e.isFavorite }),
    IsPeak(" AND (isPeak = 1)", { e -> e.buttonMarkedSummit.id }, { e -> e.isPeak }),
    Indifferent("", { e -> e.buttonMarkedAll.id }, { true })
}
