package de.drtobiasprinz.summitbook.models

import android.content.Context
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.DailyReportData

enum class LineChartDailyReportYAxisSelector(
    val nameId: Int,
    val unit: String,
    val getStringForCustomMarker: (DailyReportData, Context) -> String
) {
    ActivityMinutes(R.string.active_duration, "h", { entry, context ->
        String.format(
            "%s\n%s: %s %s\n%s: %s %s\n%s: %s %s",
            entry.markerText,
            context.getString(R.string.active_duration),
            entry.totalIntensityMinutes,
            context.getString(R.string.min),
            context.getString(R.string.moderate_intensity_minutes),
            entry.events.sumOf { it.moderateIntensityMinutes },
            context.getString(R.string.min),
            context.getString(R.string.vigorous_intensity_minutes),
            entry.events.sumOf { it.vigorousIntensityMinutes },
            context.getString(R.string.min),
        )
    }),
    Steps(R.string.steps, "", { entry, context ->
        String.format(
            "%s\n%s: %s",
            entry.markerText,
            context.getString(R.string.steps),
            entry.steps
        )
    }),
    FloorsClimbed(R.string.floors_climbed, "", { entry, context ->
        String.format(
            "%s\n%s: %s",
            entry.markerText,
            context.getString(R.string.floors_climbed),
            entry.floorsClimbed
        )
    }),
    HeartRate(R.string.heart_rate, "bpm", { entry, context ->
        String.format(
            "%s\n%s: %s %s\n%s: %s %s\n%s: %s %s",
            entry.markerText,
            context.getString(R.string.min_heart_rate),
            entry.minHeartRate,
            context.getString(R.string.bpm),
            context.getString(R.string.resting_heart_rate),
            entry.restingHeartRate,
            context.getString(R.string.bpm),
            context.getString(R.string.max_heart_rate),
            entry.maxHeartRate,
            context.getString(R.string.bpm),
        )
    }),
    SleepHours(R.string.sleep_hours, "h", { entry, context ->
        String.format(
            "%s\n%s: %s",
            entry.markerText,
            context.getString(R.string.sleep_hours),
            entry.sleepHours
        )
    }),
}