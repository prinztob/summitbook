package de.drtobiasprinz.summitbook.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class SummitBookWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SummitBookGlanceWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.i("SummitBookWidgetReceiver", "onUpdate called for ${appWidgetIds.size} widgets")
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.i("SummitBookWidgetReceiver", "onEnabled - widget added for first time")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("SummitBookWidgetReceiver", "onReceive: ${intent.action}")
    }
}
