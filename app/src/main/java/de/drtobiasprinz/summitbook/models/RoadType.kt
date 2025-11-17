package de.drtobiasprinz.summitbook.models

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import de.drtobiasprinz.summitbook.R

enum class RoadType(
    val number: Double,
    @StringRes val nameId: Int,
    @ColorInt val color: Int,
    val highwayTags: List<String>,
    val additionalTags: Map<String, String> = emptyMap(),
    val useCheckeredPattern: Boolean = false
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
        mapOf("bicycle" to "bic_designated")
    ),
    MINOR_ROAD(
        30.0,
        R.string.road_type_minor_road,
        Color.rgb(255, 215, 0),
        listOf("tertiary", "tertiary_link"),
    ),
    MAJOR_ROAD(
        40.0,
        R.string.road_type_major_road,
        Color.rgb(255, 100, 0),
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

        fun fromHighwayTag(tag: String?, additionalTags: Map<String, String>?): RoadType {
            if (additionalTags != null) {
                val roadTypeFromTags = entries.firstOrNull { roadType ->
                    roadType.additionalTags.isNotEmpty() && roadType.additionalTags.any { (key, value) ->
                        additionalTags[key] == value
                    }
                }
                if (roadTypeFromTags != null) {
                    return roadTypeFromTags
                }
            }
            return roadTypeMap[tag?.lowercase()] ?: UNKNOWN
        }

        fun mapFromRoadInfo(roadInfo: RoadInfo): RoadType {
            return fromHighwayTag(roadInfo.roadType, roadInfo.additionalTags)
        }
    }
}
