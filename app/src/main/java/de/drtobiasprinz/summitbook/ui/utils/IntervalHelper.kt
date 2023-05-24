package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.Summit
import java.util.*
import kotlin.math.ceil

class IntervalHelper(private val summitEntries: List<Summit>) {

    val participantsRangeAndAnnotationForSummitChipValues: Pair<MutableList<Float>, MutableList<String>> by lazy {
        getRangeAndAnnotationForSummitChipValues { summit -> summit.participants }
    }
    val equipmentsRangeAndAnnotationForSummitChipValues: Pair<MutableList<Float>, MutableList<String>> by lazy {
        getRangeAndAnnotationForSummitChipValues { summit -> summit.equipments }
    }
    val countriesRangeAndAnnotationForSummitChipValues: Pair<MutableList<Float>, MutableList<String>> by lazy {
        getRangeAndAnnotationForSummitChipValues { summit -> summit.countries }
    }
    val topElevationRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Float>>> by lazy {
        getRangeAndAnnotationsForSummitValue(topElevationStep.toFloat()) { it.elevationData.maxElevation.toFloat() }
    }
    val elevationGainRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Float>>> by lazy {
        getRangeAndAnnotationsForSummitValue(elevationGainStep.toFloat()) { it.elevationData.elevationGain.toFloat() }
    }
    val kilometersRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Float>>> by lazy {
        getRangeAndAnnotationsForSummitValue(kilometersStep.toFloat()) { it.kilometers.toFloat() }
    }
    val dateByWeekRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> by lazy {
        getRangeAndAnnotationsForDate(Calendar.WEEK_OF_YEAR, Calendar.DAY_OF_WEEK)
    }
    val dateByMonthRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> by lazy {
        getRangeAndAnnotationsForDate(Calendar.MONTH, Calendar.DAY_OF_MONTH, true)
    }
    val dateByYearRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> by lazy {
        getRangeAndAnnotationsForDate(Calendar.YEAR, Calendar.DAY_OF_YEAR)
    }
    val dateByYearUntilTodayRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> by lazy {
        getRangeAndAnnotationsForDate(Calendar.YEAR, Calendar.DAY_OF_YEAR, untilToday = true)
    }
    val dateByQuarterRangeAndAnnotation: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> by lazy {
        getRangeAndAnnotationQuarterly()
    }

    fun getRangeAndAnnotationForSummitChipValues(getValue: (Summit) -> List<String>): Pair<MutableList<Float>, MutableList<String>> {
        val values = summitEntries.flatMap { getValue(it) }.filter { it != "" }
        val countPerValue = values.toSet().map { name ->
            name to values.count { it == name }
        }
        val comparable =
            countPerValue.toList().sortedByDescending { (_, value) -> value }.toMap()
                .map { (key, _) -> key } as MutableList<String>
        comparable.add("")
        val annotation =
            (0 until comparable.size).map { it.toFloat() } as MutableList<Float>
        return Pair(annotation, comparable)
    }

    fun getRangeAndAnnotationsForSummitValue(
        step: Float,
        getValue: (Summit) -> Float
    ): Pair<MutableList<Float>, MutableList<ClosedRange<Float>>> {
        val ranges: Pair<MutableList<Float>, MutableList<ClosedRange<Float>>> =
            Pair(mutableListOf(), mutableListOf())
        val max = getValue(summitEntries.maxBy { getValue(it) })
        val intervals = ceil(max / step).toInt()
        for (i in 0..intervals) {
            ranges.first.add(i.toFloat())
            ranges.second.add((i * step)..((i + 1) * step - 0.01f))
        }
        return ranges
    }

