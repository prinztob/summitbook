package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.DailyReportData
import de.drtobiasprinz.summitbook.models.LineChartDailyReportYAxisSelector

class CustomMarkerViewDailyReportData(private var context: Context?, layoutResource: Int, private var lineChartDailyReportYAxisSelector: LineChartDailyReportYAxisSelector) :
    MarkerView(context, layoutResource) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)

    private var drawingPosX = 0f
    private var drawingPosY = 0f

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        try {
            val entry = e?.data as DailyReportData?
            if (entry != null) {
                tvContent?.text =
                    context?.let { lineChartDailyReportYAxisSelector.getStringForCustomMarker(entry, it) }
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