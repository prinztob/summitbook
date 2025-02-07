package de.drtobiasprinz.summitbook

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import androidx.room.Room
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.ui.MainActivity
import de.drtobiasprinz.summitbook.utils.Constants.DATABASE
import java.text.NumberFormat
import java.util.Calendar
import kotlin.math.roundToInt


class SummitBookWidgetProvider : AppWidgetProvider() {

    private lateinit var repository: DatabaseRepository
    private lateinit var dao: AppDatabase


    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        dao = Room.databaseBuilder(
            context, AppDatabase::class.java, DATABASE
        ).build()
        repository = DatabaseRepository(dao.summitsDao(), dao.segmentsDao(), dao.forecastDao(), dao.ignoredActivityDao())
        repository.getAllSummitsLiveData().observeForever { summits ->
            updateAllWidgets(summits, context, appWidgetManager, repository.getAllForecastsLiveData(), appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context.applicationContext)
        val thisWidget = ComponentName(
            context.applicationContext,
            SummitBookWidgetProvider::class.java
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        if (intent?.action != null && appWidgetIds != null && appWidgetIds.isNotEmpty()) {
            onUpdate(context, appWidgetManager, appWidgetIds)
            Log.i(TAG, "onReceive - call on update")
        } else {
            Log.i(TAG, "onReceive - nothing to do.")
        }
    }

    private fun updateAllWidgets(
        summits: MutableList<Summit>,
        context: Context,
        appWidgetManager: AppWidgetManager,
        forecastsLiveData: LiveData<MutableList<Forecast>>,
        allWidgetIds: IntArray
    ) {
        for (widgetId in allWidgetIds) {
            Log.i(TAG, "Update all widgets:  id -> $widgetId")
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)

            forecastsLiveData.observeForever { forecasts ->
                if (forecasts != null && forecasts.any { it.year == (Calendar.getInstance())[Calendar.YEAR] }) {
                    setRemoteViewsFromForecast(
                        forecasts,
                        summits,
                        remoteViews,
                        context
                    )
                } else {
                    setTextFromStatisticEntry(summits, remoteViews, context)
                }
                val configIntent = Intent(context, MainActivity::class.java)
                val pIntent = PendingIntent.getActivity(
                    context,
                    0,
                    configIntent,
                    FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                remoteViews.setOnClickPendingIntent(R.id.widget, pIntent)

                appWidgetManager.updateAppWidget(widgetId, remoteViews)
            }
        }
    }

    private fun setTextView(
        remoteViews: RemoteViews,
        context: Context,
        id: Int,
        actualValue: Int,
        expectedValue: Int,
        unit: Int? = null,
        imageResource: Int? = null,
        drawableGreen: Int? = null,
        drawableRed: Int? = null
    ) {
        val numberFormat = NumberFormat.getInstance(context.resources.configuration.locales[0])
        numberFormat.maximumFractionDigits = 0

        remoteViews.setViewVisibility(id, View.VISIBLE)
        val unitString = if (unit != null) context.getString(unit) else ""
        remoteViews.setTextViewText(
            id,
            "${numberFormat.format(actualValue)} " +
                    "${context.getString(R.string.of)} " +
                    "${numberFormat.format(expectedValue)} " +
                    unitString
        )
        if (expectedValue > actualValue) {
            remoteViews.setTextColor(id, Color.rgb(226, 223, 210))
        }
        if (imageResource != null && drawableGreen != null && drawableRed != null) {

            remoteViews.setViewVisibility(imageResource, View.VISIBLE)
            remoteViews.setImageViewResource(
                imageResource,
                if (actualValue >= expectedValue) {
                    drawableGreen
                } else {
                    drawableRed
                }
            )
        }
    }

    private fun setRemoteViewsFromForecast(
        forecasts: List<Forecast>,
        summits: List<Summit>,
        remoteViews: RemoteViews,
        context: Context
    ) {
        val calendar = Calendar.getInstance()
        val currentYear: Int = (calendar)[Calendar.YEAR]
        val currentMonth: Int = (calendar)[Calendar.MONTH] + 1
        val currentDay: Int = (calendar)[Calendar.DAY_OF_MONTH]
        val dayOfMonthPercentage =
            currentDay.toDouble() / calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val forecastForCurrentMonth = forecasts.firstOrNull {
            it.month == currentMonth && it.year == currentYear
        }
        val indoorHeightMeterPercent =
            PreferenceManager.getDefaultSharedPreferences(context)
                ?.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0) ?: 0
        val summitsForCurrentMonth = summits.filter {
            val calForSummit = Calendar.getInstance()
            calForSummit.time = it.date
            (calendar)[Calendar.MONTH] == (calForSummit)[Calendar.MONTH] && (calendar)[Calendar.YEAR] == (calForSummit)[Calendar.YEAR]

        }
        val forecastForCurrentYear = forecasts.filter {
            it.month < currentMonth && it.year == currentYear
        }
        val summitsForCurrentYear = summits.filter {
            val calForSummit = Calendar.getInstance()
            calForSummit.time = it.date
            (calendar)[Calendar.YEAR] == (calForSummit)[Calendar.YEAR]
        }
        remoteViews.setViewVisibility(R.id.header_monthly, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.header_image_monthly, View.VISIBLE)
        val distanceForecastForCurrentMonth = (forecastForCurrentMonth?.forecastNumberActivities
            ?: 0) * dayOfMonthPercentage
        val heightMeterForecastForCurrentMonth = (forecastForCurrentMonth?.forecastHeightMeter
            ?: 0) * dayOfMonthPercentage
        val kilometerForecastForCurrentMonth = (forecastForCurrentMonth?.forecastDistance
            ?: 0) * dayOfMonthPercentage
        setTextView(
            remoteViews,
            context,
            R.id.activityMonthly,
            summitsForCurrentMonth.count(),
            distanceForecastForCurrentMonth.roundToInt(),
            null,
            R.id.activity_image_monthly,
            R.drawable.ic_baseline_directions_run_24_green,
            R.drawable.ic_baseline_directions_run_24_red
        )
        setTextView(
            remoteViews,
            context,
            R.id.activityYearly,
            summitsForCurrentYear.count(),
            (forecastForCurrentYear.sumOf { it.forecastNumberActivities } +
                    distanceForecastForCurrentMonth).roundToInt(),
            null,
            R.id.activity_image,
            R.drawable.ic_baseline_directions_run_24_green,
            R.drawable.ic_baseline_directions_run_24_red
        )
        setTextView(
            remoteViews,
            context,
            R.id.heightMeterMonthly,
            summitsForCurrentMonth.sumOf {
                if (it.sportType == SportType.IndoorTrainer) {
                    it.elevationData.elevationGain * indoorHeightMeterPercent / 100
                } else {
                    it.elevationData.elevationGain
                }
            },
            heightMeterForecastForCurrentMonth.roundToInt(),
            R.string.hm,
            R.id.height_meter_image_monthly,
            R.drawable.ic_baseline_trending_up_24_green,
            R.drawable.ic_baseline_trending_up_24_red
        )
        setTextView(
            remoteViews,
            context,
            R.id.heightMeterYealy,
            summitsForCurrentYear.sumOf {
                if (it.sportType == SportType.IndoorTrainer) {
                    it.elevationData.elevationGain * indoorHeightMeterPercent / 100
                } else {
                    it.elevationData.elevationGain
                }
            },
            (forecastForCurrentYear.sumOf { it.forecastHeightMeter } +
                    heightMeterForecastForCurrentMonth).roundToInt(),
            R.string.hm,
            R.id.height_meter_image,
            R.drawable.ic_baseline_trending_up_24_green,
            R.drawable.ic_baseline_trending_up_24_red
        )
        setTextView(
            remoteViews,
            context,
            R.id.kilometersMonthly,
            (summitsForCurrentMonth.sumOf { it.kilometers }).roundToInt(),
            kilometerForecastForCurrentMonth.roundToInt(),
            R.string.km,
            R.id.kilometers_image_monthly,
            R.drawable.ic_baseline_compare_arrows_24_green,
            R.drawable.ic_baseline_compare_arrows_24_red
        )
        setTextView(
            remoteViews,
            context,
            R.id.kilometersYearly,
            (summitsForCurrentYear.sumOf { it.kilometers }).roundToInt(),
            (forecastForCurrentYear.sumOf { it.forecastDistance } +
                    kilometerForecastForCurrentMonth).roundToInt(),
            R.string.km,
            R.id.kilometers_image,
            R.drawable.ic_baseline_compare_arrows_24_green,
            R.drawable.ic_baseline_compare_arrows_24_red
        )
    }

