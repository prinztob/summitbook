package de.drtobiasprinz.summitbook.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import org.osmdroid.views.MapView

class CustomMapViewToAllowSrolling : MapView {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN ->                 // Disallow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP ->                 // Allow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(false)
        }

        return super.onTouchEvent(ev)
    }
}