package de.drtobiasprinz.summitbook.ui

import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PerformanceGraphProvider(val summits: List<Summit>, val forecasts: List<Forecast>) {

    fun getActualGraphForSummits(
        graphType: GraphType,
        year: String,
        month: String? = null,
        currentDate: Date = Date()
    ): List<Entry> {
        val dateRange = getDateRange(year, month, currentDate)
        val dateFormat: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
        val filteredSummits = getRelevantSummits(dateRange)
        if (filteredSummits.isNotEmpty()) {
            var lastY = 0f
            return getDatesBetween(dateRange.start, dateRange.endInclusive).mapIndexed { n, date ->
                val summitsForDate =
                    filteredSummits.filter {
                        it.getDateAsString() == dateFormat.format(
                            date ?: Date()
                        )
                    }
                val y = summitsForDate.sumOf { graphType.getSummitValue(it) }.toFloat() + lastY
                lastY = y
                Entry((n + 1).toFloat(), y)
            }
        } else {
            return emptyList()
        }
    }

    fun getForecastGraphForSummits(
        graphType: GraphType,
        year: String,
        month: String? = null
    ): List<Entry> {
        val graph = mutableListOf(Entry(1f, 0f))
        val months = if (month != null) listOf(month) else listOf(
            "01",
            "02",
            "03",
            "04",
            "05",
            "06",
            "07",
            "08",
            "09",
            "10",
            "11",
            "12"
        )
        months.forEach { selectedMonth ->
            val startDate = parseDate(String.format("${year}-${selectedMonth}-01 00:00:00"))
            val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
            cal.time = startDate
            val lastX = if (graph.size == 1) 0f else graph.last().x
            graph.add(
                Entry(
                    cal.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat() + lastX,
                    forecasts
                        .filter { it.year == year.toInt() && it.month == selectedMonth.toInt() }
                        .sumOf { graphType.getForecastValue(it) }.toFloat() + graph.last().y
                )
            )
        }
        return graph
    }

    fun getActualGraphMinMaxForSummits(
        graphType: GraphType,
        year: String,
        month: String? = null,
        currentDate: Date = Date()
    ): Pair<List<Entry>, List<Entry>> {
        val graphs = (year.toInt() - 5 until year.toInt()).map {
            getActualGraphForSummits(
                graphType,
                it.toString(),
                month,
                currentDate
            )
        }
        val size = graphs.minOfOrNull { it.size }
        if (size == null || size == 0) {
            return Pair(emptyList(), emptyList())
        }
        val minGraph = graphs[0].subList(0, size).map { Entry(it.x, 10000f) }
        val maxGraph = graphs[0].subList(0, size).map { Entry(it.x, 0f) }
        graphs.forEach {
            it.forEachIndexed { index, entry ->
                if (index < size) {
                    if (entry.y < minGraph[index].y) {
                        minGraph[index].y = entry.y
                    }
                    if (entry.y > maxGraph[index].y) {
                        maxGraph[index].y = entry.y
                    }
                }
            }
        }
        return Pair(minGraph, maxGraph)
    }

    fun getRelevantSummits(range: ClosedRange<Date>): List<Summit> {
        return summits.filter {
            it.date.after(range.start) && it.date.before(range.endInclusive)
        }
    }

    fun getDateRange(
        year: String,
        month: String? = null,
        currentDate: Date = Date()
    ): ClosedRange<Date> {
        val startDate = parseDate(String.format("${year}-${month ?: "01"}-01 00:00:00"))
        val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
        cal.time = startDate
        if (month == null) {
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
        } else {
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 0)
        var endDate = cal.time
        if (endDate.after(currentDate)) {
            endDate = currentDate
        }
        return startDate..endDate
    }

    private fun getDatesBetween(startDate: Date, endDate: Date): List<Date?> {
        var start = startDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val end = endDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val totalDates: MutableList<Date> = ArrayList()
        while (!start.isAfter(end)) {
            totalDates.add(Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            start = start.plusDays(1)
        }
        return totalDates
    }

    companion object {
        fun parseDate(date: String): Date {
            val df: DateFormat = SimpleDateFormat(Summit.DATETIME_FORMAT, Locale.getDefault())
            df.isLenient = false
            return df.parse(date) ?: Date()
        }
    }
}

enum class GraphType(
    val unit: String,
    val getSummitValue: (Summit) -> Double,
    val getForecastValue: (Forecast) -> Int,
) {
    Count(
        "",
        { _ -> 1.0 },
        { f -> f.forecastNumberActivities },
    ),
    ElevationGain(
        "hm",
        { e -> e.elevationData.elevationGain.toDouble() },
        { f -> f.forecastHeightMeter },
    ),
    Kilometer(
        "km",
        { e -> e.kilometers },
        { f -> f.forecastDistance },
    ),
}