package de.drtobiasprinz.summitbook.ui.utils

import android.graphics.Canvas
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import java.util.*

class BarChartCustomRenderer(chart: BarDataProvider?, animator: ChartAnimator?, viewPortHandler: ViewPortHandler?) : BarChartRenderer(chart, animator, viewPortHandler) {
    override fun drawValue(c: Canvas, valueText: String, x: Float, y: Float, color: Int) {
        if (valueText != "0") {
            mValuePaint.color = color
            c.save()
            c.rotate(90f, x-10, y)
            c.drawText(String.format(Locale.ENGLISH, "%s", valueText.split("[.,]".toRegex()).toTypedArray()[0]), x, y, mValuePaint)
            c.restore()
        }
    }
}