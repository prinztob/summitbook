package de.drtobiasprinz.summitbook.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SelectOnOsMapActivity
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.models.SummitEntry
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.ui.utils.SortFilterHelper
import java.util.*
import kotlin.math.roundToLong

class StatisticsFragment(private val sortFilterHelper: SortFilterHelper) : Fragment(), SummationFragment {
    private var summitEntries: ArrayList<SummitEntry>? = null
    private var filteredEntries: ArrayList<SummitEntry>? = null
    private lateinit var textTotalSummits: TextView
    private lateinit var textTotalKm: TextView
    private lateinit var textTotalHm: TextView
    private lateinit var textTotalHmInfo: TextView
    private lateinit var textAchievement: TextView
    private lateinit var statisticEntry: StatisticEntry
    private lateinit var statisticFragmentView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        sortFilterHelper.setFragment(this)
        statisticFragmentView = inflater.inflate(R.layout.fragment_statistics, container, false)
        setHasOptionsMenu(true)
        textTotalSummits = statisticFragmentView.findViewById(R.id.textTotalSummits)
        textTotalKm = statisticFragmentView.findViewById(R.id.textTotalKm)
        textTotalHm = statisticFragmentView.findViewById(R.id.textTotalHm)
        textTotalHmInfo = statisticFragmentView.findViewById(R.id.textTotalHmInfo)
        textAchievement = statisticFragmentView.findViewById(R.id.textAchievement)
        summitEntries = sortFilterHelper.entries
        filteredEntries = sortFilterHelper.filteredEntries
        update(filteredEntries)
        return statisticFragmentView
    }

    private fun setTextViews(statisticEntry: StatisticEntry, extremaValuesSummits: ExtremaValuesSummits?) {
        if (statisticEntry.getTotalSummits() > 0) {
            textTotalSummits.text = String.format("%s", statisticEntry.getTotalSummits())
            textTotalKm.text = String.format(Locale.ENGLISH, "%.1f km", statisticEntry.totalKm)
            textTotalHm.text = String.format(Locale.ENGLISH, "%s hm", statisticEntry.totalHm)
            if (sortFilterHelper.selectedYear == getCurrentYear()) {
                textTotalHmInfo.visibility = View.VISIBLE
                textTotalHmInfo.text = getString(R.string.current_estimate, statisticEntry.epectedAchievementHmAbsolute.toInt().toString())
            } else {
                textTotalHmInfo.visibility = View.GONE
            }
            if (sortFilterHelper.selectedYear != "") {
                statisticFragmentView.findViewById<View?>(R.id.achievementInfo)?.visibility = View.VISIBLE
                textAchievement.text = String.format(Locale.ENGLISH, "%.1f %%", statisticEntry.getAchievement())
            } else {
                statisticFragmentView.findViewById<View?>(R.id.achievementInfo)?.visibility = View.GONE
            }
            setTextViewData(extremaValuesSummits?.kilometersMinMax?.second, R.id.layoutLongestDistance, R.id.textLongestDistance, R.id.textLongestDistanceInfo, "km",
                    getValueOrNull(extremaValuesSummits?.kilometersMinMax?.second) { e -> e.kilometers}, 1)
            setTextViewData(extremaValuesSummits?.averageSpeedMinMax?.second, R.id.layoutHighestAverageSpeed, R.id.textHeighestAverageSpeed, R.id.textHeighestAverageSpeedInfo, "km/h",
                    getValueOrNull(extremaValuesSummits?.averageSpeedMinMax?.second) { e -> e.pace}, 1)
            setTextViewData(extremaValuesSummits?.durationMinMax?.second, R.id.layoutLongestDuration, R.id.textLongestDuration, R.id.textLongestDurationInfo, "h",
                    getValueOrNull(extremaValuesSummits?.durationMinMax?.second) { e -> e.duration}, 1)
            setTextViewData(extremaValuesSummits?.topSpeedMinMax?.second, R.id.layoutTopSpeed, R.id.textTopSpeed, R.id.textTopSpeedInfo, "km/h",
                    getValueOrNull(extremaValuesSummits?.topSpeedMinMax?.second) { e -> e.topSpeed}, 1)
            setTextViewData(extremaValuesSummits?.heightMetersMinMax?.second, R.id.layoutMostHeightMeter, R.id.textMostHeightMeter, R.id.textMostHeightMeterInfo, "m",
                    getValueOrNull(extremaValuesSummits?.heightMetersMinMax?.second) { e -> e.heightMeter.toDouble()})
            setTextViewData(extremaValuesSummits?.topElevationMinMax?.second, R.id.layoutHighestPeak, R.id.textHighestPeak, R.id.textHighestPeakInfo, "m.a.s.l.",
                    getValueOrNull(extremaValuesSummits?.topElevationMinMax?.second) { e -> e.topElevation.toDouble()})
            setTextViewData(extremaValuesSummits?.normPowerMinMax?.second, R.id.layoutHighestPower, R.id.textHeighestPower, R.id.textHeighestPowerInfo, "W",
                    getValueOrNull(extremaValuesSummits?.normPowerMinMax?.second) { e -> e.activityData?.power?.normPower?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power1sMinMax?.second, R.id.layoutHighestPower1sec, R.id.textHeighestPower1sec, R.id.textHeighestPower1secInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power1sMinMax?.second) { e -> e.activityData?.power?.oneSec?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power2sMinMax?.second, R.id.layoutHighestPower2sec, R.id.textHeighestPower2sec, R.id.textHeighestPower2secInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power2sMinMax?.second) { e -> e.activityData?.power?.twoSec?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power5sMinMax?.second, R.id.layoutHighestPower5sec, R.id.textHeighestPower5sec, R.id.textHeighestPower5secInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power5sMinMax?.second) { e -> e.activityData?.power?.fiveSec?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power10sMinMax?.second, R.id.layoutHighestPower10sec, R.id.textHeighestPower10sec, R.id.textHeighestPower10secInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power10sMinMax?.second) { e -> e.activityData?.power?.tenSec?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power20sMinMax?.second, R.id.layoutHighestPower20sec, R.id.textHeighestPower20sec, R.id.textHeighestPower20secInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power20sMinMax?.second) { e -> e.activityData?.power?.twentySec?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power30sMinMax?.second, R.id.layoutHighestPower30sec, R.id.textHeighestPower30sec, R.id.textHeighestPower30secInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power30sMinMax?.second) { e -> e.activityData?.power?.thirtySec?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power1minMinMax?.second, R.id.layoutHighestPower1min, R.id.textHeighestPower1min, R.id.textHeighestPower1minInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power1minMinMax?.second) { e -> e.activityData?.power?.oneMin?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power2minMinMax?.second, R.id.layoutHighestPower2min, R.id.textHeighestPower2min, R.id.textHeighestPower2minInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power2minMinMax?.second) { e -> e.activityData?.power?.twoMin?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power5minMinMax?.second, R.id.layoutHighestPower5min, R.id.textHeighestPower5min, R.id.textHeighestPower5minInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power5minMinMax?.second) { e -> e.activityData?.power?.fiveMin?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power10minMinMax?.second, R.id.layoutHighestPower10min, R.id.textHeighestPower10min, R.id.textHeighestPower10minInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power10minMinMax?.second) { e -> e.activityData?.power?.tenMin?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power20minMinMax?.second, R.id.layoutHighestPower20min, R.id.textHeighestPower20min, R.id.textHeighestPower20minInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power20minMinMax?.second) { e -> e.activityData?.power?.twentyMin?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power30minMinMax?.second, R.id.layoutHighestPower30min, R.id.textHeighestPower30min, R.id.textHeighestPower30minInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power30minMinMax?.second) { e -> e.activityData?.power?.thirtyMin?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power1hMinMax?.second, R.id.layoutHighestPower1h, R.id.textHeighestPower1h, R.id.textHeighestPower1hInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power1hMinMax?.second) { e -> e.activityData?.power?.oneHour?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power2hMinMax?.second, R.id.layoutHighestPower2h, R.id.textHeighestPower2h, R.id.textHeighestPower2hInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power2hMinMax?.second) { e -> e.activityData?.power?.twoHours?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.power5hMinMax?.second, R.id.layoutHighestPower5h, R.id.textHeighestPower5h, R.id.textHeighestPower5hInfo, "W",
                    getValueOrNull(extremaValuesSummits?.power5hMinMax?.second) { e -> e.activityData?.power?.fiveHours?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.averageHRMinMax?.second, R.id.layoutHighestAverageHR, R.id.textHeighestAverageHR, R.id.textHeighestAverageHRInfo, "bpm",
                    getValueOrNull(extremaValuesSummits?.averageHRMinMax?.second) { e -> e.activityData?.averageHR?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.flowMinMax?.second, R.id.layoutHighestFlow, R.id.textHeighestFlow, R.id.textHeighestFlowInfo, "",
                    getValueOrNull(extremaValuesSummits?.flowMinMax?.second) { e -> e.activityData?.flow?.toDouble() ?: 0.0}, 1)
            setTextViewData(extremaValuesSummits?.gritMinMax?.second, R.id.layoutHighestGrit, R.id.textHeighestGrit, R.id.textHeighestGritInfo, "",
                    getValueOrNull(extremaValuesSummits?.gritMinMax?.second) { e -> e.activityData?.grit?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.trainingsLoadMinMax?.second, R.id.layoutHighestTrainingLoad, R.id.textHeighestTrainingLoad, R.id.textHeighestTrainingLoadInfo, "",
                    getValueOrNull(extremaValuesSummits?.trainingsLoadMinMax?.second) { e -> e.activityData?.trainingLoad?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.ftpMinMax?.second, R.id.layoutHighestFTP, R.id.textHeighestFTP, R.id.textHeighestFTPInfo, "",
                    getValueOrNull(extremaValuesSummits?.ftpMinMax?.second) { e -> e.activityData?.ftp?.toDouble() ?: 0.0})
            setTextViewData(extremaValuesSummits?.vo2maxMinMax?.second, R.id.layoutHighestVO2MAX, R.id.textHeighestVO2MAX, R.id.textHeighestVO2MAXInfo, "",
                    getValueOrNull(extremaValuesSummits?.vo2maxMinMax?.second) { e -> e.activityData?.vo2max?.toDouble() ?: 0.0})
        }
    }

    private fun getValueOrNull(entry: SummitEntry?, f: (SummitEntry) -> Double): Double {
        return if (entry != null) f(entry) else 0.0
    }

    private fun setTextViewData(entry: SummitEntry?, layoutId: Int, dataId: Int, infoId: Int, unit: String?, value: Double, digits:Int=0) {
        val layout = statisticFragmentView.findViewById<LinearLayout?>(layoutId)
        val data = statisticFragmentView.findViewById<TextView?>(dataId)
        val info = statisticFragmentView.findViewById<TextView?>(infoId)
        if (entry != null) {
            layout?.setOnClickListener { v: View ->
                val context = v.context
                val intent = Intent(context, SummitEntryDetailsActivity::class.java)
                intent.putExtra(SelectOnOsMapActivity.SUMMIT_ID_EXTRA_IDENTIFIER, entry._id)
                context.startActivity(intent)
            }
            layout?.visibility = View.VISIBLE
            if (digits > 0) {
                data?.text = String.format(Locale.ENGLISH, "%."+digits+"f %s", value, unit)
            } else {
                data?.text = String.format(Locale.ENGLISH, "%s %s", value.roundToLong(), unit)
            }
            info?.text = String.format(Locale.ENGLISH, "%s: %s\n%s: %s", getString(R.string.name), entry.name, getString(R.string.tour_date), entry.getDateAsString())
        } else {
            layout?.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_sort) {
            sortFilterHelper.setFragment(this)
            summitEntries?.let { sortFilterHelper.setAllEntries(it) }
            sortFilterHelper.showDialog()
            sortFilterHelper.apply()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun update(filteredSummitEntries: ArrayList<SummitEntry>?) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val annualTargetActivity = sharedPreferences.getString("annual_target_activities", "52")?.toInt() ?: 52
        val annualTargetKm = sharedPreferences.getString("annual_target_km", "1200")?.toInt() ?: 1200
        val annualTargetHm = sharedPreferences.getString("annual_target", "50000")?.toInt() ?: 50000
        statisticEntry = StatisticEntry(filteredSummitEntries, annualTargetActivity, annualTargetKm, annualTargetHm)
        statisticEntry.calculate()
        setProgressBar()
        val extremaValuesSummits = filteredSummitEntries?.let { ExtremaValuesSummits(it) }
        setTextViews(statisticEntry, extremaValuesSummits)
    }

    private fun setProgressBar() {
        val simpleProgressBar = statisticFragmentView.findViewById<ProgressBar?>(R.id.vprogressbar)
        if (sortFilterHelper.selectedYear != "") {
            val expectedAchievement = if (sortFilterHelper.selectedYear == getCurrentYear()) statisticEntry.getExpectedAchievementHmPercent()  else 100.0
            simpleProgressBar?.visibility = View.VISIBLE
            simpleProgressBar?.max = 100
            simpleProgressBar?.progress = statisticEntry.getAchievement().toInt()
            if (expectedAchievement < statisticEntry.getAchievement()) {
                simpleProgressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)
            }
            simpleProgressBar?.secondaryProgress = expectedAchievement.toInt()
        } else {
            simpleProgressBar?.visibility = View.GONE
        }
    }


    companion object {
        fun getAllYears(entries: ArrayList<SummitEntry>?): ArrayList<String> {
            val years = ArrayList<String>()
            if (entries != null) {
                for (entry in entries) {
                    years.add(entry.getDateAsString()?.substring(0, 4) ?: "1900")
                }
            }
            val uniqueYears = ArrayList(HashSet(years))
            Collections.sort(uniqueYears, Collections.reverseOrder())
            return uniqueYears
        }

        private fun getCurrentYear(): String {
            val now = Calendar.getInstance()
            return now[Calendar.YEAR].toString()
        }
    }

}