package de.drtobiasprinz.summitbook.models

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import de.drtobiasprinz.summitbook.R

enum class RoadType(
    val number: Double,
    @StringRes val nameId: Int,
    @ColorInt val color: Int,
    val highwayTags: List<String>
) {
    WAY(
        10.0,
        R.string.road_type_way,
        Color.rgb(165, 42, 42),
        listOf("path", "footway", "track", "bridleway", "pedestrian", "steps", "hiking", "trail"),
    ),
    SIDE_STREET(
        20.0,
        R.string.road_type_side_street,
        Color.YELLOW,
        listOf(
            "unclassified",
            "residential",
            "living_street",
            "service",
            "service_link",
            "alley",
            "driveway"
        ),
    ),
    CYCLE_WAY(
        25.0, // Moved to a more logical position
        R.string.road_type_cycle_way,
        Color.BLUE,
        listOf(
            "cycleway",
            "cycleway:left",
            "cycleway:right",
            "cycleway:both",
            "bicycle",
            "cycle_path"
        ),
    ),
    MINOR_ROAD(
        30.0,
        R.string.road_type_minor_road,
        Color.rgb(255, 195, 0),
        listOf("tertiary", "tertiary_link"),
    ),
    MAJOR_ROAD(
        40.0,
        R.string.road_type_major_road,
        Color.rgb(255, 165, 0),
        listOf(
            "secondary",
            "primary",
            "secondary_link",
            "primary_link",
            "motorway",
            "trunk",
            "motorway_link",
            "trunk_link"
        ),
    ),
    ROAD(
        70.0,
        R.string.road_type_road,
        Color.RED,
        listOf("road"),
    ),
    UNKNOWN(
        0.0,
        R.string.road_unknown,
        Color.WHITE,
        emptyList(),
    );

    companion object {
        private val roadTypeMap by lazy {
            entries.flatMap { roadType ->
                roadType.highwayTags.map { tag -> tag to roadType }
            }.toMap()
        }

        fun fromHighwayTag(tag: String?): RoadType {
            // Using .lowercase() makes the matching case-insensitive, which is more robust
            return roadTypeMap[tag?.lowercase()] ?: UNKNOWN
        }

        fun mapFromRoadInfo(roadInfo: RoadInfo): RoadType {
            return fromHighwayTag(roadInfo.roadType)
        }
    }
}
