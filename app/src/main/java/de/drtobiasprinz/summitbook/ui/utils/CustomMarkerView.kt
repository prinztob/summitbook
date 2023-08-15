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
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.models.OrderBySpinnerEntry

class CustomMarkerView(
    context: Context?,
    layoutResource: Int,
    private val lineChartSpinnerEntry: OrderBySpinnerEntry
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)
    private lateinit var summit: Summit

    var drawingPosX = 0f
    var drawingPosY = 0f

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        try {
            summit = e?.data as Summit
            val format =
                if (lineChartSpinnerEntry == OrderBySpinnerEntry.Vo2Max) "%s\n%s\n%.1f %s" else "%s\n%s\n%.0f %s"
            tvContent?.text = String.format(
                format,
                summit.name,
                summit.getDateAsString(),
                lineChartSpinnerEntry.f(summit),
                context.getString(lineChartSpinnerEntry.unit)
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private val uiScreenWidth = resources.displayMetrics.widthPixels

    fun startIntent() {
        try {
            val intent = Intent(context, SummitEntryDetailsActivity::class.java)
            intent.putExtra(Summit.SUMMIT_ID_EXTRA_IDENTIFIER, summit.id)
            context.startActivity(intent)
        } catch (e: NullPointerException) {
            // DO NOTHING
        }
    }

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