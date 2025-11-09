package de.drtobiasprinz.summitbook.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.core.content.edit
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.ui.MainActivity.Companion.sharedPreferences
import org.osmdroid.views.MapView

class CustomMapViewToAllowScrolling : MapView {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    var updateBoundingBox = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN ->                 // Disallow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP ->                 // Allow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(false)
        }
        if (updateBoundingBox) {
            cleanupAndSaveCurrentStatus()
        }
        return super.onTouchEvent(ev)
    }

    private fun cleanupAndSaveCurrentStatus() {
        Log.d(TAG, "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}")
        val osMapBoundingBox =
            sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")?.split(";")
                ?: emptyList()
        val showSummits = if (osMapBoundingBox.size == 6) osMapBoundingBox[4] else "0"
        val showBookmarks = if (osMapBoundingBox.size == 6) osMapBoundingBox[5] else "0"
        sharedPreferences.edit {
            putString(
                Keys.PREF_OS_MAP_BOUNDING_BOX,
                "${boundingBox.latNorth};${boundingBox.lonEast};${boundingBox.latSouth};${boundingBox.lonWest};$showSummits;$showBookmarks"
            )
        }
        Log.d(TAG, "Content: ${sharedPreferences.getString(Keys.PREF_OS_MAP_BOUNDING_BOX, "")}")
    }

    companion object {
        const val TAG = "CustomMapViewToAllowScrolling"
    }
}