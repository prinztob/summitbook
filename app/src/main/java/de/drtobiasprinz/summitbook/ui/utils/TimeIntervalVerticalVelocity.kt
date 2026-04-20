package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.Summit


enum class TimeIntervalVerticalVelocity(
    val seconds: Int,
    val value: (Summit) -> Double
) {
    OneMinUp(
        60,
        { s -> s.elevationData.maxVerticalVelocity1Min }
    ),
    TenMinUp(
        600,
        { s -> s.elevationData.maxVerticalVelocity10Min }
    ),
    OneHourUp(
        3600,
        { s -> s.elevationData.maxVerticalVelocity1h }
    ),
    OneMinDown(
        60,
        { s -> s.elevationData.maxVerticalVelocityDown1Min }
    ),
    TenMinDown(
        600,
        { s -> s.elevationData.maxVerticalVelocityDown10Min }
    ),
    OneHourDown(
        3600,
        { s -> s.elevationData.maxVerticalVelocityDown1h }
    );

}
