package de.drtobiasprinz.summitbook.ui.view

import android.graphics.Canvas
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.renderer.CombinedChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import java.util.*
import androidx.core.graphics.withRotation

class BarChartCustomRenderer(chart: CombinedChart?, animator: ChartAnimator?, viewPortHandler: ViewPortHandler?) : CombinedChartRenderer(chart, animator, viewPortHandler) {

    private val locale: Locale =
        chart?.context?.resources?.configuration?.locales?.get(0) ?: Locale.ENGLISH

    override fun drawValue(c: Canvas, valueText: String, x: Float, y: Float, color: Int) {
        if (valueText != "0") {
            mValuePaint.color = color
            c.withRotation(90f, x - 10, y) {
                drawText(valueText.split("[.,]".toRegex()).first(), x, y, mValuePaint)
            }
        }
    }
}