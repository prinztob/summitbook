package de.drtobiasprinz.summitbook.models

data class RoadInfo(
    val roadType: String,
    val name: String? = null,
    val surface: String? = null,
    val minDistance: Int = 1000,
    val trackType: String? = "",
    val additionalTags: Map<String, String>? = null,
) {
    override fun toString(): String {
        val parts = mutableListOf<String>()
        parts.add("roadType=$roadType")
        name?.let { parts.add("name=$it") }
        surface?.let { parts.add("surface=$it") }
        parts.add("minDistance=$minDistance")
        trackType?.takeIf { it.isNotEmpty() }?.let { parts.add("trackType=$it") }
        additionalTags?.takeIf { it.isNotEmpty() }?.let { parts.add("additionalTags=$it") }
        return "RoadInfo(${parts.joinToString(", ")})"
    }
}