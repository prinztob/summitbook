package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper
import java.util.Calendar
import java.util.Date
import java.util.stream.Stream

@Suppress("UNCHECKED_CAST")
enum class BarChartXAxisSelector(
    val nameId: Int,
    val unitId: Int,
    val stepsSize: Double,
    val isAQuality: Boolean,
    val maxVisibilityRangeForBarChart: Float,
    val getStream: (entries: List<Summit>?, range: Any, calender: Calendar, month: Int) -> Stream<Summit?>?,
    val getRangeAndAnnotation: (IntervalHelper) -> Pair<List<Float>, List<Any>>,
) {
    DateByMonth(R.string.monthly, R.string.empty, 0.0, false, 25f, { entries, range, _, _ ->
        entries
            ?.stream()
            ?.filter { o: Summit ->
                o.date in (range as ClosedRange<Date>)
            }
    }, { e -> e.dateByMonthRangeAndAnnotation }),
    DateByYear(R.string.yearly, R.string.empty, 0.0, false, 12f, { entries, range, cal, month ->
        entries
            ?.stream()
            ?.filter { o: Summit ->
                cal.time = o.date
                o.date in (range as ClosedRange<Date>) && (month == 0 || cal.get(Calendar.MONTH) + 1 == month)
            }
    }, { e -> e.dateByYearRangeAndAnnotation }),
    DateByYearUntilToday(
        R.string.yearly_until_today,
        R.string.empty,
        0.0,
        false,
        12f,
        { entries, range, _, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit ->
                    o.date in (range as ClosedRange<Date>)
                }
        },
        { e -> e.dateByYearUntilTodayRangeAndAnnotation }),
    DateByWeek(R.string.wekly, R.string.empty, 0.0, false, 26f, { entries, range, _, _ ->
        entries
            ?.stream()
            ?.filter { o: Summit ->
                o.date in (range as ClosedRange<Date>)
            }
    }, { e -> e.dateByWeekRangeAndAnnotation }),
    DateByQuarter(
        R.string.quarterly,
        R.string.empty,
        0.0,
        false,
        4f,
        { entries, range, _, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit ->
                    o.date in (range as ClosedRange<Date>)
                }
        },
        { e -> e.dateByQuarterRangeAndAnnotation }),
    Kilometers(
        R.string.kilometers_hint,
        R.string.km,
        IntervalHelper.kilometersStep,
        false,
        -1f,
        { entries, range, _, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.kilometers.toFloat() in (range as ClosedRange<Float>) }
        },
        { e -> e.kilometersRangeAndAnnotation }),
    ElevationGain(
        R.string.height_meter_hint,
        R.string.hm,
        IntervalHelper.elevationGainStep,
        false,
        -1f,
        { entries, range, _, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.elevationData.elevationGain.toFloat() in range as ClosedRange<Float> }
        },
        { e -> e.elevationGainRangeAndAnnotation }),
    TopElevation(
        R.string.top_elevation_hint,
        R.string.masl,
        IntervalHelper.topElevationStep,
        false,
        -1f,
        { entries, range, _, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.elevationData.maxElevation.toFloat() in (range as ClosedRange<Float>) }
        },
        { e -> e.topElevationRangeAndAnnotation }),
    Participants(
        R.string.participants,
        R.string.empty,
        1.0,
        true,
        12f,
        { entries, range, _, _ ->
            entries
                ?.stream()
                ?.filter { o: Summit? -> o != null && o.participants.contains(range) }
        },
        { e -> e.participantsRangeAndAnnotationForSummitChipValues }),
    Equipments(R.string.equipments, R.string.empty, 1.0, true, 12f, { entries, range, _, _ ->
        entries
            ?.stream()
            ?.filter { o: Summit? -> o != null && o.equipments.contains(range) }
    }, { e -> e.equipmentsRangeAndAnnotationForSummitChipValues }),

    Countries(R.string.country_hint, R.string.empty, 1.0, true, 12f, { entries, range, _, _ ->
        entries
            ?.stream()
            ?.filter { o: Summit? -> o != null && o.countries.contains(range) }
    }, { e -> e.countriesRangeAndAnnotationForSummitChipValues })
}