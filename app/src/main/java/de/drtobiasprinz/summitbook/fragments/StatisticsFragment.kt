package de.drtobiasprinz.summitbook.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentStatisticsBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.SortFilterValues
import de.drtobiasprinz.summitbook.db.entities.StatisticEntry
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToLong

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private lateinit var binding: FragmentStatisticsBinding
    private val viewModel: DatabaseViewModel by activityViewModels()

    private lateinit var statisticEntry: StatisticEntry
    private var annualTargetActivity: String = ""
    private var annualTargetKm: String = ""
    private var annualTargetHm: String = ""
    private var indoorHeightMeterPercent: Int = 0
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = DatabaseModule.provideDatabase(requireContext())
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        annualTargetActivity = sharedPreferences.getString("annual_target_activities", "52") ?: "52"
        annualTargetKm = sharedPreferences.getString("annual_target_km", "1200") ?: "1200"
        annualTargetHm = sharedPreferences.getString("annual_target", "50000") ?: "50000"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStatisticsBinding.inflate(layoutInflater, container, false)
        update()
        return binding.root
    }

    fun update() {
        binding.apply {
            viewModel.contactsList.observe(requireActivity()) { itData ->
                itData.data?.let { summits ->
                    val sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(requireContext())
                    val annualTargetActivity =
                        sharedPreferences.getString("annual_target_activities", "52")?.toInt() ?: 52
                    val annualTargetKm =
                        sharedPreferences.getString("annual_target_km", "1200")?.toInt() ?: 1200
                    val annualTargetHm =
                        sharedPreferences.getString("annual_target", "50000")?.toInt() ?: 50000
                    indoorHeightMeterPercent =
                        sharedPreferences?.getInt("indoor_height_meter_per_cent", 0) ?: 0
                    statisticEntry = StatisticEntry(
                        summits,
                        annualTargetActivity,
                        annualTargetKm,
                        annualTargetHm,
                        indoorHeightMeterPercent
                    )
                    statisticEntry.calculate()
                    setProgressBar()
                    val extremaValuesSummits = ExtremaValuesSummits(
                        summits, shouldIndoorActivityBeExcluded = true
                    )
                    setTextViews(summits, extremaValuesSummits)
                }
            }
        }
    }

    private fun setTextViews(summits: List<Summit>, extremaValuesSummits: ExtremaValuesSummits?) {
        if (statisticEntry.getTotalActivities() > 0) {
            binding.textTotalSummits.text = String.format("%s", statisticEntry.getTotalSummits())
            binding.textTotalActivities.text =
                String.format("%s", statisticEntry.getTotalActivities())
            binding.textTotalKm.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%.1f km",
                statisticEntry.totalKm
            )
            binding.textTotalHm.text = String.format(
                requireContext().resources.configuration.locales[0], "%s hm", statisticEntry.totalHm
            )
            val currentYear: Int = (Calendar.getInstance())[Calendar.YEAR]
            val currentMonth: Int = (Calendar.getInstance())[Calendar.MONTH] + 1
            if (sortFilterValues.wasCurrentYearSelected()) {
                val forecasts = database.forecastDao()?.allForecasts
                forecasts?.forEach {
                    it.setActual(
                        summits, indoorHeightMeterPercent
                    )
                }
                val sumCurrentYear = forecasts?.let {
                    Forecast.getSumForYear(
                        currentYear, it, 0, currentYear, currentMonth
                    )
                }
                if ((sumCurrentYear ?: 0) > 0) {
                    binding.textTotalHmInfo.visibility = View.VISIBLE
                    binding.textTotalHmInfo.text = getString(
                        R.string.forecast_info_hm,
                        currentYear.toString(),
                        sumCurrentYear.toString(),
                        annualTargetHm
                    )
                    binding.textTotalHmInfo.setTextColor(
                        if (annualTargetHm.toInt() < (sumCurrentYear
                                ?: 0)
                        ) Color.GREEN else Color.RED
                    )
                } else {
                    binding.textTotalHmInfo.visibility = View.VISIBLE
                    binding.textTotalHmInfo.text = getString(
                        R.string.current_estimate,
                        statisticEntry.expectedAchievementHmAbsolute.toInt().toString()
                    )
                }
                val sumNextYear = forecasts?.filter { it.year == currentYear + 1 }
                    ?.sumOf { it.forecastHeightMeter }
                if ((sumNextYear ?: 0) > 0) {
                    binding.textTotalHmForecastInfo.visibility = View.VISIBLE
                    binding.textTotalHmForecastInfo.text = getString(
                        R.string.forecast_info_hm,
                        (currentYear + 1).toString(),
                        sumNextYear.toString(),
                        annualTargetHm
                    )
                    binding.textTotalHmForecastInfo.setTextColor(
                        if (annualTargetHm.toInt() < (sumNextYear ?: 0)) Color.GREEN else Color.RED
                    )
                }
            } else {
                binding.textTotalHmInfo.visibility = View.GONE
                binding.textTotalHmForecastInfo.visibility = View.GONE
            }
            if (sortFilterValues.wasFullYearSelected()) {
                binding.achievementInfo.visibility = View.VISIBLE
                binding.textAchievement.text = String.format(
                    requireContext().resources.configuration.locales[0],
                    "%.1f %%",
                    statisticEntry.getAchievement()
                )
            } else {
                binding.achievementInfo.visibility = View.GONE
            }
            setTextViewData(
                extremaValuesSummits?.kilometersMinMax?.second,
                binding.layoutLongestDistance,
                binding.textLongestDistance,
                binding.textLongestDistanceInfo,
                getString(R.string.km),
                getValueOrNull(extremaValuesSummits?.kilometersMinMax?.second) { e -> e.kilometers },
                1
            )
            binding.layoutVisitedCountries.visibility = View.VISIBLE
            binding.textVisitedCountries.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%s",
                statisticEntry.getVisitedCountries()
            )

            setTextViewData(
                extremaValuesSummits?.averageSpeedMinMax?.second,
                binding.layoutHighestAverageSpeed,
                binding.textHeighestAverageSpeed,
                binding.textHeighestAverageSpeedInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.averageSpeedMinMax?.second) { e -> e.velocityData.avgVelocity },
                1
            )
            setTextViewData(
                extremaValuesSummits?.durationMinMax?.second,
                binding.layoutLongestDuration,
                binding.textLongestDuration,
                binding.textLongestDurationInfo,
                "h",
                getValueOrNull(extremaValuesSummits?.durationMinMax?.second) { e -> e.duration },
                1
            )

            setTextViewData(
                extremaValuesSummits?.topSlopeMinMax?.second,
                binding.layoutMaxSlope,
                binding.textMaxSlope,
                binding.textMaxSlopeInfo,
                "%",
                getValueOrNull(extremaValuesSummits?.topSlopeMinMax?.second) { e -> e.elevationData.maxSlope },
                1
            )

            if (extremaValuesSummits?.topVerticalVelocity1MinMinMax?.second == null) {
                binding.horizontalVelocity.visibility = View.GONE
            } else {
                binding.horizontalVelocity.visibility = View.VISIBLE
            }
            setTextViewData(
                extremaValuesSummits?.topVerticalVelocity1MinMinMax?.second,
                binding.layoutMaxVerticalVelocity1Min,
                binding.textMaxVerticalVelocity1Min,
                binding.textMaxVerticalVelocity1MinInfo,
                getString(R.string.m),
                getValueOrNull(extremaValuesSummits?.topVerticalVelocity1MinMinMax?.second) { e -> e.elevationData.maxVerticalVelocity1Min },
                factor = 60,
                digits = 0
            )
            setTextViewData(
                extremaValuesSummits?.topVerticalVelocity10MinMinMax?.second,
                binding.layoutMaxVerticalVelocity10Min,
                binding.textMaxVerticalVelocity10Min,
                binding.textMaxVerticalVelocity10MinInfo,
                getString(R.string.m),
                getValueOrNull(extremaValuesSummits?.topVerticalVelocity10MinMinMax?.second) { e -> e.elevationData.maxVerticalVelocity10Min },
                factor = 600,
                digits = 0
            )
            setTextViewData(
                extremaValuesSummits?.topVerticalVelocity1hMinMax?.second,
                binding.layoutMaxVerticalVelocity1h,
                binding.textMaxVerticalVelocity1h,
                binding.textMaxVerticalVelocity1hInfo,
                getString(R.string.m),
                getValueOrNull(extremaValuesSummits?.topVerticalVelocity1hMinMax?.second) { e -> e.elevationData.maxVerticalVelocity1h },
                factor = 3600,
                digits = 0
            )
            if (extremaValuesSummits?.topSpeedMinMax?.second == null) {
                binding.horizontalSpeed.visibility = View.GONE
            } else {
                binding.horizontalSpeed.visibility = View.VISIBLE
            }
            setTextViewData(
                extremaValuesSummits?.topSpeedMinMax?.second,
                binding.layoutTopSpeed,
                binding.textTopSpeed,
                binding.textTopSpeedInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.topSpeedMinMax?.second) { e -> e.velocityData.maxVelocity },
                1
            )
            setTextViewData(
                extremaValuesSummits?.oneKmMinMax?.second,
                binding.layoutTopSpeed1Km,
                binding.textTopSpeed1Km,
                binding.textTopSpeed1KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.oneKmMinMax?.second) { e -> e.velocityData.oneKilometer },
                1
            )
            setTextViewData(
                extremaValuesSummits?.fiveKmMinMax?.second,
                binding.layoutTopSpeed5Km,
                binding.textTopSpeed5Km,
                binding.textTopSpeed5KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.fiveKmMinMax?.second) { e -> e.velocityData.fiveKilometer },
                1
            )
            setTextViewData(
                extremaValuesSummits?.tenKmMinMax?.second,
                binding.layoutTopSpeed10Km,
                binding.textTopSpeed10Km,
                binding.textTopSpeed10KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.tenKmMinMax?.second) { e -> e.velocityData.tenKilometers },
                1
            )
            setTextViewData(
                extremaValuesSummits?.fifteenKmMinMax?.second,
                binding.layoutTopSpeed15Km,
                binding.textTopSpeed15Km,
                binding.textTopSpeed15KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.fifteenKmMinMax?.second) { e -> e.velocityData.fifteenKilometers },
                1
            )
            setTextViewData(
                extremaValuesSummits?.twentyKmMinMax?.second,
                binding.layoutTopSpeed20Km,
                binding.textTopSpeed20Km,
                binding.textTopSpeed20KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.twentyKmMinMax?.second) { e -> e.velocityData.twentyKilometers },
                1
            )
            setTextViewData(
                extremaValuesSummits?.thirtyKmMinMax?.second,
                binding.layoutTopSpeed30Km,
                binding.textTopSpeed30Km,
                binding.textTopSpeed30KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.thirtyKmMinMax?.second) { e -> e.velocityData.thirtyKilometers },
                1
            )
            setTextViewData(
                extremaValuesSummits?.fortyKmMinMax?.second,
                binding.layoutTopSpeed40Km,
                binding.textTopSpeed40Km,
                binding.textTopSpeed40KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.fortyKmMinMax?.second) { e -> e.velocityData.fortyKilometers },
                1
            )
            setTextViewData(
                extremaValuesSummits?.fiftyKmMinMax?.second,
                binding.layoutTopSpeed50Km,
                binding.textTopSpeed50Km,
                binding.textTopSpeed50KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.fiftyKmMinMax?.second) { e -> e.velocityData.fiftyKilometers },
                1
            )
            setTextViewData(
                extremaValuesSummits?.seventyFiveKmMinMax?.second,
                binding.layoutTopSpeed75Km,
                binding.textTopSpeed75Km,
                binding.textTopSpeed75KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.seventyFiveKmMinMax?.second) { e -> e.velocityData.seventyFiveKilometers },
                1
            )
            setTextViewData(
                extremaValuesSummits?.hundredKmMinMax?.second,
                binding.layoutTopSpeed100Km,
                binding.textTopSpeed100Km,
                binding.textTopSpeed100KmInfo,
                getString(R.string.kmh),
                getValueOrNull(extremaValuesSummits?.hundredKmMinMax?.second) { e -> e.velocityData.hundredKilometers },
                1
            )

            setTextViewData(extremaValuesSummits?.heightMetersMinMax?.second,
                binding.layoutMostHeightMeter,
                binding.textMostHeightMeter,
                binding.textMostHeightMeterInfo,
                getString(R.string.m),
                getValueOrNull(extremaValuesSummits?.heightMetersMinMax?.second) { e -> e.elevationData.elevationGain.toDouble() })
            setTextViewData(extremaValuesSummits?.topElevationMinMax?.second,
                binding.layoutHighestPeak,
                binding.textHighestPeak,
                binding.textHighestPeakInfo,
                getString(R.string.masl),
                getValueOrNull(extremaValuesSummits?.topElevationMinMax?.second) { e -> e.elevationData.maxElevation.toDouble() })
            setTextViewData(extremaValuesSummits?.normPowerMinMax?.second,
                binding.layoutHighestPower,
                binding.textHeighestPower,
                binding.textHeighestPowerInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.normPowerMinMax?.second) { e ->
                    e.garminData?.power?.normPower?.toDouble() ?: 0.0
                })
            if (extremaValuesSummits?.power1sMinMax?.second == null) {
                binding.horizontalPower.visibility = View.GONE
            } else {
                binding.horizontalPower.visibility = View.VISIBLE
            }
            setTextViewData(extremaValuesSummits?.power1sMinMax?.second,
                binding.layoutHighestPower1sec,
                binding.textHeighestPower1sec,
                binding.textHeighestPower1secInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power1sMinMax?.second) { e ->
                    e.garminData?.power?.oneSec?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power2sMinMax?.second,
                binding.layoutHighestPower2sec,
                binding.textHeighestPower2sec,
                binding.textHeighestPower2secInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power2sMinMax?.second) { e ->
                    e.garminData?.power?.twoSec?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power5sMinMax?.second,
                binding.layoutHighestPower5sec,
                binding.textHeighestPower5sec,
                binding.textHeighestPower5secInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power5sMinMax?.second) { e ->
                    e.garminData?.power?.fiveSec?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power10sMinMax?.second,
                binding.layoutHighestPower10sec,
                binding.textHeighestPower10sec,
                binding.textHeighestPower10secInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power10sMinMax?.second) { e ->
                    e.garminData?.power?.tenSec?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power20sMinMax?.second,
                binding.layoutHighestPower20sec,
                binding.textHeighestPower20sec,
                binding.textHeighestPower20secInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power20sMinMax?.second) { e ->
                    e.garminData?.power?.twentySec?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power30sMinMax?.second,
                binding.layoutHighestPower30sec,
                binding.textHeighestPower30sec,
                binding.textHeighestPower30secInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power30sMinMax?.second) { e ->
                    e.garminData?.power?.thirtySec?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power1minMinMax?.second,
                binding.layoutHighestPower1min,
                binding.textHeighestPower1min,
                binding.textHeighestPower1minInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power1minMinMax?.second) { e ->
                    e.garminData?.power?.oneMin?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power2minMinMax?.second,
                binding.layoutHighestPower2min,
                binding.textHeighestPower2min,
                binding.textHeighestPower2minInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power2minMinMax?.second) { e ->
                    e.garminData?.power?.twoMin?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power5minMinMax?.second,
                binding.layoutHighestPower5min,
                binding.textHeighestPower5min,
                binding.textHeighestPower5minInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power5minMinMax?.second) { e ->
                    e.garminData?.power?.fiveMin?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power10minMinMax?.second,
                binding.layoutHighestPower10min,
                binding.textHeighestPower10min,
                binding.textHeighestPower10minInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power10minMinMax?.second) { e ->
                    e.garminData?.power?.tenMin?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power20minMinMax?.second,
                binding.layoutHighestPower20min,
                binding.textHeighestPower20min,
                binding.textHeighestPower20minInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power20minMinMax?.second) { e ->
                    e.garminData?.power?.twentyMin?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power30minMinMax?.second,
                binding.layoutHighestPower30min,
                binding.textHeighestPower30min,
                binding.textHeighestPower30minInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power30minMinMax?.second) { e ->
                    e.garminData?.power?.thirtyMin?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power1hMinMax?.second,
                binding.layoutHighestPower1h,
                binding.textHeighestPower1h,
                binding.textHeighestPower1hInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power1hMinMax?.second) { e ->
                    e.garminData?.power?.oneHour?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power2hMinMax?.second,
                binding.layoutHighestPower2h,
                binding.textHeighestPower2h,
                binding.textHeighestPower2hInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power2hMinMax?.second) { e ->
                    e.garminData?.power?.twoHours?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.power5hMinMax?.second,
                binding.layoutHighestPower5h,
                binding.textHeighestPower5h,
                binding.textHeighestPower5hInfo,
                getString(R.string.watt),
                getValueOrNull(extremaValuesSummits?.power5hMinMax?.second) { e ->
                    e.garminData?.power?.fiveHours?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.averageHRMinMax?.second,
                binding.layoutHighestAverageHR,
                binding.textHeighestAverageHR,
                binding.textHeighestAverageHRInfo,
                getString(R.string.bpm),
                getValueOrNull(extremaValuesSummits?.averageHRMinMax?.second) { e ->
                    e.garminData?.averageHR?.toDouble() ?: 0.0
                })
            setTextViewData(
                extremaValuesSummits?.flowMinMax?.second,
                binding.layoutHighestFlow,
                binding.textHeighestFlow,
                binding.textHeighestFlowInfo,
                "",
                getValueOrNull(extremaValuesSummits?.flowMinMax?.second) { e ->
                    e.garminData?.flow?.toDouble() ?: 0.0
                },
                1
            )
            setTextViewData(extremaValuesSummits?.gritMinMax?.second,
                binding.layoutHighestGrit,
                binding.textHeighestGrit,
                binding.textHeighestGritInfo,
                "",
                getValueOrNull(extremaValuesSummits?.gritMinMax?.second) { e ->
                    e.garminData?.grit?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.trainingsLoadMinMax?.second,
                binding.layoutHighestTrainingLoad,
                binding.textHeighestTrainingLoad,
                binding.textHeighestTrainingLoadInfo,
                "",
                getValueOrNull(extremaValuesSummits?.trainingsLoadMinMax?.second) { e ->
                    e.garminData?.trainingLoad?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.ftpMinMax?.second,
                binding.layoutHighestFTP,
                binding.textHeighestFTP,
                binding.textHeighestFTPInfo,
                "",
                getValueOrNull(extremaValuesSummits?.ftpMinMax?.second) { e ->
                    e.garminData?.ftp?.toDouble() ?: 0.0
                })
            setTextViewData(extremaValuesSummits?.vo2maxMinMax?.second,
                binding.layoutHighestVO2MAX,
                binding.textHeighestVO2MAX,
                binding.textHeighestVO2MAXInfo,
                "",
                getValueOrNull(extremaValuesSummits?.vo2maxMinMax?.second) { e ->
                    e.garminData?.vo2max?.toDouble() ?: 0.0
                })
        }
    }

    private fun getValueOrNull(entry: Summit?, f: (Summit) -> Double): Double {
        return if (entry != null) f(entry) else 0.0
    }

    private fun setTextViewData(
        entry: Summit?, layout: LinearLayout, data: TextView, info: TextView,
        unit: String?, value: Double, digits: Int = 0, factor: Int = 1,
    ) {
        if (entry != null) {
            layout.setOnClickListener { v: View ->
                val context = v.context
                val intent = Intent(context, SummitEntryDetailsActivity::class.java)
                intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, entry.id)
                context.startActivity(intent)
            }
            layout.visibility = View.VISIBLE
            if (digits > 0) {
                data.text = String.format(
                    requireContext().resources.configuration.locales[0],
                    "%." + digits + "f %s",
                    value * factor,
                    unit
                )
            } else {
                data.text = String.format(
                    requireContext().resources.configuration.locales[0],
                    "%s %s",
                    (value * factor).roundToLong(),
                    unit
                )
            }
            info.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%s: %s\n%s: %s",
                getString(R.string.name),
                entry.name,
                getString(R.string.tour_date),
                entry.getDateAsString()
            )
        } else {
            layout.visibility = View.GONE
        }
    }


    private fun setProgressBar() {
        val simpleProgressBar = binding.vprogressbar
        if (sortFilterValues.wasFullYearSelected()) {
            val expectedAchievement =
                if (sortFilterValues.wasCurrentYearSelected()) statisticEntry.getExpectedAchievementHmPercent() else 100.0
            simpleProgressBar.visibility = View.VISIBLE
            simpleProgressBar.max = 100
            simpleProgressBar.progress = statisticEntry.getAchievement().toInt()
            if (expectedAchievement < statisticEntry.getAchievement()) {
                simpleProgressBar.progressTintList = ColorStateList.valueOf(Color.GREEN)
            }
            simpleProgressBar.secondaryProgress = expectedAchievement.toInt()
        } else {
            simpleProgressBar.visibility = View.GONE
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