package de.drtobiasprinz.summitbook.models

data class LocationInfo(
    val name: String,
    val country: String? = null,
    val placeType: String,           // e.g., "city", "town", "village", "hamlet"
    val minDistance: Int = Int.MAX_VALUE,
    val additionalTags: Map<String?, String?>? = null,
)