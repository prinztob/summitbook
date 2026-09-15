package de.drtobiasprinz.summitbook.widget

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import de.drtobiasprinz.summitbook.core.widget.WidgetUpdater
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdaterImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetUpdater {

    override suspend fun updateWidget() {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(SummitBookGlanceWidget::class.java)
            glanceIds.forEach { glanceId ->
                // Update the "now" preference to force recomposition
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[longPreferencesKey("now")] = System.currentTimeMillis()
                }
                Log.i(TAG, "update SummitBookGlanceWidget with id $glanceId")
            }
            SummitBookGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget", e)
        }
    }

    companion object {
        private const val TAG = "WidgetUpdaterImpl"
    }
}
