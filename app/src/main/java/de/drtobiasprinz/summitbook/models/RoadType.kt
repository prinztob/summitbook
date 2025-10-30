package de.drtobiasprinz.summitbook.models

import android.graphics.Color
import de.drtobiasprinz.summitbook.R

enum class RoadType(
    var number: Double, var nameId: Int, var color: Int, var highwayTags: List<String>
) {

    WAY(
        10.0,
        R.string.road_type_way,
        Color.rgb(165, 42, 42),
        listOf("path", "footway", "track", "bridleway", "pedestrian"),
    ),
    SIDE_STREET(
        20.0,
        R.string.road_type_side_street,
        Color.YELLOW,
        listOf("residential", "living_street", "service"),
    ),
    COUNTRY_ROAD(
        30.0,
        R.string.road_type_country_road,
        Color.rgb(255, 165, 0),
        listOf("unclassified", "tertiary", "secondary", "primary", "trunk", "motorway"),
    ),
    CYCLE_WAY(
        40.0,
        R.string.road_type_cycle_way,
        Color.BLUE,
        listOf("cycleway"),
    ),
    ROAD(
        50.0,
        R.string.road_type_road,
        Color.RED,
        listOf("road"),
    ),
    UNKNOWN(
        0.0,
        R.string.road_unknown,
        Color.WHITE,
        listOf(),
    );

    companion object {
        fun mapFromRoadInfo(roadInfo: RoadInfo): RoadType {
            return entries.firstOrNull { roadInfo.roadType in it.highwayTags } ?: UNKNOWN
        }
    }
}