    private fun setTextFromStatisticEntry(
        entries: List<Summit>,
        remoteViews: RemoteViews,
        context: Context
    ) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val annualTargetActivity =
            sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, "52")?.toInt() ?: 52
        val annualTargetKm =
            sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200")?.toInt() ?: 1200
        val annualTargetHm =
            sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET, "50000")?.toInt() ?: 50000
        val indoorHeightMeterPercent =
            sharedPreferences?.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0) ?: 0

        val statisticEntry = StatisticEntry(
            filterByDate(entries),
            annualTargetActivity,
            annualTargetKm,
            annualTargetHm,
            indoorHeightMeterPercent
        )
        statisticEntry.calculate()
        statisticEntry.setExpectedAchievement()
        remoteViews.setTextViewText(
            R.id.activityYearly,
            "${statisticEntry.getTotalActivities()} ${context.getString(R.string.of)} ${statisticEntry.expectedAchievementActivityAbsolute.roundToInt()}"
        )
        remoteViews.setTextViewText(
            R.id.heightMeterYealy,
            "${statisticEntry.totalHm} ${context.getString(R.string.of)} ${statisticEntry.expectedAchievementHmAbsolute.roundToInt()} hm"
        )
        remoteViews.setTextViewText(
            R.id.kilometersYearly,
            "${statisticEntry.totalKm.roundToInt()} ${context.getString(R.string.of)} ${statisticEntry.expectedAchievementKmAbsolute.roundToInt()} km"
        )

        remoteViews.setImageViewResource(
            R.id.activity_image,
            if (statisticEntry.getTotalActivities() >= statisticEntry.expectedAchievementActivityAbsolute) R.drawable.ic_baseline_directions_run_24_green else R.drawable.ic_baseline_directions_run_24_red
        )
        remoteViews.setImageViewResource(
            R.id.height_meter_image,
            if (statisticEntry.totalHm >= statisticEntry.expectedAchievementHmAbsolute) R.drawable.ic_baseline_trending_up_24_green else R.drawable.ic_baseline_trending_up_24_red
        )
        remoteViews.setImageViewResource(
            R.id.kilometers_image,
            if (statisticEntry.totalKm >= statisticEntry.expectedAchievementKmAbsolute) R.drawable.ic_baseline_compare_arrows_24_green else R.drawable.ic_baseline_compare_arrows_24_red
        )
    }

    private fun filterByDate(allEntries: List<Summit>): List<Summit> {
        val now = Calendar.getInstance()
        val year = now[Calendar.YEAR]
        val startDate = Summit.parseDate(String.format("%s-12-31", (year - 1).toString()))
        val endDate = Summit.parseDate(String.format("%s-12-31", year.toString()))
        val entries = ArrayList<Summit>()
        for (entry in allEntries) {
            if (entry.date.after(startDate) && entry.date.before(endDate)) {
                entries.add(entry)
            }
        }
        return entries
    }

    companion object {
        private const val TAG = "WIDGET"
    }

}