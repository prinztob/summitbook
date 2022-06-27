package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.mikephil.charting.charts.LineChart

class CustomMarkerLineChart : LineChart {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = true
        if (isShowingMarker() && this.marker is CustomMarkerView) {
            val markerView: CustomMarkerView = this.marker as CustomMarkerView
            val rect = Rect(markerView.drawingPosX.toInt(), markerView.drawingPosY.toInt(),
                    markerView.drawingPosX.toInt() + markerView.width, markerView.drawingPosY.toInt() + markerView.height)
            if (event.action == MotionEvent.ACTION_DOWN && rect.contains(event.x.toInt(), event.y.toInt())) {
                markerView.startIntent()
                markerView.dispatchTouchEvent(event)
            } else {
                handled = super.onTouchEvent(event)
            }
        } else if (isShowingMarker() && this.marker is CustomMarkerViewSolarIntensity) {
            val markerView: CustomMarkerViewSolarIntensity = this.marker as CustomMarkerViewSolarIntensity
            val rect = Rect(markerView.drawingPosX.toInt(), markerView.drawingPosY.toInt(),
                    markerView.drawingPosX.toInt() + markerView.width, markerView.drawingPosY.toInt() + markerView.height)
            if (event.action == MotionEvent.ACTION_DOWN && rect.contains(event.x.toInt(), event.y.toInt())) {
                markerView.startIntent()
                markerView.dispatchTouchEvent(event)
            } else {
                handled = super.onTouchEvent(event)
            }
        } else {
            handled = super.onTouchEvent(event)
        }
        return handled
    }

    private fun isShowingMarker(): Boolean {
        return mMarker != null && isDrawMarkersEnabled && valuesToHighlight()
    }

}