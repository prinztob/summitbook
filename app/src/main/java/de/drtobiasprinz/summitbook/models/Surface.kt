package de.drtobiasprinz.summitbook.models

import android.graphics.Color
import de.drtobiasprinz.summitbook.R

enum class Surface(var number: Double, var nameId: Int, var color: Int, var surfaces: List<String>, var highwayTags: List<String>) {

    ASPHALT(
        10.0,
        R.string.road_surface_asphalt,
        Color.DKGRAY,
        listOf("paved", "asphalt", "chipseal", "smooth_paved", "paved"),
        listOf("residential", "service")
    ),
    STONE_PAVEMENT(
        11.0,
        R.string.road_surface_stone_paved,
        Color.BLACK,
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
        Color.BLUE,
        listOf("compacted", "unpaved"),
        listOf()
    ),
    LOSE_GROUND(
        30.0,
        R.string.road_surface_lose_ground,
        Color.GREEN,
        listOf("raw", "fine_gravel", "gravel", "dirt", "pebblestone"),
        listOf("track")
    ),
    PATH(
        50.0,
        R.string.road_surface_path, Color.rgb(165, 42, 42),
        listOf(),
        listOf("path")
    ),
    UNKNOWN(
        0.0,
        R.string.road_unknown,
        Color.WHITE,
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
