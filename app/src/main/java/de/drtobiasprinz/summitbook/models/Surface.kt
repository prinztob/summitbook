package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R

enum class Surface(var number: Double, var nameId: Int, var surfaces: List<String>, var highwayTags: List<String>) {

    ASPHALT(
        10.0,
        R.string.road_surface_asphalt,
        listOf("paved", "asphalt", "chipseal", "smooth_paved", "paved"),
        listOf("residential", "service")
    ),
    STONE_PAVEMENT(
        11.0,
        R.string.road_surface_stone_paved,
        listOf(
            "concrete",
            "concrete:lanes",
            "concrete:plates",
            "paving_stones",
            "cobblestone",
            "rough_paved",
            "unhewn_cobblestone",
            "sett"
        ),
        listOf()
    ),
    COMPACTED(
        20.0,
        R.string.road_surface_compact,
        listOf("compacted", "unpaved"),
        listOf()
    ),
    LOSE_GROUND(
        30.0,
        R.string.road_surface_lose_ground,
        listOf("raw", "fine_gravel", "gravel", "dirt", "pebblestone"),
        listOf("track")
    ),
    PATH(
        50.0,
        R.string.road_surface_path,
        listOf(),
        listOf("path")
    ),
    UNKNOWN(
        0.0,
        R.string.road_unknown,
        listOf(),
        listOf()
    );

    companion object {
        fun mapFromRoadInfo(roadInfo: RoadInfo): Surface {
            val type = Surface.entries.firstOrNull { roadInfo.surface in it.surfaces }
            return type ?: Surface.entries.firstOrNull { roadInfo.roadType in it.highwayTags }
            ?: UNKNOWN
        }
    }
}
