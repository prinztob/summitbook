package de.drtobiasprinz.summitbook.ui.dialog

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import co.ceryle.segmentedbutton.SegmentedButtonGroup
import com.google.android.material.slider.Slider
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.Forecast
import de.drtobiasprinz.summitbook.models.Forecast.Companion.getSumForYear
import de.drtobiasprinz.summitbook.models.Summit
import java.util.*
import kotlin.math.round


class ForecastDialog(val indoorHeightMeterPercent: Int) : DialogFragment() {

    private lateinit var currentContext: Context
    private lateinit var forecasts: ArrayList<Forecast>
    private lateinit var summits: List<Summit>
    private lateinit var segmentedYear: SegmentedButtonGroup
    private var selectedSegmentedYear: Int = 0
    private lateinit var segmentedForecastProperty: SegmentedButtonGroup
    private var selectedSegmentedForecastProperty: Int = 0
    private var currentYear: Int = (Calendar.getInstance())[Calendar.YEAR]
    private var currentMonth: Int = (Calendar.getInstance())[Calendar.MONTH] + 1
    private var yearsWithForcasts = listOf(currentYear, currentYear + 1)
    private var annualTargetActivity: String = ""
    private var annualTargetKm: String = ""
    private var annualTargetHm: String = ""

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_forecast, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentContext = requireContext()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        annualTargetActivity = sharedPreferences.getString("annual_target_activities", "52") ?: "52"
        annualTargetKm = sharedPreferences.getString("annual_target_km", "1200") ?: "1200"
        annualTargetHm = sharedPreferences.getString("annual_target", "50000") ?: "50000"

        segmentedYear = view.findViewById(R.id.group_year)
        segmentedForecastProperty = view.findViewById(R.id.group_forecast_property)

        val database = AppDatabase.getDatabase(currentContext)
        forecasts = database.forecastDao()?.allForecasts as ArrayList<Forecast>
        val range: Date = Summit.parseDate("${currentYear}-01-01")

        summits = database.summitDao()?.getAllSummitForYear(range.time) as List<Summit>
        setMissingForecasts(yearsWithForcasts, database)
        setAchievement()
        setForecastsInDialog(yearsWithForcasts[selectedSegmentedYear], view)
        setOverview(yearsWithForcasts[selectedSegmentedYear], view)

