package de.drtobiasprinz.summitbook.db.entities

import android.util.Log
import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.utils.Constants
import java.util.*
import kotlin.math.roundToInt


class SortFilterValues (
    var startDate: Date? = null,
    var endDate: Date? = null,
    var selectedDateSpinner: Int = 0,
    var selectedDateSpinnerDefault: Int = 0,

    var sportType: SportType? = null,

    var kilometersSlider: RangeSliderValues = RangeSliderValues("kilometers"),
    var topElevationSlider: RangeSliderValues = RangeSliderValues("maxElevation"),
    var elevationGainSlider: RangeSliderValues = RangeSliderValues("elevationGain"),

    var peakFavoriteButtonGroup: PeakFavoriteButtonGroup = PeakFavoriteButtonGroup.Indifferent,
    var hasPositionButtonGroup: HasPositionButtonGroup = HasPositionButtonGroup.Indifferent,
    var hasImageButtonGroup: HasImageButtonGroup = HasImageButtonGroup.Indifferent,
    var hasGpxTrackButtonGroup: HasGpxTrackButtonGroup = HasGpxTrackButtonGroup.Indifferent,

    var orderByAscDescButtonGroup: OrderByAscDescButtonGroup = OrderByAscDescButtonGroup.Descending,
    var orderByValueButtonGroup: OrderByValueButtonGroup = OrderByValueButtonGroup.Date,
    var years: List<String> = mutableListOf()
) {

    fun getSelectedYear(): String {
        return years[selectedDateSpinner - 2]
    }

    fun wasCurrentYearSelected(): Boolean {
        return wasFullYearSelected() && (Calendar.getInstance())[Calendar.YEAR] == getSelectedYear().toInt()
    }

    fun wasFullYearSelected(): Boolean {
        return selectedDateSpinner >= 2
    }

    fun getSqlQuery(): String {
        var query = "SELECT *  FROM ${Constants.SUMMITS_TABLE} WHERE isBookmark = 0"
        query += addSportTypeFilter(sportType)
        query += addTimeFilter(startDate, endDate)
        query += addSliderFilter(kilometersSlider)
        query += addSliderFilter(elevationGainSlider)
        query += addSliderFilter(topElevationSlider)
        query += hasImageButtonGroup.query
        query += hasGpxTrackButtonGroup.query
        query += hasPositionButtonGroup.query
        query += peakFavoriteButtonGroup.query
        query += " ORDER BY ${orderByValueButtonGroup.query} ${orderByAscDescButtonGroup.query};"
        Log.i("SortFilterValues.", "getSqlQuery() = $query")
        return query
    }

    private fun addTimeFilter(startDate: Date?, endDate: Date?): String {
        return if (startDate != null && endDate != null) {
            " AND (date BETWEEN ${startDate.time} and ${endDate.time})"
        } else {
            ""
        }
    }

    private fun addSliderFilter(sliderValues: RangeSliderValues): String {
        return if (sliderValues.selectedMin in (sliderValues.totalMin + 0.001f)..(sliderValues.totalMax - 0.001f) ||
            sliderValues.selectedMax in (sliderValues.totalMin + 0.001f)..(sliderValues.totalMax - 0.001f)
        ) {
            " AND (${sliderValues.dbColumnName} BETWEEN " +
                    "${if (sliderValues.selectedMin > sliderValues.totalMin) sliderValues.selectedMin.roundToInt() else sliderValues.totalMin.roundToInt()} " +
                    "and " +
                    "${if (sliderValues.selectedMax < sliderValues.totalMax) sliderValues.selectedMax.roundToInt() else sliderValues.totalMax.roundToInt()}" +
                    ")"
        } else {
            ""
        }
    }

    private fun addSportTypeFilter(sportType: SportType?): String {
        return if (sportType != null) {
            " and sportType == '${sportType.name}'"
        } else {
            ""
        }
    }

    fun setToDefault() {
        startDate = null
        endDate = null
        selectedDateSpinner = selectedDateSpinnerDefault
        sportType = null
        kilometersSlider = RangeSliderValues("kilometers")
        topElevationSlider = RangeSliderValues("maxElevation")
        elevationGainSlider = RangeSliderValues("elevationGain")

        peakFavoriteButtonGroup = PeakFavoriteButtonGroup.Indifferent
        hasPositionButtonGroup = HasPositionButtonGroup.Indifferent
        hasImageButtonGroup = HasImageButtonGroup.Indifferent
        hasGpxTrackButtonGroup = HasGpxTrackButtonGroup.Indifferent

        orderByAscDescButtonGroup = OrderByAscDescButtonGroup.Descending
        orderByValueButtonGroup = OrderByValueButtonGroup.Date
    }
}

class RangeSliderValues(
    var dbColumnName: String,
    var totalMin: Float = 0f,
    var selectedMin: Float = 0f,
    var selectedMax: Float = 0f,
    var totalMax: Float = 0f
)

enum class OrderByAscDescButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    Ascending("ASC", { e -> e.buttonAscending.id }),
    Descending("DESC", { e -> e.buttonDescending.id }),
}

enum class OrderByValueButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    Date("date", { e -> e.buttonByDate.id }),
    Name("name", { e -> e.buttonByName.id }),
    HeightMeter("elevationGain", { e -> e.buttonByHeightMeter.id }),
    Kilometer("kilometer", { e -> e.buttonByKilometers.id }),
    TopElevation("maxElevation", { e -> e.buttonByElevation.id })
}

enum class HasGpxTrackButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    Yes(" AND (hasTrack = 1)", { e -> e.buttonGpxYes.id }),
    No(" AND (hasTrack = 0)", { e -> e.buttonGpxNo.id }),
    Indifferent("", { e -> e.buttonGpxAll.id })
}

enum class HasPositionButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    Yes(" AND (lat IS NOT NULL) AND (lng IS NOT NULL)", { e -> e.buttonPositionYes.id }),
    No(" AND (lat IS NULL) AND (lng IS NULL)", { e -> e.buttonPositionNo.id }),
    Indifferent("", { e -> e.buttonPositionAll.id })
}

enum class HasImageButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    Yes(" AND (imageIds != '')", { e -> e.buttonImageYes.id }),
    No(" AND (imageIds = '')", { e -> e.buttonImageNo.id }),
    Indifferent("", { e -> e.buttonImageAll.id })
}

enum class PeakFavoriteButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    IsFavorite(" AND (isFavorite = 1)", { e -> e.buttonMarkedFavorite.id }),
    IsPeak(" AND (isPeak = 1)", { e -> e.buttonMarkedSummit.id }),
    Indifferent("", { e -> e.buttonMarkedAll.id })
}
