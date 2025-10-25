package de.drtobiasprinz.summitbook.models

data class RoadInfo(
    val roadType: String,           // e.g., "primary", "secondary", "residential", "path", "track"
    val name: String? = null,       // Road name if available
    val surface: String? = null,    // e.g., "asphalt", "gravel", "unpaved"
    val minDistance: Int = 1000,
    val trackType: String? = "",
    val additionalTags: Map<String?, String?>? = null,
)