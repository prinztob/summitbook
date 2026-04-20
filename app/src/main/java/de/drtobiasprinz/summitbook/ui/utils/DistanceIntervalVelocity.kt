package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.Summit


enum class DistanceIntervalVelocity(
    val kilometers: Int,
    val value: (Summit) -> Double
) {
    OneKilometer(
        1,
        { s -> s.velocityData.oneKilometer }
    ),
    FiveKilometers(
        5,
        { s -> s.velocityData.fiveKilometer }
    ),
    TenKilometers(
        10,
        { s -> s.velocityData.tenKilometers }
    ),
    FifteenKilometers(
        15,
        { s -> s.velocityData.fifteenKilometers }
    ),
    TwentyKilometers(
        20,
        { s -> s.velocityData.twentyKilometers }
    ),
    ThirtyKilometers(
        30,
        { s -> s.velocityData.thirtyKilometers }
    ),
    FortyKilometers(
        40,
        { s -> s.velocityData.fortyKilometers }
    ),
    FiftyKilometers(
        50,
        { s -> s.velocityData.fiftyKilometers }
    ),
    SeventyFiveKilometers(
        75,
        { s -> s.velocityData.seventyFiveKilometers }
    ),
    HundredKilometers(
        100,
        { s -> s.velocityData.hundredKilometers }
    );

}