    private fun calendarStartOrEndOfDate(date: Date): Calendar {
        val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun getRangeAndAnnotationQuarterly(): Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> {
        val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
        var minDate = summitEntries.minBy { it.date }.date
        cal.time = minDate
        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMinimum(Calendar.DAY_OF_YEAR))
        minDate = cal.time

        var maxDate = summitEntries.maxBy { it.date }.date
        cal.time = maxDate
        cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
        maxDate = cal.time
        var i = 0f
        val ranges: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> =
            Pair(mutableListOf(), mutableListOf())
        var date = minDate
        var to = minDate
        while (to < maxDate) {
            val cal1: Calendar = calendarStartOrEndOfDate(date)
            val from = cal1.time
            cal1.add(Calendar.MONTH, 3)
            date = cal1.time
            cal1.add(Calendar.SECOND, -1)
            to = cal1.time
            val last = ranges.first.lastOrNull() ?: -1f
            if (i > last) {
                ranges.first.add(i)
            } else {
                ranges.first.add(last + 1f)
            }
            ranges.second.add(from..to)

            i += 1f
        }
        return ranges
    }

    fun getRangeAndAnnotationsForDate(
        rangeTypeForDelta: Int,
        rangeTypeForRange: Int,
        untilEndOfYear: Boolean = false,
        untilToday: Boolean = false
    ): Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> {
        val minDate = summitEntries.minBy { it.date }.date
        var maxDate = summitEntries.maxBy { it.date }.date
        if (untilEndOfYear) {
            val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
            cal.time = maxDate
            val day = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
            cal.set(Calendar.DAY_OF_YEAR, day)
            maxDate = cal.time
        }

        return if (rangeTypeForRange == Calendar.DAY_OF_WEEK) {
            getRangeForWeek(minDate, maxDate, rangeTypeForRange, rangeTypeForDelta)
        } else {
            getRangeForMonthAndYear(
                minDate,
                maxDate,
                rangeTypeForRange,
                rangeTypeForDelta,
                untilToday
            )
        }
    }

    private fun getRangeForWeek(
        minDate: Date,
        maxDate: Date,
        rangeTypeForRange: Int,
        rangeTypeForDelta: Int
    ): Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> {
        val ranges: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> =
            Pair(mutableListOf(), mutableListOf())
        val cal: Calendar = calendarStartOrEndOfDate(minDate)
        val startDay = cal.getActualMinimum(rangeTypeForRange)
        cal.set(rangeTypeForRange, startDay)
        var to = minDate
        var from = cal.time
        while (to < maxDate) {
            cal.add(Calendar.DAY_OF_YEAR, 6)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            to = cal.time
            val annotation = cal.get(rangeTypeForDelta).toFloat()
            val last = ranges.first.lastOrNull() ?: -1f
            if (annotation > last) {
                ranges.first.add(annotation)
            } else {
                ranges.first.add(last + 1f)
            }
            ranges.second.add(from..to)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            from = cal.time
        }
        return ranges
    }

    private fun getRangeForMonthAndYear(
        minDate: Date,
        maxDate: Date,
        rangeTypeForRange: Int,
        rangeTypeForDelta: Int,
        untilToday: Boolean
    ): Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> {
        val ranges: Pair<MutableList<Float>, MutableList<ClosedRange<Date>>> =
            Pair(mutableListOf(), mutableListOf())
        var date = minDate
        var to = minDate
        while (to < maxDate) {
            val cal: Calendar = calendarStartOrEndOfDate(date)
            val startDay = cal.getActualMinimum(rangeTypeForRange)
            cal.set(rangeTypeForRange, startDay)
            val from = cal.time
            val endDay = if (untilToday) {
                val calToday = Calendar.getInstance()
                calToday.get(rangeTypeForRange)
            } else {
                cal.getActualMaximum(rangeTypeForRange)
            }
            cal.set(rangeTypeForRange, endDay)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            to = cal.time
            val annotation = cal.get(rangeTypeForDelta).toFloat()
            val last = ranges.first.lastOrNull() ?: -1f
            if (annotation > last) {
                ranges.first.add(annotation)
            } else {
                ranges.first.add(last + 1f)
            }
            ranges.second.add(from..to)
            cal.add(rangeTypeForDelta, 1)
            date = cal.time
        }
        return ranges
    }

    companion object {
        @JvmField
        var topElevationStep = 500.0

        @JvmField
        var kilometersStep = 10.0

        @JvmField
        var elevationGainStep = 250.0
    }

}