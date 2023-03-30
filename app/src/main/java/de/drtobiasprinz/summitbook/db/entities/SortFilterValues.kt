package de.drtobiasprinz.summitbook.db.entities

import java.util.*

class SortFilterValues(
    var selectedYear: String = "",
    var startDate: Date? = null,
    var endDate: Date? = null,
    var sportType: SportType? = null,
    var valuesRangeSliderKilometers: MutableList<Float> = mutableListOf(0f, 0f, 0f, 0f),
    var valuesRangeSliderHeightMeters: MutableList<Float> = mutableListOf(0f, 0f, 0f, 0f),
    var valuesRangeSliderTopElevation: MutableList<Float> = mutableListOf(0f, 0f, 0f, 0f),
    var sortDescending: Boolean = false,
    var filterByIsFavorite: Boolean = false,
    var filterByIsSummit: Boolean = false,
    var sortBy: (Summit) -> Any = { e -> e.date },
    var filterByHasGpxTrack: (Summit) -> Boolean = { e -> true },
    var filterByHasImage: (Summit) -> Boolean = { e -> true },
    var filterByHasPosition: (Summit) -> Boolean = { e -> true },
    var selectedDateSpinner: Int = 0
)