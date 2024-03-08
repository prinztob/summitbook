package de.drtobiasprinz.summitbook.ui

import android.util.Log
import com.github.mikephil.charting.data.Entry
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PerformanceGraphProvider(val summits: List<Summit>, val forecasts: List<Forecast>) {

    fun getActualGraphForSummits(
        graphType: GraphType,
        year: String,
        month: String? = null,
        currentDate: Date? = null
    ): List<Entry> {
        var (dateRange, maximum) = getDateRange(year, month)
        val filteredSummits = getRelevantSummitsSorted(dateRange, graphType)
        if (filteredSummits.isNotEmpty()) {
            val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
            var lastY = 0f
            val basicGraph: MutableMap<Int, Float> = mutableMapOf()
            basicGraph[0] = 0f
            filteredSummits.forEach {
                cal.time = it.date
                val x =
                    (cal.get(if (month != null) Calendar.DAY_OF_MONTH else Calendar.DAY_OF_YEAR))
                val newValue = graphType.getSummitValue(it).toFloat()
                val checkValue = if (graphType.filterZeroValues) newValue > 0 else newValue >= 0
                if (checkValue) {
                    basicGraph[x] = newValue + (if (graphType.cumulative) lastY else 0f)
                    lastY = basicGraph[x]!!
                }
            }

            if (currentDate != null) {
                cal.time = currentDate
                val dayNumber =
                    cal.get(if (month != null) Calendar.DAY_OF_MONTH else Calendar.DAY_OF_YEAR)
                if (dayNumber < maximum) {
                    maximum = dayNumber
                }
            }

            lastY = 0f
            return (0 until maximum).map {
                lastY = basicGraph[it + 1] ?: lastY
                Entry((it + 1).toFloat(), lastY)
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
        month: String? = null
    ): Pair<List<Entry>, List<Entry>> {
        val start = System.currentTimeMillis()
        val graphs = (year.toInt() - 5 until year.toInt()).map {
            getActualGraphForSummits(
                graphType,
                it.toString(),
                month
            )
        }.filter { it.isNotEmpty() }
        val size = graphs.minOfOrNull { it.size }
        if (size == null || size == 0) {
            return Pair(emptyList(), emptyList())
        }
        val minGraph = graphs[0].subList(0, size).map { Entry(it.x, Float.MAX_VALUE) }
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
        Log.i(
            "PerformanceGraphProvider.getActualGraphMinMaxForSummits",
            "took ${(System.currentTimeMillis() - start)}"
        )
        return Pair(minGraph, maxGraph)
    }

    fun getRelevantSummitsSorted(
        range: ClosedRange<Date>,
        graphType: GraphType = GraphType.ElevationGain
    ): List<Summit> {
        return summits.filter {
            it.date.after(range.start)
                    && it.date.before(range.endInclusive)
                    && graphType.getSummitValue(it) >= 0
        }.sortedBy { it.date }
    }

    fun getDateRange(
        year: String,
        month: String? = null
    ): Pair<ClosedRange<Date>, Int> {
        val startDate = parseDate(String.format("${year}-${month ?: "01"}-01 00:00:00"))
        val cal: Calendar = Calendar.getInstance(TimeZone.getDefault())
        cal.time = startDate
        val maximum: Int
        if (month == null) {
            maximum = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
            cal.set(Calendar.DAY_OF_YEAR, maximum)
        } else {
            maximum = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maximum)
        }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 0)
        return Pair(startDate..cal.time, maximum)
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
    val cumulative: Boolean = true,
    val hasForecast: Boolean = true,
    val filterZeroValues: Boolean = false,
    val delta: Float = 1f
) {

    Count(
        "",
        { 1.0 },
        { f -> f.forecastNumberActivities },
    ),
    ElevationGain(
        "hm",
        { e -> e.elevationData.elevationGain.toDouble() },
        { f -> f.forecastHeightMeter },
        delta = 10f
    ),
    Kilometer(
        "km",
        { e -> e.kilometers },
        { f -> f.forecastDistance },
    ),
    Power(
        "W",
        { e -> e.garminData?.power?.twentyMin?.toDouble() ?: -1.0 },
        { 0 },
        false, false, true
    ),
    Vo2Max(
        "",
        { e -> e.garminData?.vo2max?.toDouble() ?: -1.0 },
        { 0 },
        false, false, true
    ), ;
}