package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import kotlin.math.roundToInt


enum class TimeIntervalPower(
    val seconds: Int,
    val value: (Summit) -> Int,
    val minPower: (ExtremaValuesSummits?) -> Float,
    val maxPower: (ExtremaValuesSummits?) -> Float
) {
    OneSec(
        1,
        { s -> s.garminData?.power?.oneSec?:0 },
        { e -> e?.power1sMinMax?.first?.garminData?.power?.oneSec?.toFloat() ?: 0f },
        { e -> e?.power1sMinMax?.second?.garminData?.power?.oneSec?.toFloat() ?: 0f }
    ),
    TwoSec(
        2,
        { s -> s.garminData?.power?.twoSec?:0 },
        { e -> e?.power2sMinMax?.first?.garminData?.power?.twoSec?.toFloat() ?: 0f },
        { e -> e?.power2sMinMax?.second?.garminData?.power?.twoSec?.toFloat() ?: 0f }
    ),
    FiveSec(
        5,
        { s -> s.garminData?.power?.fiveSec?:0 },
        { e -> e?.power5sMinMax?.first?.garminData?.power?.fiveSec?.toFloat() ?: 0f },
        { e -> e?.power5sMinMax?.second?.garminData?.power?.fiveSec?.toFloat() ?: 0f }
    ),
    TenSec(
        10,
        { s -> s.garminData?.power?.tenSec?:0 },
        { e -> e?.power10sMinMax?.first?.garminData?.power?.tenSec?.toFloat() ?: 0f },
        { e -> e?.power10sMinMax?.second?.garminData?.power?.tenSec?.toFloat() ?: 0f }
    ),
    TwentySec(
        20,
        { s -> s.garminData?.power?.twentySec?:0 },
        { e -> e?.power20sMinMax?.first?.garminData?.power?.twentySec?.toFloat() ?: 0f },
        { e -> e?.power20sMinMax?.second?.garminData?.power?.twentySec?.toFloat() ?: 0f }
    ),
    ThirtySec(
        30,
        { s -> s.garminData?.power?.thirtySec?:0 },
        { e -> e?.power30sMinMax?.first?.garminData?.power?.thirtySec?.toFloat() ?: 0f },
        { e -> e?.power30sMinMax?.second?.garminData?.power?.thirtySec?.toFloat() ?: 0f }
    ),
    OneMin(
        60,
        { s -> s.garminData?.power?.oneMin?:0 },
        { e -> e?.power1minMinMax?.first?.garminData?.power?.oneMin?.toFloat() ?: 0f },
        { e -> e?.power1minMinMax?.second?.garminData?.power?.oneMin?.toFloat() ?: 0f }
    ),
    TwoMin(
        120,
        { s -> s.garminData?.power?.twoMin?:0 },
        { e -> e?.power2minMinMax?.first?.garminData?.power?.twoMin?.toFloat() ?: 0f },
        { e -> e?.power2minMinMax?.second?.garminData?.power?.twoMin?.toFloat() ?: 0f }
    ),
    FiveMin(
        300,
        { s -> s.garminData?.power?.fiveMin?:0 },
        { e -> e?.power5minMinMax?.first?.garminData?.power?.fiveMin?.toFloat() ?: 0f },
        { e -> e?.power5minMinMax?.second?.garminData?.power?.fiveMin?.toFloat() ?: 0f }
    ),
    TenMin(
        600,
        { s -> s.garminData?.power?.tenMin?:0 },
        { e -> e?.power10minMinMax?.first?.garminData?.power?.tenMin?.toFloat() ?: 0f },
        { e -> e?.power10minMinMax?.second?.garminData?.power?.tenMin?.toFloat() ?: 0f }
    ),
    TwentyMin(
        1200,
        { s -> s.garminData?.power?.twentyMin?:0 },
        { e -> e?.power20minMinMax?.first?.garminData?.power?.twentyMin?.toFloat() ?: 0f },
        { e -> e?.power20minMinMax?.second?.garminData?.power?.twentyMin?.toFloat() ?: 0f }
    ),
    ThirtyMin(
        1800,
        { s -> s.garminData?.power?.thirtyMin?:0 },
        { e -> e?.power30minMinMax?.first?.garminData?.power?.thirtyMin?.toFloat() ?: 0f },
        { e -> e?.power30minMinMax?.second?.garminData?.power?.thirtyMin?.toFloat() ?: 0f }
    ),
    OneHour(
        3600,
        { s -> s.garminData?.power?.oneHour?:0 },
        { e -> e?.power1hMinMax?.first?.garminData?.power?.oneHour?.toFloat() ?: 0f },
        { e -> e?.power1hMinMax?.second?.garminData?.power?.oneHour?.toFloat() ?: 0f }
    ),
    TwoHours(
        7200,
        { s -> s.garminData?.power?.twoHours?:0 },
        { e -> e?.power2hMinMax?.first?.garminData?.power?.twoHours?.toFloat() ?: 0f },
        { e -> e?.power2hMinMax?.second?.garminData?.power?.twoHours?.toFloat() ?: 0f }
    ),
    ThreeHours(
        10800,
        { s -> s.garminData?.power?.threeHours?:0 },
        { e -> e?.power3hMinMax?.first?.garminData?.power?.threeHours?.toFloat() ?: 0f },
        { e -> e?.power3hMinMax?.second?.garminData?.power?.threeHours?.toFloat() ?: 0f }
    ),
    FourHours(
        14400,
        { s -> s.garminData?.power?.fourHours?:0 },
        { e -> e?.power4hMinMax?.first?.garminData?.power?.fourHours?.toFloat() ?: 0f },
        { e -> e?.power4hMinMax?.second?.garminData?.power?.fourHours?.toFloat() ?: 0f }
    ),
    FiveHours(
        18000,
        { s -> s.garminData?.power?.fiveHours?:0 },
        { e -> e?.power5hMinMax?.first?.garminData?.power?.fiveHours?.toFloat() ?: 0f },
        { e -> e?.power5hMinMax?.second?.garminData?.power?.fiveHours?.toFloat() ?: 0f }
    );

    /**
     * Returns a localized string representation of the time interval
     */
    fun asLocalizedString(context: Context): String {
        return when {
            seconds < 60 -> context.getString(R.string.value_with_unit, seconds.toString(), context.getString(R.string.sec))
            seconds < 3600 -> context.getString(R.string.value_with_unit, (seconds / 60.0).roundToInt().toString(), context.getString(R.string.min))
            else -> context.getString(R.string.value_with_unit, (seconds / 3600.0).roundToInt().toString(), context.getString(R.string.h))
        }
    }
}
