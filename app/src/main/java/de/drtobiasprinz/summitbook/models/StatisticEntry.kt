package de.drtobiasprinz.summitbook.models

import java.lang.Math.round
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class StatisticEntry {
    private var totalActivities = 0
    private var totalSummits = 0
    private var visitedCountries = 0
    var totalHm = 0
    var totalKm = 0.0
    private var achievementHm = 0.0
    private var achievementKm = 0.0
    private var achievementActivity = 0.0
    var expectedAchievementActivityAbsolute = 0.0
    var expectedAchievementHmAbsolute = 0.0
    var expectedAchievementKmAbsolute = 0.0
    private var filteredSummitEntries: List<Summit>?
    private var activitiesPerYear = 50
    private var kilometerPerYear = 1200
    private var elevationGainPerYear = 50000
    private var indoorHeightMeterPercent = 0


    constructor(filteredSummitEntries: List<Summit>?, indoorHeightMeterPercent: Int = 0) {
        this.filteredSummitEntries = filteredSummitEntries
        this.indoorHeightMeterPercent = indoorHeightMeterPercent
    }

    constructor(filteredSummitEntries: List<Summit>?, activitiesPerYear: Int, kilometerPerYear: Int, elevationGainPerYear: Int, indoorHeightMeterPercent: Int = 0) {
        this.filteredSummitEntries = filteredSummitEntries
        this.elevationGainPerYear = elevationGainPerYear
        this.activitiesPerYear = activitiesPerYear
        this.kilometerPerYear = kilometerPerYear
        this.indoorHeightMeterPercent = indoorHeightMeterPercent
    }

    fun calculate() {
        totalActivities = filteredSummitEntries?.size ?: 0
        totalSummits = filteredSummitEntries?.filter { it.isPeak }?.size ?: 0
        visitedCountries = filteredSummitEntries?.flatMap { it.countries }?.toSet()?.filter { it != "" }?.size ?: 0
        totalHm = filteredSummitEntries?.map {
            if (it.sportType == SportType.IndoorTrainer) {
                it.elevationData.elevationGain * indoorHeightMeterPercent / 100
            } else {
                it.elevationData.elevationGain
            }
        }?.sum() ?: 0
        totalKm = filteredSummitEntries?.map { it.kilometers }?.sum() ?: 0.0
        achievementActivity = (totalSummits * 100.0 / activitiesPerYear).roundToInt().toDouble()
        achievementKm = totalKm * 100.0 / kilometerPerYear
        achievementHm = totalHm * 100.0 / elevationGainPerYear
        setExpectedAchievement()
    }

    fun setExpectedAchievement() {
        val now = Calendar.getInstance()
        val date = now.time
        val currentYear = now[Calendar.YEAR].toString()
        val df: DateFormat = SimpleDateFormat(Summit.DATE_FORMAT, Locale.ENGLISH)
        val beginOfYear: Date?
        try {
            beginOfYear = df.parse(String.format("%s-01-01", currentYear))
            val diff = date.time - beginOfYear.time
            expectedAchievementHmAbsolute = (elevationGainPerYear * TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 365.0)
            expectedAchievementKmAbsolute = (kilometerPerYear * TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 365.0)
            expectedAchievementActivityAbsolute = (activitiesPerYear * TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 365.0)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    fun getTotalActivities(): Int {
        return totalActivities
    }

    fun getTotalSummits(): Int {
        return totalSummits
    }

    fun getVisitedCountries(): Int {
        return visitedCountries
    }

    fun getAchievement(): Double {
        return achievementHm
    }

    fun getExpectedAchievementHmPercent(): Double {
        return expectedAchievementHmAbsolute / elevationGainPerYear * 100
    }
}