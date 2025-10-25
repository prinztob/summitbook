package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R

enum class RoadType(var number: Double, var nameId: Int, var highwayTags: List<String>) {

    WAY(
        10.0,
        R.string.road_type_way,
        listOf("path", "footway", "track", "bridleway", "pedestrian"),
    ),
    SIDE_STREET(
        20.0,
        R.string.road_type_side_street,
        listOf("residential", "living_street", "service"),
    ),
    COUNTRY_ROAD(
        30.0,
        R.string.road_type_country_road,
        listOf("unclassified", "tertiary", "secondary", "primary", "trunk", "motorway"),
    ),
    CYCLE_WAY(
        40.0,
        R.string.road_type_cycle_way,
        listOf("cycleway"),
    ),
    ROAD(
        50.0,
        R.string.road_type_road,
        listOf("road"),
    ),
    UNKNOWN(
        0.0,
        R.string.road_unknown,
        listOf(),
    );

    companion object {
        fun mapFromRoadInfo(roadInfo: RoadInfo): RoadType {
            return entries.firstOrNull { roadInfo.roadType in it.highwayTags }
            ?: UNKNOWN
        }
    }
}
