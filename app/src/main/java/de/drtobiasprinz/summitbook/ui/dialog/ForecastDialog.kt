package de.drtobiasprinz.summitbook.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.databinding.DialogForecastBinding
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.Forecast.Companion.getSumForYear
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import java.util.*
import kotlin.math.round

@AndroidEntryPoint
class ForecastDialog : DialogFragment() {
    private lateinit var database: AppDatabase

    private lateinit var binding: DialogForecastBinding
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var currentContext: Context
    private lateinit var forecasts: ArrayList<Forecast>
    private lateinit var summits: List<Summit>
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
        currentContext = requireContext()
        binding = DialogForecastBinding.inflate(layoutInflater, container, false)
        database = DatabaseModule.provideDatabase(currentContext)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(currentContext)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        indoorHeightMeterPercent = sharedPreferences.getInt("indoor_height_meter_per_cent", 0)
        annualTargetActivity = sharedPreferences.getString("annual_target_activities", "52") ?: "52"
        annualTargetKm = sharedPreferences.getString("annual_target_km", "1200") ?: "1200"
        annualTargetHm = sharedPreferences.getString("annual_target", "50000") ?: "50000"

        forecasts = database.forecastDao()?.allForecasts as ArrayList<Forecast>
        val range: Date = Summit.parseDate("${currentYear}-01-01")

        summits = database.summitsDao().getAllSummitForYear(range.time) as List<Summit>
        setMissingForecasts(yearsWithForecasts, database)
        setAchievement()
        setForecastsInDialog(yearsWithForecasts[selectedSegmentedYear], view)
        setOverview(yearsWithForecasts[selectedSegmentedYear])

        binding.back.setOnClickListener {
            dialog?.dismiss()
        }
        binding.save.setOnClickListener {
            forecasts.forEach {
                database.forecastDao()?.updateForecast(it)
            }
            dialog?.dismiss()
        }
        binding.groupForecastProperty.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedSegmentedForecastProperty = checkedId
                setOverview(yearsWithForecasts[selectedSegmentedYear])
                setForecastsInDialog(yearsWithForecasts[selectedSegmentedYear], view)
            }
        }
        binding.groupYear.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedSegmentedYear = if (checkedId == binding.buttonCurrentYear.id) 0 else 1
                setOverview(yearsWithForecasts[selectedSegmentedYear])
                setForecastsInDialog(yearsWithForecasts[selectedSegmentedYear], view)
            }
        }
    }

    private fun setMissingForecasts(dates: List<Int>, database: AppDatabase) {
        for (date in dates) {
            for (i in 1..12) {
                var forecast = forecasts.firstOrNull { it.month == i && it.year == date }
                if (forecast == null) {
                    forecast = Forecast(date, i, 0, 0, 0)
                    val id = database.forecastDao()?.addForecast(forecast)
                    if (id != null) {
                        forecast.id = id
                    }
                    forecasts.add(forecast)
                }
            }
        }
    }

    private fun setAchievement() {
        forecasts.forEach {
            if (it.year == currentYear && it.month <= currentMonth) {
                it.setActual(summits, indoorHeightMeterPercent)
            }
        }
    }

    private fun setOverview(year: Int) {
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
                binding.overview.text = currentContext.getString(
                    R.string.forecast_info_km,
                    year.toString(),
                    sum.toString(),
                    annualTargetKm
                )
                annualTarget = annualTargetKm.toInt()
            }
            binding.buttonActivity.id -> {
                binding.overview.text = currentContext.getString(
                    R.string.forecast_info_activities,
                    year.toString(),
                    sum.toString(),
                    annualTargetActivity
                )
                annualTarget = annualTargetActivity.toInt()
            }
            else -> {
                binding.overview.text = currentContext.getString(
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
    private fun setForecastsInDialog(year: Int, view: View) {
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
                            setOverview(year)
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