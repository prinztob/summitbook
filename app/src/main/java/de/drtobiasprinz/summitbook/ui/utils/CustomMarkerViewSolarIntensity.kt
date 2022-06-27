package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.SummitEntryDetailsActivity
import de.drtobiasprinz.summitbook.models.SolarIntensity
import de.drtobiasprinz.summitbook.models.Summit

class CustomMarkerViewSolarIntensity(context: Context?, layoutResource: Int) : MarkerView(context, layoutResource) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    var drawingPosX = 0f
    var drawingPosY = 0f

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        try {
            val entry = e?.data as SolarIntensity?
            if (entry != null) {
                tvContent?.text = String.format("%s\n%.2f %s\n%.2f %s", entry.markerText, entry.solarUtilizationInHours, "h", entry.solarExposureInHours, "h")
            } else {
                tvContent?.text = ""
            }
        } catch (ex: Exception) {
            tvContent?.text = ""
            ex.printStackTrace()
        }
    }

    private val uiScreenWidth = resources.displayMetrics.widthPixels


    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        var newPosX = posX
        if ((uiScreenWidth - posX) < width / 2f) {
            newPosX = uiScreenWidth - width / 2f - 25f
        }
        if (tvContent?.text != "") {
            super.draw(canvas, newPosX, posY)
        }
        val offset = getOffsetForDrawingAtPoint(posX, posY)
        drawingPosX = posX + offset.x
        drawingPosY = posY + offset.y
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), (-height).toFloat())
    }

}