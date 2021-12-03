package de.drtobiasprinz.summitbook.ui.utils

import android.graphics.Canvas
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.renderer.CombinedChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import java.util.*

class BarChartCustomRenderer(chart: CombinedChart?, animator: ChartAnimator?, viewPortHandler: ViewPortHandler?) : CombinedChartRenderer(chart, animator, viewPortHandler) {
    override fun drawValue(c: Canvas, valueText: String, x: Float, y: Float, color: Int) {
        if (valueText != "0") {
            mValuePaint.color = color
            c.save()
            c.rotate(90f, x - 10, y)
            c.drawText(String.format(Locale.ENGLISH, "%s", valueText.split("[.,]".toRegex()).toTypedArray()[0]), x, y, mValuePaint)
            c.restore()
        }
    }
}