        val backButton: Button = view.findViewById(R.id.back)
        backButton.setOnClickListener {
            dialog?.dismiss()
        }
        val saveButton: Button = view.findViewById(R.id.save)
        saveButton.setOnClickListener {
            forecasts.forEach {
                database.forecastDao()?.updateForecast(it)
            }
            dialog?.dismiss()
        }
        segmentedForecastProperty.setOnClickedButtonListener { position: Int ->
            selectedSegmentedForecastProperty = position
            setOverview(yearsWithForcasts[selectedSegmentedYear], view)
            setForecastsInDialog(yearsWithForcasts[selectedSegmentedYear], view)
        }
        segmentedYear.setOnClickedButtonListener { position: Int ->
            selectedSegmentedYear = position
            setOverview(yearsWithForcasts[selectedSegmentedYear], view)
            setForecastsInDialog(yearsWithForcasts[selectedSegmentedYear], view)
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

    private fun setOverview(year: Int, view: View) {
        val sum = getSumForYear(year, forecasts, selectedSegmentedForecastProperty, currentYear, currentMonth)
        val overview: TextView = view.findViewById(R.id.overview)
        val annualTarget: Int
        when (selectedSegmentedForecastProperty) {
            1 -> {
                overview.text = currentContext.getString(R.string.forecast_info_km, year.toString(), sum.toString(), annualTargetKm)
                annualTarget = annualTargetKm.toInt()
            }
            2 -> {
                overview.text = currentContext.getString(R.string.forecast_info_activities, year.toString(), sum.toString(), annualTargetActivity)
                annualTarget = annualTargetActivity.toInt()
            }
            else -> {
                overview.text = currentContext.getString(R.string.forecast_info_hm, year.toString(), sum.toString(), annualTargetHm)
                annualTarget = annualTargetHm.toInt()
            }
        }
        if (sum > annualTarget) overview.setTextColor(Color.GREEN) else overview.setTextColor(Color.RED)
    }

    private fun setForecastsInDialog(year: Int, view: View) {
        forecasts.forEach {
            if (it.year == year) {
                val resourceIdSlider = resources.getIdentifier("range_slider_forecast_month${it.month}", "id", requireContext().packageName)
                if (resourceIdSlider > 0) {
                    val slider: Slider = view.findViewById(resourceIdSlider)
                    slider.valueFrom = 0F
                    when (selectedSegmentedForecastProperty) {
                        1 -> {
                            slider.value = it.forecastDistance.toFloat()
                            slider.valueTo = 500F
                            slider.stepSize = 10F
                        }
                        2 -> {
                            slider.value = it.forecastNumberActivities.toFloat()
                            slider.valueTo = 25F
                            slider.stepSize = 1F
                        }
                        else -> {
                            slider.value = it.forecastHeightMeter.toFloat()
                            slider.valueTo = 25000F
                            slider.stepSize = 500F
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
                        if (fromUser && it.year == yearsWithForcasts[selectedSegmentedYear]) {
                            when (selectedSegmentedForecastProperty) {
                                1 -> it.forecastDistance = value.toInt()
                                2 -> it.forecastNumberActivities = value.toInt()
                                else -> it.forecastHeightMeter = value.toInt()
                            }
                            setOverview(year, view)
                            setForecastText(it, view, clickedSlider)
                        }
                    }
                }
            }
        }
    }

    private fun setForecastText(forecast: Forecast, view: View, slider: Slider) {
        val resourceIdText = resources.getIdentifier("fulfilled_forecast_month${forecast.month}", "id", requireContext().packageName)
        val textView: TextView = view.findViewById<TextView>(resourceIdText)
        textView.setTextColor(Color.BLACK)
        if (forecast.year == currentYear && forecast.month <= currentMonth) {
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_edit_12, 0, 0, 0)
            textView.setOnClickListener {
                slider.isEnabled = !slider.isEnabled
            }
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        when (selectedSegmentedForecastProperty) {
            1 -> textView.text = "${forecast.forecastDistance} ${resources.getString(R.string.km)}"
            2 -> textView.text = forecast.forecastNumberActivities.toString()
            else -> textView.text = "${forecast.forecastHeightMeter} ${resources.getString(R.string.hm)}"
        }
    }

    private fun setAchievementText(forecast: Forecast, view: View, slider: Slider) {
        val resourceIdText = resources.getIdentifier("fulfilled_forecast_month${forecast.month}", "id", requireContext().packageName)
        val textView: TextView = view.findViewById(resourceIdText)
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_edit_12, 0, 0, 0)
        textView.setOnClickListener {
            slider.isEnabled = !slider.isEnabled
        }
        when (selectedSegmentedForecastProperty) {
            1 -> setTextAndColor(forecast.actualDistance, forecast.forecastDistance, resources.getString(R.string.km), textView)
            2 -> setTextAndColor(forecast.actualNumberActivities, forecast.forecastNumberActivities, "", textView)
            else -> setTextAndColor(forecast.actualHeightMeter, forecast.forecastHeightMeter, resources.getString(R.string.hm), textView)
        }
    }

    private fun setTextAndColor(actual: Int, forecast: Int, unit: String, textView: TextView) {
        val percentAchievement = if (forecast > 0) round(actual.toDouble() / forecast.toDouble() * 100.0).toInt() else forecast
        val text = if (forecast > 0) "${actual} ${unit} (${percentAchievement} %)" else "${actual} ${unit}"
        textView.text = text
        if (percentAchievement > 0 && actual > 0) {
            textView.setTextColor(if (percentAchievement >= 100) Color.GREEN else Color.RED)
        } else {
            textView.setTextColor(Color.BLACK)
        }
    }
}