package de.drtobiasprinz.summitbook.ui.dialog

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.DialogForecastBinding
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Forecast.Companion.getSumForYear
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.SortFilterValues.Companion.getYear
import de.drtobiasprinz.summitbook.utils.DataStatus
import de.drtobiasprinz.summitbook.viewmodel.DatabaseViewModel
import java.util.*
import kotlin.math.round

@AndroidEntryPoint
class ForecastDialog : DialogFragment() {
    private lateinit var binding: DialogForecastBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val viewModel: DatabaseViewModel by viewModels()

    private var selectedSegmentedYear: Int = 0
    private var selectedSegmentedForecastProperty: Int = 0
    private var currentYear: Int = (Calendar.getInstance())[Calendar.YEAR]
    private var currentMonth: Int = (Calendar.getInstance())[Calendar.MONTH] + 1
    private var yearsWithForecasts = listOf(currentYear, currentYear + 1)
    private var annualTargetActivity: String = ""
    private var annualTargetKm: String = ""
    private var annualTargetHm: String = ""
    private var indoorHeightMeterPercent = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogForecastBinding.inflate(layoutInflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        annualTargetActivity = sharedPreferences.getString("annual_target_activities", "52") ?: "52"
        annualTargetKm = sharedPreferences.getString("annual_target_km", "1200") ?: "1200"
        annualTargetHm = sharedPreferences.getString("annual_target", "50000") ?: "50000"
        val range: Date = Summit.parseDate("${currentYear}-01-01")
        viewModel.summitsList.observe(viewLifecycleOwner,
            object : androidx.lifecycle.Observer<DataStatus<List<Summit>>> {
                override fun onChanged(summitsListDataStatus: DataStatus<List<Summit>>?) {
                    summitsListDataStatus?.data.let { summits ->
                        val summitsForSelectedYear =
                            summits?.filter { summit -> getYear(summit.date) == getYear(range) }
                        viewModel.forecastList.observe(viewLifecycleOwner) { itDataStatusForecasts ->
                            itDataStatusForecasts.data.let { forecasts ->
                                val forecastsOriginal = forecasts?.map { it.copy() } ?: emptyList()
                                setMissingForecasts(yearsWithForecasts, forecasts)

                                if (forecasts != null) {
                                    if (summitsForSelectedYear != null) {
                                        setAchievement(forecasts, summitsForSelectedYear)
                                    }
                                    setForecastsInDialog(
                                        forecasts,
                                        yearsWithForecasts[selectedSegmentedYear],
                                        view
                                    )
                                    setOverview(
                                        forecasts,
                                        yearsWithForecasts[selectedSegmentedYear]
                                    )
                                }

                                binding.back.setOnClickListener {
                                    dialog?.dismiss()
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
                                            true,
                                            forecastsWithChanges
                                        )
                                        job.invokeOnCompletion {
                                            binding.loadingPanel.visibility = View.GONE
                                            dialog?.dismiss()
                                        }
                                    }
                                }
                                binding.groupForecastProperty.addOnButtonCheckedListener { _, checkedId, isChecked ->
                                    if (isChecked && forecasts != null) {
                                        selectedSegmentedForecastProperty = checkedId
                                        setOverview(
                                            forecasts,
                                            yearsWithForecasts[selectedSegmentedYear]
                                        )
                                        setForecastsInDialog(
                                            forecasts,
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
                                            forecasts,
                                            yearsWithForecasts[selectedSegmentedYear]
                                        )
                                        setForecastsInDialog(
                                            forecasts,
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

    private fun setMissingForecasts(dates: List<Int>, forecasts: List<Forecast>?) {
        for (date in dates) {
            for (i in 1..12) {
                var forecast = forecasts?.firstOrNull { it.month == i && it.year == date }
                if (forecast == null) {
                    forecast = Forecast(date, i, 0, 0, 0)
                    viewModel.saveForecast(false, forecast)
                }
            }
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
        val sum = getSumForYear(
            year,
            forecasts,
            selectedSegmentedForecastProperty,
            currentYear,
            currentMonth
        )
        val annualTarget: Int
        when (selectedSegmentedForecastProperty) {
            binding.buttonKilometers.id -> {
                binding.overview.text = requireContext().getString(
                    R.string.forecast_info_km,
                    year.toString(),
                    sum.toString(),
                    annualTargetKm
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
                    R.string.forecast_info_hm,
                    year.toString(),
                    sum.toString(),
                    annualTargetHm
                )
                annualTarget = annualTargetHm.toInt()
            }
        }
        if (sum > annualTarget) binding.overview.setTextColor(Color.GREEN) else binding.overview.setTextColor(
            Color.RED
        )
    }

    @SuppressLint("DiscouragedApi")
    private fun setForecastsInDialog(forecasts: List<Forecast>, year: Int, view: View) {
        forecasts.forEach {
            if (it.year == year) {
                val resourceIdSlider = resources.getIdentifier(
                    "range_slider_forecast_month${it.month}",
                    "id",
                    requireContext().packageName
                )
                if (resourceIdSlider > 0) {
                    val slider: Slider = view.findViewById(resourceIdSlider)
                    slider.valueFrom = 0F
                    when (selectedSegmentedForecastProperty) {
                        binding.buttonKilometers.id -> {
                            slider.value = it.forecastDistance.toFloat()
                            slider.valueTo = 500F
                            slider.stepSize = 10F
                        }
                        binding.buttonActivity.id -> {
                            slider.value = it.forecastNumberActivities.toFloat()
                            slider.valueTo = 20F
                            slider.stepSize = 1F
                        }
                        else -> {
                            slider.value = it.forecastHeightMeter.toFloat()
                            slider.valueTo = 15000F
                            slider.stepSize = 250F
                        }
                    }
                    if (it.year == currentYear && it.month < currentMonth) {
                        slider.isEnabled = false
                        setAchievementText(it, view, slider)
                    } else {
                        slider.isEnabled = true
                        setForecastText(it, view, slider)
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

    @SuppressLint("DiscouragedApi")
    private fun setForecastText(forecast: Forecast, view: View, slider: Slider) {
        val resourceIdText = resources.getIdentifier(
            "fulfilled_forecast_month${forecast.month}",
            "id",
            requireContext().packageName
        )
        val textView: TextView = view.findViewById(resourceIdText)
        textView.setTextColor(Color.BLACK)
        if (forecast.year == currentYear && forecast.month <= currentMonth) {
            textView.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_baseline_edit_12,
                0,
                0,
                0
            )
            textView.setOnClickListener {
                slider.isEnabled = !slider.isEnabled
            }
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        when (selectedSegmentedForecastProperty) {
            binding.buttonKilometers.id -> textView.text =
                String.format(
                    resources.getString(R.string.value_with_km),
                    forecast.forecastDistance
                )
            binding.buttonActivity.id -> textView.text =
                forecast.forecastNumberActivities.toString()
            else -> textView.text = String.format(
                resources.getString(R.string.value_with_hm),
                forecast.forecastHeightMeter
            )
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun setAchievementText(forecast: Forecast, view: View, slider: Slider) {
        val resourceIdText = resources.getIdentifier(
            "fulfilled_forecast_month${forecast.month}",
            "id",
            requireContext().packageName
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
                forecast.actualNumberActivities,
                forecast.forecastNumberActivities,
                "",
                textView
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
            textView.setTextColor(Color.BLACK)
        }
    }
}