package de.drtobiasprinz.summitbook.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.models.Forecast
import de.drtobiasprinz.summitbook.models.FragmentResultReceiver
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.models.Summit
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import java.util.*
import kotlin.math.roundToLong

class StatisticsFragment : Fragment(), SummationFragment {
    private var summitEntries: List<Summit>? = null
    private var filteredEntries: List<Summit>? = null
    private var textTotalSummits: TextView? = null
    private var textTotalKm: TextView? = null
    private var textTotalHm: TextView? = null
    private var textTotalHmInfo: TextView? = null
    private var textTotalHmForecastInfo: TextView? = null
    private var textAchievement: TextView? = null
    private lateinit var statisticEntry: StatisticEntry
    private var statisticFragmentView: View? = null
    private var annualTargetActivity: String = ""
    private var annualTargetKm: String = ""
    private var annualTargetHm: String = ""
    private var indoorHeightMeterPercent: Int = 0
    private lateinit var resultReceiver: FragmentResultReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultReceiver = context as FragmentResultReceiver
        annualTargetActivity = resultReceiver.getSharedPreference().getString("annual_target_activities", "52") ?: "52"
        annualTargetKm = resultReceiver.getSharedPreference().getString("annual_target_km", "1200") ?: "1200"
        annualTargetHm = resultReceiver.getSharedPreference().getString("annual_target", "50000") ?: "50000"
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        resultReceiver.getSortFilterHelper().fragment = this
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)
        this.statisticFragmentView = view
        setHasOptionsMenu(true)
        if (view != null) {
            textTotalSummits = view.findViewById(R.id.textTotalSummits)
            textTotalKm = view.findViewById(R.id.textTotalKm)
            textTotalHm = view.findViewById(R.id.textTotalHm)
            textTotalHmInfo = view.findViewById(R.id.textTotalHmInfo)
            textTotalHmForecastInfo = view.findViewById(R.id.textTotalHmForecastInfo)
            textAchievement = view.findViewById(R.id.textAchievement)
        }
        summitEntries = resultReceiver.getSortFilterHelper().entries
        filteredEntries = resultReceiver.getSortFilterHelper().filteredEntries
        update(filteredEntries)
        return view
    }

    private fun setTextViews(extremaValuesSummits: ExtremaValuesSummits?) {
        if (statisticEntry.getTotalSummits() > 0) {
            textTotalSummits?.text = String.format("%s", statisticEntry.getTotalSummits())
            textTotalKm?.text = String.format(requireContext().resources.configuration.locales[0], "%.1f km", statisticEntry.totalKm)
            textTotalHm?.text = String.format(requireContext().resources.configuration.locales[0], "%s hm", statisticEntry.totalHm)
            val currentYear: Int = (Calendar.getInstance())[Calendar.YEAR]
            val currentMonth: Int = (Calendar.getInstance())[Calendar.MONTH] + 1
            if (resultReceiver.getSortFilterHelper().selectedYear == currentYear.toString()) {
                val forecasts = resultReceiver.getSortFilterHelper().database.forecastDao()?.allForecasts
                forecasts?.forEach { summitEntries?.let { it1 -> it.setActual(it1, indoorHeightMeterPercent) } }
                val sumCurrentYear = forecasts?.let { Forecast.getSumForYear(currentYear, it, 0, currentYear, currentMonth) }
                if ((sumCurrentYear ?: 0) > 0) {
                    textTotalHmInfo?.visibility = View.VISIBLE
                    textTotalHmInfo?.text = getString(R.string.forecast_info_hm, currentYear.toString(), sumCurrentYear.toString(), annualTargetHm)
                    textTotalHmInfo?.setTextColor(if (annualTargetHm.toInt() < (sumCurrentYear ?: 0)) Color.GREEN else Color.RED)
                } else {
                    textTotalHmInfo?.visibility = View.VISIBLE
                    textTotalHmInfo?.text = getString(R.string.current_estimate, statisticEntry.expectedAchievementHmAbsolute.toInt().toString())
                }
                val sumNextYear = forecasts?.filter { it.year == currentYear + 1 }?.sumBy { it.forecastHeightMeter }
                if (sumNextYear ?: 0 > 0) {
                    textTotalHmForecastInfo?.visibility = View.VISIBLE
                    textTotalHmForecastInfo?.text = getString(R.string.forecast_info_hm, (currentYear+1).toString(), sumNextYear.toString(), annualTargetHm)
                    textTotalHmForecastInfo?.setTextColor(if (annualTargetHm.toInt() < (sumNextYear ?: 0)) Color.GREEN else Color.RED)
                }
            } else {
                textTotalHmInfo?.visibility = View.GONE
                textTotalHmForecastInfo?.visibility = View.GONE
            }
            if (resultReceiver.getSortFilterHelper().selectedYear != "") {
                statisticFragmentView?.findViewById<View?>(R.id.achievementInfo)?.visibility = View.VISIBLE
                textAchievement?.text = String.format(requireContext().resources.configuration.locales[0], "%.1f %%", statisticEntry.getAchievement())
            } else {
                statisticFragmentView?.findViewById<View?>(R.id.achievementInfo)?.visibility = View.GONE
            }
            setTextViewData(extremaValuesSummits?.kilometersMinMax?.second, R.id.layoutLongestDistance, R.id.textLongestDistance, R.id.textLongestDistanceInfo, getString(R.string.km),
                    getValueOrNull(extremaValuesSummits?.kilometersMinMax?.second) { e -> e.kilometers }, 1)

            val layoutVisitedCountries = statisticFragmentView?.findViewById<LinearLayout?>(R.id.layoutVisitedCountries)
            val dataVisitedCountries = statisticFragmentView?.findViewById<TextView?>(R.id.textVisitedCountries)
            layoutVisitedCountries?.visibility = View.VISIBLE
            dataVisitedCountries?.text = String.format(requireContext().resources.configuration.locales[0], "%s", statisticEntry.getVisitedCountries())

            setTextViewData(extremaValuesSummits?.averageSpeedMinMax?.second, R.id.layoutHighestAverageSpeed, R.id.textHeighestAverageSpeed, R.id.textHeighestAverageSpeedInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.averageSpeedMinMax?.second) { e -> e.velocityData.avgVelocity }, 1)
            setTextViewData(extremaValuesSummits?.durationMinMax?.second, R.id.layoutLongestDuration, R.id.textLongestDuration, R.id.textLongestDurationInfo, "h",
                    getValueOrNull(extremaValuesSummits?.durationMinMax?.second) { e -> e.duration }, 1)
            setTextViewData(extremaValuesSummits?.topSpeedMinMax?.second, R.id.layoutTopSpeed, R.id.textTopSpeed, R.id.textTopSpeedInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.topSpeedMinMax?.second) { e -> e.velocityData.maxVelocity }, 1)

            setTextViewData(extremaValuesSummits?.topSlopeMinMax?.second, R.id.layoutMaxSlope, R.id.textMaxSlope, R.id.textMaxSlopeInfo, "%",
                    getValueOrNull(extremaValuesSummits?.topSlopeMinMax?.second) { e -> e.elevationData.maxSlope }, 1)

            setTextViewData(extremaValuesSummits?.topVerticalVelocity1MinMinMax?.second, R.id.layoutMaxVerticalVelocity1Min, R.id.textMaxVerticalVelocity1Min, R.id.textMaxVerticalVelocity1MinInfo, getString(R.string.m),
                    getValueOrNull(extremaValuesSummits?.topVerticalVelocity1MinMinMax?.second) { e -> e.elevationData.maxVerticalVelocity1Min }, factor = 60, digits = 0)
            setTextViewData(extremaValuesSummits?.topVerticalVelocity10MinMinMax?.second, R.id.layoutMaxVerticalVelocity10Min, R.id.textMaxVerticalVelocity10Min, R.id.textMaxVerticalVelocity10MinInfo, getString(R.string.m),
                    getValueOrNull(extremaValuesSummits?.topVerticalVelocity10MinMinMax?.second) { e -> e.elevationData.maxVerticalVelocity10Min }, factor = 600, digits = 0)
            setTextViewData(extremaValuesSummits?.topVerticalVelocity1hMinMax?.second, R.id.layoutMaxVerticalVelocity1h, R.id.textMaxVerticalVelocity1h, R.id.textMaxVerticalVelocity1hInfo, getString(R.string.m),
                    getValueOrNull(extremaValuesSummits?.topVerticalVelocity1hMinMax?.second) { e -> e.elevationData.maxVerticalVelocity1h }, factor = 3600, digits = 0)

            setTextViewData(extremaValuesSummits?.oneKmMinMax?.second, R.id.layoutTopSpeed1Km, R.id.textTopSpeed1Km, R.id.textTopSpeed1KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.oneKmMinMax?.second) { e -> e.velocityData.oneKilometer }, 1)
            setTextViewData(extremaValuesSummits?.fiveKmMinMax?.second, R.id.layoutTopSpeed5Km, R.id.textTopSpeed5Km, R.id.textTopSpeed5KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.fiveKmMinMax?.second) { e -> e.velocityData.fiveKilometer }, 1)
            setTextViewData(extremaValuesSummits?.tenKmMinMax?.second, R.id.layoutTopSpeed10Km, R.id.textTopSpeed10Km, R.id.textTopSpeed10KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.tenKmMinMax?.second) { e -> e.velocityData.tenKilometers }, 1)
            setTextViewData(extremaValuesSummits?.fifteenKmMinMax?.second, R.id.layoutTopSpeed15Km, R.id.textTopSpeed15Km, R.id.textTopSpeed15KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.fifteenKmMinMax?.second) { e -> e.velocityData.fifteenKilometers }, 1)
            setTextViewData(extremaValuesSummits?.twentyKmMinMax?.second, R.id.layoutTopSpeed20Km, R.id.textTopSpeed20Km, R.id.textTopSpeed20KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.twentyKmMinMax?.second) { e -> e.velocityData.twentyKilometers }, 1)
            setTextViewData(extremaValuesSummits?.thirtyKmMinMax?.second, R.id.layoutTopSpeed30Km, R.id.textTopSpeed30Km, R.id.textTopSpeed30KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.thirtyKmMinMax?.second) { e -> e.velocityData.thirtyKilometers }, 1)
            setTextViewData(extremaValuesSummits?.fortyKmMinMax?.second, R.id.layoutTopSpeed40Km, R.id.textTopSpeed40Km, R.id.textTopSpeed40KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.fortyKmMinMax?.second) { e -> e.velocityData.fortyKilometers }, 1)
            setTextViewData(extremaValuesSummits?.fiftyKmMinMax?.second, R.id.layoutTopSpeed50Km, R.id.textTopSpeed50Km, R.id.textTopSpeed50KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.fiftyKmMinMax?.second) { e -> e.velocityData.fiftyKilometers }, 1)
            setTextViewData(extremaValuesSummits?.seventyFiveKmMinMax?.second, R.id.layoutTopSpeed75Km, R.id.textTopSpeed75Km, R.id.textTopSpeed75KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.seventyFiveKmMinMax?.second) { e -> e.velocityData.seventyFiveKilometers }, 1)
            setTextViewData(extremaValuesSummits?.hundredKmMinMax?.second, R.id.layoutTopSpeed100Km, R.id.textTopSpeed100Km, R.id.textTopSpeed100KmInfo, getString(R.string.kmh),
                    getValueOrNull(extremaValuesSummits?.hundredKmMinMax?.second) { e -> e.velocityData.hundredKilometers }, 1)

            setTextViewData(extremaValuesSummits?.heightMetersMinMax?.second, R.id.layoutMostHeightMeter, R.id.textMostHeightMeter, R.id.textMostHeightMeterInfo, getString(R.string.m),
                    getValueOrNull(extremaValuesSummits?.heightMetersMinMax?.second) { e -> e.elevationData.elevationGain.toDouble() })
            setTextViewData(extremaValuesSummits?.topElevationMinMax?.second, R.id.layoutHighestPeak, R.id.textHighestPeak, R.id.textHighestPeakInfo, getString(R.string.masl),
                    getValueOrNull(extremaValuesSummits?.topElevationMinMax?.second) { e -> e.elevationData.maxElevation.toDouble() })
            setTextViewData(extremaValuesSummits?.normPowerMinMax?.second, R.id.layoutHighestPower, R.id.textHeighestPower, R.id.textHeighestPowerInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.normPowerMinMax?.second) { e ->
                        e.garminData?.power?.normPower?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power1sMinMax?.second, R.id.layoutHighestPower1sec, R.id.textHeighestPower1sec, R.id.textHeighestPower1secInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power1sMinMax?.second) { e ->
                        e.garminData?.power?.oneSec?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power2sMinMax?.second, R.id.layoutHighestPower2sec, R.id.textHeighestPower2sec, R.id.textHeighestPower2secInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power2sMinMax?.second) { e ->
                        e.garminData?.power?.twoSec?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power5sMinMax?.second, R.id.layoutHighestPower5sec, R.id.textHeighestPower5sec, R.id.textHeighestPower5secInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power5sMinMax?.second) { e ->
                        e.garminData?.power?.fiveSec?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power10sMinMax?.second, R.id.layoutHighestPower10sec, R.id.textHeighestPower10sec, R.id.textHeighestPower10secInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power10sMinMax?.second) { e ->
                        e.garminData?.power?.tenSec?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power20sMinMax?.second, R.id.layoutHighestPower20sec, R.id.textHeighestPower20sec, R.id.textHeighestPower20secInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power20sMinMax?.second) { e ->
                        e.garminData?.power?.twentySec?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power30sMinMax?.second, R.id.layoutHighestPower30sec, R.id.textHeighestPower30sec, R.id.textHeighestPower30secInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power30sMinMax?.second) { e ->
                        e.garminData?.power?.thirtySec?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power1minMinMax?.second, R.id.layoutHighestPower1min, R.id.textHeighestPower1min, R.id.textHeighestPower1minInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power1minMinMax?.second) { e ->
                        e.garminData?.power?.oneMin?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power2minMinMax?.second, R.id.layoutHighestPower2min, R.id.textHeighestPower2min, R.id.textHeighestPower2minInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power2minMinMax?.second) { e ->
                        e.garminData?.power?.twoMin?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power5minMinMax?.second, R.id.layoutHighestPower5min, R.id.textHeighestPower5min, R.id.textHeighestPower5minInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power5minMinMax?.second) { e ->
                        e.garminData?.power?.fiveMin?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power10minMinMax?.second, R.id.layoutHighestPower10min, R.id.textHeighestPower10min, R.id.textHeighestPower10minInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power10minMinMax?.second) { e ->
                        e.garminData?.power?.tenMin?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power20minMinMax?.second, R.id.layoutHighestPower20min, R.id.textHeighestPower20min, R.id.textHeighestPower20minInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power20minMinMax?.second) { e ->
                        e.garminData?.power?.twentyMin?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power30minMinMax?.second, R.id.layoutHighestPower30min, R.id.textHeighestPower30min, R.id.textHeighestPower30minInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power30minMinMax?.second) { e ->
                        e.garminData?.power?.thirtyMin?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power1hMinMax?.second, R.id.layoutHighestPower1h, R.id.textHeighestPower1h, R.id.textHeighestPower1hInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power1hMinMax?.second) { e ->
                        e.garminData?.power?.oneHour?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power2hMinMax?.second, R.id.layoutHighestPower2h, R.id.textHeighestPower2h, R.id.textHeighestPower2hInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power2hMinMax?.second) { e ->
                        e.garminData?.power?.twoHours?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.power5hMinMax?.second, R.id.layoutHighestPower5h, R.id.textHeighestPower5h, R.id.textHeighestPower5hInfo, getString(R.string.watt),
                    getValueOrNull(extremaValuesSummits?.power5hMinMax?.second) { e ->
                        e.garminData?.power?.fiveHours?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.averageHRMinMax?.second, R.id.layoutHighestAverageHR, R.id.textHeighestAverageHR, R.id.textHeighestAverageHRInfo, getString(R.string.bpm),
                    getValueOrNull(extremaValuesSummits?.averageHRMinMax?.second) { e ->
                        e.garminData?.averageHR?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.flowMinMax?.second, R.id.layoutHighestFlow, R.id.textHeighestFlow, R.id.textHeighestFlowInfo, "",
                    getValueOrNull(extremaValuesSummits?.flowMinMax?.second) { e ->
                        e.garminData?.flow?.toDouble() ?: 0.0
                    }, 1)
            setTextViewData(extremaValuesSummits?.gritMinMax?.second, R.id.layoutHighestGrit, R.id.textHeighestGrit, R.id.textHeighestGritInfo, "",
                    getValueOrNull(extremaValuesSummits?.gritMinMax?.second) { e ->
                        e.garminData?.grit?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.trainingsLoadMinMax?.second, R.id.layoutHighestTrainingLoad, R.id.textHeighestTrainingLoad, R.id.textHeighestTrainingLoadInfo, "",
                    getValueOrNull(extremaValuesSummits?.trainingsLoadMinMax?.second) { e ->
                        e.garminData?.trainingLoad?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.ftpMinMax?.second, R.id.layoutHighestFTP, R.id.textHeighestFTP, R.id.textHeighestFTPInfo, "",
                    getValueOrNull(extremaValuesSummits?.ftpMinMax?.second) { e ->
                        e.garminData?.ftp?.toDouble() ?: 0.0
                    })
            setTextViewData(extremaValuesSummits?.vo2maxMinMax?.second, R.id.layoutHighestVO2MAX, R.id.textHeighestVO2MAX, R.id.textHeighestVO2MAXInfo, "",
                    getValueOrNull(extremaValuesSummits?.vo2maxMinMax?.second) { e ->
                        e.garminData?.vo2max?.toDouble() ?: 0.0
                    })
        }
    }

    private fun getValueOrNull(entry: Summit?, f: (Summit) -> Double): Double {
        return if (entry != null) f(entry) else 0.0
    }

    private fun setTextViewData(
            entry: Summit?, layoutId: Int, dataId: Int, infoId: Int,
            unit: String?, value: Double, digits: Int = 0, factor: Int = 1,
    ) {
        val layout = statisticFragmentView?.findViewById<LinearLayout?>(layoutId)
        val data = statisticFragmentView?.findViewById<TextView?>(dataId)
        val info = statisticFragmentView?.findViewById<TextView?>(infoId)
        if (entry != null) {
            layout?.setOnClickListener { v: View ->
                val context = v.context
                val intent = Intent(context, SummitEntryDetailsActivity::class.java)
                intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, entry.id)
                context.startActivity(intent)
            }
            layout?.visibility = View.VISIBLE
            if (digits > 0) {
                data?.text = String.format(requireContext().resources.configuration.locales[0], "%." + digits + "f %s", value * factor, unit)
            } else {
                data?.text = String.format(requireContext().resources.configuration.locales[0], "%s %s", (value * factor).roundToLong(), unit)
            }
            info?.text = String.format(requireContext().resources.configuration.locales[0], "%s: %s\n%s: %s", getString(R.string.name), entry.name, getString(R.string.tour_date), entry.getDateAsString())
        } else {
            layout?.visibility = View.GONE
        }
    }


    override fun update(filteredSummitEntries: List<Summit>?) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val annualTargetActivity = sharedPreferences.getString("annual_target_activities", "52")?.toInt()
                ?: 52
        val annualTargetKm = sharedPreferences.getString("annual_target_km", "1200")?.toInt()
                ?: 1200
        val annualTargetHm = sharedPreferences.getString("annual_target", "50000")?.toInt() ?: 50000
        indoorHeightMeterPercent = sharedPreferences?.getInt("indoor_height_meter_per_cent", 0) ?: 0
        statisticEntry = StatisticEntry(filteredSummitEntries, annualTargetActivity, annualTargetKm, annualTargetHm, indoorHeightMeterPercent)
        statisticEntry.calculate()
        setProgressBar()
        val extremaValuesSummits = filteredSummitEntries?.let { ExtremaValuesSummits(it, shouldIndoorActivityBeExcluded = true) }
        setTextViews(extremaValuesSummits)
    }

    private fun setProgressBar() {
        val simpleProgressBar = statisticFragmentView?.findViewById<ProgressBar?>(R.id.vprogressbar)
        if (resultReceiver.getSortFilterHelper().selectedYear != "") {
            val expectedAchievement = if (resultReceiver.getSortFilterHelper().selectedYear == getCurrentYear()) statisticEntry.getExpectedAchievementHmPercent() else 100.0
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
        fun getAllYears(entries: List<Summit>?): ArrayList<String> {
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