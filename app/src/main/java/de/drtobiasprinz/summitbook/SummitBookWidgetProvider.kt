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
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.di.DatabaseModule
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.ui.MainActivity
import java.util.*
import kotlin.math.roundToInt


class SummitBookWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        val database = DatabaseModule.provideDatabase(context)
        val entries = database.summitsDao().allSummit

        if (entries != null) {
            val forecasts = database.forecastDao().allForecastsDeprecated

            val thisWidget = ComponentName(context, SummitBookWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
                if (forecasts != null && forecasts.any { it.year == (Calendar.getInstance())[Calendar.YEAR] }) {
                    Log.i("Widget", "1")
                    setRemoteViewsFromForecast(
                        forecasts,
                        entries,
                        remoteViews,
                        context
                    )
                } else {
                    Log.i("Widget", "2")
                    setTextFromStatisticEntry(entries, remoteViews, context)
                }
                val configIntent = Intent(context, MainActivity::class.java)
                val pIntent = PendingIntent.getActivity(
                    context, 0, configIntent, FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        remoteViews.setViewVisibility(id, View.VISIBLE)
        val unitString = if (unit != null) context.getString(unit) else ""
        remoteViews.setTextViewText(
            id,
            "$actualValue ${context.getString(R.string.of)} $expectedValue $unitString"
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
                ?.getInt("indoor_height_meter_per_cent", 0) ?: 0
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
        setTextView(
            remoteViews,
            context,
            R.id.activityMonthly,
            summitsForCurrentMonth.count(),
            ((forecastForCurrentMonth?.forecastNumberActivities
                ?: 0) * dayOfMonthPercentage).roundToInt(),
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
                    (forecastForCurrentMonth?.forecastNumberActivities
                        ?: 0) * dayOfMonthPercentage).roundToInt(),
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
            ((forecastForCurrentMonth?.forecastHeightMeter
                ?: 0) * dayOfMonthPercentage).roundToInt(),
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
                    (forecastForCurrentMonth?.forecastHeightMeter ?: 0) * dayOfMonthPercentage)
                .roundToInt(),
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
            ((forecastForCurrentMonth?.forecastDistance ?: 0) * dayOfMonthPercentage).roundToInt(),
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
                    (forecastForCurrentMonth?.forecastDistance ?: 0) * dayOfMonthPercentage)
                .roundToInt(),
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
            sharedPreferences.getString("annual_target_activities", "52")?.toInt() ?: 52
        val annualTargetKm =
            sharedPreferences.getString("annual_target_km", "1200")?.toInt() ?: 1200
        val annualTargetHm = sharedPreferences.getString("annual_target", "50000")?.toInt() ?: 50000
        val indoorHeightMeterPercent =
            sharedPreferences?.getInt("indoor_height_meter_per_cent", 0) ?: 0

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

}