package de.drtobiasprinz.summitbook.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.databinding.FragmentStatisticsBinding
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.models.StatisticEntryDefinitions
import de.drtobiasprinz.summitbook.models.Surface
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.utils.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    @Inject
    lateinit var sortFilterValues: SortFilterValues

    private lateinit var binding: FragmentStatisticsBinding
    private val viewModel: DatabaseViewModel by activityViewModels()
    private lateinit var numberFormat: NumberFormat
    private lateinit var statisticEntry: StatisticEntry
    private var annualTargetActivity: String = ""
    private var annualTargetKm: String = ""
    private var annualTargetHm: String = ""
    private var indoorHeightMeterPercent: Int = 0
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        annualTargetActivity =
            sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, "52") ?: "52"
        annualTargetKm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200") ?: "1200"
        annualTargetHm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET, "50000") ?: "50000"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStatisticsBinding.inflate(layoutInflater, container, false)
        numberFormat = NumberFormat.getInstance(resources.configuration.locales[0])
        update()
        return binding.root
    }

    fun update() {
        binding.apply {
            viewModel.summitsList.observe(viewLifecycleOwner) { itData ->
                itData.data?.let { summits ->
                    viewModel.forecastList.observe(viewLifecycleOwner) { itDataForecasts ->
                        itDataForecasts.data?.let { forecasts ->
                            lifecycleScope.launch {
                                val filteredSummits = withContext(Dispatchers.IO) {
                                    sortFilterValues.apply(summits, sharedPreferences)
                                }
                                val sharedPreferences =
                                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                                val annualTargetActivity =
                                    sharedPreferences.getString(
                                        Keys.PREF_ANNUAL_TARGET_ACTIVITIES,
                                        "52"
                                    )
                                        ?.toInt() ?: 52
                                val annualTargetKm =
                                    sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200")
                                        ?.toInt()
                                        ?: 1200
                                val annualTargetHm =
                                    sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET, "50000")
                                        ?.toInt()
                                        ?: 50000
                                indoorHeightMeterPercent =
                                    sharedPreferences?.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
                                        ?: 0
                                statisticEntry = StatisticEntry(
                                    filteredSummits,
                                    annualTargetActivity,
                                    annualTargetKm,
                                    annualTargetHm,
                                    indoorHeightMeterPercent
                                )
                                statisticEntry.calculate()
                                setProgressBar()
                                val extremaValuesSummits = ExtremaValuesSummits(
                                    filteredSummits, shouldIndoorActivityBeExcluded = true
                                )
                                setTextViews(filteredSummits, forecasts, extremaValuesSummits)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setTextViews(
        summits: List<Summit>,
        forecasts: List<Forecast>?,
        extremaValuesSummits: ExtremaValuesSummits?
    ) {
        if (statisticEntry.getTotalActivities() > 0) {
            setSummary(forecasts, summits)
            setRoadSummary()
            if (extremaValuesSummits != null) {
                StatisticEntryDefinitions.entries.forEach {
                    setTextViewData(extremaValuesSummits, it)
                }
            }

            binding.layoutVisitedCountries.visibility = View.VISIBLE
            binding.textVisitedCountries.text = String.format(
                requireContext().resources.configuration.locales[0],
                "%s",
                statisticEntry.getVisitedCountries()
            )
            setVisibilityForHorizontalScrollViews(extremaValuesSummits)
        }
    }

    private fun setRoadSummary() {
        binding.textSurfaceAsphalt.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadSurfaceMeter[Surface.ASPHALT] ?: 0) / 1000)
        )
        binding.textSurfaceStonePavement.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadSurfaceMeter[Surface.STONE_PAVEMENT] ?: 0) / 1000)
        )
        binding.textSurfaceCompacted.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadSurfaceMeter[Surface.COMPACTED] ?: 0) / 1000)
        )
        binding.textSurfaceLoseGround.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadSurfaceMeter[Surface.LOSE_GROUND] ?: 0) / 1000)
        )
        binding.textSurfacePath.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadSurfaceMeter[Surface.PATH] ?: 0) / 1000)
        )
        binding.textSurfaceUnknown.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadSurfaceMeter[Surface.UNKNOWN] ?: 0) / 1000)
        )

        binding.textRoadTypeWay.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadTypeMeter[RoadType.WAY] ?: 0) / 1000)
        )
        binding.textRoadTypeSideStreet.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadTypeMeter[RoadType.SIDE_STREET] ?: 0) / 1000)
        )
        binding.textRoadTypeCountryRoad.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadTypeMeter[RoadType.COUNTRY_ROAD] ?: 0) / 1000)
        )
        binding.textRoadTypeCycleWay.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadTypeMeter[RoadType.CYCLE_WAY] ?: 0) / 1000)
        )
        binding.textRoadTypeRoad.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadTypeMeter[RoadType.ROAD] ?: 0) / 1000)
        )
        binding.textRoadTypeUnknown.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format((statisticEntry.totalRoadTypeMeter[RoadType.UNKNOWN] ?: 0) / 1000)
        )
    }

    private fun setSummary(
        forecasts: List<Forecast>?,
        summits: List<Summit>
    ) {
        numberFormat.maximumFractionDigits = 0
        binding.textTotalSummits.text = numberFormat.format(statisticEntry.getTotalSummits())
        binding.textTotalActivities.text =
            numberFormat.format(statisticEntry.getTotalActivities())
        binding.textTotalHm.text = String.format(
            resources.getString(R.string.value_with_hm),
            numberFormat.format(statisticEntry.totalHm)
        )

        numberFormat.maximumFractionDigits = 1
        binding.textTotalKm.text = String.format(
            resources.getString(R.string.value_with_km),
            numberFormat.format(statisticEntry.totalKm)
        )

        numberFormat.maximumFractionDigits = 0
        val currentYear: Int = (Calendar.getInstance())[Calendar.YEAR]
        val currentMonth: Int = (Calendar.getInstance())[Calendar.MONTH] + 1
        if (sortFilterValues.wasCurrentYearSelected()) {
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
                    numberFormat.format(sumCurrentYear),
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
                    numberFormat.format(statisticEntry.expectedAchievementHmAbsolute)
                )
            }
            val sumNextYear = forecasts?.filter { it.year == currentYear + 1 }
                ?.sumOf { it.forecastHeightMeter }
            if ((sumNextYear ?: 0) > 0) {
                binding.textTotalHmForecastInfo.visibility = View.VISIBLE
                binding.textTotalHmForecastInfo.text = getString(
                    R.string.forecast_info_hm,
                    (currentYear + 1).toString(),
                    numberFormat.format(sumNextYear),
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
            numberFormat.maximumFractionDigits = 1
            binding.achievementInfo.visibility = View.VISIBLE
            binding.textAchievement.text = String.format(
                resources.getString(R.string.value_with_per_cent),
                numberFormat.format(statisticEntry.getAchievement())
            )
        } else {
            binding.achievementInfo.visibility = View.GONE
        }
    }

    private fun setVisibilityForHorizontalScrollViews(extremaValuesSummits: ExtremaValuesSummits?) {
        if (extremaValuesSummits?.topVerticalVelocity1MinMinMax?.second == null) {
            binding.horizontalVelocity.visibility = View.GONE
        } else {
            binding.horizontalVelocity.visibility = View.VISIBLE
        }

        if (extremaValuesSummits?.topSpeedMinMax?.second == null) {
            binding.horizontalSpeed.visibility = View.GONE
        } else {
            binding.horizontalSpeed.visibility = View.VISIBLE
        }

        if (extremaValuesSummits?.power1sMinMax?.second == null) {
            binding.horizontalPower.visibility = View.GONE
        } else {
            binding.horizontalPower.visibility = View.VISIBLE
        }
    }

    private fun setTextViewData(
        extremaValuesSummits: ExtremaValuesSummits,
        entry: StatisticEntryDefinitions
    ) {
        val summit = entry.getSummit(extremaValuesSummits)
        if (summit != null) {
            entry.layout(binding).setOnClickListener { v: View ->
                val context = v.context
                val intent = Intent(context, SummitEntryDetailsActivity::class.java)
                intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, summit.id)
                context.startActivity(intent)
            }
            entry.layout(binding).visibility = View.VISIBLE

            numberFormat.maximumFractionDigits = entry.digits
            if (entry.toHHms) {
                val valueInMs = (entry.getValue(summit) * 3600000.0).toLong()
                entry.data(binding).text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(valueInMs),
                    TimeUnit.MILLISECONDS.toMinutes(valueInMs) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(valueInMs) % TimeUnit.MINUTES.toSeconds(1),
                )
            } else {
                entry.data(binding).text = String.format(
                    getString(entry.unit),
                    numberFormat.format(entry.getValue(summit) * entry.factor)
                )
            }
            entry.info(binding).text = String.format(
                requireContext().resources.configuration.locales[0],
                "%s: %s\n%s: %s",
                getString(R.string.name),
                summit.name,
                getString(R.string.date),
                summit.getDateAsString()
            )
        } else {
            entry.layout(binding).visibility = View.GONE
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
    }

}
