package de.drtobiasprinz.summitbook.data.model

data class PoiInfo(
    val name: String,
    val type: String,
    val elevation: String? = null,
    val minDistance: Int,
)

/**
 * All infos deducible for a single position from the offline map files.
 * Everything here is derived on demand and not persisted.
 */
data class PositionInfo(
    val elevation: Int? = null,
    val elevationDistance: Int? = null,
    val locationInfo: LocationInfo? = null,
    val roadInfo: RoadInfo? = null,
    val nearbyPois: List<PoiInfo> = emptyList(),
    val areas: List<String> = emptyList(),
)
