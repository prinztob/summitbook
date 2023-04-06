package de.drtobiasprinz.summitbook.db.entities

import android.util.Log
import de.drtobiasprinz.summitbook.utils.Constants
import java.util.*
import kotlin.math.roundToInt


class SortFilterValues(
    var startDate: Date? = null,
    var endDate: Date? = null,
    var selectedDateSpinner: Int = 0,

    var sportType: SportType? = null,

    var kilometersSlider: RangeSliderValues = RangeSliderValues("kilometers"),
    var topElevationSlider: RangeSliderValues = RangeSliderValues("maxElevation"),
    var elevationGainSlider: RangeSliderValues = RangeSliderValues("elevationGain"),

    var filterByIsFavorite: Int = 0,
    var filterByIsSummit: Int = 0,
    var filterByHasGpxTrack: Int = -1,
    var filterByHasImage: Int = -1,
    var filterByHasPosition: Int = -1,

    var orderByDescAsc: String = "DESC",
    var orderBy: String = "date"
) {

    fun getSqlQuery(): String {
        var query = "SELECT *  FROM ${Constants.SUMMITS_TABLE} WHERE isBookmark = 0"
        query += addSportTypeFilter(sportType)
        query += addTimeFilter(startDate, endDate)
        query += addSliderFilter(kilometersSlider)
        query += addSliderFilter(elevationGainSlider)
        query += addSliderFilter(topElevationSlider)
        if (filterByIsFavorite == 1) {
            query += " AND (isFavorite = 1)"
        }
        if (filterByIsSummit == 1) {
            query += " AND (isPeak = 1)"
        }
        if (filterByHasPosition == 1) {
            query += " AND (lat IS NOT NULL) AND (lng IS NOT NULL)"
        }
        if (filterByHasPosition == 0) {
            query += " AND (lat IS NULL) AND (lng IS NULL)"
        }
        query += " ORDER BY $orderBy $orderByDescAsc;"
        Log.i("SortFilterValues.", "getSqlQuery() = ${query}")
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
                    "${if (sliderValues.selectedMin > sliderValues.totalMin) sliderValues.selectedMin.roundToInt() else sliderValues.totalMin.roundToInt() } " +
                    "and " +
                    "${if (sliderValues.selectedMax < sliderValues.totalMax) sliderValues.selectedMax.roundToInt() else sliderValues.totalMax.roundToInt() }" +
                    ")"
        } else {
            ""
        }
    }

    private fun addSportTypeFilter(sportType: SportType?): String {
        return if (sportType != null) {
            " and sportType == \"${sportType.name}\""
        } else {
            ""
        }
    }
}

class RangeSliderValues(
    var dbColumnName: String,
    var totalMin: Float = 0f,
    var selectedMin: Float = 0f,
    var selectedMax: Float = 0f,
    var totalMax: Float = 0f
)