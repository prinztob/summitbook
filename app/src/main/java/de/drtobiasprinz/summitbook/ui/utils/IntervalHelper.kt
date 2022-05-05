package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.fragments.StatisticsFragment
import de.drtobiasprinz.summitbook.models.Summit
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.*
import kotlin.math.ceil

class IntervalHelper(private val summitEntries: List<Summit>) {
    private var selectedYear: String? = null
    private val allYears: List<String> = StatisticsFragment.getAllYears(summitEntries).sorted()
    private val extremaValuesSummits: ExtremaValuesSummits = ExtremaValuesSummits(summitEntries)
    var dates: MutableList<Date> = mutableListOf()
        private set
    var dateAnnotation: MutableList<Float> = mutableListOf()
        private set

    val topElevations: MutableList<Float> = mutableListOf()
    val topElevationAnnotation: MutableList<Float> = mutableListOf()
    val kilometers: MutableList<Float> = mutableListOf()
    val kilometerAnnotation: MutableList<Float> = mutableListOf()
    val elevationGains: MutableList<Float> = mutableListOf()
    val elevationGainAnnotation: MutableList<Float> = mutableListOf()
    var participants: MutableList<String> = mutableListOf()
    var participantsAnnotation: MutableList<Float> = mutableListOf()
    var equipments: MutableList<String> = mutableListOf()
    var equipmentsAnnotation: MutableList<Float> = mutableListOf()
    fun setSelectedYear(selectedYear: String?) {
        this.selectedYear = selectedYear
    }

    @Throws(ParseException::class)
    fun calculate() {
        dates = if (selectedYear == "") datesPerAllYear else monthPerYear
        calculateTopElevation()
        calculateElevationGain()
        calculateKilometers()
        calculateKParticipantsAndEquipments()
    }

    private fun calculateTopElevation() {
        if (topElevationAnnotation.isEmpty()) {
            val intervals = ceil(extremaValuesSummits.maxTopElevation / topElevationStep).toInt()
            for (i in 0..intervals) {
                topElevations.add((i * topElevationStep).toFloat())
                topElevationAnnotation.add(i.toFloat())
            }
        }
    }

    private fun calculateElevationGain() {
        if (elevationGains.isEmpty()) {
            val intervals = ceil(extremaValuesSummits.maxHeightMeters / elevationGainStep).toInt()
            for (i in 0..intervals) {
                elevationGains.add((i * elevationGainStep).toFloat())
                elevationGainAnnotation.add(i.toFloat())
            }
        }
    }

    private fun calculateKilometers() {
        if (kilometers.isEmpty()) {
            val intervals = ceil(extremaValuesSummits.maxKilometers / kilometersStep).toInt()
            for (i in 0..intervals) {
                kilometers.add((i * kilometersStep).toFloat())
                kilometerAnnotation.add(i.toFloat())
            }
        }
    }

    private fun calculateKParticipantsAndEquipments() {
        if (participants.isEmpty()) {
            val allParticipants = summitEntries.flatMap { it.participants }.filter { it != "" }
            val countsPerParticipants = allParticipants.toSet().map { name ->
                name to allParticipants.filter { it == name }.count()
            }
            participants = countsPerParticipants.toList().sortedByDescending { (_, value) -> value }.take(12).toMap().map { (key, _) -> key } as MutableList<String>
            participants.add("")
            participantsAnnotation = (0 until participants.size).map { it.toFloat() } as MutableList<Float>
        }
        if (equipments.isEmpty()) {
            val allEquipments = summitEntries.flatMap { it.equipments }.filter { it != "" }
            val countsPerEquipments = allEquipments.toSet().map { name ->
                name to allEquipments.filter { it == name }.count()
            }
            equipments = countsPerEquipments.toList().sortedByDescending { (_, value) -> value }.take(12).toMap().map { (key, _) -> key } as MutableList<String>
            equipments.add("")
            equipmentsAnnotation = (0 until equipments.size).map { it.toFloat() } as MutableList<Float>
        }
    }

    @get:Throws(ParseException::class)
    private val datesPerAllYear: MutableList<Date>
        get() {
            dateAnnotation = mutableListOf()
            val size = allYears.size
            val dates = mutableListOf<Date>()
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

    private fun addDate(df: DateFormat, year: String, dates: MutableList<Date>) {
        val addDate = df.parse(year)
        if (addDate != null) {
            dates.add(addDate)
        }
    }

    @get:Throws(ParseException::class)
    private val monthPerYear: MutableList<Date>
        get() {
            dateAnnotation = mutableListOf()
            val size = 12
            val dates = mutableListOf<Date>()
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