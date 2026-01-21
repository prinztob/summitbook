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
    val getValue: (Summit) -> Number?,
    val getMinMaxSummit: (ExtremaValuesSummits?) -> Pair<Summit, Summit>?,
    val reverse: Boolean = false,
    val toHHms: Boolean = false,
    val digits: Int = 1,
    val factor: Int = 1
) {

    HeightMeter(
        TextFieldGroup.Base,
        "hm",
        R.string.height_meter_hint,
        { e -> e.elevationData.elevationGain },
        { e -> e?.heightMetersMinMax },
        digits = 0
    ),
    Kilometer(
        TextFieldGroup.Base,
        "km",
        R.string.kilometers_hint,
        { e -> e.kilometers },
        { e -> e?.kilometersMinMax }),
    TopElevation(
        TextFieldGroup.Base,
        "hm",
        R.string.top_elevation_hint,
        { e -> e.elevationData.maxElevation },
        { e -> e?.topElevationMinMax },
        digits = 0
    ),
    TopVerticalVelocity1Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_1Min,
        { e -> e.elevationData.maxVerticalVelocity1Min },
        { e -> e?.topVerticalVelocity1MinMinMax },
        factor = 60,
        digits = 0
    ),
    TopVerticalVelocity10Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_10Min,
        { e -> e.elevationData.maxVerticalVelocity10Min },
        { e -> e?.topVerticalVelocity10MinMinMax },
        factor = 600,
        digits = 0
    ),
    TopVerticalVelocity1H(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_1h,
        { e -> e.elevationData.maxVerticalVelocity1h },
        { e -> e?.topVerticalVelocity1hMinMax },
        factor = 3600,
        digits = 0
    ),
    TopVerticalVelocityDown1Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_down_1Min,
        { e -> e.elevationData.maxVerticalVelocityDown1Min },
        { e -> e?.topVerticalVelocityDown1MinMinMax },
        factor = 60,
        digits = 0
    ),
    TopVerticalVelocityDown10Min(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_down_10Min,
        { e -> e.elevationData.maxVerticalVelocityDown10Min },
        { e -> e?.topVerticalVelocityDown10MinMinMax },
        factor = 600,
        digits = 0
    ),
    TopVerticalVelocityDown1H(
        TextFieldGroup.Base,
        "m",
        R.string.max_verticalVelocity_down_1h,
        { e -> e.elevationData.maxVerticalVelocityDown1h },
        { e -> e?.topVerticalVelocityDown1hMinMax },
        factor = 3600,
        digits = 0
    ),
    TopSlope(
        TextFieldGroup.Base,
        "%",
        R.string.max_slope,
        { e -> e.elevationData.maxSlope },
        { e -> e?.topSlopeMinMax }),
    Pace(
        TextFieldGroup.Base,
        "km/h",
        R.string.top_speed,
        { e -> e.getAverageVelocity() },
        { e -> e?.averageSpeedMinMax }),
    TopSpeed(
        TextFieldGroup.Base,
        "km/h",
        R.string.top_speed,
        { e -> e.velocityData.maxVelocity },
        { e -> e?.topSpeedMinMax }),
    Durations(
        TextFieldGroup.Base,
        "h",
        R.string.duration,
        { e -> e.duration },
        { e -> e?.durationMinMax },
        toHHms = true
    ),

    TopSpeedOneKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_1km_hint,
        { e -> e.velocityData.oneKilometer },
        { e -> e?.oneKmMinMax }),
    TopSpeedFiveKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_5km_hint,
        { e -> e.velocityData.fiveKilometer },
        { e -> e?.fiveKmMinMax }),
    TopSpeedTenKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_10km_hint,
        { e -> e.velocityData.tenKilometers },
        { e -> e?.tenKmMinMax }),
    TopSpeedFifteenKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_15km_hint,
        { e -> e.velocityData.fifteenKilometers },
        { e -> e?.fifteenKmMinMax }),
    TopSpeedTwentyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_20km_hint,
        { e -> e.velocityData.twentyKilometers },
        { e -> e?.twentyKmMinMax }),
    TopSpeedThirtyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_30km_hint,
        { e -> e.velocityData.thirtyKilometers },
        { e -> e?.thirtyKmMinMax }),
    TopSpeedFortyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_40km_hint,
        { e -> e.velocityData.fortyKilometers },
        { e -> e?.fortyKmMinMax }),
    TopSpeedFiftyKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_50km_hint,
        { e -> e.velocityData.fiftyKilometers },
        { e -> e?.fiftyKmMinMax }),
    TopSpeedSeventyFiveKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_75km_hint,
        { e -> e.velocityData.seventyFiveKilometers },
        { e -> e?.seventyFiveKmMinMax }),
    TopSpeedHundredKm(
        TextFieldGroup.AdditionalSpeedData,
        "km/h",
        R.string.top_speed_100km_hint,
        { e -> e.velocityData.hundredKilometers },
        { e -> e?.hundredKmMinMax }),
    RoadSurfaceAsphalt(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_asphalt,
        { e ->
            if (Surface.ASPHALT in e.distancePerSurface) e.distancePerSurface[Surface.ASPHALT] else 0
        },
        { e -> null }),
    RoadSurfaceStonePavement(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_stone_paved,
        { e ->
            if (Surface.STONE_PAVEMENT in e.distancePerSurface) e.distancePerSurface[Surface.STONE_PAVEMENT] else 0
        },
        { e -> null }),
    RoadSurfaceCompacted(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_compact,
        { e ->
            if (Surface.COMPACTED in e.distancePerSurface) e.distancePerSurface[Surface.COMPACTED] else 0
        },
        { e -> null }),
    RoadSurfaceLoseGround(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_lose_ground,
        { e ->
            if (Surface.LOSE_GROUND in e.distancePerSurface) e.distancePerSurface[Surface.LOSE_GROUND] else 0
        },
        { e -> null }),
    RoadSurfacePath(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_surface_path,
        { e ->
            if (Surface.PATH in e.distancePerSurface) e.distancePerSurface[Surface.PATH] else 0
        },
        { e -> null }),
    RoadSurfaceUnknown(
        TextFieldGroup.DistancePerSurface,
        "m",
        R.string.road_unknown,
        { e ->
            if (Surface.UNKNOWN in e.distancePerSurface) e.distancePerSurface[Surface.UNKNOWN] else 0
        },
        { e -> null }),

    RoadTypeWay(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_way,
        { e ->
            if (RoadType.WAY in e.distancePerRoadType) e.distancePerRoadType[RoadType.WAY] else 0
        },
        { e -> null }),
    RoadTypeSideStreet(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_side_street,
        { e ->
            if (RoadType.SIDE_STREET in e.distancePerRoadType) e.distancePerRoadType[RoadType.SIDE_STREET] else 0
        },
        { e -> null }),
    RoadTypeMinorRoad(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_minor_road,
        { e ->
            if (RoadType.MINOR_ROAD in e.distancePerRoadType) e.distancePerRoadType[RoadType.MINOR_ROAD] else 0
        },
        { e -> null }),
    RoadTypeMajorRoad(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_major_road,
        { e ->
            if (RoadType.MAJOR_ROAD in e.distancePerRoadType) e.distancePerRoadType[RoadType.MAJOR_ROAD] else 0
        },
        { e -> null }),
    RoadTypeCycleWay(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_cycle_way,
        { e ->
            if (RoadType.CYCLE_WAY in e.distancePerRoadType) e.distancePerRoadType[RoadType.CYCLE_WAY] else 0
        },
        { e -> null }),
    RoadTypeRoad(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_type_road,
        { e ->
            if (RoadType.ROAD in e.distancePerRoadType) e.distancePerRoadType[RoadType.ROAD] else 0
        },
        { e -> null }),
    RoadTypeUnknown(
        TextFieldGroup.DistancePerRoadType,
        "m",
        R.string.road_unknown,
        { e ->
            if (RoadType.UNKNOWN in e.distancePerRoadType) e.distancePerRoadType[RoadType.UNKNOWN] else 0
        },
        { e -> null }),;

}
