package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.fragments.StatisticsFragment
import de.drtobiasprinz.summitbook.models.Summit
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

class IntervalHelper(summitEntries: List<Summit>?) {
    private var selectedYear: String? = null
    private val allYears: ArrayList<String> = StatisticsFragment.getAllYears(summitEntries)
    private val extremaValuesSummits: ExtremaValuesSummits? = summitEntries?.let { ExtremaValuesSummits(it) }
    var dates: ArrayList<Date> = ArrayList()
        private set
    var dateAnnotation = ArrayList<Float>()
        private set
    val topElevations = ArrayList<Float>()
    val topElevationAnnotation = ArrayList<Float>()
    val kilometers = ArrayList<Float>()
    val kilometerAnnotation = ArrayList<Float>()
    val elevationGains = ArrayList<Float>()
    val elevationGainAnnotation = ArrayList<Float>()
    fun setSelectedYear(selectedYear: String?) {
        this.selectedYear = selectedYear
    }

    @Throws(ParseException::class)
    fun calculate() {
        dates = if (selectedYear == "") datesPerAllYear else monthPerYear
        calculateTopElevation()
        calculateElevationGain()
        calculateKilometers()
    }

    private fun calculateTopElevation() {
        if (topElevationAnnotation.size == 0) {
            val intervals = ceil((extremaValuesSummits?.maxTopElevation ?: 0) / topElevationStep).toInt()
            for (i in 0..intervals) {
                topElevations.add((i * topElevationStep).toFloat())
                topElevationAnnotation.add(i.toFloat())
            }
        }
    }

    private fun calculateElevationGain() {
        if (elevationGains.size == 0) {
            val intervals = ceil((extremaValuesSummits?.maxHeightMeters ?: 0) / elevationGainStep).toInt()
            for (i in 0..intervals) {
                elevationGains.add((i * elevationGainStep).toFloat())
                elevationGainAnnotation.add(i.toFloat())
            }
        }
    }

    private fun calculateKilometers() {
        if (kilometers.size == 0) {
            val intervals = ceil((extremaValuesSummits?.maxKilometers ?: 0.0) / kilometersStep).toInt()
            for (i in 0..intervals) {
                kilometers.add((i * kilometersStep).toFloat())
                kilometerAnnotation.add(i.toFloat())
            }
        }
    }

    @get:Throws(ParseException::class)
    private val datesPerAllYear: ArrayList<Date>
        get() {
            dateAnnotation = ArrayList()
            val size = allYears.size
            allYears.sort()
            val dates = ArrayList<Date>(size + 1)
            val df: DateFormat = SimpleDateFormat(Summit.DATETIME_FORMAT, Locale.ENGLISH)
            for (i in 0 until size) {
                allYears[i].toFloat().let { dateAnnotation.add(it) }
            }
            for (i in allYears.indices) {
                addDate(df, String.format("%s-01-01 00:00:00", allYears[i]), dates)
            }
            addDate(df, String.format("%s-01-01 00:00:00", (allYears[size - 1].toInt()) + 1), dates)
            return dates
        }

    private fun addDate(df: DateFormat, year: String, dates: ArrayList<Date>) {
        val addDate = df.parse(year)
        if (addDate != null) {
            dates.add(addDate)
        }
    }

    @get:Throws(ParseException::class)
    private val monthPerYear: ArrayList<Date>
        get() {
            dateAnnotation = ArrayList()
            val size = 12
            val dates = ArrayList<Date>(size + 1)
            val df: DateFormat = SimpleDateFormat(Summit.DATETIME_FORMAT, Locale.ENGLISH)
            for (i in 0 until size) {
                dateAnnotation.add((i + 1).toFloat())
            }
            for (i in 1..size) {
                addDate(df, String.format("%s-%s-01 00:00:00", selectedYear, i), dates)
            }
            addDate(df, String.format("%s-01-01 00:00:00", (selectedYear?.toInt() ?: 0) + 1), dates)
            return dates
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