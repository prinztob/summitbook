package de.drtobiasprinz.summitbook.data.model

import de.drtobiasprinz.summitbook.data.db.entities.Forecast
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.analytics.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.data.db.entities.Surface
import de.drtobiasprinz.summitbook.data.db.entities.RoadType

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