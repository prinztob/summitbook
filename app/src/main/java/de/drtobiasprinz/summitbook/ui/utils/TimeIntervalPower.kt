package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.Summit


enum class TimeIntervalPower(
    val seconds: Int,
    val asString: String,
    val value: (Summit) -> Int,
    val minPower: (ExtremaValuesSummits?) -> Float,
    val maxPower: (ExtremaValuesSummits?) -> Float,
    val getMinSummit: (ExtremaValuesSummits?) -> Summit?,
    val getMaxSummit: (ExtremaValuesSummits?) -> Summit?
) {
    OneSec(
        1, "1 sec",
        { s -> s.garminData?.power?.oneSec?:0 },
        { e -> e?.power1sMinMax?.first?.garminData?.power?.oneSec?.toFloat() ?: 0f },
        { e -> e?.power1sMinMax?.second?.garminData?.power?.oneSec?.toFloat() ?: 0f },
        { e -> e?.power1sMinMax?.first }, { e -> e?.power1sMinMax?.second }),
    TwoSec(
        2, "2 sec",
        { s -> s.garminData?.power?.twoSec?:0 },
        { e -> e?.power2sMinMax?.first?.garminData?.power?.twoSec?.toFloat() ?: 0f },
        { e -> e?.power2sMinMax?.second?.garminData?.power?.twoSec?.toFloat() ?: 0f },
        { e -> e?.power2sMinMax?.first }, { e -> e?.power2sMinMax?.second }),
    FiveSec(
        5, "5 sec",
        { s -> s.garminData?.power?.fiveSec?:0 },
        { e -> e?.power5sMinMax?.first?.garminData?.power?.fiveSec?.toFloat() ?: 0f },
        { e -> e?.power5sMinMax?.second?.garminData?.power?.fiveSec?.toFloat() ?: 0f },
        { e -> e?.power5sMinMax?.first }, { e -> e?.power5sMinMax?.second }),
    TenSec(
        10, "10 sec",
        { s -> s.garminData?.power?.tenSec?:0 },
        { e -> e?.power10sMinMax?.first?.garminData?.power?.tenSec?.toFloat() ?: 0f },
        { e -> e?.power10sMinMax?.second?.garminData?.power?.tenSec?.toFloat() ?: 0f },
        { e -> e?.power10sMinMax?.first }, { e -> e?.power10sMinMax?.second }),
    TwentySec(
        20, "20 sec",
        { s -> s.garminData?.power?.twentySec?:0 },
        { e -> e?.power20sMinMax?.first?.garminData?.power?.twentySec?.toFloat() ?: 0f },
        { e -> e?.power20sMinMax?.second?.garminData?.power?.twentySec?.toFloat() ?: 0f },
        { e -> e?.power20sMinMax?.first }, { e -> e?.power20sMinMax?.second }),
    ThirtySec(
        30, "30 sec",
        { s -> s.garminData?.power?.thirtySec?:0 },
        { e -> e?.power30sMinMax?.first?.garminData?.power?.thirtySec?.toFloat() ?: 0f },
        { e -> e?.power30sMinMax?.second?.garminData?.power?.thirtySec?.toFloat() ?: 0f },
        { e -> e?.power30sMinMax?.first }, { e -> e?.power30sMinMax?.second }),
    OneMin(
        60, "1 min",
        { s -> s.garminData?.power?.oneMin?:0 },
        { e -> e?.power1minMinMax?.first?.garminData?.power?.oneMin?.toFloat() ?: 0f },
        { e -> e?.power1minMinMax?.second?.garminData?.power?.oneMin?.toFloat() ?: 0f },
        { e -> e?.power1minMinMax?.first }, { e -> e?.power1minMinMax?.second }),
    TwoMin(
        120, "2 min",
        { s -> s.garminData?.power?.twoMin?:0 },
        { e -> e?.power2minMinMax?.first?.garminData?.power?.twoMin?.toFloat() ?: 0f },
        { e -> e?.power2minMinMax?.second?.garminData?.power?.twoMin?.toFloat() ?: 0f },
        { e -> e?.power2minMinMax?.first }, { e -> e?.power2minMinMax?.second }),
    FiveMin(
        300, "5 min",
        { s -> s.garminData?.power?.fiveMin?:0 },
        { e -> e?.power5minMinMax?.first?.garminData?.power?.fiveMin?.toFloat() ?: 0f },
        { e -> e?.power5minMinMax?.second?.garminData?.power?.fiveMin?.toFloat() ?: 0f },
        { e -> e?.power5minMinMax?.first }, { e -> e?.power5minMinMax?.second }),
    TenMin(
        600, "10 min",
        { s -> s.garminData?.power?.tenMin?:0 },
        { e -> e?.power10minMinMax?.first?.garminData?.power?.tenMin?.toFloat() ?: 0f },
        { e -> e?.power10minMinMax?.second?.garminData?.power?.tenMin?.toFloat() ?: 0f },
        { e -> e?.power10minMinMax?.first }, { e -> e?.power10minMinMax?.second }),
    TwentyMin(
        1200, "20 min",
        { s -> s.garminData?.power?.twentyMin?:0 },
        { e -> e?.power20minMinMax?.first?.garminData?.power?.twentyMin?.toFloat() ?: 0f },
        { e -> e?.power20minMinMax?.second?.garminData?.power?.twentyMin?.toFloat() ?: 0f },
        { e -> e?.power20minMinMax?.first }, { e -> e?.power20minMinMax?.second }),
    ThirtyMin(
        1800, "30 min",
        { s -> s.garminData?.power?.thirtyMin?:0 },
        { e -> e?.power30minMinMax?.first?.garminData?.power?.thirtyMin?.toFloat() ?: 0f },
        { e -> e?.power30minMinMax?.second?.garminData?.power?.thirtyMin?.toFloat() ?: 0f },
        { e -> e?.power30minMinMax?.first }, { e -> e?.power30minMinMax?.second }),
    OneHour(
        3600, "1 h",
        { s -> s.garminData?.power?.oneHour?:0 },
        { e -> e?.power1hMinMax?.first?.garminData?.power?.oneHour?.toFloat() ?: 0f },
        { e -> e?.power1hMinMax?.second?.garminData?.power?.oneHour?.toFloat() ?: 0f },
        { e -> e?.power1hMinMax?.first }, { e -> e?.power1hMinMax?.second }),
    TwoHours(
        7200, "2 h",
        { s -> s.garminData?.power?.twoHours?:0 },
        { e -> e?.power2hMinMax?.first?.garminData?.power?.twoHours?.toFloat() ?: 0f },
        { e -> e?.power2hMinMax?.second?.garminData?.power?.twoHours?.toFloat() ?: 0f },
        { e -> e?.power2hMinMax?.first }, { e -> e?.power2hMinMax?.second }),
    ThreeHours(
        10800, "3 h",
        { s -> s.garminData?.power?.threeHours?:0 },
        { e -> e?.power3hMinMax?.first?.garminData?.power?.threeHours?.toFloat() ?: 0f },
        { e -> e?.power3hMinMax?.second?.garminData?.power?.threeHours?.toFloat() ?: 0f },
        { e -> e?.power3hMinMax?.first }, { e -> e?.power3hMinMax?.second }),
    FourHours(
        14400, "4 h",
        { s -> s.garminData?.power?.fourHours?:0 },
        { e -> e?.power4hMinMax?.first?.garminData?.power?.fourHours?.toFloat() ?: 0f },
        { e -> e?.power4hMinMax?.second?.garminData?.power?.fourHours?.toFloat() ?: 0f },
        { e -> e?.power4hMinMax?.first }, { e -> e?.power4hMinMax?.second }),
    FiveHours(
        18000, "5 h",
        { s -> s.garminData?.power?.fiveHours?:0 },
        { e -> e?.power5hMinMax?.first?.garminData?.power?.fiveHours?.toFloat() ?: 0f },
        { e -> e?.power5hMinMax?.second?.garminData?.power?.fiveHours?.toFloat() ?: 0f },
        { e -> e?.power5hMinMax?.first }, { e -> e?.power5hMinMax?.second })
}
