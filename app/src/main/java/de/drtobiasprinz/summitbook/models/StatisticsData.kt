package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

data class StatisticsData(
    val totalActivities: Int = 0,
    val totalSummits: Int = 0,
    val totalKm: Double = 0.0,
    val totalHm: Int = 0,
    val achievement: Double = 0.0,
    val visitedCountries: Int = 0,
    val totalRoadSurfaceMeter: Map<Surface, Int> = emptyMap(),
    val totalRoadTypeMeter: Map<RoadType, Int> = emptyMap(),
    val extremaValuesSummits: ExtremaValuesSummits? = null,
    val forecasts: List<Forecast>? = null,
    val summits: List<Summit> = emptyList()
)