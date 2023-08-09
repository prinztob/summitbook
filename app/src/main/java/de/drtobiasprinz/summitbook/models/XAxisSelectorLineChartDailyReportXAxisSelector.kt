package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.DailyReportData
import de.drtobiasprinz.summitbook.fragments.LineChartDailyReportData
import de.drtobiasprinz.summitbook.ui.utils.IntervalHelper

enum class XAxisSelectorLineChartDailyReportXAxisSelector(
    val nameId: Int,
    val filterData: (IntervalHelper, List<DailyReportData>) -> List<DailyReportData>?
) {
    Days7(R.string.seven_days, { _, dailyReportData ->
        dailyReportData.sortedBy { it.date }.takeLast(7)
    }),
    Days28(R.string.twenty_eight_days, { _, dailyReportData ->
        dailyReportData.sortedBy { it.date }.takeLast(28)
    }),
    Days(R.string.days, { _, dailyReportData ->
        dailyReportData.sortedBy { it.date }
    }),
    Weeks12(R.string.twelf_weeks, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerWeek(
            dailyReportData,
            intervalHelper
        ).takeLast(12)
    }),
    Weeks(R.string.weeks, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerWeek(
            dailyReportData,
            intervalHelper
        )
    }),
    Months(R.string.months, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerMonth(
            dailyReportData,
            intervalHelper
        )
    }),
    Quarterly(R.string.quarterly, { intervalHelper, dailyReportData ->
        LineChartDailyReportData.filterDailyReportDataPerQuarter(
            dailyReportData,
            intervalHelper
        )
    });
}