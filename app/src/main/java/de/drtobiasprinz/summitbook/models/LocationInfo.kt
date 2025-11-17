package de.drtobiasprinz.summitbook.models

data class LocationInfo(
    val name: String,
    val placeType: String,           // e.g., "city", "town", "village", "hamlet"
    val minDistance: Int = Int.MAX_VALUE,
    val additionalTags: Map<String?, String?>? = null,
) {
    override fun toString(): String {
        val parts = mutableListOf<String>()
        parts.add("name=$name")
        parts.add("placeType=$placeType")
        parts.add("minDistance=$minDistance")
        additionalTags?.takeIf { it.isNotEmpty() }?.let { parts.add("additionalTags=$it") }
        return "LocationInfo(${parts.joinToString(", ")})"
    }
}