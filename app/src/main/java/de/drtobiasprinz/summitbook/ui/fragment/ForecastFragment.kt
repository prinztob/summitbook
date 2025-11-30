package de.drtobiasprinz.summitbook.ui.fragment

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.Keys.PREF_ANNUAL_TARGET
import de.drtobiasprinz.summitbook.Keys.PREF_ANNUAL_TARGET_ACTIVITIES
import de.drtobiasprinz.summitbook.Keys.PREF_ANNUAL_TARGET_KM
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.FragmentForecastBinding
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Forecast.Companion.getSumForYear
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.fragments.SummitViewFragment
import de.drtobiasprinz.summitbook.models.SortFilterValues.Companion.getYear
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.util.Calendar
import java.util.Date
import kotlin.math.ceil
import kotlin.math.round

@AndroidEntryPoint
class ForecastFragment : Fragment() {
    private lateinit var binding: FragmentForecastBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val viewModel: DatabaseViewModel by viewModels()

    private var selectedSegmentedYear: Int = 0
    private var selectedSegmentedForecastProperty: Int = 0
    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var yearsWithForecasts = listOf(currentYear, currentYear + 1)
    private var annualTargetActivity: String = ""
    private var annualTargetKm: String = ""
    private var annualTargetHm: String = ""
    private var indoorHeightMeterPercent = 0
    private var averageOfLastXYears: Int = 0
    private var forecastsUpdated: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentForecastBinding.inflate(layoutInflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
        averageOfLastXYears = sharedPreferences.getInt(Keys.PREF_FORECAST_AVERAGE, 3)
        annualTargetActivity =
            sharedPreferences.getString(PREF_ANNUAL_TARGET_ACTIVITIES, "52") ?: "52"
        annualTargetKm = sharedPreferences.getString(PREF_ANNUAL_TARGET_KM, "1200") ?: "1200"
        annualTargetHm = sharedPreferences.getString(PREF_ANNUAL_TARGET, "50000") ?: "50000"
        val range: Date = Summit.parseDate("${currentYear}-01-01")
        viewModel.summitsList.observe(
            viewLifecycleOwner, object : Observer<DataStatus<List<Summit>>> {
                override fun onChanged(value: DataStatus<List<Summit>>) {
                    value.data.let { summits ->
                        val summitsForSelectedYear =
                            summits?.filter { summit -> getYear(summit.date) == getYear(range) }
                        viewModel.forecastList.observe(viewLifecycleOwner) { itDataStatusForecasts ->
                            itDataStatusForecasts.data.let { forecasts ->
                                val forecastsOriginal = forecasts?.map { it.copy() } ?: emptyList()
                                if (summits != null && !forecastsUpdated) {
                                    forecastsUpdated = true
                                    setMissingForecasts(yearsWithForecasts, forecasts, summits)
                                }

                                if (forecasts != null) {
                                    if (summitsForSelectedYear != null) {
                                        setAchievement(forecasts, summitsForSelectedYear)
                                    }
                                    setForecastsInDialog(
                                        forecasts,
                                        summits,
                                        yearsWithForecasts[selectedSegmentedYear],
                                        view
                                    )
                                    setOverview(
                                        forecasts, yearsWithForecasts[selectedSegmentedYear]
                                    )
                                }

                                binding.recalculateDataNextYear.setOnClickListener {
                                    if (forecasts != null && summits != null && selectedSegmentedYear == 1) {
                                        updateForecastsForYear(
                                            forecasts,
                                            yearsWithForecasts[selectedSegmentedYear],
                                            summits,
                                            true
                                        )
                                        setOverview(
                                            forecasts, yearsWithForecasts[selectedSegmentedYear]
                                        )
                                        setForecastsInDialog(
                                            forecasts,
                                            summits,
                                            yearsWithForecasts[selectedSegmentedYear],
                                            view
                                        )
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.not_recalculate_current_year),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                binding.save.setOnClickListener {
                                    binding.loadingPanel.visibility = View.VISIBLE
                                    viewModel.summitsList.removeObserver(this)
                                    if (forecasts != null) {
                                        val forecastsWithChanges = forecasts.filter { forecast ->
                                            !forecastsOriginal.any {
                                                it.equalsWithOutId(forecast)
                                            }
                                        }
                                        val job = viewModel.saveForecasts(
                                            true, forecastsWithChanges
                                        )
                                        job.invokeOnCompletion {
                                            back(R.string.forecast_successfully_saved)
                                        }
                                    }
                                }
                                binding.groupForecastProperty.addOnButtonCheckedListener { _, checkedId, isChecked ->
                                    if (isChecked && forecasts != null) {
                                        selectedSegmentedForecastProperty = checkedId
                                        setOverview(
                                            forecasts, yearsWithForecasts[selectedSegmentedYear]
                                        )
                                        setForecastsInDialog(
                                            forecasts,
                                            summits,
                                            yearsWithForecasts[selectedSegmentedYear],
                                            view
                                        )
                                    }
                                }
                                binding.groupYear.addOnButtonCheckedListener { _, checkedId, isChecked ->
                                    if (isChecked && forecasts != null) {
                                        selectedSegmentedYear =
                                            if (checkedId == binding.buttonCurrentYear.id) 0 else 1
                                        setOverview(
                                            forecasts, yearsWithForecasts[selectedSegmentedYear]
                                        )
                                        setForecastsInDialog(
                                            forecasts,
                                            summits,
                                            yearsWithForecasts[selectedSegmentedYear],
                                            view
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            })
    }

    private fun back(messageId: Int) {
        val ft = parentFragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, SummitViewFragment())
        ft.commit()
        Toast.makeText(
            activity, getString(messageId), Toast.LENGTH_LONG
        ).show()
    }

    private fun setMissingForecasts(
        years: List<Int>, forecasts: List<Forecast>?, summits: List<Summit>
    ) {
        for (year in years) {
            updateForecastsForYear(forecasts, year, summits)
        }
    }

    private fun updateForecastsForYear(
        forecasts: List<Forecast>?,
        year: Int,
        summits: List<Summit>,
        updateIfEntryExists: Boolean = false
    ) {
        for (month in 1..12) {
            updateForecastForMonthAndYear(month, year, summits, forecasts, updateIfEntryExists)
        }
    }

    private fun updateForecastForMonthAndYear(
        month: Int,
        year: Int,
        summits: List<Summit>?,
        forecasts: List<Forecast>?,
        updateIfEntryExists: Boolean
    ) {
        val updatedForecast = Forecast.getNewForecastFrom(
            month,
            year,
            summits,
            averageOfLastXYears,
            annualTargetActivity,
            annualTargetKm,
            annualTargetHm
        )
        val existingForecast = forecasts?.firstOrNull { it.month == month && it.year == year }
        if (existingForecast == null) {
            Log.d(TAG, "Add new Forecast for month $month and year $year: $updatedForecast")
            viewModel.saveForecast(false, updatedForecast)
        } else if (updateIfEntryExists) {
            existingForecast.forecastDistance = updatedForecast.forecastDistance
            existingForecast.forecastHeightMeter = updatedForecast.forecastHeightMeter
            existingForecast.forecastNumberActivities = updatedForecast.forecastNumberActivities
            Log.d(TAG, "Update Forecast for month $month and year $year: $existingForecast")
        }
    }

    private fun setAchievement(forecasts: List<Forecast>, summits: List<Summit>) {
        forecasts.forEach {
            if (it.year == currentYear && it.month <= currentMonth) {
                it.setActual(summits, indoorHeightMeterPercent)
            }
        }
    }

    private fun setOverview(forecasts: List<Forecast>, year: Int) {
        val id = when (selectedSegmentedForecastProperty) {
            binding.buttonKilometers.id -> {
                1
            }

            binding.buttonActivity.id -> {
                2
            }

            else -> {
                0
            }
        }
        val sum = getSumForYear(
            year, forecasts, id, currentYear, currentMonth
        )
        val annualTarget: Int
        when (selectedSegmentedForecastProperty) {
            binding.buttonKilometers.id -> {
                binding.overview.text = requireContext().getString(
                    R.string.forecast_info_km, year.toString(), sum.toString(), annualTargetKm
                )
                annualTarget = annualTargetKm.toInt()
            }

            binding.buttonActivity.id -> {
                binding.overview.text = requireContext().getString(
                    R.string.forecast_info_activities,
                    year.toString(),
                    sum.toString(),
                    annualTargetActivity
                )
                annualTarget = annualTargetActivity.toInt()
            }

            else -> {
                binding.overview.text = requireContext().getString(
                    R.string.forecast_info_hm, year.toString(), sum.toString(), annualTargetHm
                )
                annualTarget = annualTargetHm.toInt()
            }
        }
        if (sum > annualTarget) binding.overview.setTextColor(Color.GREEN) else binding.overview.setTextColor(
            Color.RED
        )
    }

    @SuppressLint("DiscouragedApi")
    private fun setForecastsInDialog(
        forecasts: List<Forecast>, summits: List<Summit>?, year: Int, view: View
    ) {
        forecasts.forEach {
            if (it.year == year) {
                val resourceIdSlider = resources.getIdentifier(
                    "range_slider_forecast_month${it.month}", "id", requireContext().packageName
                )
                if (resourceIdSlider > 0) {
                    val slider: Slider = view.findViewById(resourceIdSlider)
                    slider.valueFrom = 0F
                    when (selectedSegmentedForecastProperty) {
                        binding.buttonKilometers.id -> {
                            val valueTo =
                                (ceil(it.forecastDistance * 1.2 / STEP_SIZE_KM) * STEP_SIZE_KM).toFloat()
                            slider.value = it.forecastDistance.toFloat()
                            slider.valueTo = if (valueTo < 750) 750f else valueTo
                            slider.stepSize = STEP_SIZE_KM.toFloat()
                        }

                        binding.buttonActivity.id -> {
                            val valueTo =
                                (ceil(it.forecastNumberActivities * 1.2 / STEP_SIZE_ACTIVITY) * STEP_SIZE_ACTIVITY).toFloat()
                            slider.value = it.forecastNumberActivities.toFloat()
                            slider.valueTo = if (valueTo < 25) 25f else valueTo
                            slider.stepSize = STEP_SIZE_ACTIVITY.toFloat()
                        }

                        else -> {
                            val valueTo =
                                (ceil(it.forecastHeightMeter * 1.2 / STEP_SIZE_HM) * STEP_SIZE_HM).toFloat()
                            slider.value = it.forecastHeightMeter.toFloat()
                            slider.valueTo = if (valueTo < 15000) 15000f else valueTo
                            slider.stepSize = STEP_SIZE_HM.toFloat()
                        }
                    }
                    val recalculateDataMonthId = resources.getIdentifier(
                        "recalculateDataMonth${it.month}", "id", requireContext().packageName
                    )
                    val button: AppCompatImageButton = view.findViewById(recalculateDataMonthId)
                    if (it.year == currentYear && it.month < currentMonth) {
                        button.visibility = View.GONE
                        slider.isEnabled = false
                        setAchievementText(it, view, slider)
                    } else {
                        slider.isEnabled = true
                        setForecastText(it, view, slider)
                        button.visibility = View.VISIBLE
                        button.setOnClickListener { _ ->
                            updateForecastForMonthAndYear(it.month, year, summits, forecasts, true)
                            setOverview(
                                forecasts, yearsWithForecasts[selectedSegmentedYear]
                            )
                            setForecastsInDialog(
                                forecasts, summits, yearsWithForecasts[selectedSegmentedYear], view
                            )
                        }
                    }
                    slider.addOnChangeListener { clickedSlider: Slider, value: Float, fromUser: Boolean ->
                        if (fromUser && it.year == yearsWithForecasts[selectedSegmentedYear]) {
                            when (selectedSegmentedForecastProperty) {
                                binding.buttonKilometers.id -> it.forecastDistance = value.toInt()
                                binding.buttonActivity.id -> it.forecastNumberActivities =
                                    value.toInt()

                                else -> it.forecastHeightMeter = value.toInt()
                            }
                            setOverview(forecasts, year)
                            setForecastText(it, view, clickedSlider)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("DiscouragedApi", "SetTextI18n")
    private fun setForecastText(forecast: Forecast, view: View, slider: Slider) {
        val resourceIdText = resources.getIdentifier(
            "fulfilled_forecast_month${forecast.month}", "id", requireContext().packageName
        )
        val textView: TextView = view.findViewById(resourceIdText)
        when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                textView.setTextColor(Color.WHITE)
            }

            else -> {
                textView.setTextColor(Color.BLACK)
            }
        }
        if (forecast.year == currentYear && forecast.month <= currentMonth) {
            textView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_baseline_edit_12, 0, 0, 0
            )
            textView.setOnClickListener {
                slider.isEnabled = !slider.isEnabled
            }
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        when (selectedSegmentedForecastProperty) {
            binding.buttonKilometers.id -> textView.text = String.format(
                resources.getString(R.string.value_with_km), forecast.forecastDistance.toString()
            )

            binding.buttonActivity.id -> textView.text =
                forecast.forecastNumberActivities.toString()

            else -> textView.text = String.format(
                resources.getString(R.string.value_with_hm), forecast.forecastHeightMeter.toString()
            )
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun setAchievementText(forecast: Forecast, view: View, slider: Slider) {
        val resourceIdText = resources.getIdentifier(
            "fulfilled_forecast_month${forecast.month}", "id", requireContext().packageName
        )
        val textView: TextView = view.findViewById(resourceIdText)
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_edit_12, 0, 0, 0)
        textView.setOnClickListener {
            slider.isEnabled = !slider.isEnabled
        }
        when (selectedSegmentedForecastProperty) {
            binding.buttonKilometers.id -> setTextAndColor(
                forecast.actualDistance,
                forecast.forecastDistance,
                resources.getString(R.string.km),
                textView
            )

            binding.buttonActivity.id -> setTextAndColor(
                forecast.actualNumberActivities, forecast.forecastNumberActivities, "", textView
            )

            else -> setTextAndColor(
                forecast.actualHeightMeter,
                forecast.forecastHeightMeter,
                resources.getString(R.string.hm),
                textView
            )
        }
    }

    private fun setTextAndColor(actual: Int, forecast: Int, unit: String, textView: TextView) {
        val percentAchievement =
            if (forecast > 0) round(actual.toDouble() / forecast.toDouble() * 100.0).toInt() else forecast
        val text = if (forecast > 0) "$actual $unit (${percentAchievement} %)" else "$actual $unit"
        textView.text = text
        if (percentAchievement > 0 && actual > 0) {
            textView.setTextColor(if (percentAchievement >= 100) Color.GREEN else Color.RED)
        } else {
            when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    textView.setTextColor(Color.WHITE)
                }

                else -> {
                    textView.setTextColor(Color.BLACK)
                }
            }
        }
    }

    companion object {
        const val STEP_SIZE_ACTIVITY: Int = 1
        const val STEP_SIZE_KM: Int = 10
        const val STEP_SIZE_HM: Int = 250
        const val TAG: String = "ForecastFragment"
    }
}