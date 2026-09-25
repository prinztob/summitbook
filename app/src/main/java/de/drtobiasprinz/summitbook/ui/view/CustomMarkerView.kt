package de.drtobiasprinz.summitbook.ui.view

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.ui.activities.SummitEntryDetailsComposeActivity
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.filters.OrderBySpinnerEntry
import de.drtobiasprinz.summitbook.core.Constants.SUMMIT_ID_EXTRA_IDENTIFIER
import java.util.Locale

@SuppressLint("ViewConstructor")
class CustomMarkerView(
    context: Context?,
    layoutResource: Int,
    private val lineChartSpinnerEntry: OrderBySpinnerEntry
) : MarkerView(context, layoutResource) {

    private val tvContent: TextView? = findViewById(R.id.tvContent)
    private var summit: Summit? = null

    var drawingPosX = 0f
    var drawingPosY = 0f

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        val newSummit = e?.data as? Summit
        if (newSummit == null) {
            summit = null
            tvContent?.text = ""
            return
        }
        summit = newSummit
        val format =
            if (lineChartSpinnerEntry == OrderBySpinnerEntry.Vo2Max) "%s\n%s\n%.1f %s" else "%s\n%s\n%.0f %s"
        tvContent?.text = String.format(
            Locale.getDefault(),
            format,
            newSummit.name,
            newSummit.getDateAsString(),
            lineChartSpinnerEntry.f(newSummit),
            context.getString(lineChartSpinnerEntry.unit)
        )
    }

    private val uiScreenWidth = resources.displayMetrics.widthPixels

    fun startIntent() {
        val currentSummit = summit ?: return
        try {
            val intent = Intent(context, SummitEntryDetailsComposeActivity::class.java)
            intent.putExtra(SUMMIT_ID_EXTRA_IDENTIFIER, currentSummit.id)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // DO NOTHING
        }
    }

    override fun draw(canvas: Canvas?, posX: Float, posY: Float) {
        var newPosX = posX
        if ((uiScreenWidth - posX) < width / 2f) {
            newPosX = uiScreenWidth - width / 2f - EDGE_OFFSET
        }
        if (tvContent?.text?.isNotEmpty() == true) {
            super.draw(canvas, newPosX, posY)
            val offset = getOffsetForDrawingAtPoint(newPosX, posY)
            drawingPosX = newPosX + offset.x
            drawingPosY = posY + offset.y
        }
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), (-height).toFloat())
    }

    companion object {
        /** Extra margin kept between the marker and the screen edge. */
        private const val EDGE_OFFSET = 25f
    }
}
