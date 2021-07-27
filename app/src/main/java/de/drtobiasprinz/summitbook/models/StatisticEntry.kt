package de.drtobiasprinz.summitbook.models

import java.lang.Math.round
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class StatisticEntry {
    private var totalSummits = 0
    var totalHm = 0
    var totalKm = 0.0
    var achievementHm = 0.0
    var achievementKm = 0.0
    var achievementActivity = 0.0
    var epectedAchievementActivityAbsolute = 0.0
    var epectedAchievementHmAbsolute = 0.0
    var epectedAchievementKmAbsolute = 0.0
    private var filteredSummitEntries: ArrayList<SummitEntry>?
    private var activitesPerYear = 50
    private var kilometerPerYear = 1200
    private var elevationGainPerYear = 50000

    constructor(filteredSummitEntries: ArrayList<SummitEntry>?) {
        this.filteredSummitEntries = filteredSummitEntries
    }

    constructor(filteredSummitEntries: ArrayList<SummitEntry>?, activitesPerYear: Int, kilometerPerYear: Int, elevationGainPerYear: Int) {
        this.filteredSummitEntries = filteredSummitEntries
        this.elevationGainPerYear = elevationGainPerYear
        this.activitesPerYear = activitesPerYear
        this.kilometerPerYear = kilometerPerYear
    }

    fun calculate() {
        totalSummits = filteredSummitEntries?.size ?: 0
        totalHm = filteredSummitEntries?.stream()?.mapToInt { obj: SummitEntry? -> obj?.elevationData?.elevationGain ?: 0 }?.sum() ?: 0
        totalKm = filteredSummitEntries?.stream()?.mapToDouble { obj: SummitEntry? -> obj?.kilometers ?: 0.0 }?.sum() ?: 0.0
        achievementActivity = round(totalSummits * 100.0 / activitesPerYear).toDouble()
        achievementKm = totalKm * 100.0 / kilometerPerYear
        achievementHm = totalHm * 100.0 / elevationGainPerYear
        setExpectedAchievement()
    }

    fun setExpectedAchievement() {
        val now = Calendar.getInstance()
        val date = now.time
        val currentYear = now[Calendar.YEAR].toString()
        val df: DateFormat = SimpleDateFormat(SummitEntry.DATE_FORMAT, Locale.ENGLISH)
        val beginOfYear: Date?
        try {
            beginOfYear = df.parse(String.format("%s-01-01", currentYear))
            val diff = date.time - beginOfYear.time
            epectedAchievementHmAbsolute = (elevationGainPerYear * TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 365.0)
            epectedAchievementKmAbsolute = (kilometerPerYear * TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 365.0)
            epectedAchievementActivityAbsolute = (activitesPerYear * TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) / 365.0)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    fun getTotalSummits(): Int {
        return totalSummits
    }

    fun getAchievement(): Double {
        return achievementHm
    }

    fun getExpectedAchievementHmPercent(): Double {
        return epectedAchievementHmAbsolute / elevationGainPerYear * 100
    }
}