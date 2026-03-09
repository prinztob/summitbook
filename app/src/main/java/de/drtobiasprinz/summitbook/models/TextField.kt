package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits

enum class TextFieldGroup {
    Base, AdditionalSpeedData, DistancePerRoadType, DistancePerSurface,
}

enum class TextField(
    val group: TextFieldGroup,
    val unit: String,
    val nameId: Int,
    val iconId: Int,
    val getValue: (Summit) -> Number?,
    val getMinMaxSummit: (ExtremaValuesSummits?) -> Pair<Summit, Summit>? = { null },
    val reverse: Boolean = false,
    val toHHms: Boolean = false,
    val digits: Int = 1,
    val factor: Int = 1
) {

    HeightMeter(
        TextFieldGroup.Base,
        "hm",
        R.string.height_meter_hint,
        R.drawable.baseline_trending_up_black_24dp,
        { e -> e.elevationData.elevationGain },
        { e -> e?.heightMetersMinMax },
        digits = 0
    ),
    Kilometer(
        TextFieldGroup.Base,
        "km",
        R.string.kilometers_hint,
        R.drawable.outline_distance_24,
        { e -> e.kilometers },
        { e -> e?.kilometersMinMax }),
    TopElevation(
        TextFieldGroup.Base,
        "hm",
        R.string.top_elevation_hint,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxElevation },
        { e -> e?.topElevationMinMax },
        digits = 0
    ),
    TopVerticalVelocity1Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_1Min,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxVerticalVelocity1Min },
        { e -> e?.topVerticalVelocity1MinMinMax },
        factor = 60,
        digits = 0
    ),
    TopVerticalVelocity10Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_10Min,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxVerticalVelocity10Min },
        { e -> e?.topVerticalVelocity10MinMinMax },
        factor = 600,
        digits = 0
    ),
    TopVerticalVelocity1H(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_1h,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxVerticalVelocity1h },
        { e -> e?.topVerticalVelocity1hMinMax },
        factor = 3600,
        digits = 0
    ),
    TopVerticalVelocityDown1Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_down_1Min,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxVerticalVelocityDown1Min },
        { e -> e?.topVerticalVelocityDown1MinMinMax },
        factor = 60,
        digits = 0
    ),
    TopVerticalVelocityDown10Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_down_10Min,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxVerticalVelocityDown10Min },
        { e -> e?.topVerticalVelocityDown10MinMinMax },
        factor = 600,
        digits = 0
    ),
    TopVerticalVelocityDown1H(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_down_1h,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxVerticalVelocityDown1h },
        { e -> e?.topVerticalVelocityDown1hMinMax },
        factor = 3600,
        digits = 0
    ),
    TopSlope(
        TextFieldGroup.Base,
        "%",
        R.string.max_slope,
        R.drawable.baseline_terrain_24,
        { e -> e.elevationData.maxSlope },
        { e -> e?.topSlopeMinMax }),
    Pace(
        TextFieldGroup.Base,
        "km/h",
        R.string.pace_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.getAverageVelocity() },
        { e -> e?.averageSpeedMinMax }),
    TopSpeed(
        TextFieldGroup.Base,
        "km/h",
        R.string.top_speed,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.maxVelocity },
        { e -> e?.topSpeedMinMax }),
    Durations(
        TextFieldGroup.Base,
        "h",
        R.string.duration,
        R.drawable.ic_baseline_timer_24,
        { e -> e.duration },
        { e -> e?.durationMinMax },
        toHHms = true
    ),

    TopSpeedOneKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_1km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.oneKilometer },
        { e -> e?.oneKmMinMax }),
    TopSpeedFiveKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_5km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.fiveKilometer },
        { e -> e?.fiveKmMinMax }),
    TopSpeedTenKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_10km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.tenKilometers },
        { e -> e?.tenKmMinMax }),
    TopSpeedFifteenKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_15km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.fifteenKilometers },
        { e -> e?.fifteenKmMinMax }),
    TopSpeedTwentyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_20km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.twentyKilometers },
        { e -> e?.twentyKmMinMax }),
    TopSpeedThirtyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_30km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.thirtyKilometers },
        { e -> e?.thirtyKmMinMax }),
    TopSpeedFortyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_40km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.fortyKilometers },
        { e -> e?.fortyKmMinMax }),
    TopSpeedFiftyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_50km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.fiftyKilometers },
        { e -> e?.fiftyKmMinMax }),
    TopSpeedSeventyFiveKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_75km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.seventyFiveKilometers },
        { e -> e?.seventyFiveKmMinMax }),
    TopSpeedHundredKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_100km_hint,
        R.drawable.baseline_speed_black_24dp,
        { e -> e.velocityData.hundredKilometers },
        { e -> e?.hundredKmMinMax }),
    RoadSurfaceAsphalt(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_asphalt,
        R.drawable.baseline_add_road_24,
        { e ->
            if (Surface.ASPHALT in e.distancePerSurface) e.distancePerSurface[Surface.ASPHALT] else 0
        }),
    RoadSurfaceStonePavement(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_stone_paved,
        R.drawable.baseline_add_road_24,
        { e ->
            if (Surface.STONE_PAVEMENT in e.distancePerSurface) e.distancePerSurface[Surface.STONE_PAVEMENT] else 0
        }),
    RoadSurfaceCompacted(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_compact,
        R.drawable.baseline_add_road_24,
        { e ->
            if (Surface.COMPACTED in e.distancePerSurface) e.distancePerSurface[Surface.COMPACTED] else 0
        }),
    RoadSurfaceLoseGround(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_lose_ground,
        R.drawable.baseline_add_road_24,
        { e ->
            if (Surface.LOSE_GROUND in e.distancePerSurface) e.distancePerSurface[Surface.LOSE_GROUND] else 0
        }),
    RoadSurfacePath(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_path,
        R.drawable.baseline_add_road_24,
        { e ->
            if (Surface.PATH in e.distancePerSurface) e.distancePerSurface[Surface.PATH] else 0
        }),
    RoadSurfaceUnknown(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_unknown,
        R.drawable.baseline_add_road_24,
        { e ->
            if (Surface.UNKNOWN in e.distancePerSurface) e.distancePerSurface[Surface.UNKNOWN] else 0
        }),

    RoadTypeWay(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_way,
        R.drawable.baseline_add_road_24,
        { e ->
            if (RoadType.WAY in e.distancePerRoadType) e.distancePerRoadType[RoadType.WAY] else 0
        }),
    RoadTypeSideStreet(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_side_street,
        R.drawable.baseline_add_road_24,
        { e ->
            if (RoadType.SIDE_STREET in e.distancePerRoadType) e.distancePerRoadType[RoadType.SIDE_STREET] else 0
        }),
    RoadTypeMinorRoad(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_minor_road,
        R.drawable.baseline_add_road_24,
        { e ->
            if (RoadType.MINOR_ROAD in e.distancePerRoadType) e.distancePerRoadType[RoadType.MINOR_ROAD] else 0
        }),
    RoadTypeMajorRoad(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_major_road,
        R.drawable.baseline_add_road_24,
        { e ->
            if (RoadType.MAJOR_ROAD in e.distancePerRoadType) e.distancePerRoadType[RoadType.MAJOR_ROAD] else 0
        }),
    RoadTypeCycleWay(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_cycle_way,
        R.drawable.baseline_add_road_24,
        { e ->
            if (RoadType.CYCLE_WAY in e.distancePerRoadType) e.distancePerRoadType[RoadType.CYCLE_WAY] else 0
        }),
    RoadTypeRoad(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_road,
        R.drawable.baseline_add_road_24,
        { e ->
            if (RoadType.ROAD in e.distancePerRoadType) e.distancePerRoadType[RoadType.ROAD] else 0
        }),
    RoadTypeUnknown(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_unknown,
        R.drawable.baseline_add_road_24,
        { e ->
            if (RoadType.UNKNOWN in e.distancePerRoadType) e.distancePerRoadType[RoadType.UNKNOWN] else 0
        });
}
