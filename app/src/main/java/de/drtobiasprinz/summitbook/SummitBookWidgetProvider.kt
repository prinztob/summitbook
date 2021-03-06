package de.drtobiasprinz.summitbook

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import de.drtobiasprinz.summitbook.database.AppDatabase
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.models.Summit
import java.util.*
import kotlin.math.roundToInt


class SummitBookWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val database = AppDatabase.getDatabase(context)
        val entries = database.summitDao()?.allSummit
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val annualTargetActivity = sharedPreferences.getString("annual_target_activities", "52")?.toInt()
                ?: 52
        val annualTargetKm = sharedPreferences.getString("annual_target_km", "1200")?.toInt()
                ?: 1200
        val annualTargetHm = sharedPreferences.getString("annual_target", "50000")?.toInt() ?: 50000
        val indoorHeightMeterPercent = sharedPreferences?.getInt("indoor_height_meter_per_cent", 0) ?: 0

        if (entries != null) {
            val statisticEntry = StatisticEntry(filterByDate(entries), annualTargetActivity, annualTargetKm, annualTargetHm, indoorHeightMeterPercent)
            statisticEntry.calculate()
            statisticEntry.setExpectedAchievement()
            val thisWidget = ComponentName(context, SummitBookWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
                remoteViews.setTextViewText(R.id.activity, "${statisticEntry.getTotalSummits()} ${context.getString(R.string.of)} ${statisticEntry.expectedAchievementActivityAbsolute.roundToInt()}")
                remoteViews.setTextViewText(R.id.height_meter, "${statisticEntry.totalHm} ${context.getString(R.string.of)} ${statisticEntry.expectedAchievementHmAbsolute.roundToInt()} hm")
                remoteViews.setTextViewText(R.id.kilometers, "${statisticEntry.totalKm.roundToInt()} ${context.getString(R.string.of)} ${statisticEntry.expectedAchievementKmAbsolute.roundToInt()} km")

                remoteViews.setImageViewResource(R.id.activity_image, if (statisticEntry.getTotalSummits() >= statisticEntry.expectedAchievementActivityAbsolute) R.drawable.ic_baseline_directions_run_24_green else R.drawable.ic_baseline_directions_run_24_red)
                remoteViews.setImageViewResource(R.id.height_meter_image, if (statisticEntry.totalHm >= statisticEntry.expectedAchievementHmAbsolute) R.drawable.ic_baseline_trending_up_24_green else R.drawable.ic_baseline_trending_up_24_red)
                remoteViews.setImageViewResource(R.id.kilometers_image, if (statisticEntry.totalKm >= statisticEntry.expectedAchievementKmAbsolute) R.drawable.ic_baseline_compare_arrows_24_green else R.drawable.ic_baseline_compare_arrows_24_red)
                val configIntent = Intent(context, MainActivity::class.java)
                val pIntent = PendingIntent.getActivity(context, 0, configIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                remoteViews.setOnClickPendingIntent(R.id.widget, pIntent)

                appWidgetManager.updateAppWidget(widgetId, remoteViews)
            }
        }
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
                Log.i("tag", entry.toString())
            }
        }
        return entries
    }

}